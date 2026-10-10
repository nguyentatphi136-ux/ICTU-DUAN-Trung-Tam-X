package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.TrainingProgram;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) cho Chương trình đào tạo (TrainingProgram)
 * Thao tác với bảng programs và liên kết bảng classes trong CSDL MySQL / H2
 */
public class TrainingProgramDAO {

    /**
     * Tạo mới một chương trình đào tạo
     */
    public TrainingProgram create(TrainingProgram program) throws SQLException {
        String sql = "INSERT INTO programs (program_code, program_name, description, duration, standard_tuition, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, program.getCode());
            stmt.setString(2, program.getName());
            stmt.setString(3, program.getDescription());
            stmt.setInt(4, program.getDuration() != null ? program.getDuration() : 60);
            stmt.setBigDecimal(5, program.getStandardTuition() != null ? program.getStandardTuition() : BigDecimal.ZERO);
            stmt.setString(6, program.getStatus() != null ? program.getStatus() : "ACTIVE");

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    program.setId(rs.getInt(1));
                }
            }
        }
        return findById(program.getId());
    }

    /**
     * Tìm chương trình đào tạo theo ID
     */
    public TrainingProgram findById(int id) throws SQLException {
        String sql = "SELECT id, program_code, program_name, description, duration, standard_tuition, status, created_at, updated_at "
                + "FROM programs WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm chương trình đào tạo theo Code
     */
    public TrainingProgram findByCode(String code) throws SQLException {
        if (code == null) return null;
        String sql = "SELECT id, program_code, program_name, description, duration, standard_tuition, status, created_at, updated_at "
                + "FROM programs WHERE UPPER(program_code) = UPPER(?) AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Kiểm tra mã code đã tồn tại chưa
     */
    public boolean existsByCode(String code) throws SQLException {
        if (code == null) return false;
        String sql = "SELECT 1 FROM programs WHERE UPPER(program_code) = UPPER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Kiểm tra mã code đã tồn tại ở chương trình khác không (khi update)
     */
    public boolean existsByCodeAndNotId(String code, int id) throws SQLException {
        if (code == null) return false;
        String sql = "SELECT 1 FROM programs WHERE UPPER(program_code) = UPPER(?) AND id <> ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            stmt.setInt(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tìm kiếm, lọc và phân trang danh sách chương trình đào tạo
     */
    public Map<String, Object> search(String keyword, String status, int page, int pageSize) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        StringBuilder whereSql = new StringBuilder(" WHERE deleted_at IS NULL ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            whereSql.append(" AND (program_code LIKE ? OR program_name LIKE ?) ");
            params.add(kw);
            params.add(kw);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            whereSql.append(" AND status = ? ");
            params.add(status.trim().toUpperCase(java.util.Locale.ROOT));
        }

        int totalCount = 0;
        String countSql = "SELECT COUNT(*) FROM programs " + whereSql;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement countStmt = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                countStmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    totalCount = rs.getInt(1);
                }
            }
        }

        List<TrainingProgram> items = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String dataSql = "SELECT id, program_code, program_name, description, duration, standard_tuition, status, created_at, updated_at "
                + "FROM programs " + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement dataStmt = conn.prepareStatement(dataSql)) {
            int paramIndex = 1;
            for (Object param : params) {
                dataStmt.setObject(paramIndex++, param);
            }
            dataStmt.setInt(paramIndex++, pageSize);
            dataStmt.setInt(paramIndex, offset);

            try (ResultSet rs = dataStmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", (long) totalCount);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * Cập nhật thông tin chương trình đào tạo
     */
    public TrainingProgram update(TrainingProgram program) throws SQLException {
        String sql = "UPDATE programs SET program_code = ?, program_name = ?, description = ?, "
                + "duration = ?, standard_tuition = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, program.getCode());
            stmt.setString(2, program.getName());
            stmt.setString(3, program.getDescription());
            stmt.setInt(4, program.getDuration());
            stmt.setBigDecimal(5, program.getStandardTuition());
            stmt.setString(6, program.getStatus());
            stmt.setInt(7, program.getId());

            int updated = stmt.executeUpdate();
            if (updated == 0) return null;
        }
        return findById(program.getId());
    }

    /**
     * Ngừng áp dụng chương trình đào tạo (deactivate)
     */
    public boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE programs SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Chuyển chương trình vào thùng rác (xoá mềm). Dữ liệu và lộ trình môn vẫn còn, khôi phục được.
     */
    public boolean delete(int id) throws SQLException {
        return delete(id, null);
    }

    public boolean delete(int id, Long actorId) throws SQLException {
        String sql = "UPDATE programs SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, actorId);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Mã đang thuộc một chương trình trong thùng rác (vẫn chiếm ràng buộc UNIQUE nên không tạo trùng được). */
    public boolean isCodeInTrash(String code) throws SQLException {
        if (code == null) return false;
        String sql = "SELECT 1 FROM programs WHERE UPPER(program_code) = UPPER(?) AND deleted_at IS NOT NULL LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Chương trình trong thùng rác, mới xoá trước. */
    public List<Map<String, Object>> listTrashed() throws SQLException {
        String sql = "SELECT p.id, p.program_code, p.program_name, p.deleted_at, u.full_name AS deleted_by_name "
                + "FROM programs p LEFT JOIN users u ON u.id = p.deleted_by "
                + "WHERE p.deleted_at IS NOT NULL ORDER BY p.deleted_at DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("code", rs.getString("program_code"));
                item.put("name", rs.getString("program_name"));
                item.put("deletedAt", rs.getTimestamp("deleted_at").toInstant().toString());
                item.put("deletedBy", rs.getString("deleted_by_name"));
                items.add(item);
            }
        }
        return items;
    }

    public boolean restore(int id) throws SQLException {
        String sql = "UPDATE programs SET deleted_at = NULL, deleted_by = NULL WHERE id = ? AND deleted_at IS NOT NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Xoá vĩnh viễn chương trình đang trong thùng rác. Lộ trình môn bị xoá theo (ON DELETE CASCADE);
     * còn lớp học tham chiếu thì CSDL chặn và ném SQLIntegrityConstraintViolationException.
     */
    public boolean purge(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT 1 FROM programs WHERE id = ? AND deleted_at IS NOT NULL")) {
                    check.setInt(1, id);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM programs WHERE id = ?")) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Lộ trình môn của chương trình theo order_index (S2-06). */
    public List<Map<String, Object>> listSubjects(int programId) throws SQLException {
        String sql = "SELECT s.subject_code, s.subject_name, s.total_sessions, ps.order_index "
                + "FROM program_subjects ps JOIN subjects s ON s.id = ps.subject_id "
                + "WHERE ps.program_id = ? AND s.deleted_at IS NULL ORDER BY ps.order_index, ps.id";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", rs.getString("subject_code"));
                    item.put("name", rs.getString("subject_name"));
                    item.put("sessions", rs.getInt("total_sessions"));
                    item.put("order", rs.getInt("order_index"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * Lưu thứ tự môn mới (S2-06, kéo thả). subjectCodes phải gồm đúng các môn đang có trong lộ trình,
     * không thiếu không thừa, để hai người sửa cùng lúc không làm mất môn của nhau.
     *
     * @throws IllegalArgumentException danh sách không khớp lộ trình hiện tại
     */
    public void reorderSubjects(int programId, List<String> subjectCodes) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Integer> current = new LinkedHashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT ps.id, s.subject_code FROM program_subjects ps JOIN subjects s ON s.id = ps.subject_id "
                                + "WHERE ps.program_id = ? FOR UPDATE")) {
                    stmt.setInt(1, programId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) current.put(rs.getString("subject_code").toUpperCase(java.util.Locale.ROOT), rs.getInt("id"));
                    }
                }
                List<String> codes = subjectCodes.stream().map(c -> c.trim().toUpperCase(java.util.Locale.ROOT)).toList();
                if (codes.size() != current.size() || !current.keySet().equals(new java.util.HashSet<>(codes))) {
                    throw new IllegalArgumentException("Danh sách môn không khớp lộ trình hiện tại. Hãy tải lại trang rồi thử lại.");
                }
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE program_subjects SET order_index = ? WHERE id = ?")) {
                    for (int i = 0; i < codes.size(); i++) {
                        stmt.setInt(1, i + 1);
                        stmt.setInt(2, current.get(codes.get(i)));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Đếm số lượng lớp học ĐANG CHẠY gắn với chương trình này.
     * Trạng thái đang chạy: IN_PROGRESS, RUNNING, ACTIVE
     */
    public int countRunningClassesByProgramId(int programId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM classes WHERE program_id = ? "
                + "AND UPPER(status) IN ('IN_PROGRESS', 'RUNNING', 'ACTIVE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Đếm tổng số lượng lớp học gắn với chương trình này
     */
    public int countTotalClassesByProgramId(int programId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM classes WHERE program_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private TrainingProgram mapRow(ResultSet rs) throws SQLException {
        TrainingProgram program = new TrainingProgram();
        program.setId(rs.getInt("id"));
        program.setCode(rs.getString("program_code"));
        program.setName(rs.getString("program_name"));
        program.setDescription(rs.getString("description"));
        program.setDuration(rs.getInt("duration"));
        program.setStandardTuition(rs.getBigDecimal("standard_tuition"));
        program.setStatus(rs.getString("status"));
        program.setCreatedAt(rs.getTimestamp("created_at"));
        program.setUpdatedAt(rs.getTimestamp("updated_at"));
        return program;
    }
}
