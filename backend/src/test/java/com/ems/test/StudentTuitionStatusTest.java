package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.controller.TuitionPaymentApiServlet;
import com.ems.dao.TuitionPaymentDAO;
import com.ems.dto.InstallmentBillResponse;
import com.ems.dto.PaymentAdjustmentRequest;
import com.ems.dto.PaymentRecordRequest;
import com.ems.dto.PaymentReceiptResponse;
import com.ems.dto.StudentDebtSummaryResponse;
import com.ems.exception.ApiException;
import com.ems.model.TuitionInstallmentBill;
import com.ems.model.User;
import com.ems.service.TuitionPaymentService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử toàn diện 12 kịch bản bắt buộc theo Jira task S7-07 – EP-08:
 * "Học viên xem tình trạng học phí của bản thân"
 */
public class StudentTuitionStatusTest {

    private static final long ACCOUNTANT_USER_ID = 6L;
    private static final long STUDENT1_USER_ID = 5L;
    private static final long STUDENT2_USER_ID = 8L;
    private static final long STUDENT3_USER_ID = 9L; // Học viên chưa có kế hoạch học phí

    private static final long STUDENT1_ID = 1L;
    private static final long STUDENT2_ID = 2L;
    private static final long STUDENT3_ID = 3L;

    private static final long PLAN1_ID = 1L;
    private static final long PLAN2_ID = 2L;

    private static final long BILL1_ID = 1L;
    private static final long BILL2_ID = 2L;
    private static final long BILL3_ID = 3L;
    private static final long BILL4_OVERDUE_ID = 4L;

    private TuitionPaymentService service;
    private TuitionPaymentDAO dao;
    private TuitionPaymentApiServlet servlet;
    private final Gson gson = new Gson();

    private User student1User;
    private User student2User;
    private User student3User;
    private User accountantUser;

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

        student1User = new User(STUDENT1_USER_ID, "Nguyễn Văn Học", "hocvien@edumanager.vn", "ACTIVE", List.of("STUDENT"));
        student2User = new User(STUDENT2_USER_ID, "Trần Văn Hai", "hocvien2@edumanager.vn", "ACTIVE", List.of("STUDENT"));
        student3User = new User(STUDENT3_USER_ID, "Lê Thị Ba", "hocvien3@edumanager.vn", "ACTIVE", List.of("STUDENT"));
        accountantUser = new User(ACCOUNTANT_USER_ID, "Đỗ Thị Kế", "ketoan@edumanager.vn", "ACTIVE", List.of("ACCOUNTANT"));

