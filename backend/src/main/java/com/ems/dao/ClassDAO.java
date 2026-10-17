package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.exception.ApiException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp học (S3-05 mở lớp, S3-07 danh sách), ghi danh (S3-06), phân công giảng viên và trợ giảng theo môn (S3-08),
 * phòng mặc định của lớp (S3-09). Lớp trả về dạng Map { id, code, name, programCode, programName, startDate, endDate,
 * maxCapacity, enrolled, studyMode, status, room, instructor, missingInstructors, sessionsDone, sessionsTotal }.
 */
public class ClassDAO {
    /** Bộ lọc S3-07. from, to lọc theo ngày khai giảng, tính cả hai ngày. */
    public record Filter(String keyword, String status, String programCode, Long instructorId, LocalDate from, LocalDate to) {}

    private static final String SELECT = "SELECT c.id, c.class_code, c.class_name, c.start_date, c.end_date, c.max_capacity, "
            + "c.study_mode, c.status, p.program_code, p.program_name, "
            + "r.id AS room_id, r.room_code, r.room_name, r.capacity AS room_capacity, r.room_type, "
            + "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id AND e.status = 'ENROLLED') AS enrolled, "
            + "(SELECT COALESCE(SUM(s.total_sessions), 0) FROM class_subjects cs JOIN subjects s ON s.id = cs.subject_id "
            + "WHERE cs.class_id = c.id) AS sessions_total, "
            + "(SELECT COUNT(*) FROM class_sessions x WHERE x.class_id = c.id AND x.status = 'COMPLETED') AS sessions_done, "
            + "(SELECT COUNT(*) FROM class_subjects cs WHERE cs.class_id = c.id AND cs.primary_instructor_id IS NULL) AS missing_instructors, "
            + "(SELECT u.full_name FROM class_subjects cs JOIN users u ON u.id = cs.primary_instructor_id WHERE cs.class_id = c.id "
            + "ORDER BY cs.order_index, cs.id LIMIT 1) AS instructor_name "
            + "FROM classes c LEFT JOIN programs p ON p.id = c.program_id LEFT JOIN classrooms r ON r.id = c.default_classroom_id ";

    // ===== S3-05: mở lớp =====

