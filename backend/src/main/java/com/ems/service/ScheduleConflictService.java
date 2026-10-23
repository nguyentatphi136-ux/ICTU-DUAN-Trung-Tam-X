package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.constant.RoleConstant;
import com.ems.dao.SchoolClassDAO;
import com.ems.dao.ClassroomDAO;
import com.ems.dao.ScheduleDAO;
import com.ems.dao.UserDAO;
import com.ems.dto.ConflictCheckResult;
import com.ems.dto.ConflictDetail;
import com.ems.dto.ConflictType;
import com.ems.dto.RescheduleResponse;
import com.ems.dto.ScheduleRequest;
import com.ems.exception.ForbiddenAccessException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ScheduleConflictException;
import com.ems.exception.ValidationException;
import com.ems.model.ClassSession;
import com.ems.model.Classroom;
import com.ems.model.SchoolClass;
import com.ems.model.User;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class ScheduleConflictService {

    private static final Gson GSON = com.ems.config.GsonProvider.getGson();

    private final ScheduleDAO scheduleDAO;
    private final ClassroomDAO classroomDAO;
    private final SchoolClassDAO classDAO;
    private final UserDAO userDAO;
    private final ConnectionProvider connectionProvider;

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public ScheduleConflictService() {
        this(new ScheduleDAO(), new ClassroomDAO(), new SchoolClassDAO(), new UserDAO(), DBConnection::getConnection);
    }

    public ScheduleConflictService(
            ScheduleDAO scheduleDAO,
            ClassroomDAO classroomDAO,
            SchoolClassDAO classDAO,
            UserDAO userDAO
    ) {
        this(scheduleDAO, classroomDAO, classDAO, userDAO, DBConnection::getConnection);
    }

    public ScheduleConflictService(
            ScheduleDAO scheduleDAO,
            ClassroomDAO classroomDAO,
            SchoolClassDAO classDAO,
            UserDAO userDAO,
            ConnectionProvider connectionProvider
    ) {
        this.scheduleDAO = scheduleDAO;
        this.classroomDAO = classroomDAO;
        this.classDAO = classDAO;
        this.userDAO = userDAO;
        this.connectionProvider = connectionProvider != null ? connectionProvider : DBConnection::getConnection;
    }

    /**
     * Thuật toán kiểm tra xung đột thời khóa biểu:
     * - Trùng giảng viên trong cùng khung giờ giao nhau.
     * - Trùng phòng học trong cùng khung giờ giao nhau.
     * - Điều kiện giao nhau:
     *     existing.start_time < requested.end_time AND existing.end_time > requested.start_time
     * - Hai buổi học nối tiếp nhau (ví dụ: kết thúc 10:00 và bắt đầu 10:00) KHÔNG bị coi là xung đột.
     * - Khi cập nhật, bỏ qua chính buổi học đó (existing.id != requested.scheduleId).
     * - Lịch có trạng thái CANCELLED được loại trừ.
     */
    public ConflictCheckResult detectConflicts(ScheduleRequest request, List<ClassSession> candidates) {
        request.validate();

        LocalDate reqDate = request.getResolvedDate();
        LocalTime reqStart = request.getResolvedStartTime();
        LocalTime reqEnd = request.getResolvedEndTime();
        Long reqInstructorId = request.getInstructorId();
        Integer reqRoomId = request.getResolvedRoomId();
        Long excludeId = request.getScheduleId();

        List<ConflictDetail> conflictList = new ArrayList<>();

        if (candidates != null) {
            for (ClassSession existing : candidates) {
                // Loại trừ chính nó khi cập nhật
                if (excludeId != null && excludeId.equals(existing.getId())) {
                    continue;
                }
                // Loại trừ các buổi đã hủy
                if ("CANCELLED".equalsIgnoreCase(existing.getStatus())) {
                    continue;
                }
                // Khác ngày -> không xung đột
                if (existing.getSessionDate() == null || !existing.getSessionDate().equals(reqDate)) {
                    continue;
                }

                LocalTime exStart = existing.getStartTime();
                LocalTime exEnd = existing.getEndTime();
                if (exStart == null || exEnd == null) continue;

                // Kiểm tra điều kiện giao nhau thời gian:
                // exStart < reqEnd && exEnd > reqStart
                boolean isOverlapping = exStart.isBefore(reqEnd) && exEnd.isAfter(reqStart);
                if (!isOverlapping) {
                    continue;
                }

                // Map thông tin yêu cầu để hiển thị
                Map<String, Object> requestedSchedule = new LinkedHashMap<>();
                requestedSchedule.put("classId", request.getClassId());
                requestedSchedule.put("startDateTime", request.getResolvedStartDateTime() != null
                        ? request.getResolvedStartDateTime().toString() : reqDate + "T" + reqStart);
                requestedSchedule.put("endDateTime", request.getResolvedEndDateTime() != null
                        ? request.getResolvedEndDateTime().toString() : reqDate + "T" + reqEnd);

                // Map thông tin buổi học bị xung đột
                Map<String, Object> conflictingSchedule = new LinkedHashMap<>();
                conflictingSchedule.put("scheduleId", existing.getId());
                conflictingSchedule.put("classCode", existing.getClassCode() != null ? existing.getClassCode() : "Lớp #" + existing.getClassId());
                conflictingSchedule.put("className", existing.getClassName());
                conflictingSchedule.put("instructorId", existing.getInstructorId());
                conflictingSchedule.put("instructorName", existing.getInstructorName() != null ? existing.getInstructorName() : "Giảng viên #" + existing.getInstructorId());
                conflictingSchedule.put("roomId", existing.getClassroomId());
                conflictingSchedule.put("roomCode", existing.getRoomCode() != null ? existing.getRoomCode() : "Phòng #" + existing.getClassroomId());
                conflictingSchedule.put("roomName", existing.getRoomName());
                conflictingSchedule.put("startDateTime", existing.getFormattedStartDateTime());
                conflictingSchedule.put("endDateTime", existing.getFormattedEndDateTime());

                // 1. Kiểm tra trùng giảng viên
                if (reqInstructorId != null && reqInstructorId.equals(existing.getInstructorId())) {
                    conflictList.add(new ConflictDetail(
                            ConflictType.INSTRUCTOR_CONFLICT.getCode(),
                            requestedSchedule,
                            conflictingSchedule,
                            "Giảng viên [" + conflictingSchedule.get("instructorName")
                                    + "] đã có lịch dạy lớp [" + conflictingSchedule.get("classCode")
                                    + "] từ " + exStart + " đến " + exEnd + "."
                    ));
                }

                // 2. Kiểm tra trùng phòng học
                if (reqRoomId != null && reqRoomId.equals(existing.getClassroomId())) {
                    conflictList.add(new ConflictDetail(
                            ConflictType.ROOM_CONFLICT.getCode(),
                            requestedSchedule,
                            conflictingSchedule,
                            "Phòng học [" + conflictingSchedule.get("roomCode")
                                    + "] đã có lớp [" + conflictingSchedule.get("classCode")
                                    + "] sử dụng từ " + exStart + " đến " + exEnd + "."
                    ));
                }
            }
        }

        boolean hasConflict = !conflictList.isEmpty();
        String message = hasConflict
                ? "Phát hiện " + conflictList.size() + " xung đột thời khóa biểu (trùng giảng viên hoặc phòng học)."
                : "Không có xung đột lịch học.";

        return new ConflictCheckResult(hasConflict, conflictList, message);
    }

    /**
     * API 1: Kiểm tra xung đột (Read-only check, không lưu lịch)
     */
    public ConflictCheckResult checkConflicts(ScheduleRequest request) throws SQLException {
        request.validate();
        try (Connection connection = connectionProvider.getConnection()) {
            List<ClassSession> candidates = scheduleDAO.findConflictingSessions(
                    connection,
                    request.getScheduleId(),
                    request.getInstructorId(),
                    request.getResolvedRoomId(),
                    request.getResolvedDate(),
                    request.getResolvedStartTime(),
                    request.getResolvedEndTime()
            );
            return detectConflicts(request, candidates);
        }
    }

    /**
     * API 2: Tạo lịch học mới
     * Quy trình:
     * 1. Validate dữ liệu đầu vào.
     * 2. Kiểm tra lớp học, giảng viên, phòng học tồn tại.
     * 3. Mở transaction và khóa FOR UPDATE tài nguyên để chống race condition.
     * 4. Kiểm tra xung đột.
     * 5. Nếu có xung đột:
     *    - Nếu KHÔNG yêu cầu ghi đè -> Từ chối lưu (HTTP 409).
     *    - Nếu CÓ yêu cầu ghi đè:
     *      + Chỉ Quản lý đào tạo (hoặc Admin) mới có quyền ghi đè (HTTP 403).
     *      + Bắt buộc phải có lý do ghi đè (HTTP 400).
     *      + Lưu lịch và ghi nhận Audit Log chi tiết.
     * 6. Nếu không có xung đột: Lưu lịch và ghi nhận Audit Log.
     */
    public ClassSession createSchedule(
            long actorId,
            List<String> actorRoles,
            ScheduleRequest request
    ) throws SQLException {
        request.validate();

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // 1. Kiểm tra tồn tại thực thể
                verifyEntitiesExist(connection, request);

                // 2. Khóa dòng users & classrooms để ngăn chặn race condition đồng thời
                scheduleDAO.lockInstructorAndRoom(connection, request.getInstructorId(), request.getResolvedRoomId());

                // 3. Kiểm tra xung đột trong transaction
                List<ClassSession> candidates = scheduleDAO.findConflictingSessions(
                        connection,
                        null,
                        request.getInstructorId(),
                        request.getResolvedRoomId(),
                        request.getResolvedDate(),
                        request.getResolvedStartTime(),
                        request.getResolvedEndTime()
                );
                ConflictCheckResult conflictResult = detectConflicts(request, candidates);

                if (conflictResult.isHasConflict()) {
                    handleConflictResolution(actorId, actorRoles, request, conflictResult);
                }

                // 4. Lưu lịch học mới
                ClassSession session = new ClassSession();
                session.setClassId(request.getClassId());
                session.setClassSubjectId(request.getClassSubjectId());
                session.setSubjectLessonId(request.getSubjectLessonId());
                session.setSessionNumber(request.getSessionNumber());
                session.setSessionDate(request.getResolvedDate());
                session.setStartTime(request.getResolvedStartTime());
                session.setEndTime(request.getResolvedEndTime());
                session.setClassroomId(request.getResolvedRoomId());
                session.setInstructorId(request.getInstructorId());
                session.setStatus(request.getStatus() != null ? request.getStatus() : "SCHEDULED");

                long newId = scheduleDAO.createSchedule(connection, session);
                session.setId(newId);

                // 5. Ghi nhận Audit Log
                if (conflictResult.isHasConflict() && request.isOverrideConflict()) {
                    Map<String, Object> auditOld = Map.of(
                            "action", "OVERRIDE_SCHEDULE_CONFLICT",
                            "conflictingCount", conflictResult.getConflicts().size(),
                            "conflicts", conflictResult.getConflicts()
                    );
                    Map<String, Object> auditNew = Map.of(
                            "createdScheduleId", newId,
                            "overrideReason", request.getOverrideReason(),
                            "actorRoles", actorRoles != null ? actorRoles : List.of()
                    );
                    scheduleDAO.recordAuditLog(
                            connection,
                            actorId,
                            "OVERRIDE_SCHEDULE_CONFLICT",
                            "SCHEDULE",
                            String.valueOf(newId),
                            GSON.toJson(auditOld),
                            GSON.toJson(auditNew)
                    );
                } else {
                    scheduleDAO.recordAuditLog(
                            connection,
                            actorId,
                            "CREATE_SCHEDULE",
                            "SCHEDULE",
                            String.valueOf(newId),
                            null,
                            GSON.toJson(request)
                    );
                }

                connection.commit();
                return scheduleDAO.findById(connection, newId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * API 3: Cập nhật lịch học
     */
    /**
     * API 3: Cập nhật hoặc Dời lịch học (User Story S4-03)
     */
    public ClassSession updateSchedule(
            long actorId,
            List<String> actorRoles,
            long scheduleId,
            ScheduleRequest request
    ) throws SQLException {
        RescheduleResponse resp = updateScheduleWithDetails(actorId, actorRoles, scheduleId, request);
        return resp.getSession();
    }

    /**
     * API 3 (Chi tiết): Cập nhật hoặc Dời lịch học có trả về trạng thái gửi thông báo
     */
    public com.ems.dto.RescheduleResponse updateScheduleWithDetails(
            long actorId,
            List<String> actorRoles,
            long scheduleId,
            ScheduleRequest request
    ) throws SQLException {
        if (!isTrainingManagerOrAdmin(actorRoles)) {
            throw new ForbiddenAccessException("Chỉ Quản lý đào tạo hoặc Quản trị viên mới có quyền sửa hoặc dời buổi học.");
        }
        if (request == null) {
            throw new ValidationException("Dữ liệu cập nhật không được để trống.");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ClassSession currentSession = scheduleDAO.findById(connection, scheduleId);
                if (currentSession == null) {
                    throw new ResourceNotFoundException("Không tìm thấy buổi học có ID: " + scheduleId);
                }

                // 1. Kiểm tra trạng thái hiện tại của buổi học
                if ("CANCELLED".equalsIgnoreCase(currentSession.getStatus())) {
                    throw new ValidationException("Không thể sửa hoặc dời buổi học đã bị hủy.");
                }
                if ("COMPLETED".equalsIgnoreCase(currentSession.getStatus())) {
                    throw new ValidationException("Không thể sửa hoặc dời buổi học đã hoàn thành.");
                }

                // 2. Không cho phép sửa buổi học đã được điểm danh
                if (scheduleDAO.hasAttendance(connection, scheduleId)) {
                    throw new ValidationException("Không thể sửa hoặc dời buổi học đã được điểm danh.");
                }

                // Lưu bản sao cũ trước khi thay đổi để ghi log & thông báo
                String oldValuesJson = GSON.toJson(currentSession);
                LocalDate oldDate = currentSession.getSessionDate();
                LocalTime oldStart = currentSession.getStartTime();
                LocalTime oldEnd = currentSession.getEndTime();
                Integer oldRoomId = currentSession.getClassroomId();
                Long oldInstructorId = currentSession.getInstructorId();

                // 3. Quy tắc cập nhật một phần (Partial update / PATCH merge)
                request.setScheduleId(scheduleId);
                if (request.getClassId() == null) {
                    request.setClassId(currentSession.getClassId());
                }
                if (request.getInstructorId() == null) {
                    request.setInstructorId(currentSession.getInstructorId());
                }
                if (request.getResolvedRoomId() == null) {
                    request.setRoomId(currentSession.getClassroomId());
                }
                if (request.getResolvedDate() == null && oldDate != null) {
                    request.setSessionDate(oldDate.toString());
                }
                if (request.getResolvedStartTime() == null && oldStart != null) {
                    request.setStartTime(oldStart.toString());
                }
                if (request.getResolvedEndTime() == null && oldEnd != null) {
                    request.setEndTime(oldEnd.toString());
                }
                if (request.getSessionNumber() == null) {
                    request.setSessionNumber(currentSession.getSessionNumber());
                }
                if (request.getClassSubjectId() == null) {
                    request.setClassSubjectId(currentSession.getClassSubjectId());
                }
                if (request.getSubjectLessonId() == null) {
                    request.setSubjectLessonId(currentSession.getSubjectLessonId());
                }

                // Validate sau khi merge các trường cần thiết
                request.validate();

                // Kiểm tra sự tồn tại của các thực thể mới
                verifyEntitiesExist(connection, request);

                // Khóa tài nguyên phòng học & giảng viên để chống race condition
                scheduleDAO.lockInstructorAndRoom(connection, request.getInstructorId(), request.getResolvedRoomId());

                // 4. Kiểm tra xung đột lịch (loại trừ chính buổi học đang sửa)
                List<ClassSession> candidates = scheduleDAO.findConflictingSessions(
                        connection,
                        scheduleId,
                        request.getInstructorId(),
                        request.getResolvedRoomId(),
                        request.getResolvedDate(),
                        request.getResolvedStartTime(),
                        request.getResolvedEndTime()
                );
                ConflictCheckResult conflictResult = detectConflicts(request, candidates);

                if (conflictResult.isHasConflict()) {
                    handleConflictResolution(actorId, actorRoles, request, conflictResult);
                }

                // 5. Xác định xem có dời lịch (đổi ngày/giờ) hay không
                LocalDate newDate = request.getResolvedDate();
                LocalTime newStart = request.getResolvedStartTime();
                LocalTime newEnd = request.getResolvedEndTime();
                boolean isDateTimeChanged = !Objects.equals(oldDate, newDate)
                        || !Objects.equals(oldStart, newStart)
                        || !Objects.equals(oldEnd, newEnd);
                boolean isReschedule = isDateTimeChanged;

                String newStatus = isReschedule ? "RESCHEDULED"
                        : (request.getStatus() != null ? request.getStatus() : currentSession.getStatus());

                currentSession.setClassId(request.getClassId());
                currentSession.setClassSubjectId(request.getClassSubjectId());
                currentSession.setSubjectLessonId(request.getSubjectLessonId());
                currentSession.setSessionNumber(request.getSessionNumber());
                currentSession.setSessionDate(newDate);
                currentSession.setStartTime(newStart);
                currentSession.setEndTime(newEnd);
                currentSession.setClassroomId(request.getResolvedRoomId());
                currentSession.setInstructorId(request.getInstructorId());
                currentSession.setStatus(newStatus);

                scheduleDAO.updateSchedule(connection, currentSession);

                ClassSession updated = scheduleDAO.findById(connection, scheduleId);
                if (updated == null) updated = currentSession;
                String newValuesJson = GSON.toJson(updated);

                // Ghi nhận Audit Log & Change Log
                String actionType = isReschedule ? "RESCHEDULE" : "UPDATE";
                scheduleDAO.recordAuditLog(
                        connection,
                        actorId,
                        isReschedule ? "RESCHEDULE_SESSION" : "UPDATE_SESSION",
                        "SCHEDULE",
                        String.valueOf(scheduleId),
                        oldValuesJson,
                        newValuesJson
                );
                scheduleDAO.recordChangeLog(
                        connection,
                        scheduleId,
                        actionType,
                        actorId,
                        request.getReason(),
                        oldValuesJson,
                        newValuesJson
                );

                connection.commit();

                // 6. Gửi thông báo cho học viên (sau khi commit thành công, lỗi thông báo không làm mất thay đổi)
                boolean notifSent = false;
                int recipientCount = 0;
                String notifError = null;
                try {
                    List<Long> studentUserIds = scheduleDAO.getEnrolledStudentUserIds(connection, currentSession.getClassId());
                    if (studentUserIds != null && !studentUserIds.isEmpty()) {
                        String classCode = updated.getClassCode() != null ? updated.getClassCode() : "Lớp #" + updated.getClassId();
                        String title = "Thông báo " + (isReschedule ? "dời lịch học" : "cập nhật lịch học") + ": " + classCode;

                        StringBuilder contentBuilder = new StringBuilder();
                        contentBuilder.append("Lịch học lớp ").append(classCode).append(" đã được điều chỉnh:\n");
                        if (isDateTimeChanged) {
                            contentBuilder.append("- Thời gian cũ: Ngày ").append(oldDate).append(" (").append(oldStart).append(" - ").append(oldEnd).append(")\n");
                            contentBuilder.append("- Thời gian mới: Ngày ").append(newDate).append(" (").append(newStart).append(" - ").append(newEnd).append(")\n");
                        }
                        if (!Objects.equals(oldRoomId, updated.getClassroomId())) {
                            contentBuilder.append("- Phòng học mới: ").append(updated.getRoomCode() != null ? updated.getRoomCode() : updated.getClassroomId()).append("\n");
                        }
                        if (!Objects.equals(oldInstructorId, updated.getInstructorId())) {
                            contentBuilder.append("- Giảng viên phụ trách mới: ").append(updated.getInstructorName() != null ? updated.getInstructorName() : updated.getInstructorId()).append("\n");
                        }
                        contentBuilder.append("- Thời điểm cập nhật: ").append(java.time.LocalDateTime.now());

                        recipientCount = scheduleDAO.sendNotificationToStudents(
                                connection,
                                studentUserIds,
                                actorId,
                                title,
                                contentBuilder.toString(),
                                "SCHEDULE",
                                "/classes/" + updated.getClassId() + "/schedule"
                        );
                        notifSent = recipientCount > 0;
                    }
                } catch (Exception ex) {
                    notifError = ex.getMessage();
                    System.err.println("Lỗi khi gửi thông báo cập nhật lịch học: " + ex.getMessage());
                }

                com.ems.dto.RescheduleResponse resp = new com.ems.dto.RescheduleResponse(
                        true,
                        actionType,
                        updated,
                        notifSent,
                        recipientCount,
                        (isReschedule ? "Dời" : "Cập nhật") + " buổi học thành công."
                );
                resp.setNotificationError(notifError);
                return resp;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public ClassSession getScheduleById(long scheduleId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            ClassSession session = scheduleDAO.findById(connection, scheduleId);
            if (session == null) {
                throw new ResourceNotFoundException("Không tìm thấy buổi học có ID: " + scheduleId);
            }
            return session;
        }
    }

    public List<ClassSession> listSchedules(
            Long classId,
            Long instructorId,
            Integer roomId,
            LocalDate fromDate,
            LocalDate toDate
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return scheduleDAO.listSchedules(connection, classId, instructorId, roomId, fromDate, toDate);
        }
    }

    /**
     * API 4: Hủy buổi học (User Story S4-03)
     */
    public boolean cancelSchedule(
            long actorId,
            List<String> actorRoles,
            long scheduleId,
            String reason
    ) throws SQLException {
        com.ems.dto.RescheduleResponse resp = cancelScheduleWithDetails(actorId, actorRoles, scheduleId, reason);
        return resp.isSuccess();
    }

    /**
     * API 4 (Chi tiết): Hủy buổi học có trả về trạng thái gửi thông báo
     */
    public com.ems.dto.RescheduleResponse cancelScheduleWithDetails(
            long actorId,
            List<String> actorRoles,
            long scheduleId,
            String reason
    ) throws SQLException {
        if (!isTrainingManagerOrAdmin(actorRoles)) {
            throw new ForbiddenAccessException("Chỉ Quản lý đào tạo hoặc Quản trị viên mới có quyền hủy buổi học.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("Lý do hủy buổi học là bắt buộc và không được để trống.");
        }

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ClassSession session = scheduleDAO.findById(connection, scheduleId);
                if (session == null) {
                    throw new ResourceNotFoundException("Không tìm thấy buổi học có ID: " + scheduleId);
                }

                // Kiểm tra trạng thái
                if ("CANCELLED".equalsIgnoreCase(session.getStatus())) {
                    throw new ValidationException("Buổi học này đã bị hủy trước đó.");
                }
                if ("COMPLETED".equalsIgnoreCase(session.getStatus())) {
                    throw new ValidationException("Không thể hủy buổi học đã hoàn thành.");
                }

                // Không cho phép hủy buổi học đã được điểm danh
                if (scheduleDAO.hasAttendance(connection, scheduleId)) {
                    throw new ValidationException("Không thể hủy buổi học đã được điểm danh.");
                }

                String oldValuesJson = GSON.toJson(session);

                scheduleDAO.cancelSchedule(connection, scheduleId, reason.trim());

                session.setStatus("CANCELLED");
                session.setCancelReason(reason.trim());
                String newValuesJson = GSON.toJson(session);

                scheduleDAO.recordAuditLog(
                        connection,
                        actorId,
                        "CANCEL_SCHEDULE",
                        "SCHEDULE",
                        String.valueOf(scheduleId),
                        oldValuesJson,
                        newValuesJson
                );
                scheduleDAO.recordChangeLog(
                        connection,
                        scheduleId,
                        "CANCEL",
                        actorId,
                        reason.trim(),
                        oldValuesJson,
                        newValuesJson
                );

                connection.commit();

                // Gửi thông báo cho học viên
                boolean notifSent = false;
                int recipientCount = 0;
                String notifError = null;
                try {
                    List<Long> studentUserIds = scheduleDAO.getEnrolledStudentUserIds(connection, session.getClassId());
                    if (studentUserIds != null && !studentUserIds.isEmpty()) {
                        String classCode = session.getClassCode() != null ? session.getClassCode() : "Lớp #" + session.getClassId();
                        String title = "Thông báo hủy buổi học: " + classCode;
                        String content = "Buổi học ngày " + session.getSessionDate()
                                + " (" + session.getStartTime() + " - " + session.getEndTime() + ") của lớp "
                                + classCode + " đã bị hủy.\n"
                                + "- Lý do hủy: " + reason.trim() + "\n"
                                + "- Thời điểm hủy: " + java.time.LocalDateTime.now();

                        recipientCount = scheduleDAO.sendNotificationToStudents(
                                connection,
                                studentUserIds,
                                actorId,
                                title,
                                content,
                                "SCHEDULE",
                                "/classes/" + session.getClassId() + "/schedule"
                        );
                        notifSent = recipientCount > 0;
                    }
                } catch (Exception ex) {
                    notifError = ex.getMessage();
                    System.err.println("Lỗi khi gửi thông báo hủy buổi học: " + ex.getMessage());
                }

                com.ems.dto.RescheduleResponse resp = new com.ems.dto.RescheduleResponse(
                        true,
                        "CANCEL",
                        session,
                        notifSent,
                        recipientCount,
                        "Hủy buổi học thành công."
                );
                resp.setNotificationError(notifError);
                return resp;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void verifyEntitiesExist(Connection connection, ScheduleRequest request) throws SQLException {
        // Kiểm tra lớp học
        SchoolClass sc = classDAO.findById(connection, request.getClassId());
        if (sc == null) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học có ID: " + request.getClassId());
        }

        // Kiểm tra phòng học
        Classroom room = classroomDAO.findById(connection, request.getResolvedRoomId());
        if (room == null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng học có ID: " + request.getResolvedRoomId());
        }

        // Kiểm tra giảng viên
        User instructor = userDAO.findById(request.getInstructorId());
        if (instructor == null) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản giảng viên có ID: " + request.getInstructorId());
        }
    }

    private void handleConflictResolution(
            long actorId,
            List<String> actorRoles,
            ScheduleRequest request,
            ConflictCheckResult conflictResult
    ) {
        if (!request.isOverrideConflict()) {
            // Mặc định: Chặn lưu lịch và trả về thông tin chi tiết các buổi học bị xung đột
            throw new ScheduleConflictException(
                    "Xung đột thời khóa biểu: trùng giảng viên hoặc phòng học. Không thể lưu lịch.",
                    conflictResult
            );
        }

        // Nếu client yêu cầu ghi đè:
        // 1. Kiểm tra vai trò: Chỉ Training Manager hoặc Admin được phép ghi đè
        if (!isTrainingManagerOrAdmin(actorRoles)) {
            throw new ForbiddenAccessException(
                    "Chỉ Quản lý đào tạo (Training Manager) mới có quyền xác nhận ghi đè xung đột lịch học."
            );
        }

        // 2. Kiểm tra lý do ghi đè
        if (request.getOverrideReason() == null || request.getOverrideReason().trim().isEmpty()) {
            throw new ValidationException(
                    "Cần cung cấp lý do ghi đè (overrideReason) khi xác nhận lưu lịch bị xung đột."
            );
        }
    }

    public boolean isTrainingManagerOrAdmin(List<String> roles) {
        if (roles == null || roles.isEmpty()) return false;
        return roles.contains(RoleConstant.TRAINING_MANAGER)
                || roles.contains(RoleConstant.ADMIN)
                || roles.contains("TRAINING_MANAGER")
                || roles.contains("ADMIN");
    }
}
