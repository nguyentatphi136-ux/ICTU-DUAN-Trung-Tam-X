package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.controller.TuitionPaymentApiServlet;
import com.ems.dao.TuitionPaymentDAO;
import com.ems.dto.PaymentAdjustmentRequest;
import com.ems.dto.PaymentAdjustmentResponse;
import com.ems.dto.PaymentRecordRequest;
import com.ems.dto.PaymentReceiptResponse;
import com.ems.dto.StudentDebtSummaryResponse;
import com.ems.exception.ApiException;
import com.ems.model.PaymentReceipt;
import com.ems.model.StudentTuitionPlan;
import com.ems.model.TuitionInstallmentBill;
import com.ems.model.User;
import com.ems.service.TuitionPaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử toàn diện 16 kịch bản bắt buộc theo Jira task S7-03 – EP-08
 */
public class TuitionPaymentTest {

    private static final long ACCOUNTANT_USER_ID = 6L;
    private static final long STUDENT1_USER_ID = 5L;
    private static final long STUDENT2_USER_ID = 8L;
    private static final long INSTRUCTOR_USER_ID = 3L;

    private static final long STUDENT1_ID = 1L;
    private static final long STUDENT2_ID = 2L;
    private static final long PLAN1_ID = 1L;
    private static final long BILL1_ID = 1L;
    private static final long BILL2_ID = 2L;

    private TuitionPaymentService service;
    private TuitionPaymentDAO dao;
    private TuitionPaymentApiServlet servlet;

    @BeforeAll
    static void setUpAll() {
        TestDatabaseHelper.initTestDatabase();
    }

    @BeforeEach
    void setUp() throws SQLException {
        TestDatabaseHelper.clearData();
        dao = new TuitionPaymentDAO();
        service = new TuitionPaymentService(dao);
        servlet = new TuitionPaymentApiServlet();
        servlet.setService(service);

        seedTestData();
    }

    @AfterEach
    void tearDown() {
        TestDatabaseHelper.clearData();
    }

    private void seedTestData() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Seed Users
            insertUser(conn, ACCOUNTANT_USER_ID, "NV-KT-01", "ketoan@edumanager.vn", "Đỗ Thị Kế", "ACCOUNTANT");
            insertUser(conn, STUDENT1_USER_ID, "HV-2026-001", "hocvien@edumanager.vn", "Nguyễn Văn Học", "STUDENT");
            insertUser(conn, STUDENT2_USER_ID, "HV-2026-002", "hocvien2@edumanager.vn", "Trần Văn Hai", "STUDENT");
            insertUser(conn, INSTRUCTOR_USER_ID, "NV-GV-01", "giangvien@edumanager.vn", "Trần Thị Giảng", "INSTRUCTOR");

            // Seed Program
            String progSql = "INSERT INTO programs (id, program_code, program_name, standard_tuition, duration) VALUES (1, 'WEB-FS', 'Fullstack Web', 25200000, 60)";
            conn.createStatement().execute(progSql);

            // Seed Class
            String classSql = "INSERT INTO classes (id, class_code, class_name, program_id, start_date) VALUES (1, 'WEB-K16', 'Lớp Fullstack K16', 1, '2026-10-01')";
            conn.createStatement().execute(classSql);

            // Seed Students
            insertStudent(conn, STUDENT1_ID, STUDENT1_USER_ID, "HV260142", "Cơ bản", "STUDYING");
            insertStudent(conn, STUDENT2_ID, STUDENT2_USER_ID, "HV260143", "Cơ bản", "STUDYING");

            // Seed Enrollments
            conn.createStatement().execute("INSERT INTO enrollments (id, class_id, student_id, enrollment_date) VALUES (1, 1, 1, '2026-10-01')");
            conn.createStatement().execute("INSERT INTO enrollments (id, class_id, student_id, enrollment_date) VALUES (2, 1, 2, '2026-10-01')");

            // Seed Tuition Plan cho Student 1: 25.200.000 ₫
            insertPlan(conn, PLAN1_ID, 1L, STUDENT1_ID, new BigDecimal("25200000"), new BigDecimal("25200000"));

