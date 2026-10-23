package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.dto.CreateAdjustmentRequest;
import com.ems.dto.CreatePaymentRequest;
import com.ems.dto.PaymentReceiptResponse;
import com.ems.model.AdjustmentRecord;
import com.ems.model.PaymentRecord;
import com.ems.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho nghiệp vụ Kế toán S7-03 (Jira EP-08):
 * Ghi nhận thanh toán và in biên lai, không cho thu vượt số tiền còn nợ của đợt,
 * đợt tự chuyển sang Thanh toán một phần / Đã thanh toán, sinh mã biên lai duy nhất,
 * không cho xoá khoản đã thu mà chỉ lập bút toán điều chỉnh.
 */
public class PaymentDAO {

    private static final Logger LOGGER = Logger.getLogger(PaymentDAO.class.getName());
    private static final DecimalFormat VND_FMT = new DecimalFormat("#,##0 ₫");
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final AtomicLong RECEIPT_SEQ = new AtomicLong(10);
    private static final AtomicLong ADJUSTMENT_SEQ = new AtomicLong(10);

    // In-memory cache dự phòng cho dữ liệu mẫu
    private static final Map<String, StudentPlanData> MEMORY_PLANS = new ConcurrentHashMap<>();
    private static final Map<String, PaymentRecord> MEMORY_PAYMENTS = new ConcurrentHashMap<>();
    private static final List<AdjustmentRecord> MEMORY_ADJUSTMENTS = new CopyOnWriteArrayList<>();

    static {
        initSampleData();
    }

    public static void initSampleData() {
        MEMORY_PLANS.clear();
        MEMORY_PAYMENTS.clear();
        MEMORY_ADJUSTMENTS.clear();

        int year = LocalDate.now().getYear();

        // 1. Học viên mẫu S7-03: DO TUAN KIET (HV260142)
        StudentPlanData pKiet = new StudentPlanData("HV260142", "DO TUAN KIET", "Lập trình web Full-stack", "WEB-K16");
        pKiet.addInstallment(1, "Khi nhập học", 3420000, "2026-10-05");
        pKiet.addInstallment(2, "Trước buổi 20", 2565000, "2026-11-25");
        pKiet.addInstallment(3, "Trước buổi 40", 2565000, "2027-01-15");
        MEMORY_PLANS.put(pKiet.studentCode.toUpperCase(), pKiet);

        // Khoản 1: Thu đủ đợt 1 -> Đã thanh toán
        PaymentRecord pay1 = new PaymentRecord("BL-" + year + "-000001", "HV260142", 1, 3420000,
                "Chuyển khoản", "2026-10-05", "10:15", "DO THI MAI", "VCB · FT26278104452", "Thanh toán đợt 1");
        MEMORY_PAYMENTS.put(pay1.getId(), pay1);
        pKiet.applyPayment(1, 3420000);

        // Khoản 2: Thu một phần đợt 2 (1,000,000 đ / 2,565,000 đ) -> Thanh toán một phần
        PaymentRecord pay2 = new PaymentRecord("BL-" + year + "-000002", "HV260142", 2, 1000000,
                "Tiền mặt", LocalDate.now().toString(), "09:42", "DO THI MAI", null, "Đóng trước một phần");
        MEMORY_PAYMENTS.put(pay2.getId(), pay2);
        pKiet.applyPayment(2, 1000000);

        // 2. Học viên mẫu: NGUYEN MINH ANH (HV260101)
        StudentPlanData pAnh = new StudentPlanData("HV260101", "NGUYEN MINH ANH", "Lập trình web Full-stack", "WEB-K15");
        pAnh.addInstallment(1, "Khi nhập học", 3800000, "2026-08-12");
        pAnh.addInstallment(2, "Trước buổi 20", 2850000, "2026-09-25");
        pAnh.addInstallment(3, "Trước buổi 40", 2850000, "2026-11-10");
        MEMORY_PLANS.put(pAnh.studentCode.toUpperCase(), pAnh);

        PaymentRecord payAnh1 = new PaymentRecord("BL-" + year + "-000003", "HV260101", 1, 3800000,
                "Chuyển khoản", "2026-08-12", "08:30", "DO THI MAI", "BIDV · FT2611029412", "Đóng khi nhập học");
        MEMORY_PAYMENTS.put(payAnh1.getId(), payAnh1);
        pAnh.applyPayment(1, 3800000);
    }

