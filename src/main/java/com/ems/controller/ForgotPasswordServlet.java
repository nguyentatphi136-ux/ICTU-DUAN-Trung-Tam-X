package com.ems.controller;

import com.ems.dao.PasswordResetTokenDAO;
import com.ems.model.User;
import com.ems.service.EmailService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Mục 11: Controller xử lý Quên mật khẩu
 * Phục vụ: IDTTX-41 - Sinh token và gửi email đặt lại mật khẩu
 */
@WebServlet(urlPatterns = {"/forgot-password", "/auth/forgot-password", "/api/auth/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            sendResponse(req, resp, false, "Vui lòng nhập địa chỉ email hợp lệ.");
            return;
        }

        email = email.trim().toLowerCase();

        // 1. Tìm người dùng trong cơ sở dữ liệu
        User user = tokenDAO.findUserByEmail(email);

        // 2. Bảo mật chống dò quét tài khoản (User Enumeration Protection - AC3):
        // Nếu user tồn tại thì sinh token và gửi mail, nếu không tồn tại thì KHÔNG báo lỗi
        if (user != null) {
            // Sinh mã token an toàn ngẫu nhiên
            String token = UUID.randomUUID().toString();
            // Thời hạn 30 phút (AC1: có hiệu lực 30 phút)
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 30 * 60 * 1000);

            // Lưu token vào cơ sở dữ liệu (bảng password_reset_tokens)
            boolean saved = tokenDAO.saveToken(user.getId(), token, expiresAt);

            if (saved) {
                // Tạo đường dẫn liên kết đổi mật khẩu
                String resetLink = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                        + req.getContextPath() + "/reset-password.jsp?token=" + token;

                // Gửi email cho người dùng (IDTTX-41)
                EmailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            }
        }

        // 3. Phản hồi cho người dùng: Luôn hiển thị cùng 1 thông báo dù email có tồn tại hay không
        String message = "Nếu địa chỉ email tồn tại trên hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu qua email trong vài phút.";
        sendResponse(req, resp, true, message);
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
            resp.getWriter().write(String.format("{\"status\": %d, \"message\": \"%s\"}", success ? 200 : 400, message));
        } else {
            req.setAttribute("infoMessage", message);
            req.getRequestDispatcher("/forgot-password.jsp").forward(req, resp);
        }
    }
}
