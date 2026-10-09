package com.ems.controller;

import com.ems.dao.PasswordResetTokenDAO;
import com.ems.model.User;
import com.ems.service.EmailService;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Mục 11: Controller xử lý Quên mật khẩu
 * Phục vụ: IDTTX-41 - Sinh token và gửi email đặt lại mật khẩu
 */
@WebServlet(urlPatterns = {"/forgot-password", "/auth/forgot-password", "/api/auth/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    private static final java.util.concurrent.ConcurrentHashMap<String, Long> RATE_LIMIT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long RATE_LIMIT_COOLDOWN_MS = 15 * 60 * 1000L; // 15 phút

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        boolean apiRequest = isApiRequest(req);
        String email;
        try {
            if (apiRequest && req.getContentType() != null && req.getContentType().contains("application/json")) {
                JsonObject body = GSON.fromJson(req.getReader(), JsonObject.class);
                email = body == null || !body.has("email") ? null : body.get("email").getAsString();
            } else {
                email = req.getParameter("email");
            }
        } catch (RuntimeException exception) {
            sendResponse(req, resp, false, "Vui lòng nhập địa chỉ email hợp lệ.");
            return;
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            sendResponse(req, resp, false, "Vui lòng nhập địa chỉ email hợp lệ.");
            return;
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        // Giới hạn 1 email chỉ được yêu cầu liên kết 1 lần mỗi 15 phút để bảo vệ hệ thống (Rate Limiting)
        Long lastSent = RATE_LIMIT_CACHE.get(email);
        if (lastSent != null && (System.currentTimeMillis() - lastSent < RATE_LIMIT_COOLDOWN_MS)) {
            long remainingMin = Math.max(1, (RATE_LIMIT_COOLDOWN_MS - (System.currentTimeMillis() - lastSent)) / 60000L);
            sendResponse(req, resp, false, "Email này chỉ có thể nhận liên kết đặt lại mật khẩu 1 lần mỗi 15 phút để bảo vệ hệ thống. Vui lòng thử lại sau " + remainingMin + " phút.");
            return;
        }
        RATE_LIMIT_CACHE.put(email, System.currentTimeMillis());

        long startedAt = System.nanoTime();
        try {
            User user = tokenDAO.findUserByEmail(email);
            if (user != null) {
                String token = UUID.randomUUID().toString().replace("-", "");
                Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 30 * 60 * 1000L);
                if (tokenDAO.saveToken(user.getId(), token, expiresAt)) {
                    String appBase = setting("APP_BASE_URL", "http://localhost:5173").replaceAll("/+$", "");
                    String resetLink = appBase + "/ResetPasswordForm.html?token=" + token;
                    try {
                        CompletableFuture.runAsync(() -> EmailService.sendPasswordResetEmail(user.getEmail(), resetLink));
                    } catch (RejectedExecutionException exception) {
                        LOGGER.log(Level.WARNING, "Email khôi phục chưa được xếp hàng.", exception);
                    }
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Không xử lý được yêu cầu khôi phục mật khẩu.", exception);
        } finally {
            long remaining = 200_000_000L - (System.nanoTime() - startedAt);
            if (remaining > 0) LockSupport.parkNanos(remaining);
        }

        String message = "Nếu địa chỉ email tồn tại trên hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu qua email trong vài phút.";
        sendResponse(req, resp, true, message);
    }

    private void sendResponse(HttpServletRequest req, HttpServletResponse resp, boolean success, String message) throws IOException, ServletException {
        if (isApiRequest(req)) {
            if (success) {
                ApiResponse.success(resp, "AUTH_RESET_ACCEPTED", message, null);
            } else {
                ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR", message,
                        "Thử lại", "/ForgotPasswordForm.html");
            }
        } else {
            req.setAttribute("infoMessage", message);
            req.getRequestDispatcher("/forgot-password.jsp").forward(req, resp);
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().contains("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || accept != null && accept.contains("application/json");
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
