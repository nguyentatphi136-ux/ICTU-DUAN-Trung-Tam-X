package com.ems.controller;

import com.ems.dto.PaymentAdjustmentRequest;
import com.ems.dto.PaymentAdjustmentResponse;
import com.ems.dto.PaymentRecordRequest;
import com.ems.dto.PaymentReceiptResponse;
import com.ems.dto.StudentDebtSummaryResponse;
import com.ems.exception.ApiException;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.ems.service.TuitionPaymentService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API Servlet xử lý Thu học phí, Sinh biên lai và Bút toán điều chỉnh
 * Jira task: S7-03 – EP-08: Kế toán ghi nhận thanh toán và sinh biên lai
 *
 * Endpoints:
 * - POST   /api/tuition/payments                  : Ghi nhận thanh toán và sinh biên lai
 * - GET    /api/tuition/payments                  : Xem danh sách các khoản thanh toán
 * - GET    /api/tuition/payments/{id}             : Xem chi tiết một khoản thanh toán
 * - DELETE /api/tuition/payments/{id}             : Chặn xoá cứng (chỉ cho phép bút toán điều chỉnh)
 * - POST   /api/tuition/payments/{id}/adjustments : Lập bút toán điều chỉnh cho biên lai
 * - POST   /api/tuition/adjustments               : Lập bút toán điều chỉnh
 * - GET    /api/tuition/my-fees                   : Học viên xem tình trạng học phí của bản thân (S7-07)
 * - GET    /api/tuition/my-fees/receipts          : Học viên xem danh sách biên lai các khoản đã đóng (S7-07)
 * - GET    /api/tuition/my-fees/receipts/{id}/pdf : Học viên tải lại biên lai PDF (S7-07)
 * - GET    /api/tuition/receipts/{id}             : Xem chi tiết biên lai
 * - GET    /api/tuition/receipts/{id}/pdf         : Tải / xem biên lai PDF
 * - GET    /api/tuition/students/{idOrCode}/debts : Xem công nợ và kế hoạch các đợt thu của học viên
 */
