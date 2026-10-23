package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.constant.RoleConstant;
import com.ems.dto.AttendanceSheetResponse;
import com.ems.dto.SaveAttendanceRequest;
import com.ems.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho nghiệp vụ S4-07 (Jira EP-05):
 * Điểm danh học viên cho một buổi học (Bốn trạng thái, đánh dấu tất cả có mặt,
 * kiểm soát quyền điểm danh buổi hôm nay vs quá khứ, ghi nhật ký sửa đổi).
 *
 * Hỗ trợ lưu trữ CSDL MySQL và tự động fallback in-memory cache phục vụ môi trường kiểm thử/mock.
 */
public class AttendanceDAO {

    private static final Logger LOGGER = Logger.getLogger(AttendanceDAO.class.getName());
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final AtomicLong ID_GEN = new AtomicLong(1000);

    // In-memory cache dự phòng khi chạy mock / test
    private static final Map<Long, List<AttendanceRecord>> MEMORY_ATTENDANCE = new ConcurrentHashMap<>();
    private static final Map<Long, List<AttendanceAuditLog>> MEMORY_LOGS = new ConcurrentHashMap<>();
    private static final Map<Long, ClassSession> MEMORY_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Long, List<Student>> MEMORY_CLASS_STUDENTS = new ConcurrentHashMap<>();
    private static final Set<String> MEMORY_APPROVED_LEAVES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    static {
        initSampleData();
    }

    public static void initSampleData() {
        MEMORY_ATTENDANCE.clear();
        MEMORY_LOGS.clear();
        MEMORY_SESSIONS.clear();
        MEMORY_CLASS_STUDENTS.clear();
        MEMORY_APPROVED_LEAVES.clear();

        // 1. Buổi học mẫu lớp WEB-K15 (ID 401: Hôm nay, ID 402: Quá khứ 2 ngày trước, ID 403: Tương lai)
        LocalDate today = LocalDate.now();

        ClassSession sToday = new ClassSession(401L, 9102L, 2L, 101, today,
                java.time.LocalTime.of(18, 30), java.time.LocalTime.of(21, 0), "SCHEDULED");
        sToday.setClassCode("WEB-K15");
        sToday.setClassName("Web Full-stack K15");
        sToday.setSessionNumber(14);
        sToday.setRoomName("Phòng 401");
        sToday.setInstructorName("VU DUC LONG");
        MEMORY_SESSIONS.put(sToday.getId(), sToday);

        ClassSession sPast = new ClassSession(402L, 9102L, 2L, 101, today.minusDays(2),
                java.time.LocalTime.of(18, 30), java.time.LocalTime.of(21, 0), "COMPLETED");
        sPast.setClassCode("WEB-K15");
        sPast.setClassName("Web Full-stack K15");
        sPast.setSessionNumber(13);
        sPast.setRoomName("Phòng 401");
        sPast.setInstructorName("VU DUC LONG");
        MEMORY_SESSIONS.put(sPast.getId(), sPast);

        ClassSession sFuture = new ClassSession(403L, 9102L, 2L, 101, today.plusDays(3),
                java.time.LocalTime.of(18, 30), java.time.LocalTime.of(21, 0), "SCHEDULED");
        sFuture.setClassCode("WEB-K15");
        sFuture.setClassName("Web Full-stack K15");
        sFuture.setSessionNumber(15);
        sFuture.setRoomName("Phòng 401");
        sFuture.setInstructorName("VU DUC LONG");
        MEMORY_SESSIONS.put(sFuture.getId(), sFuture);

        // 2. Học viên lớp WEB-K15
        List<Student> students = new ArrayList<>();
        students.add(new Student("HV260101", "NGUYEN MINH ANH", "0912345678", "minhanh.ng@gmail.com", "Web Full-stack", "Đang học", "2026-08-12", "WEB-K15"));
        students.add(new Student("HV260102", "HOANG YEN NHI", "0934118205", "yennhi.hoang@gmail.com", "Web Full-stack", "Đang học", "2026-08-12", "WEB-K15"));
        students.add(new Student("HV260103", "TRAN DUC HUY", "0904612775", "duchuy.tran@gmail.com", "Web Full-stack", "Đang học", "2026-08-12", "WEB-K15"));
        students.add(new Student("HV260104", "LE THI MAI", "0988776655", "mailt@gmail.com", "Web Full-stack", "Đang học", "2026-08-12", "WEB-K15"));
        students.add(new Student("HV260105", "PHAM MINH CHI", "0977665544", "chipm@gmail.com", "Web Full-stack", "Đang học", "2026-08-12", "WEB-K15"));
        for (long i = 0; i < students.size(); i++) {
            students.get((int) i).setId(1001L + i);
        }
        MEMORY_CLASS_STUDENTS.put(9102L, students);

        // Giả lập buổi quá khứ (402) đã lưu điểm danh trước đó
        List<AttendanceRecord> pastRecs = new CopyOnWriteArrayList<>();
        for (Student st : students) {
            AttendanceRecord r = new AttendanceRecord(402L, st.getId(), st.getCode(), st.getName(), AttendanceStatus.PRESENT);
            r.setId(ID_GEN.incrementAndGet());
            r.setMarkedBy(2L);
            r.setMarkerName("VU DUC LONG");
            r.setMarkedAt(LocalDateTime.now().minusDays(2));
            pastRecs.add(r);
        }
        MEMORY_ATTENDANCE.put(402L, pastRecs);
    }