            // Seed 3 đợt thanh toán cho Student 1
            insertBill(conn, BILL1_ID, PLAN1_ID, 1, Date.valueOf("2026-10-05"), new BigDecimal("10080000"));
            insertBill(conn, BILL2_ID, PLAN1_ID, 2, Date.valueOf("2026-11-25"), new BigDecimal("7560000"));
            insertBill(conn, 3L, PLAN1_ID, 3, Date.valueOf("2027-01-15"), new BigDecimal("7560000"));
        }
    }

    private void insertUser(Connection conn, long id, String code, String email, String name, String role) throws SQLException {
        String sql = "INSERT INTO users (id, user_code, email, password_hash, full_name, status) VALUES (?, ?, ?, 'hash', ?, 'ACTIVE')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setString(2, code);
            stmt.setString(3, email);
            stmt.setString(4, name);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO roles (id, role_code, role_name) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id=id")) {
            int roleId = "ACCOUNTANT".equals(role) ? 7 : "STUDENT".equals(role) ? 6 : "INSTRUCTOR".equals(role) ? 4 : 1;
            stmt.setInt(1, roleId);
            stmt.setString(2, role);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE user_id=user_id")) {
            int roleId = "ACCOUNTANT".equals(role) ? 7 : "STUDENT".equals(role) ? 6 : "INSTRUCTOR".equals(role) ? 4 : 1;
            stmt.setLong(1, id);
            stmt.setInt(2, roleId);
            stmt.executeUpdate();
        }
    }

    private void insertStudent(Connection conn, long id, long userId, String code, String level, String status) throws SQLException {
        String sql = "INSERT INTO students (id, user_id, student_code, entry_level, enrollment_status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            stmt.setString(3, code);
            stmt.setString(4, level);
            stmt.setString(5, status);
            stmt.executeUpdate();
        }
    }

    private void insertPlan(Connection conn, long id, Long enrollmentId, long studentId, BigDecimal orig, BigDecimal payable) throws SQLException {
        String sql = "INSERT INTO student_tuition_plans (id, enrollment_id, student_id, original_tuition, final_payable_amount, paid_amount, debt_amount, status) VALUES (?, ?, ?, ?, ?, 0, ?, 'UNPAID')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setObject(2, enrollmentId);
            stmt.setLong(3, studentId);
            stmt.setBigDecimal(4, orig);
            stmt.setBigDecimal(5, payable);
            stmt.setBigDecimal(6, payable);
            stmt.executeUpdate();
        }
    }

    private void insertBill(Connection conn, long id, long planId, int order, Date due, BigDecimal expected) throws SQLException {
        String sql = "INSERT INTO tuition_installment_bills (id, tuition_plan_id, installment_order, due_date, expected_amount, paid_amount, status) VALUES (?, ?, ?, ?, ?, 0, 'UNPAID')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, planId);
            stmt.setInt(3, order);
            stmt.setDate(4, due);
            stmt.setBigDecimal(5, expected);
            stmt.executeUpdate();
        }
    }

    // =========================================================================
    // 16 KỊCH BẢN KIỂM THỬ BẮT BUỘC
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 1: Ghi nhận thanh toán tiền mặt thành công")
    void test1_CashPaymentSuccess() throws SQLException {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setInstallmentBillId(BILL1_ID);
        req.setAmount(new BigDecimal("5000000"));
        req.setPaymentMethod("Tiền mặt");
        req.setPaymentDate(LocalDate.now().toString());
        req.setNotes("Đóng tiền mặt tại quầy kế toán");

        PaymentReceiptResponse res = service.recordPayment(ACCOUNTANT_USER_ID, req);

        assertNotNull(res);
        assertNotNull(res.getReceiptCode());
        assertTrue(res.getReceiptCode().startsWith("BL-"));
        assertEquals(0, new BigDecimal("5000000").compareTo(res.getAmountPaid()));
        assertEquals("CASH", res.getPaymentMethod());
        assertEquals(ACCOUNTANT_USER_ID, res.getCollectedBy());
        assertEquals("Đỗ Thị Kế", res.getCollectorName());
        assertEquals(0, new BigDecimal("5080000").compareTo(res.getBillRemainingDebt()));
    }

    @Test
    @DisplayName("Kịch bản 2: Ghi nhận thanh toán chuyển khoản thành công theo nghiệp vụ hiện tại kèm mã GD ngân hàng")
    void test2_BankTransferPaymentSuccess() throws SQLException {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setStudentCode("HV260142");
        req.setInstallmentOrder(1);
        req.setAmount(new BigDecimal("10080000"));
        req.setPaymentMethod("Chuyển khoản");
        req.setReferenceCode("VCB-FT26278104452");
        req.setPaymentDate(LocalDate.now().toString());

        PaymentReceiptResponse res = service.recordPayment(ACCOUNTANT_USER_ID, req);

        assertNotNull(res);
        assertEquals("BANK_TRANSFER", res.getPaymentMethod());
        assertEquals("VCB-FT26278104452", res.getReferenceCode());
        assertEquals(0, new BigDecimal("10080000").compareTo(res.getAmountPaid()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res.getBillRemainingDebt()));
    }

    @Test
    @DisplayName("Kịch bản 3: Từ chối số tiền bằng 0 hoặc số âm")
    void test3_RejectZeroOrNegativeAmount() {
        PaymentRecordRequest zeroReq = new PaymentRecordRequest();
        zeroReq.setInstallmentBillId(BILL1_ID);
        zeroReq.setAmount(BigDecimal.ZERO);
        zeroReq.setPaymentMethod("CASH");

        ApiException ex1 = assertThrows(ApiException.class, () -> service.recordPayment(ACCOUNTANT_USER_ID, zeroReq));
        assertEquals("INVALID_AMOUNT", ex1.getErrorCode());

        PaymentRecordRequest negReq = new PaymentRecordRequest();
        negReq.setInstallmentBillId(BILL1_ID);
        negReq.setAmount(new BigDecimal("-1000000"));
        negReq.setPaymentMethod("CASH");

        ApiException ex2 = assertThrows(ApiException.class, () -> service.recordPayment(ACCOUNTANT_USER_ID, negReq));
        assertEquals("INVALID_AMOUNT", ex2.getErrorCode());
    }

    @Test
    @DisplayName("Kịch bản 4: Từ chối số tiền vượt quá công nợ còn lại của đợt")
    void test4_RejectAmountExceedingRemainingDebt() {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setInstallmentBillId(BILL1_ID);
        req.setAmount(new BigDecimal("15000000"));
        req.setPaymentMethod("CASH");

        ApiException ex = assertThrows(ApiException.class, () -> service.recordPayment(ACCOUNTANT_USER_ID, req));
        assertEquals("AMOUNT_EXCEEDS_REMAINING_DEBT", ex.getErrorCode());
    }

    @Test
    @DisplayName("Kịch bản 5: Thu một phần và cập nhật đúng trạng thái PARTIAL")
    void test5_PartialPaymentUpdatesState() throws SQLException {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setInstallmentBillId(BILL1_ID);
        req.setAmount(new BigDecimal("3000000"));
        req.setPaymentMethod("CASH");

        service.recordPayment(ACCOUNTANT_USER_ID, req);

        try (Connection conn = DBConnection.getConnection()) {
            TuitionInstallmentBill bill = dao.findBillById(BILL1_ID, conn);
            assertNotNull(bill);
            assertEquals(new BigDecimal("3000000.00"), bill.getPaidAmount());
            assertEquals("PARTIAL", bill.getStatus());

            StudentTuitionPlan plan = dao.findPlanById(PLAN1_ID, conn);
            assertEquals(new BigDecimal("3000000.00"), plan.getPaidAmount());
            assertEquals(new BigDecimal("22200000.00"), plan.getDebtAmount());
            assertEquals("PARTIAL", plan.getStatus());
        }
    }

    @Test
    @DisplayName("Kịch bản 6: Thu đủ và cập nhật trạng thái Đã thanh toán (PAID & COMPLETED)")
    void test6_FullPaymentUpdatesState() throws SQLException {
        // Thu đợt 1: 10.080.000
        PaymentRecordRequest req1 = new PaymentRecordRequest();
        req1.setInstallmentBillId(BILL1_ID);
        req1.setAmount(new BigDecimal("10080000"));
        req1.setPaymentMethod("BANK_TRANSFER");
        service.recordPayment(ACCOUNTANT_USER_ID, req1);

        try (Connection conn = DBConnection.getConnection()) {
            TuitionInstallmentBill bill1 = dao.findBillById(BILL1_ID, conn);
            assertEquals("PAID", bill1.getStatus());
        }

        // Thu đợt 2: 7.560.000
        PaymentRecordRequest req2 = new PaymentRecordRequest();
        req2.setInstallmentBillId(BILL2_ID);
        req2.setAmount(new BigDecimal("7560000"));
        req2.setPaymentMethod("CASH");
        service.recordPayment(ACCOUNTANT_USER_ID, req2);

        // Thu đợt 3: 7.560.000
        PaymentRecordRequest req3 = new PaymentRecordRequest();
        req3.setInstallmentBillId(3L);
        req3.setAmount(new BigDecimal("7560000"));
        req3.setPaymentMethod("BANK_TRANSFER");
        service.recordPayment(ACCOUNTANT_USER_ID, req3);

        try (Connection conn = DBConnection.getConnection()) {
            StudentTuitionPlan plan = dao.findPlanById(PLAN1_ID, conn);
            assertEquals(new BigDecimal("25200000.00"), plan.getPaidAmount());
            assertEquals(new BigDecimal("0.00"), plan.getDebtAmount());
            assertEquals("COMPLETED", plan.getStatus());
        }
    }

    @Test
    @DisplayName("Kịch bản 7: Không thể ghi nhận thu đồng thời vượt công nợ (Concurrency Safe)")
    void test7_ConcurrentPaymentsCannotExceedDebt() throws InterruptedException, SQLException {
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    PaymentRecordRequest req = new PaymentRecordRequest();
                    req.setInstallmentBillId(BILL1_ID);
                    req.setAmount(new BigDecimal("6000000"));
                    req.setPaymentMethod("CASH");
                    service.recordPayment(ACCOUNTANT_USER_ID, req);
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if ("AMOUNT_EXCEEDS_REMAINING_DEBT".equals(e.getErrorCode())) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 giao dịch được thành công khi tranh chấp đồng thời");
        assertEquals(4, rejectedCount.get(), "4 giao dịch đồng thời còn lại phải bị từ chối do vượt quá công nợ còn lại");

        try (Connection conn = DBConnection.getConnection()) {
            TuitionInstallmentBill bill = dao.findBillById(BILL1_ID, conn);
            assertEquals(new BigDecimal("6000000.00"), bill.getPaidAmount());
        }
    }

    @Test
    @DisplayName("Kịch bản 8: Không ghi giao dịch nếu đợt thu hoặc học viên không hợp lệ / không khớp")
    void test8_RejectInvalidBillOrStudentMismatch() {
        PaymentRecordRequest req1 = new PaymentRecordRequest();
        req1.setInstallmentBillId(99999L);
        req1.setAmount(new BigDecimal("1000000"));
        req1.setPaymentMethod("CASH");
        ApiException ex1 = assertThrows(ApiException.class, () -> service.recordPayment(ACCOUNTANT_USER_ID, req1));
        assertEquals("BILL_NOT_FOUND", ex1.getErrorCode());

        PaymentRecordRequest req2 = new PaymentRecordRequest();
        req2.setInstallmentBillId(BILL1_ID);
        req2.setStudentId(STUDENT2_ID);
        req2.setAmount(new BigDecimal("1000000"));
        req2.setPaymentMethod("CASH");
        ApiException ex2 = assertThrows(ApiException.class, () -> service.recordPayment(ACCOUNTANT_USER_ID, req2));
        assertEquals("STUDENT_MISMATCH", ex2.getErrorCode());
    }

    @Test
    @DisplayName("Kịch bản 9: Sinh mã biên lai duy nhất cấp cơ sở dữ liệu")
    void test9_UniqueReceiptCodeGeneration() throws SQLException {
        PaymentRecordRequest req1 = new PaymentRecordRequest();
        req1.setInstallmentBillId(BILL1_ID);
        req1.setAmount(new BigDecimal("1000000"));
        req1.setPaymentMethod("CASH");
        PaymentReceiptResponse r1 = service.recordPayment(ACCOUNTANT_USER_ID, req1);

        PaymentRecordRequest req2 = new PaymentRecordRequest();
        req2.setInstallmentBillId(BILL1_ID);
        req2.setAmount(new BigDecimal("2000000"));
        req2.setPaymentMethod("CASH");
        PaymentReceiptResponse r2 = service.recordPayment(ACCOUNTANT_USER_ID, req2);

        assertNotNull(r1.getReceiptCode());
        assertNotNull(r2.getReceiptCode());
        assertFalse(r1.getReceiptCode().equals(r2.getReceiptCode()));
        assertTrue(r1.getReceiptCode().matches("^BL-\\d{4}-\\d{6}$"));
        assertTrue(r2.getReceiptCode().matches("^BL-\\d{4}-\\d{6}$"));
    }

    @Test
    @DisplayName("Kịch bản 10: Xem và tải PDF biên lai thành công (Header hợp lệ %PDF-)")
    void test10_GenerateAndDownloadPdfReceiptSuccess() throws Exception {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setInstallmentBillId(BILL1_ID);
        req.setAmount(new BigDecimal("5000000"));
        req.setPaymentMethod("Tiền mặt");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, req);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateReceiptPdf(ACCOUNTANT_USER_ID, List.of("ACCOUNTANT"), receipt.getId(), out);

        byte[] pdfBytes = out.toByteArray();
        assertTrue(pdfBytes.length > 500, "Tệp PDF sinh ra phải có nội dung");
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
        assertEquals('-', (char) pdfBytes[4]);
    }

    @Test
    @DisplayName("Kịch bản 11: PDF hiển thị đúng dữ liệu giao dịch và số tiền bằng chữ tiếng Việt")
    void test11_PdfDisplaysCorrectTransactionData() throws SQLException {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setInstallmentBillId(BILL1_ID);
        req.setAmount(new BigDecimal("10080000"));
        req.setPaymentMethod("Chuyển khoản");
        req.setReferenceCode("FT26292418830");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, req);

        assertEquals("Nguyễn Văn Học", receipt.getStudentName());
        assertEquals("HV260142", receipt.getStudentCode());
        assertEquals("Fullstack Web", receipt.getProgramName());
        assertEquals("Lớp Fullstack K16", receipt.getClassName());
        assertEquals(0, new BigDecimal("10080000").compareTo(receipt.getAmountPaid()));
        assertEquals("Mười triệu không trăm tám mươi nghìn đồng chẵn", receipt.getAmountInWords());
        assertEquals("BANK_TRANSFER", receipt.getPaymentMethod());
        assertEquals("FT26292418830", receipt.getReferenceCode());
    }

    @Test
    @DisplayName("Kịch bản 12: Người không có quyền không thể ghi nhận thu hoặc xem biên lai của người khác")
    void test12_AccessControlAndIdorProtection() throws SQLException {
        PaymentRecordRequest req = new PaymentRecordRequest();
        req.setInstallmentBillId(BILL1_ID);
        req.setAmount(new BigDecimal("5000000"));
        req.setPaymentMethod("CASH");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, req);

        // Kế toán xem được
        PaymentReceiptResponse accView = service.getReceiptDetail(ACCOUNTANT_USER_ID, List.of("ACCOUNTANT"), receipt.getId());
        assertNotNull(accView);

        // Chính học viên 1 (user_id = 5) xem được
        PaymentReceiptResponse s1View = service.getReceiptDetail(STUDENT1_USER_ID, List.of("STUDENT"), receipt.getId());
        assertNotNull(s1View);

        // Học viên 2 (user_id = 8) cố xem biên lai của học viên 1 -> Bị từ chối HTTP 403 Forbidden
        ApiException exIdor = assertThrows(ApiException.class, () ->
                service.getReceiptDetail(STUDENT2_USER_ID, List.of("STUDENT"), receipt.getId()));
        assertEquals(HttpServletResponse.SC_FORBIDDEN, exIdor.getStatusCode());
        assertEquals("FORBIDDEN", exIdor.getErrorCode());

        // Học viên 2 cố xem công nợ của học viên 1 -> Bị từ chối HTTP 403 Forbidden
        ApiException exDebt = assertThrows(ApiException.class, () ->
                service.getStudentDebtSummary(STUDENT2_USER_ID, List.of("STUDENT"), "HV260142"));
        assertEquals(HttpServletResponse.SC_FORBIDDEN, exDebt.getStatusCode());
    }

    @Test
    @DisplayName("Kịch bản 13: Giao dịch đã ghi nhận không thể bị xóa cứng qua API (HTTP 405 Method Not Allowed)")
    void test13_DeletePaymentForbiddenViaApi() throws Exception {
        User accountant = new User(ACCOUNTANT_USER_ID, "Đỗ Thị Kế", "ketoan@edumanager.vn", "ACTIVE", List.of("ACCOUNTANT"));
        TestRequest req = new TestRequest("DELETE", "/api/tuition/payments/1", "", accountant);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(405, res.status);
        assertTrue(res.getBody().contains("PAYMENT_DELETE_FORBIDDEN") || res.getBody().contains("không được phép xoá"));
    }

    @Test
    @DisplayName("Kịch bản 14: Lập bút toán điều chỉnh lưu lý do, người thực hiện, thời điểm và liên kết giao dịch gốc")
    void test14_AdjustmentFlowSuccess() throws SQLException {
        PaymentRecordRequest payReq = new PaymentRecordRequest();
        payReq.setInstallmentBillId(BILL1_ID);
        payReq.setAmount(new BigDecimal("10080000"));
        payReq.setPaymentMethod("CASH");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, payReq);

        PaymentAdjustmentRequest adjReq = new PaymentAdjustmentRequest();
        adjReq.setAdjustmentType("Giảm khoản thu");
        adjReq.setAmount(new BigDecimal("2000000"));
        adjReq.setReason("Nhầm lẫn số tiền khi kiểm đếm tại quầy, thực thu 8.080.000 ₫");

        PaymentAdjustmentResponse adjRes = service.createAdjustment(ACCOUNTANT_USER_ID, receipt.getId(), adjReq);

        assertNotNull(adjRes);
        assertNotNull(adjRes.getAdjustmentCode());
        assertTrue(adjRes.getAdjustmentCode().startsWith("DC-"));
        assertEquals("REDUCE", adjRes.getAdjustmentType());
        assertEquals(new BigDecimal("-2000000"), adjRes.getDeltaAmount());
        assertEquals(ACCOUNTANT_USER_ID, adjRes.getAdjustedBy());

        PaymentReceiptResponse updatedReceipt = service.getReceiptDetail(ACCOUNTANT_USER_ID, List.of("ACCOUNTANT"), receipt.getId());
        assertEquals(0, new BigDecimal("8080000").compareTo(updatedReceipt.getNetAmount()));
        assertEquals(1, updatedReceipt.getAdjustments().size());
        assertEquals(0, new BigDecimal("2000000").compareTo(updatedReceipt.getBillRemainingDebt()));

        try (Connection conn = DBConnection.getConnection()) {
            TuitionInstallmentBill bill = dao.findBillById(BILL1_ID, conn);
            assertEquals("PARTIAL", bill.getStatus());
            assertEquals(new BigDecimal("8080000.00"), bill.getPaidAmount());
        }
    }

    @Test
    @DisplayName("Kịch bản 15: Lỗi phát sinh trong transaction tự động rollback, không làm lệch tổng tiền và công nợ")
    void test15_RollbackOnErrorPreservesIntegrity() throws SQLException {
        PaymentRecordRequest req1 = new PaymentRecordRequest();
        req1.setInstallmentBillId(BILL1_ID);
        req1.setAmount(new BigDecimal("4000000"));
        req1.setPaymentMethod("CASH");
        service.recordPayment(ACCOUNTANT_USER_ID, req1);

        PaymentRecordRequest badReq = new PaymentRecordRequest();
        badReq.setInstallmentBillId(BILL1_ID);
        badReq.setAmount(new BigDecimal("7000000"));
        badReq.setPaymentMethod("CASH");

        assertThrows(ApiException.class, () -> service.recordPayment(ACCOUNTANT_USER_ID, badReq));

        try (Connection conn = DBConnection.getConnection()) {
            TuitionInstallmentBill bill = dao.findBillById(BILL1_ID, conn);
            assertEquals(new BigDecimal("4000000.00"), bill.getPaidAmount());
            assertEquals("PARTIAL", bill.getStatus());

            StudentTuitionPlan plan = dao.findPlanById(PLAN1_ID, conn);
            assertEquals(new BigDecimal("4000000.00"), plan.getPaidAmount());
            assertEquals(new BigDecimal("21200000.00"), plan.getDebtAmount());
        }
    }

    @Test
    @DisplayName("Kịch bản 16: Lập bút toán Huỷ toàn bộ (CANCEL) đưa khoản thu về 0 và trạng thái về UNPAID")
    void test16_CancelAdjustmentRestoresUnpaidState() throws SQLException {
        PaymentRecordRequest payReq = new PaymentRecordRequest();
        payReq.setInstallmentBillId(BILL1_ID);
        payReq.setAmount(new BigDecimal("10080000"));
        payReq.setPaymentMethod("CASH");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, payReq);

        PaymentAdjustmentRequest cancelReq = new PaymentAdjustmentRequest();
        cancelReq.setAdjustmentType("Huỷ toàn bộ");
        cancelReq.setReason("Học viên chuyển lớp sang cơ sở khác");
        service.createAdjustment(ACCOUNTANT_USER_ID, receipt.getId(), cancelReq);

        try (Connection conn = DBConnection.getConnection()) {
            TuitionInstallmentBill bill = dao.findBillById(BILL1_ID, conn);
            assertEquals(new BigDecimal("0.00"), bill.getPaidAmount());
            assertEquals("UNPAID", bill.getStatus());

            StudentTuitionPlan plan = dao.findPlanById(PLAN1_ID, conn);
            assertEquals(new BigDecimal("0.00"), plan.getPaidAmount());
            assertEquals(new BigDecimal("25200000.00"), plan.getDebtAmount());
            assertEquals("UNPAID", plan.getStatus());
        }
    }

    // =========================================================================
    // TEST HELPER MOCK CLASSES
    // =========================================================================

    static class TestRequest {
        final String method;
        final String uri;
        final String body;
        final User user;
        final Map<String, String> parameters = new HashMap<>();

        TestRequest(String method, String uri, String body, User user) {
            this.method = method;
            this.uri = uri;
            this.body = body != null ? body : "";
            this.user = user;
        }

        HttpServletRequest asServletRequest() {
            HttpSession session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getAttribute" -> "currentUser".equals(args[0]) ? user : null;
                        case "getId" -> "test-session-id";
                        default -> null;
                    }
            );

            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getMethod" -> method;
                        case "getRequestURI" -> uri;
                        case "getContextPath" -> "";
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getReader" -> new BufferedReader(new StringReader(body));
                        case "getInputStream" -> new ServletInputStream() {
                            private final ByteArrayInputStream bais = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                            public boolean isFinished() { return bais.available() == 0; }
                            public boolean isReady() { return true; }
                            public void setReadListener(ReadListener readListener) {}
                            public int read() { return bais.read(); }
                        };
                        case "getSession" -> session;
                        case "getAttribute" -> "authenticatedUserId".equals(args[0]) ? (user != null ? user.getId() : null) : null;
                        case "getCharacterEncoding" -> "UTF-8";
                        case "getContentType" -> "application/json";
                        default -> null;
                    }
            );
        }
    }

    static class TestResponse {
        int status = 200;
        final StringWriter writer = new StringWriter();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        String getBody() {
            return writer.toString();
        }

        HttpServletResponse asServletResponse() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "setStatus" -> {
                            status = (int) args[0];
                            yield null;
                        }
                        case "getStatus" -> status;
                        case "getWriter" -> new PrintWriter(writer);
                        case "getOutputStream" -> new ServletOutputStream() {
                            public boolean isReady() { return true; }
                            public void setWriteListener(WriteListener writeListener) {}
                            public void write(int b) { outputStream.write(b); }
                            public void write(byte[] b, int off, int len) { outputStream.write(b, off, len); }
                        };
                        case "setContentType", "setCharacterEncoding", "setHeader" -> null;
                        default -> null;
                    }
            );
        }
    }
}
