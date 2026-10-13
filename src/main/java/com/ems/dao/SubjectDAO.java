package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.Subject;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) cho Môn học (Subject)
 * User Story: S2-05 (EP-02)
 */
public class SubjectDAO {

    public Subject create(Subject subject) throws SQLException {
        String sql = "INSERT INTO subjects (subject_code, subject_name, total_sessions, credit_weight, min_pass_score, learning_outcomes, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, subject.getCode());
            stmt.setString(2, subject.getName());
            stmt.setInt(3, subject.getSessions() != null ? subject.getSessions() : 15);
            stmt.setBigDecimal(4, subject.getWeight() != null ? subject.getWeight() : BigDecimal.ONE);
            stmt.setBigDecimal(5, subject.getMinPassScore() != null ? subject.getMinPassScore() : new BigDecimal("5.00"));
            stmt.setString(6, subject.getOutcome());
            stmt.setString(7, subject.getStatus() != null ? subject.getStatus() : "ACTIVE");

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    subject.setId(rs.getInt(1));
                }
            }
        }
        return findById(subject.getId());
    }

    public Subject findById(int id) throws SQLException {
        String sql = "SELECT id, subject_code, subject_name, total_sessions, credit_weight, min_pass_score, learning_outcomes, status, created_at, updated_at "
                + "FROM subjects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Subject subject = mapRow(rs);
                    subject.setOpenedClassesCount(countOpenedClassesBySubjectId(id));
                    subject.setProgramsCount(countProgramsUsingSubject(id));
                    return subject;
                }
            }
        }
        return null;
    }

    public Subject findByCode(String code) throws SQLException {
        if (code == null) return null;
        String sql = "SELECT id, subject_code, subject_name, total_sessions, credit_weight, min_pass_score, learning_outcomes, status, created_at, updated_at "
                + "FROM subjects WHERE UPPER(subject_code) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Subject subject = mapRow(rs);
                    subject.setOpenedClassesCount(countOpenedClassesBySubjectId(subject.getId()));
                    subject.setProgramsCount(countProgramsUsingSubject(subject.getId()));
                    return subject;
                }
            }
        }
        return null;
    }

    public boolean existsByCode(String code) throws SQLException {
        if (code == null) return false;
        String sql = "SELECT 1 FROM subjects WHERE UPPER(subject_code) = UPPER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByCodeAndNotId(String code, int id) throws SQLException {
        if (code == null) return false;
        String sql = "SELECT 1 FROM subjects WHERE UPPER(subject_code) = UPPER(?) AND id <> ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            stmt.setInt(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Map<String, Object> search(String keyword, int page, int pageSize) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            whereSql.append(" AND (subject_code LIKE ? OR subject_name LIKE ?) ");
            params.add(kw);
            params.add(kw);
        }

        int totalCount = 0;
        String countSql = "SELECT COUNT(*) FROM subjects " + whereSql;
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

        List<Subject> items = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        String dataSql = "SELECT id, subject_code, subject_name, total_sessions, credit_weight, min_pass_score, learning_outcomes, status, created_at, updated_at "
                + "FROM subjects " + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?";
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
                    Subject subject = mapRow(rs);
                    subject.setOpenedClassesCount(countOpenedClassesBySubjectId(subject.getId()));
                    subject.setProgramsCount(countProgramsUsingSubject(subject.getId()));
                    items.add(subject);
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

    public Subject update(Subject subject) throws SQLException {
        String sql = "UPDATE subjects SET subject_code = ?, subject_name = ?, total_sessions = ?, "
                + "credit_weight = ?, min_pass_score = ?, learning_outcomes = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getCode());
            stmt.setString(2, subject.getName());
            stmt.setInt(3, subject.getSessions());
            stmt.setBigDecimal(4, subject.getWeight());
            stmt.setBigDecimal(5, subject.getMinPassScore());
            stmt.setString(6, subject.getOutcome());
            stmt.setString(7, subject.getStatus());
            stmt.setInt(8, subject.getId());

            int updated = stmt.executeUpdate();
            if (updated == 0) return null;
        }
        return findById(subject.getId());
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Đếm số lượng lớp học đã mở của môn học này.
     * Môn đã có lớp học thì không xoá được.
     */
    public int countOpenedClassesBySubjectId(int subjectId) {
        String sql = "SELECT COUNT(*) FROM class_subjects WHERE subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {
            // Trường hợp bảng class_subjects chưa có dữ liệu hoặc truy vấn kiểm thử
        }
        return 0;
    }

    /**
     * Đếm số chương trình đào tạo đang dùng môn học này.
     * Một môn học dùng lại được ở nhiều chương trình.
     */
    public int countProgramsUsingSubject(int subjectId) {
        String sql = "SELECT COUNT(*) FROM program_subjects WHERE subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ignored) {}
        return 0;
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("subject_code"));
        s.setName(rs.getString("subject_name"));
        s.setSessions(rs.getInt("total_sessions"));
        s.setWeight(rs.getBigDecimal("credit_weight"));
        s.setMinPassScore(rs.getBigDecimal("min_pass_score"));
        s.setOutcome(rs.getString("learning_outcomes"));
        s.setStatus(rs.getString("status"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setUpdatedAt(rs.getTimestamp("updated_at"));
        return s;
    }
}
