package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.ClassSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {

    /**
     * T├¼m c├íc buß╗òi hß╗ìc bß╗ï giao nhau trong c├╣ng khung giß╗¥ vß╗¢i giß║úng vi├¬n hoß║╖c ph├▓ng hß╗ìc.
     * Quy tß║»c giao nhau:
     *   existing.session_date = requested.session_date
     *   AND existing.status != 'CANCELLED'
     *   AND existing.start_time < requested.end_time
     *   AND existing.end_time > requested.start_time
     *   AND (excludeScheduleId IS NULL OR existing.id != excludeScheduleId)
     *   AND (existing.instructor_id = requested.instructor_id OR existing.classroom_id = requested.classroom_id)
     */
    public List<ClassSession> findConflictingSessions(
            Connection connection,
            Long excludeScheduleId,
            Long instructorId,
            Integer roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) throws SQLException {
        List<ClassSession> conflicts = new ArrayList<>();
        String sql = "SELECT s.id, s.class_id, s.class_subject_id, s.subject_lesson_id, s.session_number, "
                + "s.session_date, s.start_time, s.end_time, s.classroom_id, s.instructor_id, s.status, s.cancel_reason, "
                + "c.class_code, c.class_name, cr.room_code, cr.room_name, u.full_name AS instructor_name, u.email AS instructor_email "
                + "FROM class_sessions s "
                + "LEFT JOIN classes c ON c.id = s.class_id "
                + "LEFT JOIN classrooms cr ON cr.id = s.classroom_id "
                + "LEFT JOIN users u ON u.id = s.instructor_id "
                + "WHERE s.session_date = ? "
                + "AND s.status != 'CANCELLED' "
                + "AND s.start_time < ? "
                + "AND s.end_time > ? "
                + (excludeScheduleId != null ? "AND s.id != ? " : "")
                + "AND (s.instructor_id = ? OR s.classroom_id = ?) "
                + "ORDER BY s.start_time";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int idx = 1;
            stmt.setDate(idx++, Date.valueOf(date));
            stmt.setTime(idx++, Time.valueOf(endTime));
            stmt.setTime(idx++, Time.valueOf(startTime));
            if (excludeScheduleId != null) {
                stmt.setLong(idx++, excludeScheduleId);
            }
            stmt.setLong(idx++, instructorId);
            stmt.setInt(idx++, roomId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conflicts.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return conflicts;
    }

    public List<ClassSession> findConflictingSessions(
            Long excludeScheduleId,
            Long instructorId,
            Integer roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findConflictingSessions(conn, excludeScheduleId, instructorId, roomId, date, startTime, endTime);
        }
    }

    public ClassSession findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT s.id, s.class_id, s.class_subject_id, s.subject_lesson_id, s.session_number, "
                + "s.session_date, s.start_time, s.end_time, s.classroom_id, s.instructor_id, s.status, s.cancel_reason, "
                + "c.class_code, c.class_name, cr.room_code, cr.room_name, u.full_name AS instructor_name, u.email AS instructor_email, "
                + "s.created_at, s.updated_at "
                + "FROM class_sessions s "
                + "LEFT JOIN classes c ON c.id = s.class_id "
                + "LEFT JOIN classrooms cr ON cr.id = s.classroom_id "
                + "LEFT JOIN users u ON u.id = s.instructor_id "
                + "WHERE s.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClassSession(rs);
                }
            }
        }
        return null;
    }

    public ClassSession findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public long createSchedule(Connection connection, ClassSession session) throws SQLException {
        String sql = "INSERT INTO class_sessions (class_id, class_subject_id, subject_lesson_id, session_number, "
                + "session_date, start_time, end_time, classroom_id, instructor_id, status, cancel_reason) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, session.getClassId());
            if (session.getClassSubjectId() != null) stmt.setLong(2, session.getClassSubjectId());
            else stmt.setNull(2, Types.BIGINT);

            if (session.getSubjectLessonId() != null) stmt.setInt(3, session.getSubjectLessonId());
            else stmt.setNull(3, Types.INTEGER);

            stmt.setInt(4, session.getSessionNumber() != null ? session.getSessionNumber() : 1);
            stmt.setDate(5, Date.valueOf(session.getSessionDate()));
            stmt.setTime(6, Time.valueOf(session.getStartTime()));
            stmt.setTime(7, Time.valueOf(session.getEndTime()));
            stmt.setInt(8, session.getClassroomId());
            stmt.setLong(9, session.getInstructorId());
            stmt.setString(10, session.getStatus() != null ? session.getStatus() : "SCHEDULED");
            stmt.setString(11, session.getCancelReason());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    session.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public boolean updateSchedule(Connection connection, ClassSession session) throws SQLException {
        String sql = "UPDATE class_sessions SET class_id = ?, class_subject_id = ?, subject_lesson_id = ?, "
                + "session_number = ?, session_date = ?, start_time = ?, end_time = ?, "
                + "classroom_id = ?, instructor_id = ?, status = ?, cancel_reason = ?, updated_at = NOW() "
                + "WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, session.getClassId());
            if (session.getClassSubjectId() != null) stmt.setLong(2, session.getClassSubjectId());
            else stmt.setNull(2, Types.BIGINT);

            if (session.getSubjectLessonId() != null) stmt.setInt(3, session.getSubjectLessonId());
            else stmt.setNull(3, Types.INTEGER);

            stmt.setInt(4, session.getSessionNumber() != null ? session.getSessionNumber() : 1);
            stmt.setDate(5, Date.valueOf(session.getSessionDate()));
            stmt.setTime(6, Time.valueOf(session.getStartTime()));
            stmt.setTime(7, Time.valueOf(session.getEndTime()));
            stmt.setInt(8, session.getClassroomId());
            stmt.setLong(9, session.getInstructorId());
            stmt.setString(10, session.getStatus() != null ? session.getStatus() : "SCHEDULED");
            stmt.setString(11, session.getCancelReason());
            stmt.setLong(12, session.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean cancelSchedule(Connection connection, long id, String cancelReason) throws SQLException {
        String sql = "UPDATE class_sessions SET status = 'CANCELLED', cancel_reason = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, cancelReason);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<ClassSession> listSchedules(
            Connection connection,
            Long classId,
            Long instructorId,
            Integer roomId,
            LocalDate fromDate,
            LocalDate toDate
    ) throws SQLException {
        List<ClassSession> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT s.id, s.class_id, s.class_subject_id, s.subject_lesson_id, s.session_number, ")
                .append("s.session_date, s.start_time, s.end_time, s.classroom_id, s.instructor_id, s.status, s.cancel_reason, ")
                .append("c.class_code, c.class_name, cr.room_code, cr.room_name, u.full_name AS instructor_name, u.email AS instructor_email ")
                .append("FROM class_sessions s ")
                .append("LEFT JOIN classes c ON c.id = s.class_id ")
                .append("LEFT JOIN classrooms cr ON cr.id = s.classroom_id ")
                .append("LEFT JOIN users u ON u.id = s.instructor_id ")
                .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (classId != null) {
            sql.append("AND s.class_id = ? ");
            params.add(classId);
        }
        if (instructorId != null) {
            sql.append("AND s.instructor_id = ? ");
            params.add(instructorId);
        }
        if (roomId != null) {
            sql.append("AND s.classroom_id = ? ");
            params.add(roomId);
        }
        if (fromDate != null) {
            sql.append("AND s.session_date >= ? ");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append("AND s.session_date <= ? ");
            params.add(Date.valueOf(toDate));
        }
        sql.append("ORDER BY s.session_date, s.start_time");

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return list;
    }

    /**
     * Kh├│a d├▓ng bß║úng users (giß║úng vi├¬n) v├á classrooms (ph├▓ng hß╗ìc) vß╗¢i FOR UPDATE
     * ─æß╗â ng─ân chß║╖n race condition khi hai request ─æß╗ông thß╗¥i c├╣ng xß║┐p lß╗ïch.
     */
    public void lockInstructorAndRoom(Connection connection, Long instructorId, Integer roomId) throws SQLException {
        if (instructorId != null) {
            String lockUser = "SELECT id FROM users WHERE id = ? FOR UPDATE";
            try (PreparedStatement stmt = connection.prepareStatement(lockUser)) {
                stmt.setLong(1, instructorId);
                stmt.executeQuery();
            }
        }
        if (roomId != null) {
            String lockRoom = "SELECT id FROM classrooms WHERE id = ? FOR UPDATE";
            try (PreparedStatement stmt = connection.prepareStatement(lockRoom)) {
                stmt.setInt(1, roomId);
                stmt.executeQuery();
            }
        }
    }

    /**
     * Ghi nhận lịch sử thao tác kiểm toán vào bảng audit_logs
     */
    public void recordAuditLog(Connection connection, Long userId, String action, String entityType,
                               String entityId, String oldValues, String newValues) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (userId != null) stmt.setLong(1, userId);
            else stmt.setNull(1, Types.BIGINT);
            stmt.setString(2, action);
            stmt.setString(3, entityType);
            stmt.setString(4, entityId);
            stmt.setString(5, oldValues);
            stmt.setString(6, newValues);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // Không ngắt luồng chính nếu audit log bị lỗi nhẹ
        }
    }

    /**
     * Kiểm tra xem một buổi học đã có dữ liệu điểm danh hay chưa.
     * Quy tắc: Có ít nhất một bản ghi trong bảng attendance với session_id = sessionId.
     */
    public boolean hasAttendance(Connection connection, long sessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE session_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean hasAttendance(long sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return hasAttendance(conn, sessionId);
        }
    }

    /**
     * Lấy danh sách user_id của các học viên đang ghi danh (status = 'ENROLLED') trong lớp.
     */
    public List<Long> getEnrolledStudentUserIds(Connection connection, long classId) throws SQLException {
        List<Long> userIds = new ArrayList<>();
        String sql = "SELECT DISTINCT s.user_id "
                + "FROM enrollments e "
                + "JOIN students s ON s.id = e.student_id "
                + "WHERE e.class_id = ? AND e.status = 'ENROLLED'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
        }
        return userIds;
    }

    public List<Long> getEnrolledStudentUserIds(long classId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return getEnrolledStudentUserIds(conn, classId);
        }
    }

    /**
     * Tạo một thông báo cho người dùng vào bảng notifications
     */
    public void createNotification(Connection connection, Long recipientId, Long senderId,
                                   String title, String content, String type, String linkUrl) throws SQLException {
        String sql = "INSERT INTO notifications (recipient_id, sender_id, title, content, notification_type, link_url, is_read, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, FALSE, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, recipientId);
            if (senderId != null) stmt.setLong(2, senderId);
            else stmt.setNull(2, Types.BIGINT);
            stmt.setString(3, title);
            stmt.setString(4, content);
            stmt.setString(5, type != null ? type : "SCHEDULE");
            stmt.setString(6, linkUrl);
            stmt.executeUpdate();
        }
    }

    /**
     * Gửi thông báo hàng loạt cho danh sách học viên
     */
    public int sendNotificationToStudents(Connection connection, List<Long> recipientUserIds, Long senderId,
                                          String title, String content, String type, String linkUrl) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        String sql = "INSERT INTO notifications (recipient_id, sender_id, title, content, notification_type, link_url, is_read, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, FALSE, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Long recipientId : recipientUserIds) {
                stmt.setLong(1, recipientId);
                if (senderId != null) stmt.setLong(2, senderId);
                else stmt.setNull(2, Types.BIGINT);
                stmt.setString(3, title);
                stmt.setString(4, content);
                stmt.setString(5, type != null ? type : "SCHEDULE");
                stmt.setString(6, linkUrl);
                stmt.addBatch();
                count++;
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Lỗi khi gửi thông báo cho học viên: " + e.getMessage());
        }
        return count;
    }

    /**
     * Ghi nhận lịch sử thay đổi vào bảng class_session_change_logs
     */
    public void recordChangeLog(Connection connection, long sessionId, String actionType,
                                long performedBy, String reason, String oldValues, String newValues) {
        String sql = "INSERT INTO class_session_change_logs (session_id, action_type, performed_by, reason, old_values, new_values, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            stmt.setString(2, actionType);
            stmt.setLong(3, performedBy);
            stmt.setString(4, reason);
            stmt.setString(5, oldValues);
            stmt.setString(6, newValues);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // Không ngắt luồng nếu bảng chưa tồn tại hoặc gặp lỗi nhẹ
        }
    }

    private ClassSession mapResultSetToClassSession(ResultSet rs) throws SQLException {
        ClassSession cs = new ClassSession();
        cs.setId(rs.getLong("id"));
        cs.setClassId(rs.getLong("class_id"));
        long csId = rs.getLong("class_subject_id");
        if (!rs.wasNull()) cs.setClassSubjectId(csId);
        int slId = rs.getInt("subject_lesson_id");
        if (!rs.wasNull()) cs.setSubjectLessonId(slId);
        cs.setSessionNumber(rs.getInt("session_number"));

        Date d = rs.getDate("session_date");
        if (d != null) cs.setSessionDate(d.toLocalDate());
        Time st = rs.getTime("start_time");
        if (st != null) cs.setStartTime(st.toLocalTime());
        Time et = rs.getTime("end_time");
        if (et != null) cs.setEndTime(et.toLocalTime());

        cs.setClassroomId(rs.getInt("classroom_id"));
        cs.setInstructorId(rs.getLong("instructor_id"));
        cs.setStatus(rs.getString("status"));
        cs.setCancelReason(rs.getString("cancel_reason"));

        cs.setClassCode(rs.getString("class_code"));
        cs.setClassName(rs.getString("class_name"));
        cs.setRoomCode(rs.getString("room_code"));
        cs.setRoomName(rs.getString("room_name"));
        cs.setInstructorName(rs.getString("instructor_name"));
        cs.setInstructorEmail(rs.getString("instructor_email"));

        try {
            Timestamp cat = rs.getTimestamp("created_at");
            if (cat != null) cs.setCreatedAt(cat.toLocalDateTime());
            Timestamp uat = rs.getTimestamp("updated_at");
            if (uat != null) cs.setUpdatedAt(uat.toLocalDateTime());
        } catch (SQLException ignored) {}

        return cs;
    }
}
