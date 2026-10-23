package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.AssignmentSubmission;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Data Access Object cho Chức năng S5-07: Nộp bài tập
 * Ràng buộc:
 * 1. Tải lên nhiều tệp, mỗi tệp tối đa 20MB, kèm ô nhập liên kết repo
 * 2. Kiểm tra định dạng liên kết trước khi lưu (GitHub, GitLab, Bitbucket)
 * 3. Nộp xong hiển thị mốc thời gian nộp và trạng thái Đã nộp
 */
public class AssignmentSubmissionDAO {

    public static final Pattern REPO_URL_PATTERN = Pattern.compile(
            "^https://(www\\.)?(github\\.com|gitlab\\.com|bitbucket\\.org)/[\\w.-]+(/[\\w.-]+)+/?(\\.git)?$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Nộp bài tập của học viên
     */
    public AssignmentSubmission submitAssignment(AssignmentSubmission submission) throws SQLException {
        validateSubmission(submission);

        String sql = "INSERT INTO assignment_submissions (assignment_id, student_id, version, repo_url, "
                + "submission_note, is_latest, is_late, status, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Đánh dấu các bản nộp cũ của học viên này không còn là bản mới nhất
                String updateOldSql = "UPDATE assignment_submissions SET is_latest = FALSE "
                        + "WHERE assignment_id = ? AND student_id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateOldSql)) {
                    updateStmt.setLong(1, submission.getAssignmentId());
                    updateStmt.setLong(2, submission.getStudentId());
                    updateStmt.executeUpdate();
                }

                // Lấy phiên bản tiếp theo
                int nextVersion = 1;
                String verSql = "SELECT COALESCE(MAX(version), 0) + 1 FROM assignment_submissions "
                        + "WHERE assignment_id = ? AND student_id = ?";
                try (PreparedStatement verStmt = connection.prepareStatement(verSql)) {
                    verStmt.setLong(1, submission.getAssignmentId());
                    verStmt.setLong(2, submission.getStudentId());
                    try (ResultSet rs = verStmt.executeQuery()) {
                        if (rs.next()) {
                            nextVersion = rs.getInt(1);
                        }
                    }
                }
                submission.setVersion(nextVersion);
                LocalDateTime now = LocalDateTime.now();
                submission.setSubmittedAt(now);
                submission.setStatus("SUBMITTED");
                submission.setIsLatest(true);

                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, submission.getAssignmentId());
                    statement.setLong(2, submission.getStudentId());
                    statement.setInt(3, submission.getVersion());
                    statement.setString(4, submission.getRepoUrl() != null ? submission.getRepoUrl().trim() : null);
                    statement.setString(5, submission.getSubmissionNote());
                    statement.setBoolean(6, true);
                    statement.setBoolean(7, submission.getIsLate() != null && submission.getIsLate());
                    statement.setString(8, "SUBMITTED");
                    statement.setTimestamp(9, Timestamp.valueOf(now));

                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            submission.setId(keys.getLong(1));
                        }
                    }
                }

                // Lưu các tệp bài làm đính kèm
                if (submission.getFiles() != null && !submission.getFiles().isEmpty()) {
                    String fileSql = "INSERT INTO submission_files (submission_id, file_name, file_url, file_size_bytes) "
                            + "VALUES (?, ?, ?, ?)";
                    try (PreparedStatement fileStmt = connection.prepareStatement(fileSql)) {
                        for (AssignmentSubmission.SubmissionFile file : submission.getFiles()) {
                            fileStmt.setLong(1, submission.getId());
                            fileStmt.setString(2, file.getFileName());
                            fileStmt.setString(3, file.getFileUrl() != null ? file.getFileUrl() : "");
                            fileStmt.setLong(4, file.getFileSizeBytes());
                            fileStmt.addBatch();
                        }
                        fileStmt.executeBatch();
                    }
                }

                connection.commit();
                return submission;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Lấy bản nộp mới nhất của học viên cho một bài tập
     */
    public AssignmentSubmission getLatestSubmission(long assignmentId, long studentId) throws SQLException {
        String sql = "SELECT * FROM assignment_submissions "
                + "WHERE assignment_id = ? AND student_id = ? AND is_latest = TRUE";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assignmentId);
            statement.setLong(2, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                AssignmentSubmission sub = mapResultSetToSubmission(rs);

                // Lấy các tệp đính kèm
                String fileSql = "SELECT * FROM submission_files WHERE submission_id = ?";
                try (PreparedStatement fileStmt = connection.prepareStatement(fileSql)) {
                    fileStmt.setLong(1, sub.getId());
                    try (ResultSet frs = fileStmt.executeQuery()) {
                        List<AssignmentSubmission.SubmissionFile> files = new ArrayList<>();
                        while (frs.next()) {
                            AssignmentSubmission.SubmissionFile f = new AssignmentSubmission.SubmissionFile();
                            f.setId(frs.getLong("id"));
                            f.setSubmissionId(frs.getLong("submission_id"));
                            f.setFileName(frs.getString("file_name"));
                            f.setFileUrl(frs.getString("file_url"));
                            f.setFileSizeBytes(frs.getLong("file_size_bytes"));
                            files.add(f);
                        }
                        sub.setFiles(files);
                    }
                }
                return sub;
            }
        }
    }

    /**
     * Kiểm tra tính hợp lệ của bài nộp theo tiêu chuẩn S5-07:
     * - Phải có ít nhất 1 tệp hoặc 1 liên kết repo
     * - Mỗi tệp tối đa 20MB
     * - Liên kết repo phải đúng định dạng
     */
    public static void validateSubmission(AssignmentSubmission sub) {
        if (sub == null) {
            throw new IllegalArgumentException("Dữ liệu nộp bài không được rỗng.");
        }
        boolean hasFiles = sub.getFiles() != null && !sub.getFiles().isEmpty();
        boolean hasRepo = sub.getRepoUrl() != null && !sub.getRepoUrl().trim().isEmpty();

        if (!hasFiles && !hasRepo) {
            throw new IllegalArgumentException("Vui lòng tải lên ít nhất một tệp hoặc nhập liên kết repo.");
        }

        // Kiểm tra dung lượng từng tệp: tối đa 20MB
        if (hasFiles) {
            for (AssignmentSubmission.SubmissionFile file : sub.getFiles()) {
                if (file.getFileSizeBytes() > AssignmentSubmission.MAX_FILE_SIZE_BYTES) {
                    throw new IllegalArgumentException("Tệp '" + file.getFileName() 
                            + "' vượt quá dung lượng tối đa cho phép là 20MB.");
                }
            }
        }

        // Kiểm tra định dạng liên kết repo
        if (hasRepo) {
            String repo = sub.getRepoUrl().trim();
            if (!isValidRepoUrl(repo)) {
                throw new IllegalArgumentException("Liên kết repo không hợp lệ. "
                        + "Phải bắt đầu bằng https:// và trỏ tới GitHub, GitLab hoặc Bitbucket.");
            }
        }
    }

    public static boolean isValidRepoUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        return REPO_URL_PATTERN.matcher(url.trim()).matches();
    }

    private AssignmentSubmission mapResultSetToSubmission(ResultSet rs) throws SQLException {
        AssignmentSubmission s = new AssignmentSubmission();
        s.setId(rs.getLong("id"));
        s.setAssignmentId(rs.getLong("assignment_id"));
        s.setStudentId(rs.getLong("student_id"));
        s.setVersion(rs.getInt("version"));
        s.setRepoUrl(rs.getString("repo_url"));
        s.setSubmissionNote(rs.getString("submission_note"));
        s.setIsLatest(rs.getBoolean("is_latest"));
        s.setIsLate(rs.getBoolean("is_late"));
        s.setStatus(rs.getString("status"));
        s.setResubmitReason(rs.getString("resubmit_reason"));
        Timestamp ndTs = rs.getTimestamp("new_deadline");
        if (ndTs != null) s.setNewDeadline(ndTs.toLocalDateTime());
        Timestamp subTs = rs.getTimestamp("submitted_at");
        if (subTs != null) s.setSubmittedAt(subTs.toLocalDateTime());
        return s;
    }
}
