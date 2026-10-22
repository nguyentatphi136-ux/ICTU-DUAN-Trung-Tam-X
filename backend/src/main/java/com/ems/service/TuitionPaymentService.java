package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.dao.TuitionPaymentDAO;
import com.ems.dto.InstallmentBillResponse;
import com.ems.dto.PaymentAdjustmentRequest;
import com.ems.dto.PaymentAdjustmentResponse;
import com.ems.dto.PaymentRecordRequest;
import com.ems.dto.PaymentReceiptResponse;
import com.ems.dto.StudentDebtSummaryResponse;
import com.ems.exception.ApiException;
import com.ems.model.PaymentAdjustment;
import com.ems.model.PaymentReceipt;
import com.ems.model.Student;
import com.ems.model.StudentTuitionPlan;
import com.ems.model.TuitionInstallmentBill;
import com.ems.util.ReceiptPdfGenerator;
import com.ems.util.VietnameseNumberToWords;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Service xử lý nghiệp vụ Học phí, Biên lai thanh toán và Bút toán điều chỉnh
 * Tuân thủ Jira task S7-03 – EP-08
 */
public class TuitionPaymentService {

    private final TuitionPaymentDAO dao;

    public TuitionPaymentService() {
        this.dao = new TuitionPaymentDAO();
    }

    public TuitionPaymentService(TuitionPaymentDAO dao) {
        this.dao = dao;
    }

    // =========================================================================
    // 1. GHI NHẬN KHOẢN THANH TOÁN (RECORD PAYMENT)
    // =========================================================================

    public PaymentReceiptResponse recordPayment(long actorUserId, PaymentRecordRequest req) throws SQLException {
        if (req == null) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", "Dữ liệu yêu cầu thanh toán không được rỗng.");
        }

