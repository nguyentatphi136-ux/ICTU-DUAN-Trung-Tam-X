package com.ems.controller;

import com.ems.config.SessionBlacklist;
import com.ems.dao.UserDAO;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Chức năng S1-04: Đổi mật khẩu khi đang đăng nhập
 * - Bắt buộc nhập mật khẩu hiện tại
 * - Mật khẩu mới tối thiểu 8 ký tự, có chữ và số
 * - Thu hồi tất cả các phiên đăng nhập khác của user
 */
@WebServlet(urlPatterns = {"/auth/change-password", "/api/auth/change-password"})
public class ChangePasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        request.getRequestDispatcher("/change-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.", "Đăng nhập lại", "/login.html");
            return;
        }

        String currentPassword;
        String newPassword;
        String confirmPassword;

        boolean isJson = request.getContentType() != null && request.getContentType().contains("application/json");
        try {
            if (isJson) {
                JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                currentPassword = body != null && body.has("currentPassword") ? body.get("currentPassword").getAsString() : null;
                newPassword = body != null && body.has("newPassword") ? body.get("newPassword").getAsString() : null;
                confirmPassword = body != null && body.has("confirmPassword") ? body.get("confirmPassword").getAsString() : null;
            } else {
                currentPassword = request.getParameter("currentPassword");
                newPassword = request.getParameter("newPassword");
                confirmPassword = request.getParameter("confirmPassword");
            }
        } catch (RuntimeException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_INVALID_JSON",
                    "Dữ liệu gửi lên không đúng định dạng JSON.", "Thử lại", null);
            return;
        }

        try {
            userDAO.changePassword(currentUser.getId(), currentPassword, newPassword, confirmPassword);

            // Thu hồi các phiên đăng nhập khác của người dùng
            int revokedSessions = SessionBlacklist.revokeOtherSessions(currentUser.getId(), session.getId());

            ApiResponse.success(response, "AUTH_CHANGE_PASSWORD_SUCCESS",
                    "Đổi mật khẩu thành công. Các phiên đăng nhập trên thiết bị khác đã được thu hồi.",
                    Map.of(
                            "userId", currentUser.getId(),
                            "revokedSessionsCount", revokedSessions,
                            "email", currentUser.getEmail()
                    ));
        } catch (IllegalArgumentException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra lại thông tin", null);
        } catch (IllegalStateException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "USER_NOT_FOUND",
                    e.getMessage(), "Đăng nhập lại", "/login.html");
        } catch (SQLException e) {
            getServletContext().log("Lỗi cơ sở dữ liệu khi đổi mật khẩu.", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SYSTEM_SERVICE_UNAVAILABLE",
                    "Hệ thống tạm thời không khả dụng. Vui lòng thử lại sau.", "Thử lại", null);
        }
    }
}