    /**
     * Lấy chi tiết bảng điểm danh của một buổi học (S4-07)
     */
    public AttendanceSheetResponse getAttendanceSheet(Long sessionId, User currentUser, LocalDate today) throws SQLException {
        if (today == null) {
            today = LocalDate.now();
        }

        ClassSession session = findSession(sessionId);
        if (session == null) {
            return null;
        }

        AttendanceSheetResponse resp = new AttendanceSheetResponse();
        resp.setSessionId(session.getId());
        resp.setClassId(session.getClassId());
        resp.setClassCode(session.getClassCode() != null ? session.getClassCode() : "CLASS-" + session.getClassId());
        resp.setClassName(session.getClassName() != null ? session.getClassName() : "Lớp " + resp.getClassCode());
        resp.setSessionNumber(session.getSessionNumber() != null ? session.getSessionNumber() : 1);
        resp.setSessionDate(session.getSessionDate() != null ? session.getSessionDate().toString() : today.toString());
        resp.setStartTime(session.getStartTime() != null ? session.getStartTime().toString().substring(0, 5) : "18:30");
        resp.setEndTime(session.getEndTime() != null ? session.getEndTime().toString().substring(0, 5) : "21:00");
        resp.setRoomName(session.getRoomName() != null ? session.getRoomName() : "Phòng 401");
        resp.setSubjectName("Lập trình Web Frontend");
        resp.setLessonTopic("Buổi " + resp.getSessionNumber() + ": DOM & Event Handling");
        resp.setInstructorName(session.getInstructorName() != null ? session.getInstructorName() : "Giảng viên");

        LocalDate sDate = session.getSessionDate() != null ? session.getSessionDate() : today;
        boolean isToday = sDate.equals(today);
        boolean isPast = sDate.isBefore(today);
        boolean isFuture = sDate.isAfter(today);
        boolean isCancelled = "CANCELLED".equalsIgnoreCase(session.getStatus());

        resp.setToday(isToday);
        resp.setPast(isPast);
        resp.setFuture(isFuture);
        resp.setCancelled(isCancelled);
        resp.setCancelReason(session.getCancelReason());

        // Kiểm tra quyền sửa (AC3):
        // Giảng viên chỉ được điểm danh buổi hôm nay.
        // Buổi quá khứ cần quyền Quản lý đào tạo (TrainingManager hoặc Admin).
        boolean isManager = currentUser != null && (currentUser.hasRole(RoleConstant.ADMIN) || currentUser.hasRole(RoleConstant.TRAINING_MANAGER));
        boolean canEdit = !isCancelled && !isFuture && (isManager || isToday);
        resp.setCanEdit(canEdit);

        // Lấy danh sách học viên trong lớp
        List<Student> roster = getRosterForClass(session.getClassId());

        // Lấy các bản ghi điểm danh hiện có
        Map<Long, AttendanceRecord> existingMap = new HashMap<>();
        List<AttendanceRecord> records = getRecordsForSession(sessionId);
        if (records != null && !records.isEmpty()) {
            resp.setSaved(true);
            AttendanceRecord first = records.get(0);
            resp.setSavedAt(first.getMarkedAt() != null ? first.getMarkedAt().format(ISO_DT) : null);
            resp.setSavedBy(first.getMarkerName() != null ? first.getMarkerName() : "Giảng viên");
            for (AttendanceRecord r : records) {
                if (r.getStudentId() != null) {
                    existingMap.put(r.getStudentId(), r);
                }
            }
        }

        // Tạo danh sách phản hồi cho từng học viên trong lớp
        List<AttendanceSheetResponse.StudentAttendanceItem> items = new ArrayList<>();
        for (Student st : roster) {
            AttendanceSheetResponse.StudentAttendanceItem item = new AttendanceSheetResponse.StudentAttendanceItem();
            item.setStudentId(st.getId());
            item.setStudentCode(st.getCode());
            item.setStudentName(st.getName());

            boolean hasApprovedLeave = hasApprovedAbsence(sessionId, st.getId());
            item.setApprovedLeave(hasApprovedLeave);

            AttendanceRecord existing = existingMap.get(st.getId());
            if (existing != null) {
                item.setStatus(existing.getStatusLabel());
                item.setNotes(existing.getNotes());
            } else {
                // Nếu chưa điểm danh mà có đơn xin nghỉ đã duyệt -> tự động đặt 'Vắng có phép' (AC1)
                if (hasApprovedLeave) {
                    item.setStatus(AttendanceStatus.EXCUSED_ABSENCE.getLabel());
                    item.setNotes("Đơn xin phép đã duyệt");
                } else {
                    // Mặc định ban đầu để trống hoặc Có mặt khi mở
                    item.setStatus(null);
                    item.setNotes("");
                }
            }
            items.add(item);
        }

        resp.setStudents(items);
        resp.recalculateSummary();
        resp.setLogs(getAuditLogs(sessionId));

        return resp;
    }

