package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.ProgramSubject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) cho Gắn môn học vào chương trình (ProgramSubject)
 * User Story: S2-06 (EP-02)
 */
public class ProgramSubjectDAO {

    public List<ProgramSubject> findByProgramId(int programId) throws SQLException {
        String sql = "SELECT ps.id, ps.program_id, ps.subject_id, ps.order_index, ps.prerequisite_subject_id, "
                + "s.subject_code, s.subject_name, s.total_sessions, "
                + "pre.subject_code AS prereq_code, pre.subject_name AS prereq_name "
                + "FROM program_subjects ps "
                + "JOIN subjects s ON ps.subject_id = s.id "
                + "LEFT JOIN subjects pre ON ps.prerequisite_subject_id = pre.id "
                + "WHERE ps.program_id = ? "
                + "ORDER BY ps.order_index ASC";

        List<ProgramSubject> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProgramSubject ps = new ProgramSubject();
                    ps.setId(rs.getInt("id"));
                    ps.setProgramId(rs.getInt("program_id"));
                    ps.setSubjectId(rs.getInt("subject_id"));
                    ps.setOrderIndex(rs.getInt("order_index"));
                    int prereqId = rs.getInt("prerequisite_subject_id");
                    if (!rs.wasNull()) {
                        ps.setPrerequisiteSubjectId(prereqId);
                    }
                    ps.setSubjectCode(rs.getString("subject_code"));
                    ps.setSubjectName(rs.getString("subject_name"));
                    ps.setSessions(rs.getInt("total_sessions"));
                    ps.setPrerequisiteSubjectCode(rs.getString("prereq_code"));
                    ps.setPrerequisiteSubjectName(rs.getString("prereq_name"));
                    list.add(ps);
                }
            }
        }
        return list;
    }

    public boolean addSubject(int programId, int subjectId, Integer orderIndex, Integer prerequisiteSubjectId) throws SQLException {
        if (orderIndex == null) {
            orderIndex = getNextOrderIndex(programId);
        }
        String sql = "INSERT INTO program_subjects (program_id, subject_id, order_index, prerequisite_subject_id) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            stmt.setInt(2, subjectId);
            stmt.setInt(3, orderIndex);
            if (prerequisiteSubjectId != null && prerequisiteSubjectId > 0) {
                stmt.setInt(4, prerequisiteSubjectId);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeSubject(int programId, int subjectId) throws SQLException {
        // Xoá môn khỏi chương trình và xóa bỏ liên kết nếu môn này làm tiên quyết cho môn khác trong cùng chương trình
        String clearPrereqSql = "UPDATE program_subjects SET prerequisite_subject_id = NULL "
                + "WHERE program_id = ? AND prerequisite_subject_id = ?";
        String deleteSql = "DELETE FROM program_subjects WHERE program_id = ? AND subject_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt1 = conn.prepareStatement(clearPrereqSql)) {
                    stmt1.setInt(1, programId);
                    stmt1.setInt(2, subjectId);
                    stmt1.executeUpdate();
                }
                boolean deleted;
                try (PreparedStatement stmt2 = conn.prepareStatement(deleteSql)) {
                    stmt2.setInt(1, programId);
                    stmt2.setInt(2, subjectId);
                    deleted = stmt2.executeUpdate() > 0;
                }
                conn.commit();
                return deleted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean updateOrder(int programId, List<Integer> subjectIdsInOrder) throws SQLException {
        if (subjectIdsInOrder == null || subjectIdsInOrder.isEmpty()) return true;

        String sql = "UPDATE program_subjects SET order_index = ? WHERE program_id = ? AND subject_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < subjectIdsInOrder.size(); i++) {
                    stmt.setInt(1, i + 1);
                    stmt.setInt(2, programId);
                    stmt.setInt(3, subjectIdsInOrder.get(i));
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean updatePrerequisite(int programId, int subjectId, Integer prerequisiteSubjectId) throws SQLException {
        String sql = "UPDATE program_subjects SET prerequisite_subject_id = ? WHERE program_id = ? AND subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (prerequisiteSubjectId != null && prerequisiteSubjectId > 0) {
                stmt.setInt(1, prerequisiteSubjectId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, programId);
            stmt.setInt(3, subjectId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean existsInProgram(int programId, int subjectId) throws SQLException {
        String sql = "SELECT 1 FROM program_subjects WHERE program_id = ? AND subject_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            stmt.setInt(2, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int getNextOrderIndex(int programId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(order_index), 0) + 1 FROM program_subjects WHERE program_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }
}