    public boolean codeExists(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM classes WHERE class_code = ?")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Tạo lớp trạng thái PLANNING và sao chép môn của chương trình theo thứ tự lộ trình. */
    public long create(String code, String name, int programId, LocalDate start, int maxCapacity, String mode, Integer roomId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO classes (class_code, class_name, program_id, start_date, max_capacity, study_mode, default_classroom_id, status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PLANNING')", Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, code);
                    stmt.setString(2, name);
                    stmt.setInt(3, programId);
                    stmt.setDate(4, Date.valueOf(start));
                    stmt.setInt(5, maxCapacity);
                    stmt.setString(6, mode);
                    stmt.setObject(7, roomId);
                    stmt.executeUpdate();
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        rs.next();
                        id = rs.getLong(1);
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO class_subjects (class_id, subject_id, order_index, status) "
                                + "SELECT ?, ps.subject_id, ps.order_index, 'NOT_STARTED' FROM program_subjects ps "
                                + "JOIN subjects s ON s.id = ps.subject_id WHERE ps.program_id = ? AND s.deleted_at IS NULL")) {
                    stmt.setLong(1, id);
                    stmt.setInt(2, programId);
                    if (stmt.executeUpdate() == 0) {
                        throw new ApiException(400, "PROGRAM_HAS_NO_SUBJECTS", "Chương trình chưa có môn học nào, hãy xếp lộ trình trước khi mở lớp.");
                    }
                }
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Đổi trạng thái khi lớp vẫn đang ở trạng thái from, tránh hai người cùng đổi. */
    public boolean changeStatus(long id, String from, String to) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE classes SET status = ?, end_date = CASE WHEN ? IN ('FINISHED', 'CANCELLED') THEN CURRENT_DATE ELSE end_date END, "
                             + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = ?")) {
            stmt.setString(1, to);
            stmt.setString(2, to);
            stmt.setLong(3, id);
            stmt.setString(4, from);
            return stmt.executeUpdate() == 1;
        }
    }

    public void setRoom(long id, Integer roomId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE classes SET default_classroom_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            stmt.setObject(1, roomId);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    // ===== S3-07: danh sách lớp =====

    /** Danh sách có bộ lọc; summary đếm số lớp theo trạng thái với cùng bộ lọc trừ trạng thái (chỉ số nhanh). */
    public Map<String, Object> search(Filter f, int page, int size) throws SQLException {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (f.keyword() != null && !f.keyword().isBlank()) {
            where.append(" AND (LOWER(c.class_code) LIKE ? OR LOWER(c.class_name) LIKE ?)");
            String kw = "%" + f.keyword().trim().toLowerCase(java.util.Locale.ROOT) + "%";
            params.add(kw);
            params.add(kw);
        }
        if (f.programCode() != null) {
            where.append(" AND p.program_code = ?");
            params.add(f.programCode());
        }
        if (f.instructorId() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM class_subjects cs WHERE cs.class_id = c.id AND cs.primary_instructor_id = ?)");
            params.add(f.instructorId());
        }
        if (f.from() != null) {
            where.append(" AND c.start_date >= ?");
            params.add(Date.valueOf(f.from()));
        }
        if (f.to() != null) {
            where.append(" AND c.start_date <= ?");
            params.add(Date.valueOf(f.to()));
        }
        String from = "FROM classes c LEFT JOIN programs p ON p.id = c.program_id ";
        try (Connection conn = DBConnection.getConnection()) {
            Map<String, Object> summary = new LinkedHashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT c.status, COUNT(*) " + from + where + " GROUP BY c.status")) {
                SubjectDAO.bind(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) summary.put(rs.getString(1), rs.getLong(2));
                }
            }
            if (f.status() != null) {
                where.append(" AND c.status = ?");
                params.add(f.status());
            }
            long total;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) " + from + where)) {
                SubjectDAO.bind(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<Object> all = new ArrayList<>(params);
            all.add(size);
            all.add((page - 1) * size);
            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT + where + " ORDER BY c.start_date DESC, c.id DESC LIMIT ? OFFSET ?")) {
                SubjectDAO.bind(stmt, all);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) items.add(mapRow(rs));
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", items);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", size);
            result.put("summary", summary);
            return result;
        }
    }

    public Map<String, Object> findByCode(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE c.class_code = ?")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ===== S3-08: phân công theo môn =====

    /** Môn của lớp theo thứ tự, mỗi môn có instructor { id, name } và assistants [{ id, name }]. */
    public List<Map<String, Object>> subjects(long classId) throws SQLException {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT cs.id, s.subject_code, s.subject_name, s.total_sessions, cs.status, cs.primary_instructor_id, u.full_name "
                            + "FROM class_subjects cs JOIN subjects s ON s.id = cs.subject_id LEFT JOIN users u ON u.id = cs.primary_instructor_id "
                            + "WHERE cs.class_id = ? ORDER BY cs.order_index, cs.id")) {
                stmt.setLong(1, classId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", rs.getLong("id"));
                        item.put("code", rs.getString("subject_code"));
                        item.put("name", rs.getString("subject_name"));
                        item.put("sessions", rs.getInt("total_sessions"));
                        item.put("status", rs.getString("status"));
                        long instructor = rs.getLong("primary_instructor_id");
                        if (!rs.wasNull()) item.put("instructor", person(instructor, rs.getString("full_name")));
                        item.put("assistants", new ArrayList<Map<String, Object>>());
                        byId.put(rs.getLong("id"), item);
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT a.class_subject_id, u.id, u.full_name FROM class_subject_assistants a JOIN class_subjects cs ON cs.id = a.class_subject_id "
                            + "JOIN users u ON u.id = a.ta_user_id WHERE cs.class_id = ? ORDER BY u.full_name")) {
                stmt.setLong(1, classId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> tas = (List<Map<String, Object>>) byId.get(rs.getLong(1)).get("assistants");
                        tas.add(person(rs.getLong(2), rs.getString(3)));
                    }
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    /** Id dòng class_subjects của một môn trong lớp, null nếu môn không thuộc lớp. */
    public Long classSubjectId(long classId, String subjectCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT cs.id FROM class_subjects cs JOIN subjects s ON s.id = cs.subject_id WHERE cs.class_id = ? AND s.subject_code = ?")) {
            stmt.setLong(1, classId);
            stmt.setString(2, subjectCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    /** Ghi đè giảng viên chính và danh sách trợ giảng của một môn trong lớp. */
    public void assign(long classSubjectId, Long instructorId, List<Long> assistantIds) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE class_subjects SET primary_instructor_id = ? WHERE id = ?")) {
                    stmt.setObject(1, instructorId);
                    stmt.setLong(2, classSubjectId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM class_subject_assistants WHERE class_subject_id = ?")) {
                    stmt.setLong(1, classSubjectId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO class_subject_assistants (class_subject_id, ta_user_id) VALUES (?, ?)")) {
                    for (long ta : assistantIds) {
                        stmt.setLong(1, classSubjectId);
                        stmt.setLong(2, ta);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Tài khoản đang hoạt động có vai trò roleCode (INSTRUCTOR hoặc TA), kèm số lớp đang chạy người đó tham gia. */
    public List<Map<String, Object>> staff(String roleCode) throws SQLException {
        String sql = "SELECT u.id, u.full_name, u.email, (SELECT COUNT(DISTINCT cs.class_id) FROM class_subjects cs "
                + "JOIN classes c ON c.id = cs.class_id LEFT JOIN class_subject_assistants a ON a.class_subject_id = cs.id "
                + "WHERE c.status IN ('PLANNING', 'IN_PROGRESS') AND (cs.primary_instructor_id = u.id OR a.ta_user_id = u.id)) AS open_classes "
                + "FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id "
                + "WHERE r.role_code = ? AND u.status = 'ACTIVE' AND u.deleted_at IS NULL ORDER BY u.full_name";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = person(rs.getLong("id"), rs.getString("full_name"));
                    item.put("email", rs.getString("email"));
                    item.put("openClasses", rs.getInt("open_classes"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public boolean hasActiveRole(long userId, String roleCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id "
                             + "WHERE u.id = ? AND r.role_code = ? AND u.status = 'ACTIVE' AND u.deleted_at IS NULL")) {
            stmt.setLong(1, userId);
            stmt.setString(2, roleCode);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Lớp của tôi: lớp có người này dạy chính hoặc trợ giảng ít nhất một môn, kèm các môn và vai trò của họ. */
    public List<Map<String, Object>> classesOf(long userId) throws SQLException {
        String sql = "SELECT DISTINCT c.class_code FROM classes c JOIN class_subjects cs ON cs.class_id = c.id "
                + "LEFT JOIN class_subject_assistants a ON a.class_subject_id = cs.id "
                + "WHERE cs.primary_instructor_id = ? OR a.ta_user_id = ?";
        List<String> codes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) codes.add(rs.getString(1));
            }
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (String code : codes) {
            Map<String, Object> k = findByCode(code);
            List<Map<String, Object>> mine = new ArrayList<>();
            for (Map<String, Object> s : subjects((long) k.get("id"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> instructor = (Map<String, Object>) s.get("instructor");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tas = (List<Map<String, Object>>) s.get("assistants");
                String role = instructor != null && Long.valueOf(userId).equals(instructor.get("id")) ? "INSTRUCTOR"
                        : tas.stream().anyMatch(t -> Long.valueOf(userId).equals(t.get("id"))) ? "TA" : null;
                if (role == null) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("code", s.get("code"));
                m.put("name", s.get("name"));
                m.put("role", role);
                mine.add(m);
            }
            k.put("mySubjects", mine);
            items.add(k);
        }
        items.sort((a, b) -> ((String) b.get("startDate")).compareTo((String) a.get("startDate")));
        return items;
    }

    // ===== S3-06: ghi danh =====

    /**
     * Ghi danh nhiều học viên trong một giao dịch, khoá dòng lớp để hai người ghi danh cùng lúc không vượt sĩ số.
     * Cả nhóm bị từ chối nếu có học viên đã trong lớp, không tồn tại, không còn học, hoặc lớp không đủ chỗ.
     */
    public int enroll(long classId, List<String> studentCodes, long actorId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int max;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT max_capacity, status FROM classes WHERE id = ? FOR UPDATE")) {
                    stmt.setLong(1, classId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new ResourceNotFoundException("Không tìm thấy lớp.");
                        String status = rs.getString("status");
                        if (!"PLANNING".equals(status) && !"IN_PROGRESS".equals(status)) {
                            throw new ConflictException("CLASS_NOT_OPEN", "Lớp đã kết thúc hoặc đã huỷ, không ghi danh thêm được.");
                        }
                        max = rs.getInt("max_capacity");
                    }
                }
                String in = String.join(", ", Collections.nCopies(studentCodes.size(), "?"));
                Map<String, Long> ids = new LinkedHashMap<>();
                List<String> notStudying = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT s.id, s.student_code, s.enrollment_status, u.full_name "
                        + "FROM students s JOIN users u ON u.id = s.user_id WHERE u.deleted_at IS NULL AND s.student_code IN (" + in + ")")) {
                    SubjectDAO.bind(stmt, studentCodes);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ids.put(rs.getString("student_code"), rs.getLong("id"));
                            if (!"STUDYING".equals(rs.getString("enrollment_status"))) notStudying.add(rs.getString("full_name"));
                        }
                    }
                }
                List<String> missing = studentCodes.stream().filter(c -> !ids.containsKey(c)).toList();
                if (!missing.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy học viên " + String.join(", ", missing) + ".");
                if (!notStudying.isEmpty()) {
                    throw new ConflictException("STUDENT_NOT_STUDYING", "Học viên không ở trạng thái Đang học: " + String.join(", ", notStudying) + ".");
                }
                List<String> already = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT u.full_name FROM enrollments e JOIN students s ON s.id = e.student_id "
                        + "JOIN users u ON u.id = s.user_id WHERE e.class_id = ? AND s.student_code IN (" + in + ")")) {
                    List<Object> params = new ArrayList<>();
                    params.add(classId);
                    params.addAll(studentCodes);
                    SubjectDAO.bind(stmt, params);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) already.add(rs.getString(1));
                    }
                }
                if (!already.isEmpty()) {
                    throw new ConflictException("ALREADY_ENROLLED", "Đã có trong danh sách lớp: " + String.join(", ", already) + ".");
                }
                int now;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM enrollments WHERE class_id = ? AND status = 'ENROLLED'")) {
                    stmt.setLong(1, classId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        now = rs.getInt(1);
                    }
                }
                if (now + ids.size() > max) {
                    throw new ConflictException("CLASS_FULL", "Lớp chỉ còn " + Math.max(0, max - now) + " chỗ (" + now + "/" + max
                            + "), không ghi danh được " + ids.size() + " học viên.");
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO enrollments (class_id, student_id, enrollment_date, status, enrolled_by) VALUES (?, ?, ?, 'ENROLLED', ?)")) {
                    for (long studentId : ids.values()) {
                        stmt.setLong(1, classId);
                        stmt.setLong(2, studentId);
                        stmt.setDate(3, Date.valueOf(LocalDate.now()));
                        stmt.setLong(4, actorId);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                return ids.size();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Danh sách học viên của lớp: ai, ngày ghi danh, người thực hiện. */
    public List<Map<String, Object>> enrollments(long classId) throws SQLException {
        String sql = "SELECT s.student_code, u.full_name, u.phone, e.enrollment_date, e.status, b.full_name AS by_name "
                + "FROM enrollments e JOIN students s ON s.id = e.student_id JOIN users u ON u.id = s.user_id "
                + "LEFT JOIN users b ON b.id = e.enrolled_by WHERE e.class_id = ? ORDER BY u.full_name";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("studentCode", rs.getString("student_code"));
                    item.put("name", rs.getString("full_name"));
                    item.put("phone", rs.getString("phone"));
                    item.put("enrolledAt", rs.getDate("enrollment_date").toLocalDate().toString());
                    item.put("status", rs.getString("status"));
                    item.put("by", rs.getString("by_name"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private static Map<String, Object> person(long id, String name) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", id);
        p.put("name", name);
        return p;
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("code", rs.getString("class_code"));
        item.put("name", rs.getString("class_name"));
        item.put("programCode", rs.getString("program_code"));
        item.put("programName", rs.getString("program_name"));
        item.put("startDate", rs.getDate("start_date").toLocalDate().toString());
        Date end = rs.getDate("end_date");
        item.put("endDate", end == null ? null : end.toLocalDate().toString());
        item.put("maxCapacity", rs.getInt("max_capacity"));
        item.put("enrolled", rs.getInt("enrolled"));
        item.put("studyMode", rs.getString("study_mode"));
        item.put("status", rs.getString("status"));
        int roomId = rs.getInt("room_id");
        if (!rs.wasNull()) {
            Map<String, Object> room = new LinkedHashMap<>();
            room.put("id", roomId);
            room.put("code", rs.getString("room_code"));
            room.put("name", rs.getString("room_name"));
            room.put("type", rs.getString("room_type"));
            int capacity = rs.getInt("room_capacity");
            room.put("capacity", rs.wasNull() ? null : capacity);
            item.put("room", room);
        }
        item.put("instructor", rs.getString("instructor_name"));
        item.put("missingInstructors", rs.getInt("missing_instructors"));
        item.put("sessionsDone", rs.getInt("sessions_done"));
        item.put("sessionsTotal", rs.getInt("sessions_total"));
        return item;
    }
}