@WebServlet(name = "TuitionPaymentApiServlet", urlPatterns = {
        "/api/tuition/payments",
        "/api/tuition/payments/*",
        "/api/tuition/receipts/*",
        "/api/tuition/students/*",
        "/api/tuition/my-fees",
        "/api/tuition/my-fees/*",
        "/api/tuition/adjustments",
        "/api/tuition/adjustments/*"
})
public class TuitionPaymentApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private static final Pattern MY_FEES_RECEIPT_PDF_PATTERN = Pattern.compile("^/my-fees/receipts/([A-Za-z0-9._-]+)/pdf$");
    private static final Pattern MY_FEES_RECEIPTS_PATTERN = Pattern.compile("^/my-fees/receipts/?$");
    private static final Pattern MY_FEES_PATTERN = Pattern.compile("^/my-fees/?$");

    private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("^/payments/(\\d+)$");
    private static final Pattern PAYMENT_ADJUST_PATTERN = Pattern.compile("^/payments/(\\d+)/adjustments$");
    private static final Pattern RECEIPT_DETAIL_PATTERN = Pattern.compile("^/receipts/([A-Za-z0-9._-]+)$");
    private static final Pattern RECEIPT_PDF_PATTERN = Pattern.compile("^/receipts/([A-Za-z0-9._-]+)/pdf$");
    private static final Pattern STUDENT_DEBTS_PATTERN = Pattern.compile("^/students/([A-Za-z0-9._-]+)/debts$");
    private static final Pattern STUDENT_PAYMENTS_PATTERN = Pattern.compile("^/students/([A-Za-z0-9._-]+)/payments$");

    private TuitionPaymentService service;

    @Override
    public void init() throws ServletException {
        this.service = new TuitionPaymentService();
    }

    public void setService(TuitionPaymentService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getNormalizedSubPath(req);
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập để tiếp tục.");
            return;
        }
        List<String> roles = currentUser.getRoles() != null ? currentUser.getRoles() : Collections.emptyList();

        try {
            // Case 0.1: GET /api/tuition/my-fees/receipts/{id}/pdf (S7-07)
            Matcher myReceiptPdfMatcher = MY_FEES_RECEIPT_PDF_PATTERN.matcher(path);
            if (myReceiptPdfMatcher.matches()) {
                String idStr = myReceiptPdfMatcher.group(1);
                long receiptId;
                try {
                    receiptId = Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    PaymentReceiptResponse r = service.getReceiptDetailByCode(currentUser.getId(), roles, idStr);
                    receiptId = r.getId();
                }

                resp.setContentType("application/pdf;charset=UTF-8");
                resp.setHeader("Content-Disposition", "inline; filename=\"Bien-Lai-" + idStr + ".pdf\"");
                service.generateReceiptPdf(currentUser.getId(), roles, receiptId, resp.getOutputStream());
                return;
            }

            // Case 0.2: GET /api/tuition/my-fees/receipts (S7-07)
            Matcher myReceiptsMatcher = MY_FEES_RECEIPTS_PATTERN.matcher(path);
            if (myReceiptsMatcher.matches()) {
                int page = 1;
                int pageSize = 10;
                try {
                    if (req.getParameter("page") != null) page = Integer.parseInt(req.getParameter("page"));
                    if (req.getParameter("size") != null) pageSize = Integer.parseInt(req.getParameter("size"));
                    else if (req.getParameter("pageSize") != null) pageSize = Integer.parseInt(req.getParameter("pageSize"));
                } catch (NumberFormatException ignored) {}

                String keyword = req.getParameter("keyword");
                if (keyword == null) keyword = req.getParameter("q");

                Map<String, Object> data = service.getMyReceipts(currentUser.getId(), roles, page, pageSize, keyword);
                ApiResponse.success(resp, "MY_RECEIPTS_SUCCESS", "Lấy danh sách biên lai của bạn thành công.", data);
                return;
            }

            // Case 0.3: GET /api/tuition/my-fees (S7-07)
            Matcher myFeesMatcher = MY_FEES_PATTERN.matcher(path);
            if (myFeesMatcher.matches()) {
                String status = req.getParameter("status");
                StudentDebtSummaryResponse summary = service.getMyTuitionStatus(currentUser.getId(), roles, status);
                ApiResponse.success(resp, "MY_FEES_SUCCESS", "Lấy tình trạng học phí cá nhân thành công.", summary);
                return;
            }

            // Case 1: GET /api/tuition/receipts/{id}/pdf
            Matcher pdfMatcher = RECEIPT_PDF_PATTERN.matcher(path);
            if (pdfMatcher.matches()) {
                String idStr = pdfMatcher.group(1);
                long receiptId;
                try {
                    receiptId = Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    PaymentReceiptResponse r = service.getReceiptDetailByCode(currentUser.getId(), roles, idStr);
                    receiptId = r.getId();
                }

                resp.setContentType("application/pdf;charset=UTF-8");
                resp.setHeader("Content-Disposition", "inline; filename=\"Bien-Lai-" + idStr + ".pdf\"");
                service.generateReceiptPdf(currentUser.getId(), roles, receiptId, resp.getOutputStream());
                return;
            }

            // Case 2: GET /api/tuition/receipts/{id}
            Matcher receiptMatcher = RECEIPT_DETAIL_PATTERN.matcher(path);
            if (receiptMatcher.matches()) {
                String idStr = receiptMatcher.group(1);
                PaymentReceiptResponse res;
                try {
                    long receiptId = Long.parseLong(idStr);
                    res = service.getReceiptDetail(currentUser.getId(), roles, receiptId);
                } catch (NumberFormatException e) {
                    res = service.getReceiptDetailByCode(currentUser.getId(), roles, idStr);
                }
                ApiResponse.success(resp, "RECEIPT_DETAIL_SUCCESS", "Lấy chi tiết biên lai thanh toán thành công.", res);
                return;
            }

            // Case 3: GET /api/tuition/students/{idOrCode}/debts
            Matcher debtsMatcher = STUDENT_DEBTS_PATTERN.matcher(path);
            if (debtsMatcher.matches()) {
                String studentKey = debtsMatcher.group(1);
                String status = req.getParameter("status");
                StudentDebtSummaryResponse summary = "me".equalsIgnoreCase(studentKey)
                        ? service.getMyTuitionStatus(currentUser.getId(), roles, status)
                        : service.getStudentDebtSummary(currentUser.getId(), roles, studentKey, status);
                ApiResponse.success(resp, "STUDENT_DEBTS_SUCCESS", "Lấy thông tin công nợ học viên thành công.", summary);
                return;
            }

            // Case 4: GET /api/tuition/students/{idOrCode}/payments
            Matcher studentPaymentsMatcher = STUDENT_PAYMENTS_PATTERN.matcher(path);
            if (studentPaymentsMatcher.matches()) {
                String studentKey = studentPaymentsMatcher.group(1);
                if ("me".equalsIgnoreCase(studentKey)) {
                    int page = 1;
                    int pageSize = 10;
                    try {
                        if (req.getParameter("page") != null) page = Integer.parseInt(req.getParameter("page"));
                        if (req.getParameter("size") != null) pageSize = Integer.parseInt(req.getParameter("size"));
                        else if (req.getParameter("pageSize") != null) pageSize = Integer.parseInt(req.getParameter("pageSize"));
                    } catch (NumberFormatException ignored) {}
                    String keyword = req.getParameter("keyword");
                    if (keyword == null) keyword = req.getParameter("q");

                    Map<String, Object> data = service.getMyReceipts(currentUser.getId(), roles, page, pageSize, keyword);
                    ApiResponse.success(resp, "STUDENT_PAYMENTS_SUCCESS", "Lấy lịch sử thanh toán của học viên thành công.", data.get("items"));
                } else {
                    StudentDebtSummaryResponse summary = service.getStudentDebtSummary(currentUser.getId(), roles, studentKey);
                    ApiResponse.success(resp, "STUDENT_PAYMENTS_SUCCESS", "Lấy lịch sử thanh toán của học viên thành công.", summary.getReceipts());
                }
                return;
            }

            // Case 5: GET /api/tuition/payments/{id}
            Matcher paymentIdMatcher = PAYMENT_ID_PATTERN.matcher(path);
            if (paymentIdMatcher.matches()) {
                long paymentId = Long.parseLong(paymentIdMatcher.group(1));
                PaymentReceiptResponse res = service.getReceiptDetail(currentUser.getId(), roles, paymentId);
                ApiResponse.success(resp, "PAYMENT_DETAIL_SUCCESS", "Lấy chi tiết khoản thanh toán thành công.", res);
                return;
            }

            // Case 6: GET /api/tuition/payments (Danh sách)
            if ("/payments".equals(path) || "/payments/".equals(path) || path.isEmpty() || "/".equals(path)) {
                int page = 1;
                int pageSize = 10;
                try {
                    if (req.getParameter("page") != null) page = Integer.parseInt(req.getParameter("page"));
                    if (req.getParameter("size") != null) pageSize = Integer.parseInt(req.getParameter("size"));
                    else if (req.getParameter("pageSize") != null) pageSize = Integer.parseInt(req.getParameter("pageSize"));
                } catch (NumberFormatException ignored) {}

                String keyword = req.getParameter("keyword");
                if (keyword == null) keyword = req.getParameter("q");

                Long studentId = null;
                try {
                    if (req.getParameter("studentId") != null) studentId = Long.parseLong(req.getParameter("studentId"));
                } catch (NumberFormatException ignored) {}

                List<PaymentReceiptResponse> list = service.listPayments(currentUser.getId(), roles, page, pageSize, keyword, studentId);
                int total = service.countPayments(currentUser.getId(), roles, keyword, studentId);

                Map<String, Object> pageData = Map.of(
                        "items", list,
                        "total", total,
                        "page", page,
                        "pageSize", pageSize,
                        "totalPages", pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 1
                );
                ApiResponse.success(resp, "PAYMENT_LIST_SUCCESS", "Lấy danh sách các khoản thanh toán thành công.", pageData);
                return;
            }

            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND", "Đường dẫn API không tồn tại.");
        } catch (ApiException e) {
            ApiResponse.error(resp, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getNormalizedSubPath(req);
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập để tiếp tục.");
            return;
        }

        try {
            // Case 1: POST /api/tuition/payments/{id}/adjustments
            Matcher adjustMatcher = PAYMENT_ADJUST_PATTERN.matcher(path);
            if (adjustMatcher.matches()) {
                long receiptId = Long.parseLong(adjustMatcher.group(1));
                PaymentAdjustmentRequest body = parseJson(req, PaymentAdjustmentRequest.class);
                PaymentAdjustmentResponse res = service.createAdjustment(currentUser.getId(), receiptId, body);
                ApiResponse.created(resp, "ADJUSTMENT_CREATED", "Lập bút toán điều chỉnh thành công.", res);
                return;
            }

            // Case 2: POST /api/tuition/adjustments
            if ("/adjustments".equals(path) || "/adjustments/".equals(path)) {
                PaymentAdjustmentRequest body = parseJson(req, PaymentAdjustmentRequest.class);
                if (body == null || body.getPaymentReceiptId() == null) {
                    throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "MISSING_RECEIPT_ID",
                            "Thiếu paymentReceiptId của biên lai cần điều chỉnh.");
                }
                PaymentAdjustmentResponse res = service.createAdjustment(currentUser.getId(), body.getPaymentReceiptId(), body);
                ApiResponse.created(resp, "ADJUSTMENT_CREATED", "Lập bút toán điều chỉnh thành công.", res);
                return;
            }

            // Case 3: POST /api/tuition/payments (Ghi nhận thanh toán)
            if ("/payments".equals(path) || "/payments/".equals(path) || path.isEmpty() || "/".equals(path)) {
                PaymentRecordRequest body = parseJson(req, PaymentRecordRequest.class);
                PaymentReceiptResponse res = service.recordPayment(currentUser.getId(), body);
                ApiResponse.created(resp, "PAYMENT_RECORDED", "Ghi nhận thanh toán và sinh biên lai thành công.", res);
                return;
            }

            // Chặn học viên tạo giao dịch qua endpoint my-fees
            if (path.startsWith("/my-fees")) {
                ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                        "Học viên không có quyền tạo hoặc chỉnh sửa giao dịch thanh toán.");
                return;
            }

            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND", "Đường dẫn API không tồn tại.");
        } catch (ApiException e) {
            ApiResponse.error(resp, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getNormalizedSubPath(req);
        if (path.startsWith("/my-fees") || path.startsWith("/payments")) {
            ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                    "Không được phép chỉnh sửa trực tiếp thông tin thanh toán hoặc công nợ.");
            return;
        }
        ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND", "Đường dẫn API không tồn tại.");
    }

    /**
     * Requirement 6: Đã ghi nhận thu thì không được xóa; chỉ được bút toán điều chỉnh theo quy trình có kiểm soát.
     * Chặn xóa cứng qua API.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = getNormalizedSubPath(req);
        if (path.startsWith("/my-fees")) {
            ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                    "Học viên không có quyền xoá giao dịch hoặc công nợ.");
            return;
        }
        ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "PAYMENT_DELETE_FORBIDDEN",
                "Giao dịch thanh toán đã ghi nhận không được phép xoá. Vui lòng lập bút toán điều chỉnh theo quy trình kiểm toán.");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String getNormalizedSubPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String context = req.getContextPath();
        String path = uri.substring(context.length());
        if (path.startsWith("/api/tuition")) {
            path = path.substring("/api/tuition".length());
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private User getAuthenticatedUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) return user;
        }
        Long userId = (Long) req.getAttribute("authenticatedUserId");
        if (userId != null) {
            try {
                return new com.ems.dao.UserDAO().findById(userId);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private <T> T parseJson(HttpServletRequest req, Class<T> clazz) {
        try (BufferedReader reader = req.getReader()) {
            return GSON.fromJson(reader, clazz);
        } catch (JsonSyntaxException e) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Định dạng JSON gửi lên không hợp lệ.");
        } catch (IOException e) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "READ_ERROR", "Không đọc được dữ liệu yêu cầu.");
        }
    }
}