    /**
     * Lưu bảng điểm danh cho một buổi học (S4-07):
     * - Bốn trạng thái: Có mặt, Đi muộn, Vắng có phép, Vắng không phép (AC1)
     * - Chỉ điểm danh được buổi hôm nay; buổi quá khứ cần quyền Quản lý đào tạo và lý do sửa (AC3)
     * - Mọi thay đổi điểm danh đã lưu được ghi vào nhật ký kiểm toán (AC4)
     */
    public synchronized AttendanceSheetResponse saveAttendance(SaveAttendanceRequest request, User currentUser, LocalDate today) throws SQLException {
        if (request == null || request.getSessionId() == null) {
            throw new IllegalArgumentException("Mã buổi học (sessionId) không được để trống.");
        }
        if (today == null) {
            today = LocalDate.now();
        }

        Long sessionId = request.getSessionId();
        ClassSession session = findSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Không tìm thấy buổi học với mã: " + sessionId);
        }

        LocalDate sDate = session.getSessionDate() != null ? session.getSessionDate() : today;

        // AC3: Kiểm tra buổi tương lai
        if (sDate.isAfter(today)) {
            throw new IllegalArgumentException("Chưa tới ngày diễn ra buổi học (" + sDate + "), không thể điểm danh trước.");
        }

