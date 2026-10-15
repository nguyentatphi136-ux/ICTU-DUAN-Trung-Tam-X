package com.ems.dao;

import com.ems.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy cập bảng leads (S2-08, S2-09, S2-11) và lead_assignments (S2-10, lịch sử phân công).
 * Lead trả về dạng Map: { id, name, phone, email, source, programCode, programName, status, ownerId, ownerName, note, createdAt }.
 * Nhãn tiếng Việt của status và source do LeadService gắn thêm.
 */
public class LeadDAO {
    /** Bộ lọc S2-11. owner: null là mọi người, 0 là chưa phân công, số dương là id tư vấn viên. */
    public record Filter(String keyword, String status, String source, Long owner, LocalDate from, LocalDate to) {}

    private static final String SELECT = "SELECT l.id, l.full_name, l.phone, l.email, l.source, l.status, l.notes, l.created_at, "
            + "l.assigned_counselor_id, u.full_name AS owner_name, p.program_code, p.program_name "
            + "FROM leads l LEFT JOIN users u ON u.id = l.assigned_counselor_id "
            + "LEFT JOIN programs p ON p.id = l.interested_program_id ";

    /** onlyOwner khác null: tư vấn viên chỉ thấy lead được giao cho mình. */
    public Map<String, Object> search(Filter f, Long onlyOwner, int page, int size) throws SQLException {
        StringBuilder where = new StringBuilder("WHERE l.deleted_at IS NULL");
        List<Object> params = new ArrayList<>();
        if (onlyOwner != null) {
            where.append(" AND l.assigned_counselor_id = ?");
            params.add(onlyOwner);
        }
        if (f.keyword() != null && !f.keyword().isBlank()) {
            String kw = f.keyword().trim().toLowerCase(java.util.Locale.ROOT);
            String digits = kw.replaceAll("\\D", "");
            // Tìm theo số điện thoại khi gõ từ 3 chữ số trở lên, tránh gõ "1" là ra cả danh sách.
            if (digits.length() >= 3) {
                where.append(" AND (LOWER(l.full_name) LIKE ? OR l.phone LIKE ?)");
                params.add("%" + kw + "%");
                params.add("%" + digits + "%");
            } else {
                where.append(" AND LOWER(l.full_name) LIKE ?");
                params.add("%" + kw + "%");
            }
        }
        if (f.status() != null) {
            where.append(" AND l.status = ?");
            params.add(f.status());
        }
        if (f.source() != null) {
            where.append(" AND l.source = ?");
            params.add(f.source());
        }
        if (f.owner() != null) {
            if (f.owner() == 0) {
                where.append(" AND l.assigned_counselor_id IS NULL");
            } else {
                where.append(" AND l.assigned_counselor_id = ?");
                params.add(f.owner());
            }
        }
        if (f.from() != null) {
            where.append(" AND l.created_at >= ?");
            params.add(Timestamp.valueOf(f.from().atStartOfDay()));
        }
        if (f.to() != null) {
            where.append(" AND l.created_at < ?");
            params.add(Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        }
        try (Connection conn = DBConnection.getConnection()) {
            long total;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM leads l " + where)) {
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
            try (PreparedStatement stmt = conn.prepareStatement(SELECT + where + " ORDER BY l.created_at DESC, l.id DESC LIMIT ? OFFSET ?")) {
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
            return result;
        }
    }

    public Map<String, Object> findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE l.id = ? AND l.deleted_at IS NULL")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Lead khác có cùng số điện thoại (cảnh báo trùng ở S2-09). */
    public List<Map<String, Object>> findByPhone(String phone, Long excludeId) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE l.phone = ? AND l.id <> ? AND l.deleted_at IS NULL ORDER BY l.created_at DESC")) {
            stmt.setString(1, phone);
            stmt.setLong(2, excludeId == null ? -1 : excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public long create(String name, String phone, String email, String source, Integer programId, Long ownerId,
                       String status, String note) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO leads (full_name, phone, email, source, interested_program_id, assigned_counselor_id, status, notes) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, email);
            stmt.setString(4, source);
            stmt.setObject(5, programId);
            stmt.setObject(6, ownerId);
            stmt.setString(7, status);
            stmt.setString(8, note);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(long id, String name, String phone, String email, String source, Integer programId,
                       String status, String note) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE leads SET full_name = ?, phone = ?, email = ?, source = ?, interested_program_id = ?, status = ?, "
                             + "notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, email);
            stmt.setString(4, source);
            stmt.setObject(5, programId);
            stmt.setString(6, status);
            stmt.setString(7, note);
            stmt.setLong(8, id);
            stmt.executeUpdate();
        }
    }