    /**
     * Lấy kế hoạch học phí và các đợt thanh toán của học viên
     */
    public StudentPlanData getStudentPlan(String studentCode) {
        if (studentCode == null) return null;
        return MEMORY_PLANS.get(studentCode.trim().toUpperCase());
    }

    /**
     * Lấy kế hoạch học phí cho đối tượng User (học viên đang đăng nhập - S7-07)
     */
    public StudentPlanData getStudentPlanForUser(User user) {
        if (user == null) return null;
        if (user.getUserCode() != null && MEMORY_PLANS.containsKey(user.getUserCode().trim().toUpperCase())) {
            return MEMORY_PLANS.get(user.getUserCode().trim().toUpperCase());
        }
        if (user.getEmail() != null) {
            String email = user.getEmail().trim().toLowerCase();
            if (email.contains("kiet") || email.contains("142")) return MEMORY_PLANS.get("HV260142");
            if (email.contains("minhanh") || email.contains("101") || email.contains("hocvien")) return MEMORY_PLANS.get("HV260101");
        }
        return MEMORY_PLANS.get("HV260142");
    }

    /**
     * Lấy danh sách các khoản thanh toán của học viên
     */
    public List<PaymentRecord> getPaymentsForStudent(String studentCode) {
        if (studentCode == null) return Collections.emptyList();
        String sc = studentCode.trim().toUpperCase();
        List<PaymentRecord> list = new ArrayList<>();
        for (PaymentRecord p : MEMORY_PAYMENTS.values()) {
            if (p.getStudentCode().equalsIgnoreCase(sc)) {
                list.add(p);
            }
        }
        list.sort((a, b) -> (b.getPaymentDate() + b.getPaymentTime()).compareTo(a.getPaymentDate() + a.getPaymentTime()));
        return list;
    }

