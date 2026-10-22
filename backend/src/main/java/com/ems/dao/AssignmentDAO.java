package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.Assignment;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Chức năng S5-05: Tạo bài tập và giao cho lớp
 */
public class AssignmentDAO {

    /**
     * Tạo bài tập mới (hỗ trợ cả lưu nháp và giao trực tiếp cho lớp)
     */
    public Assignment createAssignment(Assignment assignment) throws SQLException {
        validateAssignment(assignment);

        String sql = "INSERT INTO assignments (class_id, class_subject_id, session_id, title, description, "
                + "max_score, due_date, allow_late_submission, is_draft, created_by, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, assignment.getClassId());
                statement.setLong(2, assignment.getClassSubjectId());
                if (assignment.getSessionId() != null) {
                    statement.setLong(3, assignment.getSessionId());
                } else {
                    statement.setNull(3, Types.BIGINT);
                }
                statement.setString(4, assignment.getTitle().trim());
                statement.setString(5, assignment.getDescription());
                statement.setBigDecimal(6, assignment.getMaxScore() != null ? assignment.getMaxScore() : BigDecimal.TEN);
                statement.setTimestamp(7, Timestamp.valueOf(assignment.getDueDate()));
                statement.setBoolean(8, assignment.getAllowLateSubmission() != null && assignment.getAllowLateSubmission());
                statement.setBoolean(9, assignment.getIsDraft() != null && assignment.getIsDraft());
                statement.setLong(10, assignment.getCreatedBy());

                int affected = statement.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Tạo bài tập thất bại, không có bản ghi nào được thêm.");
                }

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        assignment.setId(keys.getLong(1));
                    }
                }

                // Lưu các tệp đính kèm (nếu có)
                if (assignment.getAttachments() != null && !assignment.getAttachments().isEmpty()) {
                    String fileSql = "INSERT INTO assignment_attachments (assignment_id, file_name, file_url, file_size_bytes) "
                            + "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement fileStmt = connection.prepareStatement(fileSql)) {
                        for (Assignment.Attachment file : assignment.getAttachments()) {
                            fileStmt.setLong(1, assignment.getId());
                            fileStmt.setString(2, file.getFileName());
                            fileStmt.setString(3, file.getFileUrl() != null ? file.getFileUrl() : "");
                            fileStmt.setLong(4, file.getFileSizeBytes() != null ? file.getFileSizeBytes() : 0L);
                            fileStmt.addBatch();
                        }
                        fileStmt.executeBatch();
                    }
                }

                connection.commit();
                return assignment;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Lấy danh sách bài tập theo lớp
     */
    public List<Assignment> getAssignmentsByClass(long classId) throws SQLException {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.*, c.class_code, u.full_name AS creator_name "
                + "FROM assignments a "
                + "JOIN classes c ON c.id = a.class_id "
                + "JOIN users u ON u.id = a.created_by "
                + "WHERE a.class_id = ? "
                + "ORDER BY a.due_date DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAssignment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Phát hành (Giao) một bài tập đang ở trạng thái Nháp cho lớp
     */
    public boolean publishAssignment(long assignmentId) throws SQLException {
        String sql = "UPDATE assignments SET is_draft = FALSE, updated_at = NOW() WHERE id = ? AND is_draft = TRUE";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assignmentId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Kiểm tra tính hợp lệ theo tiêu chí chấp nhận S5-05:
     * - Tiêu đề không để trống
     * - Hạn nộp không được ở quá khứ
     * - Điểm tối đa từ 1 đến 100
     */
    public static void validateAssignment(Assignment a) {
        if (a == null) {
            throw new IllegalArgumentException("Dữ liệu bài tập không được rỗng.");
        }
        if (a.getTitle() == null || a.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tiêu đề bài tập.");
        }
        if (a.getDueDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn hạn nộp cho bài tập.");
        }
        if (a.getDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Hạn nộp không được ở quá khứ.");
        }
        if (a.getMaxScore() != null) {
            if (a.getMaxScore().compareTo(BigDecimal.ONE) < 0 || a.getMaxScore().compareTo(new BigDecimal(100)) > 0) {
                throw new IllegalArgumentException("Điểm tối đa phải nằm trong khoảng từ 1 đến 100.");
            }
        }
    }

    private Assignment mapResultSetToAssignment(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setId(rs.getLong("id"));
        a.setClassId(rs.getLong("class_id"));
        a.setClassCode(rs.getString("class_code"));
        a.setClassSubjectId(rs.getLong("class_subject_id"));
        long sessId = rs.getLong("session_id");
        if (!rs.wasNull()) a.setSessionId(sessId);
        a.setTitle(rs.getString("title"));
        a.setDescription(rs.getString("description"));
        a.setMaxScore(rs.getBigDecimal("max_score"));
        Timestamp dueTs = rs.getTimestamp("due_date");
        if (dueTs != null) a.setDueDate(dueTs.toLocalDateTime());
        a.setAllowLateSubmission(rs.getBoolean("allow_late_submission"));
        a.setIsDraft(rs.getBoolean("is_draft"));
        a.setCreatedBy(rs.getLong("created_by"));
        a.setCreatorName(rs.getString("creator_name"));
        Timestamp crTs = rs.getTimestamp("created_at");
        if (crTs != null) a.setCreatedAt(crTs.toLocalDateTime());
        return a;
    }
}
