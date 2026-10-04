package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.Lead;

import java.sql.*;
import java.util.*;

/**
 * Data Access Object (DAO) cho quản lý Khách hàng tiềm năng (Leads)
 * Triển khai các thao tác CRUD, tìm kiếm, phân trang và xử lý trùng lặp số điện thoại
 */
public class LeadDAO {

    public Lead findById(long id) throws SQLException {
        String sql = "SELECT l.*, "
                + "u_assign.full_name AS assigned_to_name, "
                + "u_create.full_name AS created_by_name, "
                + "u_update.full_name AS updated_by_name "
                + "FROM leads l "
                + "LEFT JOIN users u_assign ON u_assign.id = l.assigned_to "
                + "LEFT JOIN users u_create ON u_create.id = l.created_by "
                + "LEFT JOIN users u_update ON u_update.id = l.updated_by "
                + "WHERE l.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLead(rs);
                }
            }
        }
        return null;
    }

    public Lead findByPhone(String phone) throws SQLException {
        if (phone == null || phone.isBlank()) return null;
        String normalizedPhone = phone.trim();

        String sql = "SELECT l.*, "
                + "u_assign.full_name AS assigned_to_name, "
                + "u_create.full_name AS created_by_name, "
                + "u_update.full_name AS updated_by_name "
                + "FROM leads l "
                + "LEFT JOIN users u_assign ON u_assign.id = l.assigned_to "
                + "LEFT JOIN users u_create ON u_create.id = l.created_by "
                + "LEFT JOIN users u_update ON u_update.id = l.updated_by "
                + "WHERE l.phone = ? "
                + "ORDER BY l.id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedPhone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLead(rs);
                }
            }
        }
        return null;
    }

    public boolean existsByPhone(String phone, Long excludeId) throws SQLException {
        if (phone == null || phone.isBlank()) return false;
        String normalizedPhone = phone.trim();

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM leads WHERE phone = ?");
        if (excludeId != null) {
            sql.append(" AND id != ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, normalizedPhone);
            if (excludeId != null) {
                ps.setLong(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public long create(Lead lead) throws SQLException {
        String sql = "INSERT INTO leads (full_name, phone, email, source, program_id, "
                + "program_interest, status, notes, assigned_to, created_by, updated_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lead.getFullName() != null ? lead.getFullName().trim() : "");
            ps.setString(2, lead.getPhone() != null ? lead.getPhone().trim() : "");
            ps.setString(3, (lead.getEmail() != null && !lead.getEmail().isBlank()) ? lead.getEmail().trim() : null);
            ps.setString(4, (lead.getSource() != null && !lead.getSource().isBlank()) ? lead.getSource().trim() : Lead.SOURCE_WEBSITE);

            if (lead.getProgramId() != null) {
                ps.setInt(5, lead.getProgramId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, lead.getProgramInterest() != null ? lead.getProgramInterest().trim() : null);
            ps.setString(7, (lead.getStatus() != null && !lead.getStatus().isBlank()) ? lead.getStatus().trim() : Lead.STATUS_NEW);
            ps.setString(8, lead.getNotes());

            if (lead.getAssignedTo() != null) {
                ps.setLong(9, lead.getAssignedTo());
            } else {
                ps.setNull(9, Types.BIGINT);
            }

            if (lead.getCreatedBy() != null) {
                ps.setLong(10, lead.getCreatedBy());
            } else {
                ps.setNull(10, Types.BIGINT);
            }

            if (lead.getUpdatedBy() != null) {
                ps.setLong(11, lead.getUpdatedBy());
            } else {
                ps.setNull(11, Types.BIGINT);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Thêm lead thất bại, không có bản ghi nào được ghi nhận.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    lead.setId(id);
                    return id;
                } else {
                    throw new SQLException("Thêm lead thành công nhưng không lấy được ID tự sinh.");
                }
            }
        }
    }

    public boolean update(Lead lead) throws SQLException {
        if (lead.getId() == null) return false;

        String sql = "UPDATE leads SET "
                + "full_name = ?, "
                + "phone = ?, "
                + "email = ?, "
                + "source = ?, "
                + "program_id = ?, "
                + "program_interest = ?, "
                + "status = ?, "
                + "notes = ?, "
                + "assigned_to = ?, "
                + "updated_by = ?, "
                + "updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lead.getFullName() != null ? lead.getFullName().trim() : "");
            ps.setString(2, lead.getPhone() != null ? lead.getPhone().trim() : "");
            ps.setString(3, (lead.getEmail() != null && !lead.getEmail().isBlank()) ? lead.getEmail().trim() : null);
            ps.setString(4, (lead.getSource() != null && !lead.getSource().isBlank()) ? lead.getSource().trim() : Lead.SOURCE_WEBSITE);

            if (lead.getProgramId() != null) {
                ps.setInt(5, lead.getProgramId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, lead.getProgramInterest() != null ? lead.getProgramInterest().trim() : null);
            ps.setString(7, (lead.getStatus() != null && !lead.getStatus().isBlank()) ? lead.getStatus().trim() : Lead.STATUS_NEW);
            ps.setString(8, lead.getNotes());

            if (lead.getAssignedTo() != null) {
                ps.setLong(9, lead.getAssignedTo());
            } else {
                ps.setNull(9, Types.BIGINT);
            }

            if (lead.getUpdatedBy() != null) {
                ps.setLong(10, lead.getUpdatedBy());
            } else {
                ps.setNull(10, Types.BIGINT);
            }

            ps.setLong(11, lead.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM leads WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Map<String, Object> searchLeads(String keyword, String status, String source,
                                           Long assignedTo, String startDate, String endDate,
                                           int page, int pageSize) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        int offset = (page - 1) * pageSize;

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            whereClause.append(" AND (l.full_name LIKE ? OR l.phone LIKE ? OR l.email LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            whereClause.append(" AND l.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (source != null && !source.isBlank() && !"ALL".equalsIgnoreCase(source)) {
            whereClause.append(" AND l.source = ? ");
            params.add(source.trim().toUpperCase());
        }

        if (assignedTo != null && assignedTo > 0) {
            whereClause.append(" AND l.assigned_to = ? ");
            params.add(assignedTo);
        }

        if (startDate != null && !startDate.isBlank()) {
            whereClause.append(" AND l.created_at >= ? ");
            params.add(startDate.trim() + " 00:00:00");
        }

        if (endDate != null && !endDate.isBlank()) {
            whereClause.append(" AND l.created_at <= ? ");
            params.add(endDate.trim() + " 23:59:59");
        }

        // 1. Đếm tổng số bản ghi
        String countSql = "SELECT COUNT(*) FROM leads l " + whereClause;
        int totalItems = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalItems = rs.getInt(1);
                }
            }
        }

        // 2. Lấy danh sách phân trang
        String querySql = "SELECT l.*, "
                + "u_assign.full_name AS assigned_to_name, "
                + "u_create.full_name AS created_by_name, "
                + "u_update.full_name AS updated_by_name "
                + "FROM leads l "
                + "LEFT JOIN users u_assign ON u_assign.id = l.assigned_to "
                + "LEFT JOIN users u_create ON u_create.id = l.created_by "
                + "LEFT JOIN users u_update ON u_update.id = l.updated_by "
                + whereClause
                + " ORDER BY l.id DESC LIMIT ? OFFSET ?";

        List<Lead> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            int paramIndex = 1;
            for (Object param : params) {
                ps.setObject(paramIndex++, param);
            }
            ps.setInt(paramIndex++, pageSize);
            ps.setInt(paramIndex, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToLead(rs));
                }
            }
        }

        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages == 0) totalPages = 1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalItems", totalItems);
        result.put("totalPages", totalPages);
        return result;
    }

    public int countTotalLeads() throws SQLException {
        String sql = "SELECT COUNT(*) FROM leads";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Lead mapResultSetToLead(ResultSet rs) throws SQLException {
        Lead lead = new Lead();
        lead.setId(rs.getLong("id"));
        lead.setFullName(rs.getString("full_name"));
        lead.setPhone(rs.getString("phone"));
        lead.setEmail(rs.getString("email"));
        lead.setSource(rs.getString("source"));

        int programId = rs.getInt("program_id");
        if (!rs.wasNull()) {
            lead.setProgramId(programId);
        }

        try {
            lead.setProgramInterest(rs.getString("program_interest"));
        } catch (SQLException ignored) {}

        lead.setStatus(rs.getString("status"));
        lead.setNotes(rs.getString("notes"));

        long assignedTo = rs.getLong("assigned_to");
        if (!rs.wasNull()) {
            lead.setAssignedTo(assignedTo);
        }

        try {
            lead.setAssignedToName(rs.getString("assigned_to_name"));
        } catch (SQLException ignored) {}

        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) {
            lead.setCreatedBy(createdBy);
        }

        try {
            lead.setCreatedByName(rs.getString("created_by_name"));
        } catch (SQLException ignored) {}

        long updatedBy = rs.getLong("updated_by");
        if (!rs.wasNull()) {
            lead.setUpdatedBy(updatedBy);
        }

        try {
            lead.setUpdatedByName(rs.getString("updated_by_name"));
        } catch (SQLException ignored) {}

        lead.setCreatedAt(rs.getTimestamp("created_at"));
        lead.setUpdatedAt(rs.getTimestamp("updated_at"));

        return lead;
    }
}