        // 1. Validate Số tiền
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_AMOUNT", "Số tiền thanh toán phải lớn hơn 0.");
        }

        // 2. Validate Hình thức thanh toán
        String method = normalizePaymentMethod(req.getPaymentMethod());
        if (method == null) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_PAYMENT_METHOD",
                    "Hình thức thanh toán không hợp lệ. Chỉ chấp nhận Tiền mặt (CASH) hoặc Chuyển khoản (BANK_TRANSFER).");
        }

        // 3. Validate Ngày thu
        LocalDate paymentDate;
        if (req.getPaymentDate() == null || req.getPaymentDate().isBlank()) {
            paymentDate = LocalDate.now();
        } else {
            try {
                paymentDate = LocalDate.parse(req.getPaymentDate().trim());
            } catch (DateTimeParseException e) {
                throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_PAYMENT_DATE",
                        "Định dạng ngày thu không hợp lệ (yêu cầu định dạng YYYY-MM-DD).");
            }
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "PAYMENT_DATE_FUTURE",
                    "Ngày thu không được lớn hơn ngày hiện tại.");
        }

        // 4. Bắt đầu Transaction và kiểm soát đồng thời (Pessimistic Locking)
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Xác định đợt thu
                TuitionInstallmentBill bill = null;
                if (req.getInstallmentBillId() != null && req.getInstallmentBillId() > 0) {
                    bill = dao.findBillForUpdate(req.getInstallmentBillId(), conn);
                } else if ((req.getStudentId() != null || req.getStudentCode() != null) && req.getInstallmentOrder() != null) {
                    Student s = req.getStudentId() != null ? dao.findStudentById(req.getStudentId(), conn) : dao.findStudentByCode(req.getStudentCode(), conn);
                    if (s != null) {
                        StudentTuitionPlan p = dao.findPlanByStudentId(s.getId(), conn);
                        if (p != null) {
                            List<TuitionInstallmentBill> bills = dao.listBillsByPlanId(p.getId(), conn);
                            for (TuitionInstallmentBill b : bills) {
                                if (b.getInstallmentOrder().equals(req.getInstallmentOrder())) {
                                    bill = dao.findBillForUpdate(b.getId(), conn);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (bill == null) {
                    throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "BILL_NOT_FOUND",
                            "Không tìm thấy đợt thanh toán học phí tương ứng.");
                }

                // Khóa kế hoạch học phí tương ứng
                StudentTuitionPlan plan = dao.findPlanForUpdate(bill.getTuitionPlanId(), conn);
                if (plan == null) {
                    throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "PLAN_NOT_FOUND",
                            "Không tìm thấy kế hoạch học phí liên kết với đợt thu này.");
                }

                // Kiểm tra học viên thuộc đúng đợt thu
                Student student = dao.findStudentById(plan.getStudentId(), conn);
                if (student == null) {
                    throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "STUDENT_NOT_FOUND",
                            "Không tìm thấy hồ sơ học viên của kế hoạch học phí.");
                }

                if (req.getStudentId() != null && !req.getStudentId().equals(student.getId())) {
                    throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "STUDENT_MISMATCH",
                            "Học viên được gửi lên không khớp với đợt thanh toán.");
                }
                if (req.getStudentCode() != null && !req.getStudentCode().trim().equalsIgnoreCase(student.getStudentCode())) {
                    throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "STUDENT_MISMATCH",
                            "Mã học viên được gửi lên không khớp với đợt thanh toán.");
                }

                // Tính toán công nợ thực tế từ database (không dựa vào Frontend)
                BigDecimal actualPaidSoFar = dao.calculateTotalPaidForBill(bill.getId(), conn);
                BigDecimal remainingDebt = bill.getExpectedAmount().subtract(actualPaidSoFar);

                if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "BILL_ALREADY_PAID",
                            "Đợt thu này đã hoàn thành thanh toán đủ, không thể ghi nhận thêm.");
                }

                if (req.getAmount().compareTo(remainingDebt) > 0) {
                    throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "AMOUNT_EXCEEDS_REMAINING_DEBT",
                            String.format("Số tiền thu (%s) vượt quá số tiền còn nợ của đợt (%s). Chỉ được thu tối đa số tiền còn nợ.",
                                    formatMoney(req.getAmount()), formatMoney(remainingDebt)));
                }

                // Sinh mã biên lai duy nhất
                String receiptCode = dao.generateNextReceiptCode(conn);

                // Tạo đối tượng biên lai
                PaymentReceipt receipt = new PaymentReceipt();
                receipt.setReceiptCode(receiptCode);
                receipt.setInstallmentBillId(bill.getId());
                receipt.setAmountPaid(req.getAmount());
                receipt.setPaymentMethod(method);
                receipt.setPaymentDate(paymentDate);
                receipt.setReferenceCode(req.getReferenceCode() != null ? req.getReferenceCode().trim() : null);
                receipt.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);
                receipt.setCollectedBy(actorUserId);

                long receiptId = dao.insertReceipt(receipt, conn);

                // Cập nhật trạng thái đợt thu
                BigDecimal newBillPaid = actualPaidSoFar.add(req.getAmount());
                String newBillStatus = (newBillPaid.compareTo(bill.getExpectedAmount()) >= 0) ? "PAID" : "PARTIAL";
                dao.updateBillAmountsAndStatus(bill.getId(), newBillPaid, newBillStatus, conn);

                // Cập nhật trạng thái kế hoạch học phí của học viên
                List<TuitionInstallmentBill> allBills = dao.listBillsByPlanId(plan.getId(), conn);
                BigDecimal totalPlanPaid = BigDecimal.ZERO;
                for (TuitionInstallmentBill b : allBills) {
                    if (b.getId().equals(bill.getId())) {
                        totalPlanPaid = totalPlanPaid.add(newBillPaid);
                    } else {
                        totalPlanPaid = totalPlanPaid.add(dao.calculateTotalPaidForBill(b.getId(), conn));
                    }
                }
                BigDecimal totalPlanDebt = plan.getFinalPayableAmount().subtract(totalPlanPaid);
                if (totalPlanDebt.compareTo(BigDecimal.ZERO) < 0) totalPlanDebt = BigDecimal.ZERO;

                String newPlanStatus = (totalPlanPaid.compareTo(plan.getFinalPayableAmount()) >= 0) ? "COMPLETED" :
                        (totalPlanPaid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNPAID");
                dao.updatePlanAmountsAndStatus(plan.getId(), totalPlanPaid, totalPlanDebt, newPlanStatus, conn);

                // Ghi nhật ký kiểm toán (Audit Log)
                String auditNote = String.format("Ghi nhận thu tiền biên lai %s, số tiền %s cho học viên %s (Đợt %d)",
                        receiptCode, formatMoney(req.getAmount()), student.getStudentCode(), bill.getInstallmentOrder());
                dao.recordAuditLog(conn, actorUserId, "RECORD_PAYMENT", "PAYMENT_RECEIPT", String.valueOf(receiptId),
                        null, auditNote);

                conn.commit();

                // Lấy thông tin chi tiết đầy đủ để trả về
                PaymentReceipt createdReceipt = dao.findReceiptById(receiptId, conn);
                return toReceiptResponse(createdReceipt);
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ApiException) throw (ApiException) e;
                throw new SQLException("Lỗi khi ghi nhận thanh toán: " + e.getMessage(), e);
            }
        }
    }

    // =========================================================================
    // 2. LẬP BÚT TOÁN ĐIỀU CHỈNH (CREATE PAYMENT ADJUSTMENT)
    // =========================================================================

    public PaymentAdjustmentResponse createAdjustment(long actorUserId, long receiptId, PaymentAdjustmentRequest req) throws SQLException {
        if (req == null) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", "Dữ liệu yêu cầu điều chỉnh không được rỗng.");
        }

        if (req.getReason() == null || req.getReason().trim().isBlank()) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "REASON_REQUIRED", "Lý do lập bút toán điều chỉnh là bắt buộc.");
        }

        String kind = normalizeAdjustmentType(req.getAdjustmentType());
        if (kind == null) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_ADJUST_TYPE",
                    "Loại điều chỉnh không hợp lệ. Chỉ chấp nhận Giảm khoản thu (REDUCE), Tăng khoản thu (INCREASE), hoặc Huỷ toàn bộ (CANCEL).");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Kiểm tra biên lai gốc
                PaymentReceipt receipt = dao.findReceiptById(receiptId, conn);
                if (receipt == null) {
                    throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "RECEIPT_NOT_FOUND",
                            "Không tìm thấy biên lai thanh toán cần điều chỉnh.");
                }

                // Khóa đợt thu và kế hoạch học phí
                TuitionInstallmentBill bill = dao.findBillForUpdate(receipt.getInstallmentBillId(), conn);
                if (bill == null) {
                    throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "BILL_NOT_FOUND", "Không tìm thấy đợt thanh toán gốc.");
                }
                StudentTuitionPlan plan = dao.findPlanForUpdate(bill.getTuitionPlanId(), conn);

                // Tính số tiền thực thu hiện tại của biên lai (sau các lần điều chỉnh trước)
                BigDecimal currentNetReceipt = dao.calculateNetReceiptAmount(receiptId, conn);
                if (currentNetReceipt.compareTo(BigDecimal.ZERO) <= 0 && !"INCREASE".equals(kind)) {
                    throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "RECEIPT_ALREADY_CANCELLED",
                            "Biên lai này đã bị huỷ hoặc có số tiền thực thu bằng 0, không thể giảm thêm.");
                }

                BigDecimal delta;
                if ("CANCEL".equals(kind)) {
                    delta = currentNetReceipt.negate();
                } else if ("REDUCE".equals(kind)) {
                    if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_ADJUST_AMOUNT",
                                "Số tiền giảm điều chỉnh phải lớn hơn 0.");
                    }
                    if (req.getAmount().compareTo(currentNetReceipt) > 0) {
                        throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "ADJUST_EXCEEDS_NET",
                                String.format("Số tiền điều chỉnh giảm (%s) không được vượt quá số tiền thực thu của biên lai (%s).",
                                        formatMoney(req.getAmount()), formatMoney(currentNetReceipt)));
                    }
                    delta = req.getAmount().negate();
                } else { // INCREASE
                    if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_ADJUST_AMOUNT",
                                "Số tiền tăng điều chỉnh phải lớn hơn 0.");
                    }
                    BigDecimal billActualPaid = dao.calculateTotalPaidForBill(bill.getId(), conn);
                    BigDecimal billRemaining = bill.getExpectedAmount().subtract(billActualPaid);
                    if (req.getAmount().compareTo(billRemaining) > 0) {
                        throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "ADJUST_EXCEEDS_ROOM",
                                String.format("Số tiền điều chỉnh tăng (%s) vượt quá số tiền còn nợ của đợt (%s).",
                                        formatMoney(req.getAmount()), formatMoney(billRemaining)));
                    }
                    delta = req.getAmount();
                }

                // Sinh mã bút toán điều chỉnh duy nhất
                String adjCode = dao.generateNextAdjustmentCode(conn);

                // Lưu bút toán điều chỉnh
                PaymentAdjustment adj = new PaymentAdjustment();
                adj.setAdjustmentCode(adjCode);
                adj.setPaymentReceiptId(receipt.getId());
                adj.setAdjustmentType(kind);
                adj.setDeltaAmount(delta);
                adj.setReason(req.getReason().trim());
                adj.setAdjustedBy(actorUserId);

                long adjId = dao.insertAdjustment(adj, conn);

                // Cập nhật lại số tiền đã thu và trạng thái của đợt
                BigDecimal newBillPaid = dao.calculateTotalPaidForBill(bill.getId(), conn);
                String newBillStatus;
                if (newBillPaid.compareTo(bill.getExpectedAmount()) >= 0) {
                    newBillStatus = "PAID";
                } else if (newBillPaid.compareTo(BigDecimal.ZERO) > 0) {
                    newBillStatus = "PARTIAL";
                } else {
                    newBillStatus = "UNPAID";
                }
                dao.updateBillAmountsAndStatus(bill.getId(), newBillPaid, newBillStatus, conn);

                // Cập nhật lại kế hoạch học phí
                List<TuitionInstallmentBill> allBills = dao.listBillsByPlanId(plan.getId(), conn);
                BigDecimal totalPlanPaid = BigDecimal.ZERO;
                for (TuitionInstallmentBill b : allBills) {
                    totalPlanPaid = totalPlanPaid.add(dao.calculateTotalPaidForBill(b.getId(), conn));
                }
                BigDecimal totalPlanDebt = plan.getFinalPayableAmount().subtract(totalPlanPaid);
                if (totalPlanDebt.compareTo(BigDecimal.ZERO) < 0) totalPlanDebt = BigDecimal.ZERO;

                String newPlanStatus = (totalPlanPaid.compareTo(plan.getFinalPayableAmount()) >= 0) ? "COMPLETED" :
                        (totalPlanPaid.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "UNPAID");
                dao.updatePlanAmountsAndStatus(plan.getId(), totalPlanPaid, totalPlanDebt, newPlanStatus, conn);

                // Ghi nhật ký kiểm toán
                String auditNote = String.format("Bút toán điều chỉnh %s: %s %s cho biên lai %s. Lý do: %s",
                        adjCode, kind, formatMoney(delta.abs()), receipt.getReceiptCode(), req.getReason().trim());
                dao.recordAuditLog(conn, actorUserId, "CREATE_ADJUSTMENT", "PAYMENT_ADJUSTMENT", String.valueOf(adjId),
                        null, auditNote);

                conn.commit();

                // Trả về DTO kết quả
                PaymentAdjustmentResponse res = new PaymentAdjustmentResponse();
                res.setId(adjId);
                res.setAdjustmentCode(adjCode);
                res.setPaymentReceiptId(receipt.getId());
                res.setReceiptCode(receipt.getReceiptCode());
                res.setAdjustmentType(kind);
                res.setDeltaAmount(delta);
                res.setReason(req.getReason().trim());
                res.setAdjustedBy(actorUserId);
                res.setCreatedAt(LocalDate.now().toString());
                return res;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ApiException) throw (ApiException) e;
                throw new SQLException("Lỗi khi lập bút toán điều chỉnh: " + e.getMessage(), e);
            }
        }
    }

    // =========================================================================
    // 3. XEM CHI TIẾT BIÊN LAI & XUẤT PDF
    // =========================================================================

    public PaymentReceiptResponse getReceiptDetail(long currentUserId, Collection<String> roles, long receiptId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            PaymentReceipt receipt = dao.findReceiptById(receiptId, conn);
            if (receipt == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "RECEIPT_NOT_FOUND", "Không tìm thấy biên lai thanh toán.");
            }
            checkReceiptAccess(currentUserId, roles, receipt, conn);
            return toReceiptResponse(receipt);
        }
    }

    public PaymentReceiptResponse getReceiptDetailByCode(long currentUserId, Collection<String> roles, String receiptCode) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            PaymentReceipt receipt = dao.findReceiptByCode(receiptCode, conn);
            if (receipt == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "RECEIPT_NOT_FOUND", "Không tìm thấy biên lai thanh toán " + receiptCode);
            }
            checkReceiptAccess(currentUserId, roles, receipt, conn);
            return toReceiptResponse(receipt);
        }
    }

    public void generateReceiptPdf(long currentUserId, Collection<String> roles, long receiptId, OutputStream out) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            PaymentReceipt receipt = dao.findReceiptById(receiptId, conn);
            if (receipt == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "RECEIPT_NOT_FOUND", "Không tìm thấy biên lai thanh toán.");
            }
            checkReceiptAccess(currentUserId, roles, receipt, conn);
            ReceiptPdfGenerator.generateReceiptPdf(receipt, out);
        }
    }

    // =========================================================================
    // =========================================================================
    // 4. XEM CÔNG NỢ & LỊCH SỬ THANH TOÁN (S7-03 & S7-07)
    // =========================================================================

    /**
     * S7-07: Học viên đang đăng nhập xem tình trạng học phí cá nhân của mình
     */
    public StudentDebtSummaryResponse getMyTuitionStatus(long currentUserId, Collection<String> roles, String statusFilter) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Student student = dao.findStudentByUserId(currentUserId, conn);
            if (student == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "STUDENT_NOT_FOUND",
                        "Tài khoản hiện tại chưa được liên kết với hồ sơ học viên. Trang này dành riêng cho học viên.");
            }
            return buildStudentDebtSummary(student, conn, statusFilter);
        }
    }

    /**
     * S7-07: Học viên đang đăng nhập xem danh sách biên lai các khoản đã đóng (hỗ trợ phân trang)
     */
    public java.util.Map<String, Object> getMyReceipts(long currentUserId, Collection<String> roles, int page, int pageSize, String keyword) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Student student = dao.findStudentByUserId(currentUserId, conn);
            if (student == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "STUDENT_NOT_FOUND",
                        "Tài khoản hiện tại chưa được liên kết với hồ sơ học viên.");
            }
            List<PaymentReceipt> receipts = dao.listAllReceipts(page, pageSize, keyword, student.getId());
            int total = dao.countReceipts(keyword, student.getId());
            List<PaymentReceiptResponse> list = new ArrayList<>();
            for (PaymentReceipt r : receipts) {
                list.add(toReceiptResponse(r));
            }
            return java.util.Map.of(
                    "items", list,
                    "total", total,
                    "page", page,
                    "pageSize", pageSize,
                    "totalPages", pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 1
            );
        }
    }

    public StudentDebtSummaryResponse getStudentDebtSummary(long currentUserId, Collection<String> roles, String studentIdOrCode) throws SQLException {
        return getStudentDebtSummary(currentUserId, roles, studentIdOrCode, null);
    }

    public StudentDebtSummaryResponse getStudentDebtSummary(long currentUserId, Collection<String> roles, String studentIdOrCode, String statusFilter) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Student student = null;
            if ("me".equalsIgnoreCase(studentIdOrCode)) {
                student = dao.findStudentByUserId(currentUserId, conn);
            } else {
                try {
                    long sid = Long.parseLong(studentIdOrCode);
                    student = dao.findStudentById(sid, conn);
                } catch (NumberFormatException ignored) {}

                if (student == null) {
                    student = dao.findStudentByCode(studentIdOrCode, conn);
                }
            }

            if (student == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "STUDENT_NOT_FOUND", "Không tìm thấy thông tin học viên.");
            }

            // Kiểm tra quyền: Chỉ Kế toán/Admin hoặc chính học viên sở hữu hồ sơ
            checkStudentAccess(currentUserId, roles, student);

            return buildStudentDebtSummary(student, conn, statusFilter);
        }
    }

    private StudentDebtSummaryResponse buildStudentDebtSummary(Student student, Connection conn, String statusFilter) throws SQLException {
        StudentTuitionPlan plan = dao.findPlanByStudentId(student.getId(), conn);
        if (plan == null) {
            StudentDebtSummaryResponse emptyRes = new StudentDebtSummaryResponse();
            emptyRes.setStudentId(student.getId());
            emptyRes.setStudentCode(student.getStudentCode());
            emptyRes.setStudentName(student.getFullName());
            emptyRes.setClassName(student.getClassName());
            emptyRes.setProgramName(student.getProgramName());
            emptyRes.setOriginalTuition(BigDecimal.ZERO);
            emptyRes.setFinalPayableAmount(BigDecimal.ZERO);
            emptyRes.setTotalPaidAmount(BigDecimal.ZERO);
            emptyRes.setTotalDebtAmount(BigDecimal.ZERO);
            emptyRes.setOverdueDays(0);
            emptyRes.setPlanStatus("NO_PLAN");
            emptyRes.setBills(new ArrayList<>());
            emptyRes.setReceipts(new ArrayList<>());
            return emptyRes;
        }

        List<TuitionInstallmentBill> bills = dao.listBillsByPlanId(plan.getId(), conn);
        List<PaymentReceipt> receipts = dao.listReceiptsByStudentId(student.getId(), conn);
        List<PaymentReceiptResponse> receiptResponses = new ArrayList<>();
        for (PaymentReceipt r : receipts) {
            receiptResponses.add(toReceiptResponse(r));
        }

        List<InstallmentBillResponse> billResponses = new ArrayList<>();
        int maxOverdue = 0;
        LocalDate today = LocalDate.now();

        for (TuitionInstallmentBill b : bills) {
            BigDecimal actualPaid = dao.calculateTotalPaidForBill(b.getId(), conn);
            BigDecimal remaining = b.getExpectedAmount().subtract(actualPaid);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

            InstallmentBillResponse br = new InstallmentBillResponse();
            br.setId(b.getId());
            br.setTuitionPlanId(b.getTuitionPlanId());
            br.setInstallmentOrder(b.getInstallmentOrder());
            br.setInstallmentName("Đợt " + b.getInstallmentOrder());
            br.setDueDate(b.getDueDate() != null ? b.getDueDate().toString() : null);
            br.setExpectedAmount(b.getExpectedAmount());
            br.setPaidAmount(actualPaid);
            br.setRemainingDebt(remaining);

            String st;
            if (actualPaid.compareTo(b.getExpectedAmount()) >= 0) {
                st = "PAID";
            } else if (remaining.compareTo(BigDecimal.ZERO) > 0 && b.getDueDate() != null && b.getDueDate().isBefore(today)) {
                st = "OVERDUE";
            } else if (actualPaid.compareTo(BigDecimal.ZERO) > 0) {
                st = "PARTIAL";
            } else {
                st = "UNPAID";
            }
            br.setStatus(st);

            // Gắn danh sách biên lai thuộc đợt thu này
            List<PaymentReceiptResponse> billReceipts = new ArrayList<>();
            for (PaymentReceiptResponse rr : receiptResponses) {
                if (rr.getInstallmentBillId() != null && rr.getInstallmentBillId().equals(b.getId())) {
                    billReceipts.add(rr);
                }
            }
            br.setReceipts(billReceipts);

            if (remaining.compareTo(BigDecimal.ZERO) > 0 && b.getDueDate() != null && b.getDueDate().isBefore(today)) {
                int days = (int) ChronoUnit.DAYS.between(b.getDueDate(), today);
                if (days > maxOverdue) {
                    maxOverdue = days;
                }
            }

            if (matchesStatusFilter(st, statusFilter)) {
                billResponses.add(br);
            }
        }

        StudentDebtSummaryResponse res = new StudentDebtSummaryResponse();
        res.setStudentId(student.getId());
        res.setStudentCode(student.getStudentCode());
        res.setStudentName(student.getFullName());
        res.setProgramName(student.getProgramName());
        res.setClassName(student.getClassName());
        res.setTuitionPlanId(plan.getId());
        res.setOriginalTuition(plan.getOriginalTuition());
        res.setDiscountLabel(plan.getDiscountReason());
        res.setDiscountAmount(plan.getDiscountAmount());
        res.setFinalPayableAmount(plan.getFinalPayableAmount());
        res.setTotalPaidAmount(plan.getPaidAmount());
        res.setTotalDebtAmount(plan.getDebtAmount());
        res.setOverdueDays(maxOverdue);
        res.setPlanStatus(plan.getStatus());
        res.setBills(billResponses);
        res.setReceipts(receiptResponses);
        return res;
    }

    private boolean matchesStatusFilter(String billStatus, String filter) {
        if (filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter.trim())) {
            return true;
        }
        String f = filter.trim().toUpperCase(Locale.ROOT);
        String s = billStatus != null ? billStatus.toUpperCase(Locale.ROOT) : "";
        if (s.equals(f)) return true;
        if ("OVERDUE".equals(s) && ("QUÁ HẠN".equals(f) || "QUA HAN".equals(f))) return true;
        if ("PAID".equals(s) && ("ĐÃ THANH TOÁN".equals(f) || "DA THANH TOAN".equals(f))) return true;
        if ("PARTIAL".equals(s) && ("THANH TOÁN MỘT PHẦN".equals(f) || "THANH TOAN MOT PHAN".equals(f))) return true;
        if ("UNPAID".equals(s) && ("CHƯA THANH TOÁN".equals(f) || "CHUA THANH TOAN".equals(f))) return true;
        return false;
    }

    public List<PaymentReceiptResponse> listPayments(long currentUserId, Collection<String> roles, int page, int pageSize, String keyword, Long studentId) throws SQLException {
        boolean isStaff = roles != null && (roles.contains("ADMIN") || roles.contains("ACCOUNTANT") || roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ACCOUNTANT"));
        if (!isStaff) {
            // Nếu là học viên, chỉ cho phép xem thanh toán của chính mình
            try (Connection conn = DBConnection.getConnection()) {
                Student s = dao.findStudentByUserId(currentUserId, conn);
                if (s == null) {
                    return new ArrayList<>();
                }
                studentId = s.getId();
            }
        }
        List<PaymentReceipt> receipts = dao.listAllReceipts(page, pageSize, keyword, studentId);
        List<PaymentReceiptResponse> responses = new ArrayList<>();
        for (PaymentReceipt r : receipts) {
            responses.add(toReceiptResponse(r));
        }
        return responses;
    }

    public int countPayments(long currentUserId, Collection<String> roles, String keyword, Long studentId) throws SQLException {
        boolean isStaff = roles != null && (roles.contains("ADMIN") || roles.contains("ACCOUNTANT") || roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ACCOUNTANT"));
        if (!isStaff) {
            try (Connection conn = DBConnection.getConnection()) {
                Student s = dao.findStudentByUserId(currentUserId, conn);
                if (s == null) return 0;
                studentId = s.getId();
            }
        }
        return dao.countReceipts(keyword, studentId);
    }

    // =========================================================================
    // 5. HELPER & VALIDATION METHODS
    // =========================================================================

    private void checkReceiptAccess(long currentUserId, Collection<String> roles, PaymentReceipt receipt, Connection conn) throws SQLException {
        boolean isStaff = roles != null && (roles.contains("ADMIN") || roles.contains("ACCOUNTANT") || roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ACCOUNTANT"));
        if (isStaff) {
            return;
        }
        // Kiểm tra xem user có phải học viên sở hữu biên lai này không
        Student student = dao.findStudentById(receipt.getStudentId(), conn);
        if (student == null || student.getUserId() == null || student.getUserId() != currentUserId) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Bạn không có quyền truy cập biên lai của học viên khác.");
        }
    }

    private void checkStudentAccess(long currentUserId, Collection<String> roles, Student student) {
        boolean isStaff = roles != null && (roles.contains("ADMIN") || roles.contains("ACCOUNTANT") || roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ACCOUNTANT"));
        if (isStaff) {
            return;
        }
        if (student.getUserId() == null || student.getUserId() != currentUserId) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Bạn không có quyền xem thông tin công nợ của học viên khác.");
        }
    }

    private String normalizePaymentMethod(String method) {
        if (method == null) return null;
        String m = method.trim().toUpperCase(Locale.ROOT);
        if ("CASH".equals(m) || "TIỀN MẶT".equals(m) || "TIEN MAT".equals(m)) {
            return "CASH";
        }
        if ("BANK_TRANSFER".equals(m) || "CHUYỂN KHOẢN".equals(m) || "CHUYEN KHOAN".equals(m)) {
            return "BANK_TRANSFER";
        }
        return null;
    }

    private String normalizeAdjustmentType(String type) {
        if (type == null) return null;
        String t = type.trim().toUpperCase(Locale.ROOT);
        if ("REDUCE".equals(t) || "GIẢM KHOẢN THU".equals(t) || "GIAM KHOAN THU".equals(t)) {
            return "REDUCE";
        }
        if ("INCREASE".equals(t) || "TĂNG KHOẢN THU".equals(t) || "TANG KHOAN THU".equals(t)) {
            return "INCREASE";
        }
        if ("CANCEL".equals(t) || "HUỶ TOÀN BỘ".equals(t) || "HUY TOAN BO".equals(t)) {
            return "CANCEL";
        }
        return null;
    }

    private PaymentReceiptResponse toReceiptResponse(PaymentReceipt r) {
        if (r == null) return null;
        PaymentReceiptResponse res = new PaymentReceiptResponse();
        res.setId(r.getId());
        res.setReceiptCode(r.getReceiptCode());
        res.setInstallmentBillId(r.getInstallmentBillId());
        res.setInstallmentOrder(r.getInstallmentOrder());
        res.setStudentId(r.getStudentId());
        res.setStudentCode(r.getStudentCode());
        res.setStudentName(r.getStudentName());
        res.setProgramName(r.getProgramName());
        res.setClassName(r.getClassName());
        res.setAmountPaid(r.getAmountPaid());
        res.setNetAmount(r.getNetAmount());
        res.setAmountInWords(VietnameseNumberToWords.convert(r.getNetAmount()));
        res.setPaymentMethod(r.getPaymentMethod());
        res.setPaymentDate(r.getPaymentDate() != null ? r.getPaymentDate().toString() : null);
        res.setReferenceCode(r.getReferenceCode());
        res.setNotes(r.getNotes());
        res.setCollectedBy(r.getCollectedBy());
        res.setCollectorName(r.getCollectorName());
        res.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        res.setBillExpectedAmount(r.getExpectedBillAmount());
        res.setBillRemainingDebt(r.getBillRemainingDebtAfter());

        if (r.getAdjustments() != null) {
            List<PaymentAdjustmentResponse> adjResponses = new ArrayList<>();
            for (PaymentAdjustment a : r.getAdjustments()) {
                PaymentAdjustmentResponse ar = new PaymentAdjustmentResponse();
                ar.setId(a.getId());
                ar.setAdjustmentCode(a.getAdjustmentCode());
                ar.setPaymentReceiptId(a.getPaymentReceiptId());
                ar.setReceiptCode(a.getReceiptCode());
                ar.setAdjustmentType(a.getAdjustmentType());
                ar.setDeltaAmount(a.getDeltaAmount());
                ar.setReason(a.getReason());
                ar.setAdjustedBy(a.getAdjustedBy());
                ar.setAdjusterName(a.getAdjusterName());
                ar.setCreatedAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
                adjResponses.add(ar);
            }
            res.setAdjustments(adjResponses);
        }
        return res;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0 ₫", symbols);
        return df.format(amount);
    }
}
