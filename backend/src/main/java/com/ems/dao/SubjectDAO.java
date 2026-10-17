package com.ems.dao;

import com.ems.config.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy cập bảng subjects (S2-05) và subject_lessons (S2-07). Kết quả trả về dạng Map để servlet đổi thẳng sang JSON:
 * { id, code, name, sessions, weight, outcome, status, openedClasses, programs: [{ code, name }] }.
 */
public class SubjectDAO {
    private static final String COLUMNS = "s.id, s.subject_code, s.subject_name, s.total_sessions, s.credit_weight, "
            + "s.learning_outcomes, s.status";

    /**
     * Tìm theo mã hoặc tên, lọc theo chương trình. size &lt;= 0 trả về tất cả (trang lộ trình cần đủ danh sách môn).
     */
    public Map<String, Object> search(String keyword, String programCode, int page, int size) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE s.deleted_at IS NULL");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (LOWER(s.subject_code) LIKE ? OR LOWER(s.subject_name) LIKE ?)");
            String kw = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            params.add(kw);
            params.add(kw);
        }
        if (programCode != null && !programCode.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM program_subjects ps JOIN programs p ON p.id = ps.program_id "
                    + "WHERE ps.subject_id = s.id AND UPPER(p.program_code) = UPPER(?))");
            params.add(programCode.trim());
        }
        try (Connection conn = DBConnection.getConnection()) {
            long total;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM subjects s" + where)) {
                bind(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            String sql = "SELECT " + COLUMNS + ", " + classCountSql(conn) + " AS opened FROM subjects s" + where
                    + " ORDER BY s.subject_code";
            List<Object> all = new ArrayList<>(params);
            if (size > 0) {
                sql += " LIMIT ? OFFSET ?";
                all.add(size);
                all.add((Math.max(page, 1) - 1) * size);
            }
            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bind(stmt, all);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) items.add(mapRow(rs));
                }
            }
            attachPrograms(conn, items);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", items);
            result.put("total", total);
            result.put("page", size > 0 ? Math.max(page, 1) : 1);
            result.put("pageSize", size > 0 ? size : items.size());
            return result;
        }
    }

    /** Môn đang dùng (không nằm trong thùng rác) theo mã, kèm số lớp và chương trình; không có thì null. */
    public Map<String, Object> findByCode(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT " + COLUMNS + ", " + classCountSql(conn)
                     + " AS opened FROM subjects s WHERE UPPER(s.subject_code) = UPPER(?) AND s.deleted_at IS NULL")) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                List<Map<String, Object>> one = new ArrayList<>(List.of(mapRow(rs)));
                attachPrograms(conn, one);
                return one.get(0);
            }
        }
    }

    /** Mã đã có ở môn khác, kể cả môn trong thùng rác (ràng buộc UNIQUE vẫn tính). */
    public boolean codeTaken(String code, Integer exceptId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM subjects WHERE UPPER(subject_code) = UPPER(?) AND id <> ?")) {
            stmt.setString(1, code);
            stmt.setInt(2, exceptId == null ? -1 : exceptId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean codeInTrash(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM subjects WHERE UPPER(subject_code) = UPPER(?) AND deleted_at IS NOT NULL")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int create(String code, String name, int sessions, BigDecimal weight, String outcome) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO subjects (subject_code, subject_name, total_sessions, credit_weight, learning_outcomes, status) "
                             + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setInt(3, sessions);
            stmt.setBigDecimal(4, weight);
            stmt.setString(5, outcome);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(int id, String code, String name, int sessions, BigDecimal weight, String outcome) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE subjects SET subject_code = ?, subject_name = ?, total_sessions = ?, credit_weight = ?, "
                             + "learning_outcomes = ? WHERE id = ?")) {
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setInt(3, sessions);
            stmt.setBigDecimal(4, weight);
            stmt.setString(5, outcome);
            stmt.setInt(6, id);
            stmt.executeUpdate();
        }
    }

    /** Số lớp đã học môn này (bảng class_subjects của S3); kể cả môn trong thùng rác. */
    public int countClasses(int subjectId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            if (!hasTable(conn, "class_subjects")) return 0;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT class_id) FROM class_subjects WHERE subject_id = ?")) {
                stmt.setInt(1, subjectId);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    /** Chuyển vào thùng rác. Môn vẫn nằm trong lộ trình nhưng bị ẩn, khôi phục thì hiện lại đúng chỗ cũ. */
    public boolean softDelete(int id, long actorId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE subjects SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? WHERE id = ? AND deleted_at IS NULL")) {
            stmt.setLong(1, actorId);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> listTrash() throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT s.id, s.subject_code, s.subject_name, s.deleted_at, u.full_name AS deleted_by_name "
                             + "FROM subjects s LEFT JOIN users u ON u.id = s.deleted_by "
                             + "WHERE s.deleted_at IS NOT NULL ORDER BY s.deleted_at DESC");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("code", rs.getString("subject_code"));
                item.put("name", rs.getString("subject_name"));
                item.put("deletedAt", rs.getTimestamp("deleted_at").toInstant().toString());
                item.put("deletedBy", rs.getString("deleted_by_name"));
                items.add(item);
            }
        }
        return items;
    }

    public boolean restore(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE subjects SET deleted_at = NULL, deleted_by = NULL WHERE id = ? AND deleted_at IS NOT NULL")) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Xoá vĩnh viễn môn trong thùng rác cùng buổi học, vị trí trong lộ trình và quan hệ tiên quyết. */
    public boolean purge(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM subjects WHERE id = ? AND deleted_at IS NOT NULL")) {
                    check.setInt(1, id);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM program_subject_prereqs WHERE subject_id = ? OR prereq_subject_id = ?")) {
                    stmt.setInt(1, id);
                    stmt.setInt(2, id);
                    stmt.executeUpdate();
                }
                for (String sql : new String[] {"DELETE FROM program_subjects WHERE subject_id = ?",
                        "DELETE FROM subject_lessons WHERE subject_id = ?", "DELETE FROM subjects WHERE id = ?"}) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ===== Buổi học (S2-07) =====

    /** Buổi học theo thứ tự: [{ order, topic, goal }]. */
    public List<Map<String, Object>> listLessons(int subjectId) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT session_order, topic, objectives FROM subject_lessons WHERE subject_id = ? ORDER BY session_order")) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("order", rs.getInt("session_order"));
                    item.put("topic", rs.getString("topic"));
                    item.put("goal", rs.getString("objectives") == null ? "" : rs.getString("objectives"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public int countLessons(int subjectId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM subject_lessons WHERE subject_id = ?")) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Thêm buổi vào cuối khi số buổi đã khai báo còn nhỏ hơn limit; trả false khi đã đủ. */
    public boolean addLesson(int subjectId, int limit, String topic, String goal) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int count = 0;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT session_order FROM subject_lessons WHERE subject_id = ? FOR UPDATE")) {
                    stmt.setInt(1, subjectId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) count = Math.max(count, rs.getInt(1));
                    }
                }
                if (count >= limit) {
                    conn.rollback();
                    return false;
                }
                insertLesson(conn, subjectId, count + 1, topic, goal);
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean updateLesson(int subjectId, int order, String topic, String goal) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE subject_lessons SET topic = ?, objectives = ? WHERE subject_id = ? AND session_order = ?")) {
            stmt.setString(1, topic);
            stmt.setString(2, goal);
            stmt.setInt(3, subjectId);
            stmt.setInt(4, order);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Xoá một buổi và dồn số thứ tự các buổi sau lên một. */
    public boolean deleteLesson(int subjectId, int order) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deleted;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM subject_lessons WHERE subject_id = ? AND session_order = ?")) {
                    stmt.setInt(1, subjectId);
                    stmt.setInt(2, order);
                    deleted = stmt.executeUpdate();
                }
                if (deleted > 0) {
                    // Đổi dấu rồi mới trừ để không đụng ràng buộc UNIQUE (subject_id, session_order) giữa chừng.
                    try (PreparedStatement neg = conn.prepareStatement(
                            "UPDATE subject_lessons SET session_order = -session_order WHERE subject_id = ? AND session_order > ?");
                         PreparedStatement back = conn.prepareStatement(
                                 "UPDATE subject_lessons SET session_order = -session_order - 1 WHERE subject_id = ? AND session_order < 0")) {
                        neg.setInt(1, subjectId);
                        neg.setInt(2, order);
                        neg.executeUpdate();
                        back.setInt(1, subjectId);
                        back.executeUpdate();
                    }
                }
                conn.commit();
                return deleted > 0;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Thay toàn bộ buổi học của môn bằng danh sách mới (nhân bản từ môn khác). */
    public void replaceLessons(int subjectId, List<Map<String, Object>> lessons) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM subject_lessons WHERE subject_id = ?")) {
                    stmt.setInt(1, subjectId);
                    stmt.executeUpdate();
                }
                for (int i = 0; i < lessons.size(); i++) {
                    insertLesson(conn, subjectId, i + 1, (String) lessons.get(i).get("topic"), (String) lessons.get(i).get("goal"));
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void insertLesson(Connection conn, int subjectId, int order, String topic, String goal) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO subject_lessons (subject_id, session_order, topic, objectives) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, subjectId);
            stmt.setInt(2, order);
            stmt.setString(3, topic);
            stmt.setString(4, goal);
            stmt.executeUpdate();
        }
    }

    // ===== Tiện ích =====

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getInt("id"));
        item.put("code", rs.getString("subject_code"));
        item.put("name", rs.getString("subject_name"));
        item.put("sessions", rs.getInt("total_sessions"));
        item.put("weight", rs.getBigDecimal("credit_weight").stripTrailingZeros());
        item.put("outcome", rs.getString("learning_outcomes") == null ? "" : rs.getString("learning_outcomes"));
        item.put("status", rs.getString("status"));
        item.put("openedClasses", rs.getInt("opened"));
        return item;
    }

    /** Gắn danh sách chương trình đang dùng mỗi môn (cột "Dùng trong" của S2-05). */
    private static void attachPrograms(Connection conn, List<Map<String, Object>> items) throws SQLException {
        if (items.isEmpty()) return;
        Map<Integer, List<Map<String, Object>>> byId = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            List<Map<String, Object>> programs = new ArrayList<>();
            item.put("programs", programs);
            byId.put((Integer) item.get("id"), programs);
        }
        String marks = String.join(",", Collections.nCopies(byId.size(), "?"));
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT ps.subject_id, p.program_code, p.program_name FROM program_subjects ps "
                        + "JOIN programs p ON p.id = ps.program_id WHERE p.deleted_at IS NULL AND ps.subject_id IN (" + marks + ") "
                        + "ORDER BY p.program_name")) {
            bind(stmt, new ArrayList<>(byId.keySet()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> program = new LinkedHashMap<>();
                    program.put("code", rs.getString("program_code"));
                    program.put("name", rs.getString("program_name"));
                    byId.get(rs.getInt("subject_id")).add(program);
                }
            }
        }
    }

    /** Bảng class_subjects thuộc S3, CSDL chưa có thì coi như chưa mở lớp nào. */
    private static String classCountSql(Connection conn) throws SQLException {
        return hasTable(conn, "class_subjects")
                ? "(SELECT COUNT(DISTINCT cs.class_id) FROM class_subjects cs WHERE cs.subject_id = s.id)"
                : "0";
    }

    private static boolean hasTable(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        for (String name : new String[] {table, table.toUpperCase(java.util.Locale.ROOT)}) {
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, name, new String[] {"TABLE"})) {
                if (rs.next()) return true;
            }
        }
        return false;
    }

    static void bind(PreparedStatement stmt, List<?> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
    }
}
