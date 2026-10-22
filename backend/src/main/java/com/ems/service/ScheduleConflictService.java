package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.constant.RoleConstant;
import com.ems.dao.ClassDAO;
import com.ems.dao.ClassroomDAO;
import com.ems.dao.ScheduleDAO;
import com.ems.dao.UserDAO;
import com.ems.dto.ConflictCheckResult;
import com.ems.dto.ConflictDetail;
import com.ems.dto.ConflictType;
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
    private final ClassDAO classDAO;
    private final UserDAO userDAO;
    private final ConnectionProvider connectionProvider;

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public ScheduleConflictService() {
        this(new ScheduleDAO(), new ClassroomDAO(), new ClassDAO(), new UserDAO(), DBConnection::getConnection);
    }

    public ScheduleConflictService(
            ScheduleDAO scheduleDAO,
            ClassroomDAO classroomDAO,
            ClassDAO classDAO,
            UserDAO userDAO
    ) {
        this(scheduleDAO, classroomDAO, classDAO, userDAO, DBConnection::getConnection);
    }

    public ScheduleConflictService(
            ScheduleDAO scheduleDAO,
            ClassroomDAO classroomDAO,
            ClassDAO classDAO,
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
     * Thuß║¡t to├ín kiß╗âm tra xung ─æß╗Öt thß╗¥i kh├│a biß╗âu:
     * - Tr├╣ng giß║úng vi├¬n trong c├╣ng khung giß╗¥ giao nhau.
     * - Tr├╣ng ph├▓ng hß╗ìc trong c├╣ng khung giß╗¥ giao nhau.
     * - ─Éiß╗üu kiß╗çn giao nhau:
     *     existing.start_time < requested.end_time AND existing.end_time > requested.start_time
     * - Hai buß╗òi hß╗ìc nß╗æi tiß║┐p nhau (v├¡ dß╗Ñ: kß║┐t th├║c 10:00 v├á bß║»t ─æß║ºu 10:00) KH├öNG bß╗ï coi l├á xung ─æß╗Öt.
     * - Khi cß║¡p nhß║¡t, bß╗Å qua ch├¡nh buß╗òi hß╗ìc ─æ├│ (existing.id != requested.scheduleId).
     * - Lß╗ïch c├│ trß║íng th├íi CANCELLED ─æ╞░ß╗úc loß║íi trß╗½.
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
                // Loß║íi trß╗½ ch├¡nh n├│ khi cß║¡p nhß║¡t
                if (excludeId != null && excludeId.equals(existing.getId())) {
                    continue;
                }
                // Loß║íi trß╗½ c├íc buß╗òi ─æ├ú hß╗ºy
                if ("CANCELLED".equalsIgnoreCase(existing.getStatus())) {
                    continue;
                }
                // Kh├íc ng├áy -> kh├┤ng xung ─æß╗Öt
                if (existing.getSessionDate() == null || !existing.getSessionDate().equals(reqDate)) {
                    continue;
                }

                LocalTime exStart = existing.getStartTime();
                LocalTime exEnd = existing.getEndTime();
                if (exStart == null || exEnd == null) continue;

                // Kiß╗âm tra ─æiß╗üu kiß╗çn giao nhau thß╗¥i gian:
                // exStart < reqEnd && exEnd > reqStart
                boolean isOverlapping = exStart.isBefore(reqEnd) && exEnd.isAfter(reqStart);
                if (!isOverlapping) {
                    continue;
                }

                // Map th├┤ng tin y├¬u cß║ºu ─æß╗â hiß╗ân thß╗ï
                Map<String, Object> requestedSchedule = new LinkedHashMap<>();
                requestedSchedule.put("classId", request.getClassId());
                requestedSchedule.put("startDateTime", request.getResolvedStartDateTime() != null
                        ? request.getResolvedStartDateTime().toString() : reqDate + "T" + reqStart);
                requestedSchedule.put("endDateTime", request.getResolvedEndDateTime() != null
                        ? request.getResolvedEndDateTime().toString() : reqDate + "T" + reqEnd);

                // Map th├┤ng tin buß╗òi hß╗ìc bß╗ï xung ─æß╗Öt
                Map<String, Object> conflictingSchedule = new LinkedHashMap<>();
                conflictingSchedule.put("scheduleId", existing.getId());
                conflictingSchedule.put("classCode", existing.getClassCode() != null ? existing.getClassCode() : "Lß╗¢p #" + existing.getClassId());
                conflictingSchedule.put("className", existing.getClassName());
                conflictingSchedule.put("instructorId", existing.getInstructorId());
                conflictingSchedule.put("instructorName", existing.getInstructorName() != null ? existing.getInstructorName() : "Giß║úng vi├¬n #" + existing.getInstructorId());
                conflictingSchedule.put("roomId", existing.getClassroomId());
                conflictingSchedule.put("roomCode", existing.getRoomCode() != null ? existing.getRoomCode() : "Ph├▓ng #" + existing.getClassroomId());
                conflictingSchedule.put("roomName", existing.getRoomName());
                conflictingSchedule.put("startDateTime", existing.getFormattedStartDateTime());
                conflictingSchedule.put("endDateTime", existing.getFormattedEndDateTime());

                // 1. Kiß╗âm tra tr├╣ng giß║úng vi├¬n
                if (reqInstructorId != null && reqInstructorId.equals(existing.getInstructorId())) {
                    conflictList.add(new ConflictDetail(
                            ConflictType.INSTRUCTOR_CONFLICT.getCode(),
                            requestedSchedule,
                            conflictingSchedule,
                            "Giß║úng vi├¬n [" + conflictingSchedule.get("instructorName")
                                    + "] ─æ├ú c├│ lß╗ïch dß║íy lß╗¢p [" + conflictingSchedule.get("classCode")
                                    + "] tß╗½ " + exStart + " ─æß║┐n " + exEnd + "."
                    ));
                }

                // 2. Kiß╗âm tra tr├╣ng ph├▓ng hß╗ìc
                if (reqRoomId != null && reqRoomId.equals(existing.getClassroomId())) {
                    conflictList.add(new ConflictDetail(
                            ConflictType.ROOM_CONFLICT.getCode(),
                            requestedSchedule,
                            conflictingSchedule,
                            "Ph├▓ng hß╗ìc [" + conflictingSchedule.get("roomCode")
                                    + "] ─æ├ú c├│ lß╗¢p [" + conflictingSchedule.get("classCode")
                                    + "] sß╗¡ dß╗Ñng tß╗½ " + exStart + " ─æß║┐n " + exEnd + "."
                    ));
                }
            }
        }

        boolean hasConflict = !conflictList.isEmpty();
        String message = hasConflict
                ? "Ph├ít hiß╗çn " + conflictList.size() + " xung ─æß╗Öt thß╗¥i kh├│a biß╗âu (tr├╣ng giß║úng vi├¬n hoß║╖c ph├▓ng hß╗ìc)."
                : "Kh├┤ng c├│ xung ─æß╗Öt lß╗ïch hß╗ìc.";

        return new ConflictCheckResult(hasConflict, conflictList, message);
    }

    /**
     * API 1: Kiß╗âm tra xung ─æß╗Öt (Read-only check, kh├┤ng l╞░u lß╗ïch)
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
     * API 2: Tß║ío lß╗ïch hß╗ìc mß╗¢i
     * Quy tr├¼nh:
     * 1. Validate dß╗» liß╗çu ─æß║ºu v├áo.
     * 2. Kiß╗âm tra lß╗¢p hß╗ìc, giß║úng vi├¬n, ph├▓ng hß╗ìc tß╗ôn tß║íi.
     * 3. Mß╗ƒ transaction v├á kh├│a FOR UPDATE t├ái nguy├¬n ─æß╗â chß╗æng race condition.
     * 4. Kiß╗âm tra xung ─æß╗Öt.
     * 5. Nß║┐u c├│ xung ─æß╗Öt:
     *    - Nß║┐u KH├öNG y├¬u cß║ºu ghi ─æ├¿ -> Tß╗½ chß╗æi l╞░u (HTTP 409).
     *    - Nß║┐u C├ô y├¬u cß║ºu ghi ─æ├¿:
     *      + Chß╗ë Quß║ún l├╜ ─æ├áo tß║ío (hoß║╖c Admin) mß╗¢i c├│ quyß╗ün ghi ─æ├¿ (HTTP 403).
     *      + Bß║»t buß╗Öc phß║úi c├│ l├╜ do ghi ─æ├¿ (HTTP 400).
     *      + L╞░u lß╗ïch v├á ghi nhß║¡n Audit Log chi tiß║┐t.
     * 6. Nß║┐u kh├┤ng c├│ xung ─æß╗Öt: L╞░u lß╗ïch v├á ghi nhß║¡n Audit Log.
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
                // 1. Kiß╗âm tra tß╗ôn tß║íi thß╗▒c thß╗â
                verifyEntitiesExist(connection, request);

                // 2. Kh├│a d├▓ng users & classrooms ─æß╗â ng─ân chß║╖n race condition ─æß╗ông thß╗¥i
                scheduleDAO.lockInstructorAndRoom(connection, request.getInstructorId(), request.getResolvedRoomId());

                // 3. Kiß╗âm tra xung ─æß╗Öt trong transaction
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

                // 4. L╞░u lß╗ïch hß╗ìc mß╗¢i
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

                // 5. Ghi nhß║¡n Audit Log
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
     * API 3: Cß║¡p nhß║¡t lß╗ïch hß╗ìc
     */
    public ClassSession updateSchedule(
            long actorId,
            List<String> actorRoles,
            long scheduleId,
            ScheduleRequest request
    ) throws SQLException {
        request.setScheduleId(scheduleId);
        request.validate();

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ClassSession currentSession = scheduleDAO.findById(connection, scheduleId);
                if (currentSession == null) {
                    throw new ResourceNotFoundException("Kh├┤ng t├¼m thß║Ñy buß╗òi hß╗ìc c├│ ID: " + scheduleId);
                }

                verifyEntitiesExist(connection, request);

                // Kh├│a t├ái nguy├¬n
                scheduleDAO.lockInstructorAndRoom(connection, request.getInstructorId(), request.getResolvedRoomId());

                // Kiß╗âm tra xung ─æß╗Öt (loß║íi trß╗½ ch├¡nh n├│)
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

                currentSession.setClassId(request.getClassId());
                currentSession.setClassSubjectId(request.getClassSubjectId());
                currentSession.setSubjectLessonId(request.getSubjectLessonId());
                currentSession.setSessionNumber(request.getSessionNumber());
                currentSession.setSessionDate(request.getResolvedDate());
                currentSession.setStartTime(request.getResolvedStartTime());
                currentSession.setEndTime(request.getResolvedEndTime());
                currentSession.setClassroomId(request.getResolvedRoomId());
                currentSession.setInstructorId(request.getInstructorId());
                if (request.getStatus() != null) currentSession.setStatus(request.getStatus());

                scheduleDAO.updateSchedule(connection, currentSession);

                // Ghi nhß║¡n Audit Log
                if (conflictResult.isHasConflict() && request.isOverrideConflict()) {
                    Map<String, Object> auditOld = Map.of(
                            "action", "OVERRIDE_UPDATE_SCHEDULE_CONFLICT",
                            "conflictingCount", conflictResult.getConflicts().size(),
                            "conflicts", conflictResult.getConflicts()
                    );
                    Map<String, Object> auditNew = Map.of(
                            "updatedScheduleId", scheduleId,
                            "overrideReason", request.getOverrideReason(),
                            "actorRoles", actorRoles != null ? actorRoles : List.of()
                    );
                    scheduleDAO.recordAuditLog(
                            connection,
                            actorId,
                            "OVERRIDE_SCHEDULE_CONFLICT",
                            "SCHEDULE",
                            String.valueOf(scheduleId),
                            GSON.toJson(auditOld),
                            GSON.toJson(auditNew)
                    );
                } else {
                    scheduleDAO.recordAuditLog(
                            connection,
                            actorId,
                            "UPDATE_SCHEDULE",
                            "SCHEDULE",
                            String.valueOf(scheduleId),
                            null,
                            GSON.toJson(request)
                    );
                }

                connection.commit();
                return scheduleDAO.findById(connection, scheduleId);
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
                throw new ResourceNotFoundException("Kh├┤ng t├¼m thß║Ñy buß╗òi hß╗ìc c├│ ID: " + scheduleId);
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

    public boolean cancelSchedule(
            long actorId,
            List<String> actorRoles,
            long scheduleId,
            String reason
    ) throws SQLException {
        if (!isTrainingManagerOrAdmin(actorRoles)) {
            throw new ForbiddenAccessException("Chß╗ë Quß║ún l├╜ ─æ├áo tß║ío hoß║╖c Quß║ún trß╗ï vi├¬n mß╗¢i c├│ quyß╗ün hß╗ºy buß╗òi hß╗ìc.");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ClassSession session = scheduleDAO.findById(connection, scheduleId);
                if (session == null) {
                    throw new ResourceNotFoundException("Kh├┤ng t├¼m thß║Ñy buß╗òi hß╗ìc c├│ ID: " + scheduleId);
                }
                boolean ok = scheduleDAO.cancelSchedule(connection, scheduleId, reason);
                scheduleDAO.recordAuditLog(
                        connection,
                        actorId,
                        "CANCEL_SCHEDULE",
                        "SCHEDULE",
                        String.valueOf(scheduleId),
                        GSON.toJson(session),
                        GSON.toJson(Map.of("reason", reason != null ? reason : ""))
                );
                connection.commit();
                return ok;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void verifyEntitiesExist(Connection connection, ScheduleRequest request) throws SQLException {
        // Kiß╗âm tra lß╗¢p hß╗ìc
        SchoolClass sc = classDAO.findById(connection, request.getClassId());
        if (sc == null) {
            throw new ResourceNotFoundException("Kh├┤ng t├¼m thß║Ñy lß╗¢p hß╗ìc c├│ ID: " + request.getClassId());
        }

        // Kiß╗âm tra ph├▓ng hß╗ìc
        Classroom room = classroomDAO.findById(connection, request.getResolvedRoomId());
        if (room == null) {
            throw new ResourceNotFoundException("Kh├┤ng t├¼m thß║Ñy ph├▓ng hß╗ìc c├│ ID: " + request.getResolvedRoomId());
        }

        // Kiß╗âm tra giß║úng vi├¬n
        User instructor = userDAO.findById(request.getInstructorId());
        if (instructor == null) {
            throw new ResourceNotFoundException("Kh├┤ng t├¼m thß║Ñy t├ái khoß║ún giß║úng vi├¬n c├│ ID: " + request.getInstructorId());
        }
    }

    private void handleConflictResolution(
            long actorId,
            List<String> actorRoles,
            ScheduleRequest request,
            ConflictCheckResult conflictResult
    ) {
        if (!request.isOverrideConflict()) {
            // Mß║╖c ─æß╗ïnh: Chß║╖n l╞░u lß╗ïch v├á trß║ú vß╗ü th├┤ng tin chi tiß║┐t c├íc buß╗òi hß╗ìc bß╗ï xung ─æß╗Öt
            throw new ScheduleConflictException(
                    "Xung ─æß╗Öt thß╗¥i kh├│a biß╗âu: tr├╣ng giß║úng vi├¬n hoß║╖c ph├▓ng hß╗ìc. Kh├┤ng thß╗â l╞░u lß╗ïch.",
                    conflictResult
            );
        }

        // Nß║┐u client y├¬u cß║ºu ghi ─æ├¿:
        // 1. Kiß╗âm tra vai tr├▓: Chß╗ë Training Manager hoß║╖c Admin ─æ╞░ß╗úc ph├⌐p ghi ─æ├¿
        if (!isTrainingManagerOrAdmin(actorRoles)) {
            throw new ForbiddenAccessException(
                    "Chß╗ë Quß║ún l├╜ ─æ├áo tß║ío (Training Manager) mß╗¢i c├│ quyß╗ün x├íc nhß║¡n ghi ─æ├¿ xung ─æß╗Öt lß╗ïch hß╗ìc."
            );
        }

        // 2. Kiß╗âm tra l├╜ do ghi ─æ├¿
        if (request.getOverrideReason() == null || request.getOverrideReason().trim().isEmpty()) {
            throw new ValidationException(
                    "Cß║ºn cung cß║Ñp l├╜ do ghi ─æ├¿ (overrideReason) khi x├íc nhß║¡n l╞░u lß╗ïch bß╗ï xung ─æß╗Öt."
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
