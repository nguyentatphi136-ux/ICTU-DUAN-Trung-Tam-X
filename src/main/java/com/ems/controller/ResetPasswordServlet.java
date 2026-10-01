package com.ems.controller;

import com.ems.dao.PasswordResetTokenDAO;
import com.ems.model.PasswordResetToken;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Mục 11: Controller xử lý Đặt lại mật khẩu mới
 * Phục vụ: IDTTX-42 - Kiểm tra token hợp lệ, chưa dùng, đổi mật khẩu
 */
@WebServlet(urlPatterns = {"/reset-password", "/auth/reset-password", "/api/auth/reset-password"})
public class ResetPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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

        String token = req.getParameter("token");
        String newPassword = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        // 1. Kiểm tra đầu vào
        if (token == null || token.trim().isEmpty()) {
            sendResponse(req, resp, false, "Mã token không hợp lệ.");
            return;
        }

        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*[0-9].*")) {
            sendResponse(req, resp, false, "Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ và số.");
            return;
        }

        if (confirmPassword != null && !newPassword.equals(confirmPassword)) {
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
        String accept = req.getHeader("Accept");
        String xRequestedWith = req.getHeader("X-Requested-With");

        boolean isApi = "XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
                || (accept != null && accept.contains("application/json"))
                || req.getRequestURI().contains("/api/");

        if (isApi) {
            resp.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(String.format("{\"status\": %d, \"message\": \"%s\", \"redirectUrl\": \"/login.jsp?message=password_reset_success\"}", success ? 200 : 400, message));
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
}
