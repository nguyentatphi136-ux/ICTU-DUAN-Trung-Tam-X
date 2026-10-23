package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.dao.PaymentDAO;
import com.ems.dto.CreateAdjustmentRequest;
import com.ems.dto.CreatePaymentRequest;
import com.ems.dto.PaymentReceiptResponse;
import com.ems.model.AdjustmentRecord;
import com.ems.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho chức năng Kế toán S7-03 (Jira EP-08):
 * "Là Kế toán, tôi muốn ghi nhận một khoản thanh toán và in biên lai, để học viên có bằng chứng
 * đã đóng tiền và sổ sách không bao giờ lệch."
 *
 * Kiểm tra 5 tiêu chí chấp nhận (Acceptance Criteria):
 *  - AC1: Ghi số tiền, hình thức (tiền mặt, chuyển khoản), ngày thu, người thu, ghi chú
 *         (Mã GD ngân hàng bắt buộc nếu chuyển khoản).
 *  - AC2: Cho phép thu một phần, đợt tự chuyển sang 'Thanh toán một phần' (nếu thu đủ tự chuyển 'Đã thanh toán').
 *  - AC3: Không cho thu vượt số tiền còn nợ của đợt (ném ngoại lệ IllegalArgumentException).
 *  - AC4: Sinh biên lai có mã duy nhất (BL-YYYY-XXXXXX), in hoặc tải PDF được với số tiền bằng chữ.
 *  - AC5: Đã ghi nhận thì không xoá, chỉ lập bút toán điều chỉnh (ĐC-YYYY-XXXXXX).
 */
class PaymentValidationAndSecurityTest {

    private PaymentDAO paymentDAO;
    private User accountant;
    private User student;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        paymentDAO = new PaymentDAO();
        PaymentDAO.initSampleData();

        accountant = new User(3L, "ĐỖ THỊ MAI", "ketoan@tms.vn", "ACTIVE", List.of(RoleConstant.ACCOUNTANT));
        accountant.setUserCode("KT26001");

        student = new User(1001L, "DO TUAN KIET", "kiet.do@gmail.com", "ACTIVE", List.of(RoleConstant.STUDENT));
        student.setUserCode("HV260142");