        // AC3: Kiểm tra buổi quá khứ
        boolean isManager = currentUser != null && (currentUser.hasRole(RoleConstant.ADMIN) || currentUser.hasRole(RoleConstant.TRAINING_MANAGER));
        if (sDate.isBefore(today)) {
            if (!isManager) {
                throw new SecurityException("Buổi học này đã qua. Giảng viên chỉ điểm danh được buổi của ngày hôm nay. "
                        + "Sửa buổi quá khứ cần quyền Quản lý đào tạo.");
            }
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do sửa điểm danh của buổi học đã qua.");
            }
        }

        // AC1: Validate và chuẩn hóa trạng thái của các dòng học viên
        List<SaveAttendanceRequest.AttendanceItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách điểm danh không được để trống.");
        }

        Map<Long, AttendanceRecord> existingRecords = new HashMap<>();
        for (AttendanceRecord r : getRecordsForSession(sessionId)) {
            if (r.getStudentId() != null) {
                existingRecords.put(r.getStudentId(), r);
            }
        }
        boolean isFirstSave = existingRecords.isEmpty();

        String actorName = currentUser != null ? currentUser.getName() : "Hệ thống";
        Long actorId = currentUser != null ? currentUser.getId() : 1L;
        String actorRole = (currentUser != null && currentUser.getPrimaryRole() != null)
                ? currentUser.getPrimaryRole() : (isManager ? "Quản lý đào tạo" : "Giảng viên");

        List<AttendanceRecord> updatedList = new ArrayList<>();
        List<AttendanceAuditLog> newLogs = new ArrayList<>();

        // Thử thực thi trong MySQL nếu có kết nối
        boolean dbSuccess = false;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Đảm bảo bảng tồn tại
                ensureTables(conn);

                String insertSql = "INSERT INTO attendance (session_id, student_id, status, notes, marked_by, marked_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                String updateSql = "UPDATE attendance SET status = ?, notes = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE session_id = ? AND student_id = ?";
                String logSql = "INSERT INTO attendance_audit_logs (session_id, student_id, student_code, student_name, "
                        + "actor_id, actor_name, actor_role, old_status, new_status, reason, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

                try (PreparedStatement psInsert = conn.prepareStatement(insertSql);
                     PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                     PreparedStatement psLog = conn.prepareStatement(logSql)) {

                    for (SaveAttendanceRequest.AttendanceItem item : items) {
                        Long studentId = item.getStudentId();
                        if (studentId == null) {
                            Student st = findStudentByCode(item.getStudentCode());
                            studentId = st != null ? st.getId() : ID_GEN.incrementAndGet();
                        }

                        // Parse 4 trạng thái
                        AttendanceStatus status = AttendanceStatus.from(item.getStatus());
                        String notes = item.getNotes() != null ? item.getNotes().trim() : "";

                        AttendanceRecord existing = existingRecords.get(studentId);
                        if (existing != null) {
                            // Đã tồn tại bản ghi cũ -> Sửa
                            if (!existing.getStatus().equals(status)) {
                                // AC4: Ghi nhật ký khi có thay đổi trạng thái
                                AttendanceAuditLog log = new AttendanceAuditLog(
                                        sessionId, studentId, item.getStudentCode(),
                                        existing.getStudentName() != null ? existing.getStudentName() : item.getStudentCode(),
                                        actorId, actorName, actorRole,
                                        existing.getStatusLabel(), status.getLabel(), request.getReason()
                                );
                                newLogs.add(log);

                                psLog.setLong(1, sessionId);
                                psLog.setLong(2, studentId);
                                psLog.setString(3, item.getStudentCode());
                                psLog.setString(4, log.getStudentName());
                                psLog.setLong(5, actorId);
                                psLog.setString(6, actorName);
                                psLog.setString(7, actorRole);
                                psLog.setString(8, existing.getStatusLabel());
                                psLog.setString(9, status.getLabel());
                                psLog.setString(10, request.getReason());
                                psLog.executeUpdate();
                            }

                            psUpdate.setString(1, status.getCode());
                            psUpdate.setString(2, notes);
                            psUpdate.setLong(3, actorId);
                            psUpdate.setLong(4, sessionId);
                            psUpdate.setLong(5, studentId);
                            psUpdate.executeUpdate();

                            existing.setStatus(status);
                            existing.setNotes(notes);
                            existing.setUpdatedBy(actorId);
                            existing.setUpdaterName(actorName);
                            existing.setUpdatedAt(LocalDateTime.now());
                            updatedList.add(existing);
                        } else {
                            // Chưa có bản ghi -> Thêm mới
                            psInsert.setLong(1, sessionId);
                            psInsert.setLong(2, studentId);
                            psInsert.setString(3, status.getCode());
                            psInsert.setString(4, notes);
                            psInsert.setLong(5, actorId);
                            psInsert.executeUpdate();

                            AttendanceRecord newRecord = new AttendanceRecord(sessionId, studentId, item.getStudentCode(), null, status);
                            newRecord.setId(ID_GEN.incrementAndGet());
                            newRecord.setNotes(notes);
                            newRecord.setMarkedBy(actorId);
                            newRecord.setMarkerName(actorName);
                            newRecord.setMarkedAt(LocalDateTime.now());
                            updatedList.add(newRecord);
                        }
                    }

                    if (isFirstSave) {
                        // Ghi nhận mốc lưu điểm danh lần đầu vào nhật ký
                        psLog.setLong(1, sessionId);
                        psLog.setLong(2, 0L);
                        psLog.setString(3, "ALL");
                        psLog.setString(4, "Cả lớp");
                        psLog.setLong(5, actorId);
                        psLog.setString(6, actorName);
                        psLog.setString(7, actorRole);
                        psLog.setString(8, "Chưa điểm danh");
                        psLog.setString(9, "Đã lưu điểm danh lần đầu");
                        psLog.setString(10, "Lưu điểm danh");
                        psLog.executeUpdate();
                    }
                }

                conn.commit();
                dbSuccess = true;
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.WARNING, "Lỗi DB lưu điểm danh, chuyển sang lưu bộ nhớ: " + e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Không kết nối được DB, lưu vào bộ nhớ cache: " + e.getMessage());
        }

        // Cập nhật vào Memory Cache
        if (!dbSuccess) {
            for (SaveAttendanceRequest.AttendanceItem item : items) {
                Long studentId = item.getStudentId();
                if (studentId == null) {
                    Student st = findStudentByCode(item.getStudentCode());
                    studentId = st != null ? st.getId() : ID_GEN.incrementAndGet();
                }
                AttendanceStatus status = AttendanceStatus.from(item.getStatus());
                String notes = item.getNotes() != null ? item.getNotes().trim() : "";

                AttendanceRecord existing = existingRecords.get(studentId);
                if (existing != null) {
                    if (!existing.getStatus().equals(status)) {
                        AttendanceAuditLog log = new AttendanceAuditLog(
                                sessionId, studentId, item.getStudentCode(),
                                existing.getStudentName() != null ? existing.getStudentName() : item.getStudentCode(),
                                actorId, actorName, actorRole,
                                existing.getStatusLabel(), status.getLabel(), request.getReason()
                        );
                        newLogs.add(log);
                    }
                    existing.setStatus(status);
                    existing.setNotes(notes);
                    existing.setUpdatedBy(actorId);
                    existing.setUpdaterName(actorName);
                    existing.setUpdatedAt(LocalDateTime.now());
                    updatedList.add(existing);
                } else {
                    AttendanceRecord newRec = new AttendanceRecord(sessionId, studentId, item.getStudentCode(), null, status);
                    newRec.setId(ID_GEN.incrementAndGet());
                    newRec.setNotes(notes);
                    newRec.setMarkedBy(actorId);
                    newRec.setMarkerName(actorName);
                    newRec.setMarkedAt(LocalDateTime.now());
                    updatedList.add(newRec);
                }
            }
            if (isFirstSave) {
                newLogs.add(new AttendanceAuditLog(sessionId, 0L, "ALL", "Cả lớp", actorId, actorName, actorRole,
                        "Chưa điểm danh", "Đã lưu điểm danh lần đầu", "Lưu điểm danh"));
            }
        }

        MEMORY_ATTENDANCE.put(sessionId, updatedList);
        if (!newLogs.isEmpty()) {
            List<AttendanceAuditLog> logs = MEMORY_LOGS.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
            logs.addAll(0, newLogs);
        }

        return getAttendanceSheet(sessionId, currentUser, today);
    }

    /**
     * AC2: Đánh dấu tất cả có mặt cho một buổi học và lưu lại
     */
    public AttendanceSheetResponse markAllPresent(Long sessionId, User currentUser, LocalDate today) throws SQLException {
        ClassSession session = findSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Không tìm thấy buổi học với mã: " + sessionId);
        }

        List<Student> roster = getRosterForClass(session.getClassId());
        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionId);
        req.setReason("Đánh dấu tất cả có mặt");

        List<SaveAttendanceRequest.AttendanceItem> items = new ArrayList<>();
        for (Student st : roster) {
            items.add(new SaveAttendanceRequest.AttendanceItem(st.getId(), st.getCode(), AttendanceStatus.PRESENT.getLabel(), ""));
        }
        req.setItems(items);

        return saveAttendance(req, currentUser, today);
    }

    /**
     * Lấy danh sách lịch sử sửa điểm danh của buổi học (AC4)
     */
    public List<AttendanceAuditLog> getAuditLogs(Long sessionId) {
        List<AttendanceAuditLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            ensureTables(conn);
            String sql = "SELECT * FROM attendance_audit_logs WHERE session_id = ? ORDER BY created_at DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AttendanceAuditLog log = new AttendanceAuditLog();
                        log.setId(rs.getLong("id"));
                        log.setSessionId(rs.getLong("session_id"));
                        log.setStudentId(rs.getLong("student_id"));
                        log.setStudentCode(rs.getString("student_code"));
                        log.setStudentName(rs.getString("student_name"));
                        log.setActorId(rs.getLong("actor_id"));
                        log.setActorName(rs.getString("actor_name"));
                        log.setActorRole(rs.getString("actor_role"));
                        log.setOldStatus(rs.getString("old_status"));
                        log.setNewStatus(rs.getString("new_status"));
                        log.setReason(rs.getString("reason"));
                        Timestamp ts = rs.getTimestamp("created_at");
                        if (ts != null) {
                            log.setCreatedAt(ts.toLocalDateTime());
                        }
                        list.add(log);
                    }
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
        }

        // Fallback memory
        return MEMORY_LOGS.getOrDefault(sessionId, Collections.emptyList());
    }

    // --- Private Helper Methods ---

    private ClassSession findSession(Long sessionId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT cs.*, c.class_code, c.class_name, cr.room_name, u.full_name as instructor_name "
                    + "FROM class_sessions cs "
                    + "LEFT JOIN classes c ON cs.class_id = c.id "
                    + "LEFT JOIN classrooms cr ON cs.classroom_id = cr.id "
                    + "LEFT JOIN users u ON cs.instructor_id = u.id "
                    + "WHERE cs.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ClassSession s = new ClassSession();
                        s.setId(rs.getLong("id"));
                        s.setClassId(rs.getLong("class_id"));
                        s.setClassCode(rs.getString("class_code"));
                        s.setClassName(rs.getString("class_name"));
                        s.setSessionNumber(rs.getInt("session_number"));
                        Date d = rs.getDate("session_date");
                        if (d != null) s.setSessionDate(d.toLocalDate());
                        Time st = rs.getTime("start_time");
                        if (st != null) s.setStartTime(st.toLocalTime());
                        Time et = rs.getTime("end_time");
                        if (et != null) s.setEndTime(et.toLocalTime());
                        s.setRoomName(rs.getString("room_name"));
                        s.setInstructorName(rs.getString("instructor_name"));
                        s.setStatus(rs.getString("status"));
                        s.setCancelReason(rs.getString("cancel_reason"));
                        return s;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return MEMORY_SESSIONS.get(sessionId);
    }

    private List<Student> getRosterForClass(Long classId) {
        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT s.id, s.student_code, u.full_name, u.phone, u.email "
                    + "FROM enrollments e "
                    + "JOIN students s ON e.student_id = s.id "
                    + "JOIN users u ON s.user_id = u.id "
                    + "WHERE e.class_id = ? AND e.status = 'ENROLLED' "
                    + "ORDER BY s.student_code ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, classId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Student st = new Student();
                        st.setId(rs.getLong("id"));
                        st.setCode(rs.getString("student_code"));
                        st.setName(rs.getString("full_name"));
                        st.setPhone(rs.getString("phone"));
                        st.setEmail(rs.getString("email"));
                        list.add(st);
                    }
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
        }

        return MEMORY_CLASS_STUDENTS.getOrDefault(classId,
                MEMORY_CLASS_STUDENTS.values().stream().findFirst().orElse(Collections.emptyList()));
    }

    private List<AttendanceRecord> getRecordsForSession(Long sessionId) {
        List<AttendanceRecord> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            ensureTables(conn);
            String sql = "SELECT a.*, s.student_code, u.full_name as student_name, marker.full_name as marker_name "
                    + "FROM attendance a "
                    + "JOIN students s ON a.student_id = s.id "
                    + "JOIN users u ON s.user_id = u.id "
                    + "LEFT JOIN users marker ON a.marked_by = marker.id "
                    + "WHERE a.session_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AttendanceRecord r = new AttendanceRecord();
                        r.setId(rs.getLong("id"));
                        r.setSessionId(rs.getLong("session_id"));
                        r.setStudentId(rs.getLong("student_id"));
                        r.setStudentCode(rs.getString("student_code"));
                        r.setStudentName(rs.getString("student_name"));
                        r.setStatus(AttendanceStatus.from(rs.getString("status")));
                        r.setNotes(rs.getString("notes"));
                        r.setMarkedBy(rs.getLong("marked_by"));
                        r.setMarkerName(rs.getString("marker_name"));
                        Timestamp mt = rs.getTimestamp("marked_at");
                        if (mt != null) r.setMarkedAt(mt.toLocalDateTime());
                        list.add(r);
                    }
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
        }
        return MEMORY_ATTENDANCE.getOrDefault(sessionId, Collections.emptyList());
    }

    private boolean hasApprovedAbsence(Long sessionId, Long studentId) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM absence_requests WHERE session_id = ? AND student_id = ? AND status = 'APPROVED'";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, sessionId);
                ps.setLong(2, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return MEMORY_APPROVED_LEAVES.contains(sessionId + ":" + studentId);
    }

    private Student findStudentByCode(String code) {
        for (List<Student> list : MEMORY_CLASS_STUDENTS.values()) {
            for (Student s : list) {
                if (s.getCode() != null && s.getCode().equalsIgnoreCase(code)) {
                    return s;
                }
            }
        }
        return null;
    }

    private void ensureTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
                    notes VARCHAR(255) NULL,
                    marked_by BIGINT NULL,
                    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_by BIGINT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_session_student UNIQUE (session_id, student_id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS attendance_audit_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    student_code VARCHAR(50) NULL,
                    student_name VARCHAR(100) NULL,
                    actor_id BIGINT NULL,
                    actor_name VARCHAR(100) NOT NULL,
                    actor_role VARCHAR(50) NOT NULL,
                    old_status VARCHAR(50) NOT NULL,
                    new_status VARCHAR(50) NOT NULL,
                    reason VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }
}