    /** Tư vấn viên đang hoạt động (vai trò ADMISSIONS) kèm số lead chưa chốt đang giữ, để chia việc đều (S2-10). */
    public List<Map<String, Object>> counselors() throws SQLException {
        String sql = "SELECT u.id, u.full_name, u.email, "
                + "(SELECT COUNT(*) FROM leads l WHERE l.assigned_counselor_id = u.id AND l.deleted_at IS NULL "
                + "AND l.status NOT IN ('WON', 'REJECTED')) AS open_leads "
                + "FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id "
                + "WHERE r.role_code = 'ADMISSIONS' AND u.status = 'ACTIVE' AND u.deleted_at IS NULL ORDER BY u.full_name";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rs.getLong("id"));
                item.put("name", rs.getString("full_name"));
                item.put("email", rs.getString("email"));
                item.put("openLeads", rs.getInt("open_leads"));
                items.add(item);
            }
        }
        return items;
    }

    public boolean isActiveCounselor(long userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id "
                             + "WHERE u.id = ? AND r.role_code = 'ADMISSIONS' AND u.status = 'ACTIVE' AND u.deleted_at IS NULL")) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Giao các lead cho một tư vấn viên và ghi lịch sử (ai giao, từ ai sang ai, lúc nào, ghi chú).
     * Lead đã đúng người phụ trách thì bỏ qua. Trả về số lead thực sự đổi người.
     */
    public int assign(List<Long> leadIds, long counselorId, long actorId, String note) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement read = conn.prepareStatement(
                         "SELECT assigned_counselor_id FROM leads WHERE id = ? AND deleted_at IS NULL FOR UPDATE");
                 PreparedStatement update = conn.prepareStatement(
                         "UPDATE leads SET assigned_counselor_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?");
                 PreparedStatement log = conn.prepareStatement(
                         "INSERT INTO lead_assignments (lead_id, from_counselor_id, to_counselor_id, assigned_by, note) VALUES (?, ?, ?, ?, ?)")) {
                int changed = 0;
                for (long id : leadIds) {
                    read.setLong(1, id);
                    Long from;
                    try (ResultSet rs = read.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Lead " + id + " không tồn tại hoặc đã bị xoá.");
                        from = rs.getObject(1) == null ? null : rs.getLong(1);
                    }
                    if (from != null && from == counselorId) continue;
                    update.setLong(1, counselorId);
                    update.setLong(2, id);
                    update.executeUpdate();
                    log.setLong(1, id);
                    log.setObject(2, from);
                    log.setLong(3, counselorId);
                    log.setLong(4, actorId);
                    log.setString(5, note);
                    log.executeUpdate();
                    changed++;
                }
                conn.commit();
                return changed;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Lịch sử chuyển giao của một lead, mới nhất trước. */
    public List<Map<String, Object>> assignments(long leadId) throws SQLException {
        String sql = "SELECT a.created_at, a.note, f.full_name AS from_name, t.full_name AS to_name, b.full_name AS by_name "
                + "FROM lead_assignments a LEFT JOIN users f ON f.id = a.from_counselor_id "
                + "LEFT JOIN users t ON t.id = a.to_counselor_id LEFT JOIN users b ON b.id = a.assigned_by "
                + "WHERE a.lead_id = ? ORDER BY a.created_at DESC, a.id DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, leadId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("at", rs.getTimestamp("created_at").toLocalDateTime().toString());
                    item.put("from", rs.getString("from_name"));
                    item.put("to", rs.getString("to_name"));
                    item.put("by", rs.getString("by_name"));
                    item.put("note", rs.getString("note"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public boolean softDelete(long id, long actorId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE leads SET deleted_at = CURRENT_TIMESTAMP, deleted_by = ? WHERE id = ? AND deleted_at IS NULL")) {
            stmt.setLong(1, actorId);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> listTrash() throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT l.id, l.full_name, l.phone, l.deleted_at, u.full_name AS deleted_by_name FROM leads l "
                             + "LEFT JOIN users u ON u.id = l.deleted_by WHERE l.deleted_at IS NOT NULL ORDER BY l.deleted_at DESC");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rs.getLong("id"));
                item.put("name", rs.getString("full_name"));
                item.put("phone", rs.getString("phone"));
                item.put("deletedAt", rs.getTimestamp("deleted_at").toInstant().toString());
                item.put("deletedBy", rs.getString("deleted_by_name"));
                items.add(item);
            }
        }
        return items;
    }

    public boolean restore(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE leads SET deleted_at = NULL, deleted_by = NULL WHERE id = ? AND deleted_at IS NOT NULL")) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Xoá vĩnh viễn lead trong thùng rác cùng lịch sử phân công. */
    public boolean purge(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM leads WHERE id = ? AND deleted_at IS NOT NULL")) {
                    check.setLong(1, id);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }
                for (String sql : new String[] {"DELETE FROM lead_assignments WHERE lead_id = ?", "DELETE FROM leads WHERE id = ?"}) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setLong(1, id);
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

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("name", rs.getString("full_name"));
        item.put("phone", rs.getString("phone"));
        item.put("email", rs.getString("email"));
        item.put("source", rs.getString("source"));
        item.put("programCode", rs.getString("program_code"));
        item.put("programName", rs.getString("program_name"));
        item.put("status", rs.getString("status"));
        item.put("ownerId", rs.getObject("assigned_counselor_id") == null ? null : rs.getLong("assigned_counselor_id"));
        item.put("ownerName", rs.getString("owner_name"));
        item.put("note", rs.getString("notes"));
        Timestamp created = rs.getTimestamp("created_at");
        item.put("createdAt", created == null ? null : created.toLocalDateTime().toString());
        return item;
    }
}
