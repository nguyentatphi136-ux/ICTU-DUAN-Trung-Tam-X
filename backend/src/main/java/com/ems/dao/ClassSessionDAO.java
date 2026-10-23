package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.dto.ClassSessionResponse;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO truy vấn lịch học từng buổi (class_sessions)
 * EP-04: Xem thời khóa biểu dạng lịch tuần và tháng – S4-04
 */
public class ClassSessionDAO {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Cột chung của một buổi. LEFT JOIN để không mất buổi chưa gắn môn (S4-02 cho phép); seq là thứ tự buổi trong lớp
     * (buổi huỷ vẫn giữ số), topic lấy từ giáo trình (subject_lessons).
     */
    private static final String BASE_SELECT = """
            SELECT cs.id, cs.class_id, c.class_code, c.class_name, cs.class_subject_id,
                   s.id AS subject_id, s.subject_code, s.subject_name,
                   cs.instructor_id, u.full_name AS instructor_name,
                   cs.classroom_id, cr.room_code, cr.room_name,
                   cs.session_date, cs.start_time, cs.end_time, cs.status, cs.session_number, cs.cancel_reason,
                   sl.topic,
                   (SELECT COUNT(*) FROM class_sessions x WHERE x.class_id = cs.class_id
                     AND (x.session_date < cs.session_date OR (x.session_date = cs.session_date
                     AND (x.start_time < cs.start_time OR (x.start_time = cs.start_time AND x.id <= cs.id))))) AS seq
            FROM class_sessions cs
            JOIN classes c ON cs.class_id = c.id
            LEFT JOIN class_subjects csub ON cs.class_subject_id = csub.id
            LEFT JOIN subjects s ON csub.subject_id = s.id
            LEFT JOIN subject_lessons sl ON sl.id = cs.subject_lesson_id
            LEFT JOIN users u ON cs.instructor_id = u.id
            LEFT JOIN classrooms cr ON cs.classroom_id = cr.id
            """;

