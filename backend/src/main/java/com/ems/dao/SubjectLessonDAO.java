package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.SubjectLesson;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) cho Khai báo Buổi học của Môn (SubjectLesson)
 * User Story: S2-07 (EP-02)
 */
public class SubjectLessonDAO {

    public List<SubjectLesson> findBySubjectId(int subjectId) throws SQLException {
        String sql = "SELECT id, subject_id, session_order, topic, objectives "
                + "FROM subject_lessons WHERE subject_id = ? ORDER BY session_order ASC";
        List<SubjectLesson> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public SubjectLesson findById(int id) throws SQLException {
        String sql = "SELECT id, subject_id, session_order, topic, objectives "
                + "FROM subject_lessons WHERE id = ?";
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

    public int countBySubjectId(int subjectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM subject_lessons WHERE subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public SubjectLesson create(SubjectLesson lesson) throws SQLException {
        String sql = "INSERT INTO subject_lessons (subject_id, session_order, topic, objectives) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, lesson.getSubjectId());
            stmt.setInt(2, lesson.getSessionOrder());
            stmt.setString(3, lesson.getTopic());
            stmt.setString(4, lesson.getObjectives());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    lesson.setId(rs.getInt(1));
                }
            }
        }
        return findById(lesson.getId());
    }

    public SubjectLesson update(SubjectLesson lesson) throws SQLException {
        String sql = "UPDATE subject_lessons SET session_order = ?, topic = ?, objectives = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, lesson.getSessionOrder());
            stmt.setString(2, lesson.getTopic());
            stmt.setString(3, lesson.getObjectives());
            stmt.setInt(4, lesson.getId());

            int updated = stmt.executeUpdate();
            if (updated == 0) return null;
        }
        return findById(lesson.getId());
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM subject_lessons WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public void deleteAllBySubjectId(int subjectId) throws SQLException {
        String sql = "DELETE FROM subject_lessons WHERE subject_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            stmt.executeUpdate();
        }
    }

    /**
     * Nhân bản danh sách buổi học từ môn nguồn sang môn đích.
     * Số buổi nhân bản không vượt quá giới hạn maxSessions của môn đích.
     */
    public List<SubjectLesson> cloneLessons(int sourceSubjectId, int targetSubjectId, int maxSessions) throws SQLException {
        List<SubjectLesson> sourceLessons = findBySubjectId(sourceSubjectId);
        if (sourceLessons.isEmpty()) {
            return new ArrayList<>();
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Xoá danh sách buổi hiện tại của môn đích (ghi đè)
                deleteAllBySubjectId(targetSubjectId);

                String insertSql = "INSERT INTO subject_lessons (subject_id, session_order, topic, objectives) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    int count = Math.min(sourceLessons.size(), maxSessions);
                    for (int i = 0; i < count; i++) {
                        SubjectLesson src = sourceLessons.get(i);
                        stmt.setInt(1, targetSubjectId);
                        stmt.setInt(2, i + 1);
                        stmt.setString(3, src.getTopic());
                        stmt.setString(4, src.getObjectives());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return findBySubjectId(targetSubjectId);
    }

    private SubjectLesson mapRow(ResultSet rs) throws SQLException {
        SubjectLesson l = new SubjectLesson();
        l.setId(rs.getInt("id"));
        l.setSubjectId(rs.getInt("subject_id"));
        l.setSessionOrder(rs.getInt("session_order"));
        l.setTopic(rs.getString("topic"));
        l.setObjectives(rs.getString("objectives"));
        return l;
    }
}
