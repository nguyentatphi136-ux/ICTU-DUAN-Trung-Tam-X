package com.ems.controller;

import com.ems.dao.PasswordResetTokenDAO;
import com.ems.model.PasswordResetToken;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mục 11: Controller xử lý Đặt lại mật khẩu mới
 * Phục vụ: IDTTX-42 - Kiểm tra token hợp lệ, chưa dùng, đổi mật khẩu
 */
@WebServlet(urlPatterns = {"/reset-password", "/auth/reset-password", "/api/auth/reset-password"})
public class ResetPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*(?:\\d|[^A-Za-z0-9])).{8,}$");
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            req.setAttribute("errorMessage", "Liên kết không hợp lệ hoặc thiếu mã xác thực.");
            req.getRequestDispatcher("/reset-password.jsp").forward(req, resp);
            return;
        }

        // Kiểm tra tính hợp lệ của token trước khi mở form nhập mật khẩu
        PasswordResetToken resetToken = tokenDAO.findByToken(token);
        if (resetToken == null) {
            req.setAttribute("errorMessage", "Liên kết đặt lại mật khẩu không tồn tại.");
        } else if (resetToken.isUsed()) {
            req.setAttribute("errorMessage", "Liên kết này đã được sử dụng trước đó (chỉ dùng được 1 lần).");
        } else if (resetToken.isExpired()) {
            req.setAttribute("errorMessage", "Liên kết đặt lại mật khẩu đã hết hạn (chỉ có hiệu lực trong 30 phút).");
        } else {
            req.setAttribute("token", token);
        }

        req.getRequestDispatcher("/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String token;
        String newPassword;
        String confirmPassword;
        try {
            if (isApiRequest(req) && req.getContentType() != null
                    && req.getContentType().contains("application/json")) {
                JsonObject body = GSON.fromJson(req.getReader(), JsonObject.class);
                token = value(body, "token");
                newPassword = value(body, "password");
                confirmPassword = value(body, "confirmPassword");
            } else {
                token = req.getParameter("token");
                newPassword = req.getParameter("password");
                confirmPassword = req.getParameter("confirmPassword");
            }
        } catch (RuntimeException exception) {
            sendResponse(req, resp, false, "Dữ liệu đặt lại mật khẩu không hợp lệ.");
            return;
        }

        // 1. Kiểm tra đầu vào
        if (token == null || token.trim().isEmpty()) {
            sendResponse(req, resp, false, "Mã token không hợp lệ.");
            return;
        }

        if (newPassword == null || !STRONG_PASSWORD.matcher(newPassword).matches()) {
            sendResponse(req, resp, false, "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số hoặc ký tự đặc biệt.");
            return;
        }

        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            sendResponse(req, resp, false, "Mật khẩu xác nhận không khớp.");
            return;
        }

        // 2. Kiểm tra token trong cơ sở dữ liệu (IDTTX-42)
        PasswordResetToken resetToken = tokenDAO.findByToken(token);
        if (resetToken == null) {
            sendResponse(req, resp, false, "Liên kết đặt lại mật khẩu không tồn tại.");
            return;
        }

        // AC2: Kiểm tra liên kết chỉ dùng được 1 lần
        if (resetToken.isUsed()) {
            sendResponse(req, resp, false, "Liên kết này đã được sử dụng trước đó. Mỗi liên kết chỉ dùng được một lần.");
            return;
        }

        // AC1: Kiểm tra hiệu lực trong 30 phút
        if (resetToken.isExpired()) {
            sendResponse(req, resp, false, "Liên kết đặt lại mật khẩu đã hết hạn (chỉ có hiệu lực 30 phút).");
            return;
        }

        // 3. Mã hóa mật khẩu mới bằng BCrypt và cập nhật database qua Transaction
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        boolean success = tokenDAO.resetPasswordWithTransaction(resetToken.getUserId(), token, hashedPassword);

        if (success) {
            sendResponse(req, resp, true, "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
        } else {
            sendResponse(req, resp, false, "Đã xảy ra lỗi trong quá trình cập nhật mật khẩu. Vui lòng thử lại.");
        }
    }

    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, boolean success, String message) throws IOException, ServletException {
        if (isApiRequest(req)) {
            if (success) {
            ApiResponse.success(resp, "AUTH_PASSWORD_RESET_SUCCESS", message,
                Map.of("redirectUrl", "/login.html?message=password_reset_success"));
            } else {
            String code = message.startsWith("Mật khẩu") || message.startsWith("Dữ liệu")
                ? "REQUEST_VALIDATION_ERROR"
                : "AUTH_RESET_TOKEN_INVALID";
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, code, message,
                "Gửi lại liên kết", "/ForgotPasswordForm.html");
            }
        } else {
            if (success) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?message=password_reset_success");
            } else {
                req.setAttribute("errorMessage", message);
                req.setAttribute("token", req.getParameter("token"));
                req.getRequestDispatcher("/reset-password.jsp").forward(req, resp);
            }
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().contains("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || accept != null && accept.contains("application/json");
    }

    private String value(JsonObject body, String key) {
        return body == null || !body.has(key) ? null : body.get(key).getAsString();
    }
}