    /** Buổi học theo điều kiện tuỳ ý trên bí danh cs, c (ví dụ "WHERE cs.class_id IN (...)"), sắp theo ngày giờ. */
    public List<ClassSessionResponse> findWhere(String condition, List<Object> params) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(BASE_SELECT + condition + " ORDER BY cs.session_date, cs.start_time, cs.id")) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    // -------------------------------------------------------------------------
    // A. Tìm buổi học theo khoảng thời gian với bộ lọc tùy chọn
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách buổi học nằm trong [startDate, endDate].
     * classId / lecturerId / roomId = null nghĩa là không lọc theo tiêu chí đó.
     */
    public List<ClassSessionResponse> findByDateRange(
            LocalDate startDate, LocalDate endDate,
            Long classId, Long lecturerId, Integer roomId) throws SQLException {

        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE cs.session_date BETWEEN ? AND ?\n");

        List<Object> params = new ArrayList<>();
        params.add(startDate.toString());
        params.add(endDate.toString());

        if (classId != null) {
            sql.append("  AND cs.class_id = ?\n");
            params.add(classId);
        }
        if (lecturerId != null) {
            sql.append("  AND cs.instructor_id = ?\n");
            params.add(lecturerId);
        }
        if (roomId != null) {
            sql.append("  AND cs.classroom_id = ?\n");
            params.add(roomId);
        }

        sql.append("ORDER BY cs.session_date, cs.start_time");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s) stmt.setString(i + 1, s);
                else if (p instanceof Long l) stmt.setLong(i + 1, l);
                else if (p instanceof Integer in) stmt.setInt(i + 1, in);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    // -------------------------------------------------------------------------
    // B. Lấy chi tiết một buổi học theo ID
    // -------------------------------------------------------------------------

    /**
     * Trả về chi tiết đầy đủ một buổi học, hoặc null nếu không tìm thấy.
     */
    public ClassSessionResponse findById(long id) throws SQLException {
        String sql = BASE_SELECT + "WHERE cs.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ClassSessionResponse> rows = mapRows(rs);
                return rows.isEmpty() ? null : rows.get(0);
            }
        }
    }

    // -------------------------------------------------------------------------
    // C. Phát hiện xung đột lịch học trong khoảng thời gian
    // Điều kiện giao nhau: startA < endB AND endA > startB (không tính tiếp nối)
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách tất cả cặp buổi học bị trùng (phòng / giảng viên / lớp)
     * trong khoảng [startDate, endDate]. Mỗi bản ghi có thêm trường conflictType.
     */
    public List<java.util.Map<String, Object>> findConflicts(
            LocalDate startDate, LocalDate endDate) throws SQLException {

        // Trùng phòng
        String sqlRoom = """
            SELECT 'ROOM' AS conflict_type,
                   a.id AS session_a_id, b.id AS session_b_id,
                   a.session_date, a.start_time, a.end_time,
                   cr.room_code, cr.room_name,
                   NULL AS instructor_name, NULL AS class_code
            FROM class_sessions a
            JOIN class_sessions b ON a.classroom_id = b.classroom_id
                 AND a.session_date = b.session_date
                 AND a.id < b.id
                 AND a.start_time < b.end_time
                 AND a.end_time   > b.start_time
            JOIN classrooms cr ON a.classroom_id = cr.id
            WHERE a.session_date BETWEEN ? AND ?
            """;

        // Trùng giảng viên
        String sqlInstructor = """
            SELECT 'INSTRUCTOR' AS conflict_type,
                   a.id AS session_a_id, b.id AS session_b_id,
                   a.session_date, a.start_time, a.end_time,
                   NULL AS room_code, NULL AS room_name,
                   u.full_name AS instructor_name, NULL AS class_code
            FROM class_sessions a
            JOIN class_sessions b ON a.instructor_id = b.instructor_id
                 AND a.session_date = b.session_date
                 AND a.id < b.id
                 AND a.start_time < b.end_time
                 AND a.end_time   > b.start_time
            JOIN users u ON a.instructor_id = u.id
            WHERE a.session_date BETWEEN ? AND ?
            """;

        // Trùng lớp
        String sqlClass = """
            SELECT 'CLASS' AS conflict_type,
                   a.id AS session_a_id, b.id AS session_b_id,
                   a.session_date, a.start_time, a.end_time,
                   NULL AS room_code, NULL AS room_name,
                   NULL AS instructor_name, c.class_code
            FROM class_sessions a
            JOIN class_sessions b ON a.class_id = b.class_id
                 AND a.session_date = b.session_date
                 AND a.id < b.id
                 AND a.start_time < b.end_time
                 AND a.end_time   > b.start_time
            JOIN classes c ON a.class_id = c.id
            WHERE a.session_date BETWEEN ? AND ?
            """;

        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (String sql : new String[]{sqlRoom, sqlInstructor, sqlClass}) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, startDate.toString());
                stmt.setString(2, endDate.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                        row.put("conflictType", rs.getString("conflict_type"));
                        row.put("sessionAId", rs.getLong("session_a_id"));
                        row.put("sessionBId", rs.getLong("session_b_id"));
                        row.put("sessionDate", rs.getString("session_date"));
                        row.put("startTime", rs.getString("start_time"));
                        row.put("endTime", rs.getString("end_time"));
                        row.put("roomCode", rs.getString("room_code"));
                        row.put("roomName", rs.getString("room_name"));
                        row.put("instructorName", rs.getString("instructor_name"));
                        row.put("classCode", rs.getString("class_code"));
                        result.add(row);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Trả về tập hợp ID buổi học bị xung đột trong khoảng [startDate, endDate].
     * Dùng để đánh dấu hasConflict=true trong danh sách lịch.
     */
    public java.util.Set<Long> findConflictingSessionIds(
            LocalDate startDate, LocalDate endDate) throws SQLException {

        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (java.util.Map<String, Object> conflict : findConflicts(startDate, endDate)) {
            ids.add((Long) conflict.get("sessionAId"));
            ids.add((Long) conflict.get("sessionBId"));
        }
        return ids;
    }

    // -------------------------------------------------------------------------
    // D. Danh sách lớp học, giảng viên, phòng học (dùng cho bộ lọc)
    // -------------------------------------------------------------------------

    /** Danh sách lớp học có buổi học (không lặp). */
    public List<java.util.Map<String, Object>> listClasses() throws SQLException {
        String sql = """
            SELECT DISTINCT c.id, c.class_code, c.class_name
            FROM classes c
            JOIN class_sessions cs ON cs.class_id = c.id
            ORDER BY c.class_code
            """;
        return queryToMaps(sql);
    }

    /** Danh sách giảng viên có buổi học (không lặp). */
    public List<java.util.Map<String, Object>> listInstructors() throws SQLException {
        String sql = """
            SELECT DISTINCT u.id, u.full_name
            FROM users u
            JOIN class_sessions cs ON cs.instructor_id = u.id
            ORDER BY u.full_name
            """;
        return queryToMaps(sql);
    }

    /** Danh sách phòng học có buổi học (không lặp). */
    public List<java.util.Map<String, Object>> listClassrooms() throws SQLException {
        String sql = """
            SELECT DISTINCT cr.id, cr.room_code, cr.room_name, cr.room_type, cr.capacity
            FROM classrooms cr
            JOIN class_sessions cs ON cs.classroom_id = cr.id
            ORDER BY cr.room_code
            """;
        return queryToMaps(sql);
    }

    // -------------------------------------------------------------------------
    // E. Helper – map ResultSet -> DTO
    // -------------------------------------------------------------------------

    private List<ClassSessionResponse> mapRows(ResultSet rs) throws SQLException {
        List<ClassSessionResponse> list = new ArrayList<>();
        while (rs.next()) {
            ClassSessionResponse r = new ClassSessionResponse();
            r.setId(rs.getLong("id"));
            r.setClassId(rs.getLong("class_id"));
            r.setClassCode(rs.getString("class_code"));
            r.setClassName(rs.getString("class_name"));
            r.setClassSubjectId(rs.getLong("class_subject_id"));
            r.setSubjectId(rs.getInt("subject_id"));
            r.setSubjectCode(rs.getString("subject_code"));
            r.setSubjectName(rs.getString("subject_name"));
            r.setInstructorId(rs.getLong("instructor_id"));
            r.setInstructorName(rs.getString("instructor_name"));
            r.setClassroomId(rs.getInt("classroom_id"));
            r.setRoomCode(rs.getString("room_code"));
            r.setRoomName(rs.getString("room_name"));
            r.setStatus(rs.getString("status"));
            r.setSessionNumber(rs.getInt("session_number"));
            r.setSeq(rs.getInt("seq"));
            r.setTopic(rs.getString("topic"));
            r.setCancelReason(rs.getString("cancel_reason"));

            Date sessionDate = rs.getDate("session_date");
            Time startTime   = rs.getTime("start_time");
            Time endTime     = rs.getTime("end_time");

            if (sessionDate != null) {
                String dateStr = sessionDate.toLocalDate().format(DATE_FMT);
                r.setSessionDate(dateStr);
                if (startTime != null) {
                    String timeStr = startTime.toLocalTime().format(TIME_FMT);
                    r.setStartTime(timeStr);
                    r.setStartDateTime(dateStr + "T" + timeStr + ":00");
                }
                if (endTime != null) {
                    String timeStr = endTime.toLocalTime().format(TIME_FMT);
                    r.setEndTime(timeStr);
                    r.setEndDateTime(dateStr + "T" + timeStr + ":00");
                }
            }
            list.add(r);
        }
        return list;
    }

    private List<java.util.Map<String, Object>> queryToMaps(String sql) throws SQLException {
        List<java.util.Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }
        }
        return result;
    }
}
