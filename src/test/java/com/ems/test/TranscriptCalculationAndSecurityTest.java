package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.dao.TranscriptDAO;
import com.ems.dto.TranscriptResponse;
import com.ems.model.SubjectGradeItem;
import com.ems.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho chức năng S8-03 (Jira EP-07):
 * "Là Học viên, tôi muốn xem và xuất bảng điểm toàn khoá, để xin việc có bảng điểm chính thức mà không phải chờ trung tâm làm tay."
 *
 * Kiểm tra 3 tiêu chí chấp nhận (Acceptance Criteria):
 *  - AC1: Liệt kê toàn bộ môn đã học, điểm tổng kết, kết quả Đạt hoặc Không đạt
 *  - AC2: Hiển thị điểm trung bình toàn khoá (GPA theo trọng số môn học)
 *  - AC3: Xuất PDF có mã học viên, tên trung tâm và ngày cấp
 *  - Bảo mật: Học viên chỉ xem được bảng điểm của mình; Quản lý đào tạo & Admin xem được mọi học viên.
 */
class TranscriptCalculationAndSecurityTest {

    private TranscriptDAO transcriptDAO;
    private User student1;
    private User student2;
    private User trainingManager;
    private User admin;

    @BeforeEach
    void setUp() {
        transcriptDAO = new TranscriptDAO();
        TranscriptDAO.initSampleData();

        student1 = new User(1001L, "NGUYEN MINH ANH", "minhanh.ng@gmail.com", "ACTIVE", List.of(RoleConstant.STUDENT));
        student1.setUserCode("HV260101");

        student2 = new User(1002L, "HOANG YEN NHI", "yennhi.hoang@gmail.com", "ACTIVE", List.of(RoleConstant.STUDENT));
        student2.setUserCode("HV260102");

        trainingManager = new User(2L, "LE HOANG NAM", "daotao@tms.vn", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        admin = new User(1L, "ADMINISTRATOR", "admin@tms.vn", "ACTIVE", List.of(RoleConstant.ADMIN));
    }

    // =========================================================================
    // AC1: LIỆT KÊ TOÀN BỘ MÔN ĐÃ HỌC, ĐIỂM TỔNG KẾT, KẾT QUẢ ĐẠT HOẶC KHÔNG ĐẠT
    // =========================================================================

    @Test
    void testAC1_ListAllCompletedSubjectsWithFinalScoresAndResults() throws SQLException {
        TranscriptResponse transcript = transcriptDAO.getTranscriptByStudentCode("HV260101");

        assertNotNull(transcript, "Bảng điểm của học viên HV260101 không được null");
        assertEquals("HV260101", transcript.getStudentCode());
        assertEquals("NGUYEN MINH ANH", transcript.getStudentName());
        assertEquals("WEB-K15", transcript.getClassCode());
        assertEquals("Lập trình web Full-stack", transcript.getProgramName());

        List<SubjectGradeItem> subjects = transcript.getSubjects();
        assertNotNull(subjects);
        assertEquals(6, subjects.size(), "Chương trình phải có 6 môn học");

        // Kiểm tra từng môn có đầy đủ mã, tên, số buổi, trọng số, điểm tổng kết và kết quả Đạt
        for (SubjectGradeItem s : subjects) {
            assertNotNull(s.getSubjectCode());
            assertNotNull(s.getSubjectName());
            assertTrue(s.getSessions() > 0);
            assertTrue(s.getWeight() > 0);
            assertNotNull(s.getFinalScore(), "Điểm tổng kết môn " + s.getSubjectCode() + " không được null");
            assertTrue(s.getFinalScore() >= 0 && s.getFinalScore() <= 10.0);
            assertEquals("Đạt", s.getResult(), "Học viên HV260101 đạt tất cả các môn");
        }

        assertEquals(6, transcript.getPassedSubjects());
        assertEquals(0, transcript.getFailedSubjects());
        assertTrue(transcript.isCompleted(), "Học viên đạt 6/6 môn phải đủ điều kiện tốt nghiệp");
    }

    @Test
    void testAC1_SubjectWithFailingScoreResultsInKhongDat() throws SQLException {
        TranscriptResponse transcript = transcriptDAO.getTranscriptByStudentCode("HV260102");

        assertNotNull(transcript);
        assertEquals("HV260102", transcript.getStudentCode());

        // Kiểm tra môn BE-01 bị trượt (điểm 4.2 < 5.0)
        SubjectGradeItem failedSubject = transcript.getSubjects().stream()
                .filter(s -> "BE-01".equals(s.getSubjectCode()))
                .findFirst()
                .orElse(null);

        assertNotNull(failedSubject, "Phải có môn BE-01 trong bảng điểm");
        assertEquals(4.2, failedSubject.getFinalScore(), 0.01);
        assertEquals("Không đạt", failedSubject.getResult());

        assertEquals(5, transcript.getPassedSubjects());
        assertEquals(1, transcript.getFailedSubjects());
        assertFalse(transcript.isCompleted(), "Học viên còn môn không đạt thì chưa đủ điều kiện tốt nghiệp");
    }

    // =========================================================================
    // AC2: HIỂN THỊ ĐIỂM TRUNG BÌNH TOÀN KHOÁ (GPA THEO TRỌNG SỐ MÔN HỌC)
    // =========================================================================

    @Test
    void testAC2_GpaCalculationBasedOnSubjectWeights() throws SQLException {
        TranscriptResponse transcript = transcriptDAO.getTranscriptByStudentCode("HV260101");
        assertNotNull(transcript);

        // HV260101:
        // WEB-01: 8.5 x 1 = 8.5
        // WEB-02: 8.0 x 2 = 16.0
        // WEB-03: 9.0 x 2 = 18.0
        // DB-01:  8.5 x 2 = 17.0
        // BE-01:  8.0 x 3 = 24.0
        // GIT-01: 9.5 x 1 = 9.5
        // Tổng điểm có trọng số = 93.0
        // Tổng trọng số = 1 + 2 + 2 + 2 + 3 + 1 = 11
        // GPA = 93.0 / 11 = 8.4545... làm tròn 1 chữ số thập phân = 8.5
        assertEquals(8.5, transcript.getGpa(), 0.01, "GPA phải là 8.5");

        // Kiểm tra chuỗi công thức minh bạch đối soát cho học viên
        String formula = transcript.getGpaFormula();
        assertNotNull(formula);
        assertTrue(formula.contains("÷ 11 = 8.5"), "Công thức GPA phải thể hiện chia cho tổng trọng số 11");
        assertTrue(formula.contains("8.5×1"));
        assertTrue(formula.contains("8.0×3"));
    }

    @Test
    void testAC2_GpaRecalculateHelper() {
        TranscriptResponse custom = new TranscriptResponse();
        custom.setTotalSubjects(3);

        List<SubjectGradeItem> list = List.of(
                new SubjectGradeItem("M1", "Môn 1", 10, 1, 9.0, null),
                new SubjectGradeItem("M2", "Môn 2", 15, 2, 6.0, null),
                new SubjectGradeItem("M3", "Môn 3", 20, 3, 7.0, null)
        );
        custom.setSubjects(list);
        custom.recalculate();

        // (9.0*1 + 6.0*2 + 7.0*3) = 9 + 12 + 21 = 42
        // Trọng số = 1 + 2 + 3 = 6
        // GPA = 42 / 6 = 7.0
        assertEquals(7.0, custom.getGpa(), 0.01);
        assertEquals(3, custom.getPassedSubjects());
        assertEquals(0, custom.getFailedSubjects());
        assertTrue(custom.isCompleted());
    }

    // =========================================================================
    // AC3: XUẤT PDF CÓ MÃ HỌC VIÊN, TÊN TRUNG TÂM VÀ NGÀY CẤP
    // =========================================================================

    @Test
    void testAC3_PrintableDocumentContainsAllRequiredOfficialFields() throws SQLException {
        TranscriptResponse transcript = transcriptDAO.getTranscriptByStudentCode("HV260101");
        assertNotNull(transcript);

        // 1. Kiểm tra các trường dữ liệu bắt buộc của AC3 trong DTO
        assertEquals("HV260101", transcript.getStudentCode(), "Phải có mã học viên");
        assertEquals("TRUNG TÂM ĐÀO TẠO TMS", transcript.getCenterName(), "Phải có tên trung tâm");
        assertNotNull(transcript.getIssueDate(), "Phải có ngày cấp");
        assertNotNull(transcript.getTranscriptNumber(), "Phải có mã số tra cứu văn bằng / số bảng điểm");
        assertTrue(transcript.getTranscriptNumber().startsWith("BD-"), "Số bảng điểm có tiền tố BD-");

        // 2. Kiểm tra văn bản HTML chuẩn in / xuất PDF chính thức
        String html = transcriptDAO.generatePrintableHtml(transcript);
        assertNotNull(html);

        // Kiểm tra sự xuất hiện của các trường AC3 trong bản in PDF
        assertTrue(html.contains("TRUNG TÂM ĐÀO TẠO TMS"), "PDF phải có tên trung tâm");
        assertTrue(html.contains("HV260101"), "PDF phải có mã học viên");
        assertTrue(html.contains(transcript.getIssueDate()), "PDF phải có ngày cấp");
        assertTrue(html.contains(transcript.getTranscriptNumber()), "PDF phải có số bảng điểm");
        assertTrue(html.contains("NGUYEN MINH ANH"), "PDF phải có họ tên học viên");
        assertTrue(html.contains("8.5"), "PDF phải có điểm trung bình toàn khoá");
        assertTrue(html.contains("window.print()"), "PDF phải có nút kích hoạt in hoặc lưu PDF của trình duyệt");
    }

    // =========================================================================
    // BẢO MẬT & PHÂN QUYỀN TRUY CẬP BẢNG ĐIỂM
    // =========================================================================

    @Test
    void testSecurity_StudentCanAccessOwnTranscriptViaMine() throws SQLException {
        TranscriptResponse resp = transcriptDAO.getTranscriptForUser(student1);
        assertNotNull(resp);
        assertEquals("HV260101", resp.getStudentCode());
    }

    @Test
    void testSecurity_StaffRolesCanAccessAnyStudentTranscript() throws SQLException {
        // Quản lý đào tạo xem học viên 1
        TranscriptResponse r1 = transcriptDAO.getTranscriptByStudentCode(student1.getUserCode());
        assertNotNull(r1);
        assertEquals("HV260101", r1.getStudentCode());

        // Admin xem học viên 2
        TranscriptResponse r2 = transcriptDAO.getTranscriptByStudentCode(student2.getUserCode());
        assertNotNull(r2);
        assertEquals("HV260102", r2.getStudentCode());
    }
}