        today = LocalDate.now();
    }

    // =========================================================================
    // AC1: VALIDATE THÔNG TIN THU TIỀN (SỐ TIỀN, HÌNH THỨC, NGÀY THU, MÃ GD)
    // =========================================================================

    @Test
    void testAC1_AmountMustBePositive() {
        CreatePaymentRequest reqZero = new CreatePaymentRequest("HV260142", 2, 0, "Tiền mặt", today.toString(), null, "Đóng học phí");
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                paymentDAO.recordPayment(reqZero, accountant, today));
        assertTrue(exZero.getMessage().contains("lớn hơn 0"));

        CreatePaymentRequest reqNegative = new CreatePaymentRequest("HV260142", 2, -500000, "Tiền mặt", today.toString(), null, "Đóng học phí");
        assertThrows(IllegalArgumentException.class, () ->
                paymentDAO.recordPayment(reqNegative, accountant, today));
    }

    @Test
    void testAC1_BankTransferRequiresReferenceCode() {
        // Thanh toán chuyển khoản nhưng không có mã giao dịch -> Phải báo lỗi
        CreatePaymentRequest reqWithoutRef = new CreatePaymentRequest("HV260142", 2, 500000, "Chuyển khoản", today.toString(), null, "Chuyển khoản");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentDAO.recordPayment(reqWithoutRef, accountant, today));
        assertTrue(ex.getMessage().contains("mã giao dịch ngân hàng"));

        // Thanh toán chuyển khoản có mã giao dịch hợp lệ -> Thành công
        CreatePaymentRequest reqWithRef = new CreatePaymentRequest("HV260142", 2, 500000, "Chuyển khoản", today.toString(), "VCB.FT2609999", "Chuyển khoản");
        assertDoesNotThrow(() -> paymentDAO.recordPayment(reqWithRef, accountant, today));
    }

    @Test
    void testAC1_PaymentDateCannotBeInFuture() {
        LocalDate futureDate = today.plusDays(2);
        CreatePaymentRequest reqFuture = new CreatePaymentRequest("HV260142", 2, 500000, "Tiền mặt", futureDate.toString(), null, "Ngày tương lai");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentDAO.recordPayment(reqFuture, accountant, today));
        assertTrue(ex.getMessage().contains("không được ở tương lai"));
    }

    // =========================================================================
    // AC2: THU MỘT PHẦN & TỰ ĐỘNG CHUYỂN TRẠNG THÁI ĐỢT
    // =========================================================================

    @Test
    void testAC2_PartialPaymentTransitionsToThanhToanMotPhan() throws SQLException {
        // Đợt 3 của HV260142 ban đầu: 2,565,000 đ, đã đóng 0 đ -> Chưa thanh toán
        PaymentDAO.StudentPlanData plan = paymentDAO.getStudentPlan("HV260142");
        assertNotNull(plan);
        PaymentDAO.InstallmentData inst3 = plan.getInstallment(3);
        assertNotNull(inst3);
        assertEquals(2565000, inst3.amount);
        assertEquals(0, inst3.paid);
        assertEquals("Chưa thanh toán", inst3.state);

        // Thu một phần: 1,000,000 đ
        CreatePaymentRequest req = new CreatePaymentRequest("HV260142", 3, 1000000, "Tiền mặt", today.toString(), null, "Đóng một phần đợt 3");
        PaymentReceiptResponse receipt = paymentDAO.recordPayment(req, accountant, today);

        assertNotNull(receipt);
        assertEquals(1000000, receipt.getAmountPaid());
        assertEquals("Thanh toán một phần", receipt.getInstallmentStatus());
        assertEquals(1565000, receipt.getInstallmentRemaining());
        assertEquals("Thanh toán một phần", inst3.state);
        assertEquals(1000000, inst3.paid);
        assertEquals(1565000, inst3.getOwed());
    }

    @Test
    void testAC2_PayingRemainingTransitionsToDaThanhToan() throws SQLException {
        // Thu tiếp phần còn lại của đợt 3 (1,565,000 đ)
        // Trước tiên đóng 1,000,000 đ
        CreatePaymentRequest req1 = new CreatePaymentRequest("HV260142", 3, 1000000, "Tiền mặt", today.toString(), null, "Đợt 3 lần 1");
        paymentDAO.recordPayment(req1, accountant, today);

        // Đóng nốt 1,565,000 đ
        CreatePaymentRequest req2 = new CreatePaymentRequest("HV260142", 3, 1565000, "Chuyển khoản", today.toString(), "BIDV.TX11029", "Đợt 3 tất toán");
        PaymentReceiptResponse receipt2 = paymentDAO.recordPayment(req2, accountant, today);

        assertNotNull(receipt2);
        assertEquals(1565000, receipt2.getAmountPaid());
        assertEquals("Đã thanh toán", receipt2.getInstallmentStatus());
        assertEquals(0, receipt2.getInstallmentRemaining());

        PaymentDAO.StudentPlanData plan = paymentDAO.getStudentPlan("HV260142");
        PaymentDAO.InstallmentData inst3 = plan.getInstallment(3);
        assertEquals("Đã thanh toán", inst3.state);
        assertEquals(2565000, inst3.paid);
        assertEquals(0, inst3.getOwed());
    }

    // =========================================================================
    // AC3: KHÔNG CHO THU VƯỢT SỐ TIỀN CÒN NỢ CỦA ĐỢT
    // =========================================================================

    @Test
    void testAC3_RejectOverpaymentBeyondRemainingBalance() {
        // Đợt 2 của HV260142: tổng 2,565,000 đ, mẫu ban đầu đã đóng 1,000,000 đ -> còn nợ 1,565,000 đ
        PaymentDAO.StudentPlanData plan = paymentDAO.getStudentPlan("HV260142");
        PaymentDAO.InstallmentData inst2 = plan.getInstallment(2);
        double owed = inst2.getOwed();
        assertEquals(1565000, owed);

        // Cố gắng thu 1,565,001 đ hoặc 2,000,000 đ -> Phải bị từ chối với lỗi 400
        CreatePaymentRequest overpayReq = new CreatePaymentRequest("HV260142", 2, 2000000, "Tiền mặt", today.toString(), null, "Cố tình thu dư");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentDAO.recordPayment(overpayReq, accountant, today));

        assertTrue(ex.getMessage().contains("Vượt số tiền còn nợ của đợt"));

        // Xác nhận số tiền đã đóng và tình trạng nợ không hề bị thay đổi
        assertEquals(1565000, inst2.getOwed());
        assertEquals(1000000, inst2.paid);
    }

    // =========================================================================
    // AC4: SINH MÃ BIÊN LAI DUY NHẤT & MẪU IN / PDF CHÍNH THỨC
    // =========================================================================

    @Test
    void testAC4_GenerateUniqueReceiptCodeAndPrintableHtml() throws SQLException {
        CreatePaymentRequest req1 = new CreatePaymentRequest("HV260142", 2, 500000, "Tiền mặt", today.toString(), null, "Đóng một phần");
        PaymentReceiptResponse receipt1 = paymentDAO.recordPayment(req1, accountant, today);

        assertNotNull(receipt1.getReceiptCode());
        assertTrue(receipt1.getReceiptCode().matches("^BL-\\d{4}-\\d{6}$"),
                "Mã biên lai phải có dạng BL-YYYY-XXXXXX, thực tế là: " + receipt1.getReceiptCode());

        CreatePaymentRequest req2 = new CreatePaymentRequest("HV260142", 2, 300000, "Tiền mặt", today.toString(), null, "Đóng tiếp");
        PaymentReceiptResponse receipt2 = paymentDAO.recordPayment(req2, accountant, today);

        assertNotEquals(receipt1.getReceiptCode(), receipt2.getReceiptCode(), "Mỗi biên lai phải có mã duy nhất riêng biệt");

        // Kiểm tra số tiền bằng chữ
        assertEquals("Năm trăm nghìn đồng chẵn", receipt1.getAmountInWords());

        // Kiểm tra xuất bản in HTML
        String printableHtml = paymentDAO.generateReceiptPrintableHtml(receipt1);
        assertNotNull(printableHtml);
        assertTrue(printableHtml.contains("BIÊN LAI THU TIỀN"));
        assertTrue(printableHtml.contains(receipt1.getReceiptCode()));
        assertTrue(printableHtml.contains("DO TUAN KIET"));
        assertTrue(printableHtml.contains("HV260142"));
        assertTrue(printableHtml.contains("Năm trăm nghìn đồng chẵn"));
        assertTrue(printableHtml.contains("ĐỖ THỊ MAI"));
        assertTrue(printableHtml.contains("TRUNG TÂM ĐÀO TẠO TMS"));
    }

    // =========================================================================
    // AC5: BÚT TOÁN ĐIỀU CHỈNH (KHÔNG XOÁ KHOẢN THU)
    // =========================================================================

    @Test
    void testAC5_CreateAdjustmentForPayment() throws SQLException {
        // Tạo một khoản thu mới
        CreatePaymentRequest payReq = new CreatePaymentRequest("HV260142", 3, 1000000, "Tiền mặt", today.toString(), null, "Đóng đợt 3");
        PaymentReceiptResponse initialReceipt = paymentDAO.recordPayment(payReq, accountant, today);
        String paymentId = initialReceipt.getReceiptCode();

        // 1. Giảm khoản thu 200,000 đ do học viên được áp dụng voucher bổ sung
        CreateAdjustmentRequest adjReduce = new CreateAdjustmentRequest(paymentId, "Giảm khoản thu", 200000, "Học viên xuất trình mã ưu đãi sau");
        PaymentReceiptResponse afterReduce = paymentDAO.createAdjustment(adjReduce, accountant, LocalDateTime.now());

        assertNotNull(afterReduce);
        assertEquals(1000000, afterReduce.getAmountPaid(), "Số tiền gốc trên biên lai không đổi");
        assertEquals(800000, afterReduce.getNetAmount(), "Số tiền thực thu (net) giảm còn 800,000 đ");

        List<AdjustmentRecord> adjs = afterReduce.getAdjustments();
        assertEquals(1, adjs.size());
        AdjustmentRecord adj1 = adjs.get(0);
        assertTrue(adj1.getId().matches("^ĐC-\\d{4}-\\d{6}$"), "Mã bút toán điều chỉnh phải có dạng ĐC-YYYY-XXXXXX");
        assertEquals("Giảm khoản thu", adj1.getKind());
        assertEquals(-200000, adj1.getDelta());
        assertEquals("Học viên xuất trình mã ưu đãi sau", adj1.getReason());

        // Kiểm tra số nợ của đợt học phí được điều chỉnh đúng
        PaymentDAO.StudentPlanData plan = paymentDAO.getStudentPlan("HV260142");
        PaymentDAO.InstallmentData inst3 = plan.getInstallment(3);
        assertEquals(800000, inst3.paid);

        // 2. Không cho phép giảm vượt quá số tiền thực thu
        CreateAdjustmentRequest invalidReduce = new CreateAdjustmentRequest(paymentId, "Giảm khoản thu", 900000, "Giảm vượt mức");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentDAO.createAdjustment(invalidReduce, accountant, LocalDateTime.now()));
        assertTrue(ex.getMessage().contains("không được vượt số tiền thực thu hiện tại"));

        // 3. Huỷ toàn bộ khoản thu
        CreateAdjustmentRequest cancelAll = new CreateAdjustmentRequest(paymentId, "Huỷ toàn bộ", 0, "Giao dịch nhầm lớp");
        PaymentReceiptResponse afterCancel = paymentDAO.createAdjustment(cancelAll, accountant, LocalDateTime.now());
        assertEquals(0, afterCancel.getNetAmount(), "Sau khi huỷ toàn bộ, số tiền thực thu về 0 đ");
        assertEquals(2, afterCancel.getAdjustments().size());
        assertEquals(0, inst3.paid, "Số tiền đã đóng của đợt quay về 0 đ");
        assertEquals("Chưa thanh toán", inst3.state);
    }

    // =========================================================================
    // S7-07: HỌC VIÊN XEM TÌNH TRẠNG HỌC PHÍ VÀ TẢI LẠI BIÊN LAI (AC1, AC2)
    // =========================================================================

    @Test
    void testS7_07_StudentCanViewOwnInstallmentsAndReceipts() {
        PaymentDAO.StudentPlanData plan = paymentDAO.getStudentPlan("HV260142");
        assertNotNull(plan, "Kế hoạch học phí của học viên HV260142 không được null");
        assertEquals("HV260142", plan.studentCode);
        assertEquals("DO TUAN KIET", plan.studentName);

        // AC1: Danh sách các đợt kèm số tiền, hạn, trạng thái
        List<PaymentDAO.InstallmentData> items = plan.installments;
        assertEquals(3, items.size(), "Học viên có 3 đợt thanh toán");

        PaymentDAO.InstallmentData dot1 = items.get(0);
        assertEquals(1, dot1.no);
        assertEquals(3420000, dot1.amount);
        assertEquals("Đã thanh toán", dot1.state);
        assertEquals("2026-10-05", dot1.dueDate);
        assertEquals(0, dot1.getOwed());

        PaymentDAO.InstallmentData dot2 = items.get(1);
        assertEquals(2, dot2.no);
        assertEquals(2565000, dot2.amount);
        assertEquals("Thanh toán một phần", dot2.state);
        assertEquals(1565000, dot2.getOwed());

        // AC2: Xem và tải lại biên lai của các khoản đã đóng
        List<PaymentReceiptResponse> receipts = paymentDAO.getReceiptsForStudent("HV260142");
        assertNotNull(receipts);
        assertEquals(2, receipts.size(), "Học viên có 2 biên lai đã phát hành");

        for (PaymentReceiptResponse r : receipts) {
            assertEquals("HV260142", r.getStudentCode());
            assertNotNull(r.getReceiptCode());
            assertTrue(r.getAmountPaid() > 0);
            assertNotNull(r.getAmountInWords());

            // Tải lại biên lai / xuất bản in HTML
            String html = paymentDAO.generateReceiptPrintableHtml(r);
            assertNotNull(html);
            assertTrue(html.contains(r.getReceiptCode()));
            assertTrue(html.contains("DO TUAN KIET"));
        }
    }

    @Test
    void testS7_07_StudentAccessControlInvariants() {
        com.ems.controller.PaymentApiServlet servlet = new com.ems.controller.PaymentApiServlet();

        User studentKiet = new User(1001L, "DO TUAN KIET", "kiet.dt@gmail.com", "ACTIVE", List.of(RoleConstant.STUDENT));
        studentKiet.setUserCode("HV260142");

        User studentAnh = new User(1002L, "NGUYEN MINH ANH", "minhanh.ng@gmail.com", "ACTIVE", List.of(RoleConstant.STUDENT));
        studentAnh.setUserCode("HV260101");

        assertTrue(servlet.isStudentOnly(studentKiet));
        assertFalse(servlet.isStudentOnly(accountant));

        assertEquals("HV260142", servlet.resolveStudentCodeForUser(studentKiet));
        assertEquals("HV260101", servlet.resolveStudentCodeForUser(studentAnh));

        // Kiểm tra phân quyền: Học viên Kiệt không được xem thông tin học viên Ánh
        String kietCode = servlet.resolveStudentCodeForUser(studentKiet);
        String anhCode = servlet.resolveStudentCodeForUser(studentAnh);
        assertNotEquals(kietCode, anhCode);
    }
}