        seedTestData();
    }

    @AfterEach
    void tearDown() {
        TestDatabaseHelper.clearData();
    }

    private void seedTestData() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Seed Users
            insertUser(conn, ACCOUNTANT_USER_ID, "NV-KT-01", "ketoan@edumanager.vn", "Đỗ Thị Kế", "ACCOUNTANT");
            insertUser(conn, STUDENT1_USER_ID, "HV-2026-001", "hocvien@edumanager.vn", "Nguyễn Văn Học", "STUDENT");
            insertUser(conn, STUDENT2_USER_ID, "HV-2026-002", "hocvien2@edumanager.vn", "Trần Văn Hai", "STUDENT");
            insertUser(conn, STUDENT3_USER_ID, "HV-2026-003", "hocvien3@edumanager.vn", "Lê Thị Ba", "STUDENT");

            // 2. Seed Program
            String progSql = "INSERT INTO programs (id, program_code, program_name, standard_tuition, duration) VALUES (1, 'WEB-FS', 'Fullstack Web', 25200000, 60)";
            conn.createStatement().execute(progSql);

            // 3. Seed Class
            String classSql = "INSERT INTO classes (id, class_code, class_name, program_id, start_date) VALUES (1, 'WEB-K16', 'Lớp Fullstack K16', 1, '2026-10-01')";
            conn.createStatement().execute(classSql);

            // 4. Seed Students
            insertStudent(conn, STUDENT1_ID, STUDENT1_USER_ID, "HV260142", "Cơ bản", "STUDYING");
            insertStudent(conn, STUDENT2_ID, STUDENT2_USER_ID, "HV260143", "Cơ bản", "STUDYING");
            insertStudent(conn, STUDENT3_ID, STUDENT3_USER_ID, "HV260144", "Cơ bản", "STUDYING");

            // 5. Seed Enrollments
            conn.createStatement().execute("INSERT INTO enrollments (id, class_id, student_id, enrollment_date) VALUES (1, 1, 1, '2026-10-01')");
            conn.createStatement().execute("INSERT INTO enrollments (id, class_id, student_id, enrollment_date) VALUES (2, 1, 2, '2026-10-01')");
            conn.createStatement().execute("INSERT INTO enrollments (id, class_id, student_id, enrollment_date) VALUES (3, 1, 3, '2026-10-01')");

            // 6. Seed Tuition Plan cho Student 1: 25.200.000 ₫
            insertPlan(conn, PLAN1_ID, 1L, STUDENT1_ID, new BigDecimal("25200000"), new BigDecimal("25200000"));

            // Seed 3 đợt thanh toán cho Student 1:
            // Đợt 1: Hạn 2026-10-05 (đã qua), 10.080.000 ₫
            insertBill(conn, BILL1_ID, PLAN1_ID, 1, Date.valueOf("2026-10-05"), new BigDecimal("10080000"));
            // Đợt 2: Hạn tương lai (cách 30 ngày), 7.560.000 ₫
            insertBill(conn, BILL2_ID, PLAN1_ID, 2, Date.valueOf(LocalDate.now().plusDays(30)), new BigDecimal("7560000"));
            // Đợt 3: Hạn tương lai (cách 90 ngày), 7.560.000 ₫
            insertBill(conn, BILL3_ID, PLAN1_ID, 3, Date.valueOf(LocalDate.now().plusDays(90)), new BigDecimal("7560000"));

            // 7. Seed Tuition Plan cho Student 2 có 1 đợt quá hạn
            insertPlan(conn, PLAN2_ID, 2L, STUDENT2_ID, new BigDecimal("15000000"), new BigDecimal("15000000"));
            insertBill(conn, BILL4_OVERDUE_ID, PLAN2_ID, 1, Date.valueOf(LocalDate.now().minusDays(10)), new BigDecimal("15000000"));
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
    }

    private void insertStudent(Connection conn, long id, long userId, String code, String entryLevel, String status) throws SQLException {
        String sql = "INSERT INTO students (id, user_id, student_code, entry_level, enrollment_status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, userId);
            stmt.setString(3, code);
            stmt.setString(4, entryLevel);
            stmt.setString(5, status);
            stmt.executeUpdate();
        }
    }

    private void insertPlan(Connection conn, long id, long enrollmentId, long studentId, BigDecimal orig, BigDecimal payable) throws SQLException {
        String sql = "INSERT INTO student_tuition_plans (id, enrollment_id, student_id, original_tuition, final_payable_amount, paid_amount, debt_amount, status) " +
                "VALUES (?, ?, ?, ?, ?, 0.00, ?, 'UNPAID')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, enrollmentId);
            stmt.setLong(3, studentId);
            stmt.setBigDecimal(4, orig);
            stmt.setBigDecimal(5, payable);
            stmt.setBigDecimal(6, payable);
            stmt.executeUpdate();
        }
    }

    private void insertBill(Connection conn, long id, long planId, int order, Date due, BigDecimal amount) throws SQLException {
        String sql = "INSERT INTO tuition_installment_bills (id, tuition_plan_id, installment_order, due_date, expected_amount, paid_amount, status) " +
                "VALUES (?, ?, ?, ?, ?, 0.00, 'UNPAID')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, planId);
            stmt.setInt(3, order);
            stmt.setDate(4, due);
            stmt.setBigDecimal(5, amount);
            stmt.executeUpdate();
        }
    }

    // =========================================================================
    // 12 KỊCH BẢN KIỂM THỬ BẮT BUỘC
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 1: Học viên đăng nhập xem được danh sách đợt thu của mình qua GET /api/tuition/my-fees")
    void test01_StudentViewsOwnTuitionStatus() throws Exception {
        TestRequest req = new TestRequest("GET", "/api/tuition/my-fees", "", student1User);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(200, res.status);
        JsonObject json = JsonParser.parseString(res.getBody()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("MY_FEES_SUCCESS", json.get("code").getAsString());

        JsonObject data = json.getAsJsonObject("data");
        assertEquals("HV260142", data.get("studentCode").getAsString());
        assertEquals("Nguyễn Văn Học", data.get("studentName").getAsString());
        assertEquals("Fullstack Web", data.get("programName").getAsString());
        assertEquals("Lớp Fullstack K16", data.get("className").getAsString());
        assertEquals(3, data.getAsJsonArray("bills").size());
    }

    @Test
    @DisplayName("Kịch bản 2: Danh sách trả đúng số tiền phải đóng, đã đóng và còn nợ")
    void test02_AmountsConsistencyExpectedPaidDebt() throws SQLException {
        // Ghi nhận thanh toán đủ đợt 1 (10.080.000 ₫)
        PaymentRecordRequest payReq = new PaymentRecordRequest();
        payReq.setInstallmentBillId(BILL1_ID);
        payReq.setAmount(new BigDecimal("10080000"));
        payReq.setPaymentMethod("BANK_TRANSFER");
        service.recordPayment(ACCOUNTANT_USER_ID, payReq);

        StudentDebtSummaryResponse summary = service.getMyTuitionStatus(STUDENT1_USER_ID, List.of("STUDENT"), null);

        assertNotNull(summary);
        assertEquals(0, new BigDecimal("25200000").compareTo(summary.getFinalPayableAmount()), "Tổng phải đóng");
        assertEquals(0, new BigDecimal("10080000").compareTo(summary.getTotalPaidAmount()), "Tổng đã đóng");
        assertEquals(0, new BigDecimal("15120000").compareTo(summary.getTotalDebtAmount()), "Còn nợ = Phải đóng - Đã đóng");

        List<InstallmentBillResponse> bills = summary.getBills();
        assertEquals(3, bills.size());

        // Đợt 1
        InstallmentBillResponse b1 = bills.get(0);
        assertEquals(1, b1.getInstallmentOrder());
        assertEquals("Đợt 1", b1.getInstallmentName());
        assertEquals(0, new BigDecimal("10080000").compareTo(b1.getExpectedAmount()));
        assertEquals(0, new BigDecimal("10080000").compareTo(b1.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(b1.getRemainingDebt()));
        assertEquals("PAID", b1.getStatus());
        assertEquals(1, b1.getReceipts().size(), "Đợt 1 phải chứa 1 biên lai thanh toán");

        // Đợt 2
        InstallmentBillResponse b2 = bills.get(1);
        assertEquals(2, b2.getInstallmentOrder());
        assertEquals("Đợt 2", b2.getInstallmentName());
        assertEquals(0, new BigDecimal("7560000").compareTo(b2.getExpectedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(b2.getPaidAmount()));
        assertEquals(0, new BigDecimal("7560000").compareTo(b2.getRemainingDebt()));
        assertEquals("UNPAID", b2.getStatus());
    }

    @Test
    @DisplayName("Kịch bản 3: Trạng thái UNPAID, PARTIAL, PAID, OVERDUE được xác định đúng và hỗ trợ lọc")
    void test03_StatusDetermination_Unpaid_Partial_Paid_Overdue() throws SQLException {
        // Đợt 1 đã trả đủ -> PAID
        PaymentRecordRequest pay1 = new PaymentRecordRequest();
        pay1.setInstallmentBillId(BILL1_ID);
        pay1.setAmount(new BigDecimal("10080000"));
        pay1.setPaymentMethod("BANK_TRANSFER");
        service.recordPayment(ACCOUNTANT_USER_ID, pay1);

        // Đợt 2 thanh toán 3.000.000 ₫ (hạn tương lai) -> PARTIAL
        PaymentRecordRequest pay2 = new PaymentRecordRequest();
        pay2.setInstallmentBillId(BILL2_ID);
        pay2.setAmount(new BigDecimal("3000000"));
        pay2.setPaymentMethod("CASH");
        service.recordPayment(ACCOUNTANT_USER_ID, pay2);

        // Đợt 3 chưa thanh toán (hạn tương lai) -> UNPAID

        StudentDebtSummaryResponse all = service.getMyTuitionStatus(STUDENT1_USER_ID, List.of("STUDENT"), null);
        assertEquals("PAID", all.getBills().get(0).getStatus());
        assertEquals("PARTIAL", all.getBills().get(1).getStatus());
        assertEquals("UNPAID", all.getBills().get(2).getStatus());

        // Kiểm tra học viên 2 có đợt quá hạn -> OVERDUE
        StudentDebtSummaryResponse s2Debt = service.getMyTuitionStatus(STUDENT2_USER_ID, List.of("STUDENT"), null);
        assertEquals(1, s2Debt.getBills().size());
        assertEquals("OVERDUE", s2Debt.getBills().get(0).getStatus());
        assertTrue(s2Debt.getOverdueDays() > 0);

        // Kiểm tra bộ lọc ?status=PARTIAL
        StudentDebtSummaryResponse partialFilter = service.getMyTuitionStatus(STUDENT1_USER_ID, List.of("STUDENT"), "PARTIAL");
        assertEquals(1, partialFilter.getBills().size());
        assertEquals(2, partialFilter.getBills().get(0).getInstallmentOrder());

        // Kiểm tra bộ lọc ?status=PAID
        StudentDebtSummaryResponse paidFilter = service.getMyTuitionStatus(STUDENT1_USER_ID, List.of("STUDENT"), "PAID");
        assertEquals(1, paidFilter.getBills().size());
        assertEquals(1, paidFilter.getBills().get(0).getInstallmentOrder());
    }

    @Test
    @DisplayName("Kịch bản 4: Học viên chưa có kế hoạch hoặc đợt thu nhận được kết quả rỗng hợp lệ (HTTP 200)")
    void test04_StudentWithNoTuitionPlanReceivesValidEmptyResult() throws Exception {
        TestRequest req = new TestRequest("GET", "/api/tuition/my-fees", "", student3User);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(200, res.status);
        JsonObject json = JsonParser.parseString(res.getBody()).getAsJsonObject();
        assertTrue(json.get("success").getAsBoolean());

        JsonObject data = json.getAsJsonObject("data");
        assertEquals("HV260144", data.get("studentCode").getAsString());
        assertEquals("Lê Thị Ba", data.get("studentName").getAsString());
        assertEquals(0, data.getAsJsonArray("bills").size());
        assertEquals(0, data.getAsJsonArray("receipts").size());
        assertEquals(0, data.get("finalPayableAmount").getAsBigDecimal().compareTo(BigDecimal.ZERO));
        assertEquals(0, data.get("totalDebtAmount").getAsBigDecimal().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Kịch bản 5: Học viên không xem được dữ liệu của học viên khác bằng cách thay ID trên URL (Chặn IDOR)")
    void test05_IdorProtection_CannotViewOtherStudentData() throws Exception {
        // Học viên 1 (user_id = 5) cố tình xem công nợ của học viên 2 (code HV260143)
        TestRequest req = new TestRequest("GET", "/api/tuition/students/HV260143/debts", "", student1User);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(403, res.status);
        JsonObject json = JsonParser.parseString(res.getBody()).getAsJsonObject();
        assertFalse(json.get("success").getAsBoolean());
        JsonObject errorObj = json.getAsJsonObject("error");
        assertEquals("FORBIDDEN", errorObj.get("code").getAsString());
        assertTrue(errorObj.get("message").getAsString().contains("không có quyền xem"));

        // Thử thay bằng studentId số (ID = 2)
        TestRequest reqId = new TestRequest("GET", "/api/tuition/students/2/debts", "", student1User);
        TestResponse resId = new TestResponse();
        servlet.service(reqId.asServletRequest(), resId.asServletResponse());
        assertEquals(403, resId.status);
    }

    @Test
    @DisplayName("Kịch bản 6: Người chưa đăng nhập không truy cập được API cần xác thực (HTTP 401)")
    void test06_UnauthenticatedAccessDenied() throws Exception {
        TestRequest req = new TestRequest("GET", "/api/tuition/my-fees", "", null);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(401, res.status);
        JsonObject json = JsonParser.parseString(res.getBody()).getAsJsonObject();
        assertFalse(json.get("success").getAsBoolean());
        JsonObject errorObj = json.getAsJsonObject("error");
        assertEquals("AUTH_UNAUTHORIZED", errorObj.get("code").getAsString());
    }

    @Test
    @DisplayName("Kịch bản 7: Học viên tải được biên lai của giao dịch thuộc mình dưới dạng PDF hợp lệ")
    void test07_StudentCanDownloadOwnReceipt() throws Exception {
        PaymentRecordRequest pay = new PaymentRecordRequest();
        pay.setInstallmentBillId(BILL1_ID);
        pay.setAmount(new BigDecimal("10080000"));
        pay.setPaymentMethod("BANK_TRANSFER");
        pay.setReferenceCode("VCB-FT12345");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, pay);

        // Tải qua endpoint /my-fees/receipts/{code}/pdf
        TestRequest req = new TestRequest("GET", "/api/tuition/my-fees/receipts/" + receipt.getReceiptCode() + "/pdf", "", student1User);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(200, res.status);
        byte[] pdfBytes = res.outputStream.toByteArray();
        assertTrue(pdfBytes.length > 500, "Tệp PDF phải có kích thước hợp lệ");
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header, "Header của tệp phải bắt đầu bằng %PDF-");
    }

    @Test
    @DisplayName("Kịch bản 8: Học viên không tải được biên lai của người khác (HTTP 403)")
    void test08_StudentCannotDownloadOtherStudentReceipt() throws Exception {
        PaymentRecordRequest pay = new PaymentRecordRequest();
        pay.setInstallmentBillId(BILL1_ID); // Biên lai của Học viên 1
        pay.setAmount(new BigDecimal("10080000"));
        pay.setPaymentMethod("BANK_TRANSFER");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, pay);

        // Học viên 2 (user_id = 8) cố tải biên lai của Học viên 1
        TestRequest req = new TestRequest("GET", "/api/tuition/receipts/" + receipt.getReceiptCode() + "/pdf", "", student2User);
        TestResponse res = new TestResponse();

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(403, res.status);
        JsonObject json = JsonParser.parseString(res.getBody()).getAsJsonObject();
        JsonObject errorObj = json.getAsJsonObject("error");
        assertEquals("FORBIDDEN", errorObj.get("code").getAsString());
    }

    @Test
    @DisplayName("Kịch bản 9: Biên lai tải về chứa đúng thông tin học viên, số tiền và phương thức thanh toán")
    void test09_ReceiptDetailContainsAccurateData() throws SQLException {
        PaymentRecordRequest pay = new PaymentRecordRequest();
        pay.setInstallmentBillId(BILL1_ID);
        pay.setAmount(new BigDecimal("10080000"));
        pay.setPaymentMethod("BANK_TRANSFER");
        pay.setReferenceCode("FT26292418830");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, pay);

        PaymentReceiptResponse view = service.getReceiptDetail(STUDENT1_USER_ID, List.of("STUDENT"), receipt.getId());
        assertNotNull(view);
        assertEquals("HV260142", view.getStudentCode());
        assertEquals("Nguyễn Văn Học", view.getStudentName());
        assertEquals("Fullstack Web", view.getProgramName());
        assertEquals("Lớp Fullstack K16", view.getClassName());
        assertEquals(0, new BigDecimal("10080000").compareTo(view.getAmountPaid()));
        assertEquals(0, new BigDecimal("10080000").compareTo(view.getNetAmount()));
        assertEquals("Mười triệu không trăm tám mươi nghìn đồng chẵn", view.getAmountInWords());
        assertEquals("BANK_TRANSFER", view.getPaymentMethod());
        assertEquals("FT26292418830", view.getReferenceCode());
    }

    @Test
    @DisplayName("Kịch bản 10: Tải lại biên lai nhiều lần tuyệt đối không tạo giao dịch thanh toán mới")
    void test10_DownloadingReceiptDoesNotCreateNewPayment() throws Exception {
        PaymentRecordRequest pay = new PaymentRecordRequest();
        pay.setInstallmentBillId(BILL1_ID);
        pay.setAmount(new BigDecimal("10080000"));
        pay.setPaymentMethod("BANK_TRANSFER");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, pay);

        int countBefore = countTotalReceipts();
        BigDecimal debtBefore = getStudentDebt(STUDENT1_ID);

        // Học viên tải lại biên lai 5 lần liên tiếp
        for (int i = 0; i < 5; i++) {
            TestRequest req = new TestRequest("GET", "/api/tuition/my-fees/receipts/" + receipt.getId() + "/pdf", "", student1User);
            TestResponse res = new TestResponse();
            servlet.service(req.asServletRequest(), res.asServletResponse());
            assertEquals(200, res.status);
        }

        int countAfter = countTotalReceipts();
        BigDecimal debtAfter = getStudentDebt(STUDENT1_ID);

        assertEquals(countBefore, countAfter, "Số lượng biên lai trong database không được tăng");
        assertEquals(debtBefore, debtAfter, "Công nợ học viên không bị thay đổi");
    }

    @Test
    @DisplayName("Kịch bản 11: Giao dịch bị điều chỉnh phản ánh chính xác số dư công nợ của học viên")
    void test11_AdjustmentsAccuratelyReflectedInStudentDebt() throws SQLException {
        // 1. Thu đủ 10.080.000 ₫
        PaymentRecordRequest payReq = new PaymentRecordRequest();
        payReq.setInstallmentBillId(BILL1_ID);
        payReq.setAmount(new BigDecimal("10080000"));
        payReq.setPaymentMethod("CASH");
        PaymentReceiptResponse receipt = service.recordPayment(ACCOUNTANT_USER_ID, payReq);

        // 2. Kế toán điều chỉnh giảm 2.000.000 ₫ (nhầm lẫn số tiền khi kiểm đếm)
        PaymentAdjustmentRequest adjReq = new PaymentAdjustmentRequest();
        adjReq.setAdjustmentType("Giảm khoản thu");
        adjReq.setAmount(new BigDecimal("2000000"));
        adjReq.setReason("Nhầm lẫn kiểm đếm tiền mặt");
        service.createAdjustment(ACCOUNTANT_USER_ID, receipt.getId(), adjReq);

        // 3. Học viên xem lại tình trạng học phí
        StudentDebtSummaryResponse myStatus = service.getMyTuitionStatus(STUDENT1_USER_ID, List.of("STUDENT"), null);

        // Đã thanh toán thực tế = 8.080.000 ₫
        assertEquals(0, new BigDecimal("8080000").compareTo(myStatus.getTotalPaidAmount()));
        // Còn nợ = 25.200.000 - 8.080.000 = 17.120.000 ₫
        assertEquals(0, new BigDecimal("17120000").compareTo(myStatus.getTotalDebtAmount()));

        // Đợt 1 còn nợ 2.000.000 ₫
        InstallmentBillResponse bill1 = myStatus.getBills().get(0);
        assertEquals(0, new BigDecimal("8080000").compareTo(bill1.getPaidAmount()));
        assertEquals(0, new BigDecimal("2000000").compareTo(bill1.getRemainingDebt()));
        // Vì hạn đợt 1 (2026-10-05) đã qua nên trạng thái chính xác là OVERDUE
        assertEquals("OVERDUE", bill1.getStatus());
    }

    @Test
    @DisplayName("Kịch bản 12: Chức năng kế toán S7-03 hoạt động bình thường và học viên bị chặn thao tác sửa/xoá")
    void test12_AccountantAndS703ApisRemainIntact_StudentForbiddenToWrite() throws Exception {
        // Kế toán xem được công nợ của bất kỳ học viên nào
        StudentDebtSummaryResponse accView = service.getStudentDebtSummary(ACCOUNTANT_USER_ID, List.of("ACCOUNTANT"), "HV260142");
        assertNotNull(accView);
        assertEquals("HV260142", accView.getStudentCode());

        // Học viên cố tình gọi POST để tạo giao dịch qua /my-fees -> 405 Method Not Allowed
        TestRequest postReq = new TestRequest("POST", "/api/tuition/my-fees", "{}", student1User);
        TestResponse postRes = new TestResponse();
        servlet.service(postReq.asServletRequest(), postRes.asServletResponse());
        assertEquals(405, postRes.status);

        // Học viên cố tình gọi DELETE /api/tuition/my-fees -> 405 Method Not Allowed
        TestRequest delReq = new TestRequest("DELETE", "/api/tuition/my-fees", "", student1User);
        TestResponse delRes = new TestResponse();
        servlet.service(delReq.asServletRequest(), delRes.asServletResponse());
        assertEquals(405, delRes.status);
    }

    // =========================================================================
    // HELPER DATABASE CHECK METHODS
    // =========================================================================

    private int countTotalReceipts() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM payment_receipts")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private BigDecimal getStudentDebt(long studentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT debt_amount FROM student_tuition_plans WHERE student_id = ?")) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
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