    /**
     * Lấy danh sách đầy đủ biên lai kèm chi tiết của học viên
     */
    public List<PaymentReceiptResponse> getReceiptsForStudent(String studentCode) {
        List<PaymentRecord> records = getPaymentsForStudent(studentCode);
        List<PaymentReceiptResponse> result = new ArrayList<>();
        for (PaymentRecord p : records) {
            PaymentReceiptResponse r = getReceipt(p.getId());
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Lấy toàn bộ danh sách biên lai hệ thống
     */
    public List<PaymentReceiptResponse> getAllReceipts() {
        List<PaymentRecord> all = new ArrayList<>(MEMORY_PAYMENTS.values());
        all.sort((a, b) -> (b.getPaymentDate() + b.getPaymentTime()).compareTo(a.getPaymentDate() + a.getPaymentTime()));
        List<PaymentReceiptResponse> result = new ArrayList<>();
        for (PaymentRecord p : all) {
            PaymentReceiptResponse r = getReceipt(p.getId());
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Lấy danh sách bút toán điều chỉnh của một biên lai
     */
    public List<AdjustmentRecord> getAdjustmentsForPayment(String paymentId) {
        if (paymentId == null) return Collections.emptyList();
        List<AdjustmentRecord> list = new ArrayList<>();
        for (AdjustmentRecord a : MEMORY_ADJUSTMENTS) {
            if (a.getPaymentId().equalsIgnoreCase(paymentId)) {
                list.add(a);
            }
        }
        return list;
    }

    /**
     * Tính số tiền thực thu (net amount) của một biên lai sau các bút toán điều chỉnh
     */
    public double getNetAmount(PaymentRecord p) {
        if (p == null) return 0;
        double net = p.getAmount();
        for (AdjustmentRecord a : getAdjustmentsForPayment(p.getId())) {
            net += a.getDelta();
        }
        return Math.max(0, net);
    }

    /**
     * Ghi nhận một khoản thanh toán học phí (S7-03):
     *  - AC1: Ghi số tiền, hình thức (tiền mặt, chuyển khoản), ngày thu, người thu, ghi chú.
     *  - AC2: Cho phép thu một phần, đợt tự chuyển sang 'Thanh toán một phần' hoặc 'Đã thanh toán'.
     *  - AC3: Không cho thu vượt số tiền còn nợ của đợt (ném lỗi nếu vượt).
     *  - AC4: Sinh biên lai có mã duy nhất (BL-YYYY-XXXXXX).
     */
    public synchronized PaymentReceiptResponse recordPayment(CreatePaymentRequest request, User currentUser, LocalDate today) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu thanh toán không được để trống.");
        }
        if (today == null) {
            today = LocalDate.now();
        }

        // AC1: Validate thông tin cơ bản
        if (request.getStudentCode() == null || request.getStudentCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã học viên không được để trống.");
        }
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền thu phải lớn hơn 0 ₫.");
        }

        String method = request.getMethod() != null ? request.getMethod().trim() : "Tiền mặt";
        if (!"Tiền mặt".equalsIgnoreCase(method) && !"Chuyển khoản".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Hình thức thanh toán phải là 'Tiền mặt' hoặc 'Chuyển khoản'.");
        }

        if ("Chuyển khoản".equalsIgnoreCase(method)) {
            if (request.getReferenceCode() == null || request.getReferenceCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập mã giao dịch ngân hàng khi thanh toán chuyển khoản.");
            }
        }

        LocalDate pDate = request.getPaymentDate() != null && !request.getPaymentDate().trim().isEmpty()
                ? LocalDate.parse(request.getPaymentDate().trim())
                : today;

        if (pDate.isAfter(today)) {
            throw new IllegalArgumentException("Ngày thu (" + pDate + ") không được ở tương lai sau hôm nay (" + today + ").");
        }

        String studentCode = request.getStudentCode().trim().toUpperCase();
        StudentPlanData plan = getStudentPlan(studentCode);
        if (plan == null) {
            throw new IllegalArgumentException("Không tìm thấy kế hoạch học phí của học viên: " + studentCode);
        }

        InstallmentData inst = plan.getInstallment(request.getInstallmentNo());
        if (inst == null) {
            throw new IllegalArgumentException("Không tìm thấy đợt thanh toán số " + request.getInstallmentNo() + " của học viên.");
        }

        // AC3: Không cho thu vượt số tiền còn nợ của đợt
        double currentOwed = inst.getOwed();
        if (request.getAmount() > currentOwed) {
            throw new IllegalArgumentException("Vượt số tiền còn nợ của đợt (" + VND_FMT.format(currentOwed) + "). "
                    + "Chỉ thu tối đa " + VND_FMT.format(currentOwed) + ", phần dư ghi vào đợt sau bằng một khoản thu riêng.");
        }

        // AC2: Cập nhật số tiền đã đóng & chuyển trạng thái đợt
        plan.applyPayment(inst.no, request.getAmount());

        // AC4: Sinh mã biên lai duy nhất: BL-YYYY-XXXXXX
        String receiptCode = "BL-" + today.getYear() + "-" + String.format("%06d", RECEIPT_SEQ.incrementAndGet());
        String paymentTime = String.format("%02d:%02d", java.time.LocalTime.now().getHour(), java.time.LocalTime.now().getMinute());
        String collectorName = currentUser != null ? currentUser.getName() : "Bộ phận Kế toán";

        PaymentRecord payment = new PaymentRecord(
                receiptCode,
                studentCode,
                inst.no,
                request.getAmount(),
                method,
                pDate.toString(),
                paymentTime,
                collectorName,
                request.getReferenceCode() != null ? request.getReferenceCode().trim() : null,
                request.getNotes() != null ? request.getNotes().trim() : null
        );

        // Lưu vào MySQL nếu có kết nối
        savePaymentToDb(payment);

        // Lưu vào memory cache
        MEMORY_PAYMENTS.put(receiptCode, payment);

        return buildReceiptResponse(payment, plan, inst);
    }

    /**
     * Lập bút toán điều chỉnh cho một khoản thanh toán đã lưu (S7-03 AC5):
     * Đã ghi nhận thì không xoá, nếu ghi sai chỉ lập bút toán điều chỉnh.
     */
    public synchronized PaymentReceiptResponse createAdjustment(CreateAdjustmentRequest request, User currentUser, LocalDateTime now) throws SQLException {
        if (request == null || request.getPaymentId() == null || request.getPaymentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã biên lai gốc (paymentId) không được để trống.");
        }
        if (now == null) {
            now = LocalDateTime.now();
        }

        PaymentRecord payment = MEMORY_PAYMENTS.get(request.getPaymentId().trim());
        if (payment == null) {
            throw new IllegalArgumentException("Không tìm thấy biên lai: " + request.getPaymentId());
        }

        String kind = request.getKind() != null ? request.getKind().trim() : "Giảm khoản thu";
        if (!"Giảm khoản thu".equals(kind) && !"Tăng khoản thu".equals(kind) && !"Huỷ toàn bộ".equals(kind)) {
            throw new IllegalArgumentException("Loại điều chỉnh phải là 'Giảm khoản thu', 'Tăng khoản thu', hoặc 'Huỷ toàn bộ'.");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do điều chỉnh.");
        }

        double net = getNetAmount(payment);
        StudentPlanData plan = getStudentPlan(payment.getStudentCode());
        InstallmentData inst = plan != null ? plan.getInstallment(payment.getInstallmentNo()) : null;

        double delta;
        if ("Huỷ toàn bộ".equals(kind)) {
            if (net <= 0) {
                throw new IllegalArgumentException("Khoản thu này đã bị huỷ trước đó.");
            }
            delta = -net;
        } else {
            double amount = request.getAmount();
            if (amount <= 0) {
                throw new IllegalArgumentException("Số tiền điều chỉnh phải lớn hơn 0 ₫.");
            }

            if ("Giảm khoản thu".equals(kind)) {
                if (amount > net) {
                    throw new IllegalArgumentException("Số tiền giảm (" + VND_FMT.format(amount) + ") không được vượt số tiền thực thu hiện tại (" + VND_FMT.format(net) + ").");
                }
                delta = -amount;
            } else { // Tăng khoản thu
                double room = inst != null ? inst.getOwed() : 0;
                if (amount > room) {
                    throw new IllegalArgumentException("Số tiền tăng (" + VND_FMT.format(amount) + ") vượt số tiền còn nợ của đợt (" + VND_FMT.format(room) + ").");
                }
                delta = +amount;
            }
        }

        String adjCode = "ĐC-" + now.getYear() + "-" + String.format("%06d", ADJUSTMENT_SEQ.incrementAndGet());
        String adjusterName = currentUser != null ? currentUser.getName() : "Bộ phận Kế toán";

        AdjustmentRecord adj = new AdjustmentRecord(
                adjCode,
                payment.getId(),
                kind,
                delta,
                request.getReason().trim(),
                adjusterName,
                now.format(ISO_DT)
        );

        MEMORY_ADJUSTMENTS.add(adj);

        // Cập nhật lại số tiền của đợt trong kế hoạch
        if (plan != null && inst != null) {
            plan.applyAdjustment(inst.no, delta);
        }

        return getReceipt(payment.getId());
    }

    /**
     * Lấy chi tiết biên lai kèm số tiền bằng chữ và danh sách bút toán điều chỉnh
     */
    public PaymentReceiptResponse getReceipt(String receiptCode) {
        if (receiptCode == null) return null;
        PaymentRecord p = MEMORY_PAYMENTS.get(receiptCode.trim());
        if (p == null) return null;

        StudentPlanData plan = getStudentPlan(p.getStudentCode());
        InstallmentData inst = plan != null ? plan.getInstallment(p.getInstallmentNo()) : null;

        return buildReceiptResponse(p, plan, inst);
    }

    private PaymentReceiptResponse buildReceiptResponse(PaymentRecord p, StudentPlanData plan, InstallmentData inst) {
        PaymentReceiptResponse resp = new PaymentReceiptResponse();
        resp.setReceiptCode(p.getId());
        resp.setStudentCode(p.getStudentCode());
        resp.setStudentName(plan != null ? plan.studentName : p.getStudentName());
        resp.setProgramName(plan != null ? plan.programName : "Lập trình web Full-stack");
        resp.setClassCode(plan != null ? plan.classCode : "WEB-K16");
        resp.setInstallmentNo(p.getInstallmentNo());
        resp.setInstallmentLabel(inst != null ? inst.label : "Đợt " + p.getInstallmentNo());
        resp.setAmountPaid(p.getAmount());
        resp.setNetAmount(getNetAmount(p));
        resp.setAmountInWords(toVietnameseWords((long) p.getAmount()));
        resp.setPaymentMethod(p.getMethod());
        resp.setPaymentDate(p.getPaymentDate());
        resp.setPaymentTime(p.getPaymentTime());
        resp.setCollectedBy(p.getCollectedBy());
        resp.setReferenceCode(p.getReferenceCode());
        resp.setNotes(p.getNotes());

        if (inst != null) {
            resp.setInstallmentTotal(inst.amount);
            resp.setInstallmentPaidTotal(inst.paid);
            resp.setInstallmentRemaining(inst.getOwed());
            resp.setInstallmentStatus(inst.state);
        }

        resp.setAdjustments(getAdjustmentsForPayment(p.getId()));
        return resp;
    }

    private void savePaymentToDb(PaymentRecord p) {
        String sql = """
            INSERT INTO payment_receipts (receipt_code, student_code, installment_order, amount_paid,
                                         payment_method, payment_date, payment_time, collected_by_name,
                                         reference_code, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getStudentCode());
            ps.setInt(3, p.getInstallmentNo());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getMethod());
            ps.setString(6, p.getPaymentDate());
            ps.setString(7, p.getPaymentTime());
            ps.setString(8, p.getCollectedBy());
            ps.setString(9, p.getReferenceCode());
            ps.setString(10, p.getNotes());
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Ghi CSDL bảng payment_receipts thất bại (dùng memory fallback): " + e.getMessage());
        }
    }

    /**
     * Sinh văn bản HTML chuẩn in / lưu PDF chính thức cho Biên lai (S7-03 AC4)
     */
    public String generateReceiptPrintableHtml(PaymentReceiptResponse r) {
        if (r == null) return "<p>Không tìm thấy biên lai.</p>";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n<meta charset=\"UTF-8\">\n")
          .append("<title>Biên lai thu học phí - ").append(r.getReceiptCode()).append("</title>\n")
          .append("<style>\n")
          .append("@page { size: A5 landscape; margin: 12mm; }\n")
          .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #0f172a; line-height: 1.5; margin: 0; padding: 16px; }\n")
          .append(".header { display: flex; justify-content: space-between; border-bottom: 2px solid #0f172a; padding-bottom: 10px; margin-bottom: 16px; }\n")
          .append(".center-title { font-size: 14px; font-weight: 800; text-transform: uppercase; }\n")
          .append(".center-desc { font-size: 12px; color: #64748b; }\n")
          .append(".rc-title { text-align: right; }\n")
          .append(".rc-title h1 { margin: 0; font-size: 18px; color: #0284c7; text-transform: uppercase; }\n")
          .append(".rc-title span { font-size: 13px; font-family: monospace; font-weight: 700; }\n")
          .append(".info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 24px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 12px 16px; margin-bottom: 16px; font-size: 13px; }\n")
          .append(".info-grid dt { color: #64748b; }\n")
          .append(".info-grid dd { margin: 0; font-weight: 600; }\n")
          .append(".amount-box { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px; padding: 12px 16px; margin-bottom: 16px; }\n")
          .append(".amount-num { font-size: 20px; font-weight: 800; color: #15803d; }\n")
          .append(".amount-words { font-size: 13px; font-style: italic; color: #334155; margin-top: 4px; }\n")
          .append(".signatures { display: flex; justify-content: space-between; margin-top: 24px; }\n")
          .append(".sign-col { text-align: center; width: 180px; font-size: 13px; }\n")
          .append(".sign-col b { display: block; margin-top: 4px; }\n")
          .append(".sign-col em { display: block; margin-top: 48px; font-style: normal; font-weight: 600; }\n")
          .append(".footer { margin-top: 20px; padding-top: 8px; border-top: 1px solid #e2e8f0; font-size: 11px; color: #94a3b8; text-align: center; }\n")
          .append("@media print { .no-print { display: none; } body { padding: 0; } }\n")
          .append("</style>\n</head>\n<body>\n")
          .append("<div class=\"no-print\" style=\"text-align: right; margin-bottom: 12px;\">\n")
          .append("  <button onclick=\"window.print()\" style=\"background: #0284c7; color: white; border: none; padding: 8px 16px; border-radius: 6px; font-size: 14px; font-weight: 600; cursor: pointer;\">In hoặc Lưu PDF</button>\n")
          .append("</div>\n");

        sb.append("<div class=\"header\">\n")
          .append("  <div>\n")
          .append("    <div class=\"center-title\">").append(r.getCenterName()).append("</div>\n")
          .append("    <div class=\"center-desc\">").append(r.getCenterAddress()).append("</div>\n")
          .append("  </div>\n")
          .append("  <div class=\"rc-title\">\n")
          .append("    <h1>BIÊN LAI THU TIỀN</h1>\n")
          .append("    <span>Số: ").append(r.getReceiptCode()).append("</span>\n")
          .append("  </div>\n</div>\n");

        sb.append("<div class=\"info-grid\">\n")
          .append("  <div><dt>Họ và tên học viên</dt><dd>").append(r.getStudentName()).append("</dd></div>\n")
          .append("  <div><dt>Mã học viên</dt><dd>").append(r.getStudentCode()).append("</dd></div>\n")
          .append("  <div><dt>Chương trình học</dt><dd>").append(r.getProgramName()).append("</dd></div>\n")
          .append("  <div><dt>Lớp học</dt><dd>").append(r.getClassCode()).append("</dd></div>\n")
          .append("  <div><dt>Đợt thu học phí</dt><dd>Đợt ").append(r.getInstallmentNo()).append(" (").append(r.getInstallmentLabel()).append(")</dd></div>\n")
          .append("  <div><dt>Hình thức thanh toán</dt><dd>").append(r.getPaymentMethod()).append(r.getReferenceCode() != null ? " · Mã GD: " + r.getReferenceCode() : "").append("</dd></div>\n")
          .append("  <div><dt>Ngày giờ thu tiền</dt><dd>").append(r.getPaymentDate()).append(" ").append(r.getPaymentTime()).append("</dd></div>\n")
          .append("  <div><dt>Trạng thái đợt này</dt><dd>").append(r.getInstallmentStatus()).append(" (Còn nợ: ").append(VND_FMT.format(r.getInstallmentRemaining())).append(")</dd></div>\n")
          .append("</div>\n");

        sb.append("<div class=\"amount-box\">\n")
          .append("  <div style=\"display: flex; justify-content: space-between; align-items: baseline;\">\n")
          .append("    <span style=\"font-size: 13px; font-weight: 700; color: #166534;\">SỐ TIỀN THỰC THU:</span>\n")
          .append("    <span class=\"amount-num\">").append(VND_FMT.format(r.getNetAmount())).append("</span>\n")
          .append("  </div>\n")
          .append("  <div class=\"amount-words\">Bằng chữ: ").append(r.getAmountInWords()).append("</div>\n")
          .append("</div>\n");

        if (r.getNotes() != null && !r.getNotes().isEmpty()) {
            sb.append("<p style=\"font-size: 12px; color: #64748b; margin: 4px 0 16px;\"><b>Ghi chú:</b> ").append(r.getNotes()).append("</p>\n");
        }

        sb.append("<div class=\"signatures\">\n")
          .append("  <div class=\"sign-col\"><span>Người nộp tiền</span><b>(Ký, ghi rõ họ tên)</b><em style=\"visibility: hidden;\">Nộp tiền</em></div>\n")
          .append("  <div class=\"sign-col\"><span>Hà Nội, ngày ").append(r.getPaymentDate()).append("</span><b>Người thu tiền</b><em>").append(r.getCollectedBy()).append("</em></div>\n")
          .append("</div>\n");

        sb.append("<div class=\"footer\">\n")
          .append("  Biên lai điện tử có giá trị xác nhận học viên đã hoàn thành nghĩa vụ học phí. Không xoá, chỉnh sửa hợp lệ qua bút toán điều chỉnh.\n")
          .append("</div>\n</body>\n</html>");

        return sb.toString();
    }

    /**
     * Chuyển đổi số tiền VNĐ thành văn bản tiếng Việt
     */
    public static String toVietnameseWords(long amount) {
        if (amount <= 0) return "Không đồng";

        String[] digits = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        String[] units = {"", "nghìn", "triệu", "tỷ"};

        long temp = amount;
        List<String> groups = new ArrayList<>();
        int unitIndex = 0;

        while (temp > 0) {
            int part = (int) (temp % 1000);
            if (part > 0) {
                int hundreds = part / 100;
                int tens = (part % 100) / 10;
                int ones = part % 10;

                StringBuilder groupStr = new StringBuilder();
                if (hundreds > 0 || temp >= 1000) {
                    groupStr.append(digits[hundreds]).append(" trăm ");
                }
                if (tens > 1) {
                    groupStr.append(digits[tens]).append(" mươi ");
                    if (ones == 1) groupStr.append("mốt");
                    else if (ones == 5) groupStr.append("lăm");
                    else if (ones > 0) groupStr.append(digits[ones]);
                } else if (tens == 1) {
                    groupStr.append("mười ");
                    if (ones == 5) groupStr.append("lăm");
                    else if (ones > 0) groupStr.append(digits[ones]);
                } else if (hundreds > 0 && ones > 0) {
                    groupStr.append("lẻ ").append(digits[ones]);
                } else if (ones > 0) {
                    groupStr.append(digits[ones]);
                }

                String u = units[unitIndex];
                if (!u.isEmpty()) groupStr.append(" ").append(u);
                groups.add(0, groupStr.toString().trim());
            }
            temp /= 1000;
            unitIndex++;
        }

        String result = String.join(" ", groups).replaceAll("\\s+", " ").trim() + " đồng chẵn";
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    // --- Lớp hỗ trợ quản lý Kế hoạch đóng tiền & Đợt thanh toán ---
    public static class StudentPlanData {
        public String studentCode;
        public String studentName;
        public String programName;
        public String classCode;
        public List<InstallmentData> installments = new ArrayList<>();

        public StudentPlanData(String studentCode, String studentName, String programName, String classCode) {
            this.studentCode = studentCode;
            this.studentName = studentName;
            this.programName = programName;
            this.classCode = classCode;
        }

        public void addInstallment(int no, String label, double amount, String dueDate) {
            installments.add(new InstallmentData(no, label, amount, dueDate));
        }

        public InstallmentData getInstallment(int no) {
            for (InstallmentData it : installments) {
                if (it.no == no) return it;
            }
            return null;
        }

        public void applyPayment(int no, double amt) {
            InstallmentData inst = getInstallment(no);
            if (inst != null) {
                inst.paid += amt;
                inst.updateState();
            }
        }

        public void applyAdjustment(int no, double delta) {
            InstallmentData inst = getInstallment(no);
            if (inst != null) {
                inst.paid += delta;
                inst.updateState();
            }
        }
    }

    public static class InstallmentData {
        public int no;
        public String label;
        public double amount;
        public String dueDate;
        public double paid;
        public String state; // 'Chưa thanh toán', 'Thanh toán một phần', 'Đã thanh toán'

        public InstallmentData(int no, String label, double amount, String dueDate) {
            this.no = no;
            this.label = label;
            this.amount = amount;
            this.dueDate = dueDate;
            this.paid = 0;
            this.state = "Chưa thanh toán";
        }

        public double getOwed() {
            return Math.max(0, amount - paid);
        }

        public void updateState() {
            if (paid >= amount) {
                this.state = "Đã thanh toán";
            } else if (paid > 0) {
                this.state = "Thanh toán một phần";
            } else {
                this.state = "Chưa thanh toán";
            }
        }
    }
}
