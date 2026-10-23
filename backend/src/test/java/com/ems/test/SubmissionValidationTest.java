package com.ems.test;

import com.ems.dao.AssignmentSubmissionDAO;
import com.ems.model.AssignmentSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho chức năng S5-07: Nộp bài tập
 * Ràng buộc:
 * 1. Tải lên nhiều tệp, mỗi tệp tối đa 20MB, kèm ô nhập liên kết repo
 * 2. Kiểm tra định dạng liên kết trước khi lưu (GitHub, GitLab, Bitbucket)
 * 3. Nộp xong hiển thị mốc thời gian nộp và trạng thái Đã nộp
 */
class SubmissionValidationTest {

    @Test
    void testEmptySubmissionThrowsException() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignmentId(1L);
        sub.setStudentId(101L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AssignmentSubmissionDAO.validateSubmission(sub));
        assertTrue(ex.getMessage().contains("ít nhất một tệp hoặc"));
    }

    @Test
    void testFileExceeding20MBThrowsException() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignmentId(1L);
        sub.setStudentId(101L);

        // Tệp 21 MB (vượt ngưỡng 20MB)
        long twentyOneMb = 21 * 1024 * 1024L;
        sub.setFiles(List.of(new AssignmentSubmission.SubmissionFile("project.zip", "url", twentyOneMb)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AssignmentSubmissionDAO.validateSubmission(sub));
        assertTrue(ex.getMessage().contains("20MB"));
    }

    @Test
    void testFileUnder20MBIsValid() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignmentId(1L);
        sub.setStudentId(101L);

        // Tệp 5 MB (hợp lệ)
        long fiveMb = 5 * 1024 * 1024L;
        sub.setFiles(List.of(new AssignmentSubmission.SubmissionFile("solution.pdf", "url", fiveMb)));

        assertDoesNotThrow(() -> AssignmentSubmissionDAO.validateSubmission(sub));
    }

    @Test
    void testInvalidRepoUrlsThrowException() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignmentId(1L);
        sub.setStudentId(101L);

        String[] invalidUrls = {
                "http://github.com/user/repo", // Không phải https
                "https://facebook.com/user/repo", // Không phải Git host
                "https://github.com", // Thiếu user/repo
                "javascript:alert(1)", // XSS payload
                "ftp://github.com/user/repo"
        };

        for (String url : invalidUrls) {
            sub.setRepoUrl(url);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> AssignmentSubmissionDAO.validateSubmission(sub),
                    "Phải từ chối URL không hợp lệ: " + url);
            assertTrue(ex.getMessage().contains("Liên kết repo không hợp lệ"));
        }
    }

    @Test
    void testValidRepoUrlsPassValidation() {
        AssignmentSubmission sub = new AssignmentSubmission();
        sub.setAssignmentId(1L);
        sub.setStudentId(101L);

        String[] validUrls = {
                "https://github.com/nguyentatphi136-ux/ICTU-DUAN-Trung-Tam-X",
                "https://www.github.com/student/java-project",
                "https://gitlab.com/enterprise/backend-api",
                "https://bitbucket.org/team-dev/web-app.git"
        };

        for (String url : validUrls) {
            sub.setRepoUrl(url);
            assertDoesNotThrow(() -> AssignmentSubmissionDAO.validateSubmission(sub),
                    "Phải chấp nhận URL hợp lệ: " + url);
        }
    }

    @Test
    void testSubmissionStatusAndTimestamp() {
        AssignmentSubmission sub = new AssignmentSubmission();
        assertEquals("SUBMITTED", sub.getStatus(), "Trạng thái sau khi nộp là SUBMITTED (Đã nộp)");
        assertNotNull(sub.getSubmittedAt(), "Phải có mốc thời gian nộp bài");
        assertTrue(sub.getIsLatest(), "Bản mới nộp mặc định là bản mới nhất");
    }
}
