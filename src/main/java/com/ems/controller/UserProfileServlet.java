package com.ems.controller;

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
 * Chức năng Sprint 2 S2-02: Xem và cập nhật hồ sơ cá nhân
 * - Người dùng xem và sửa: Họ tên, số điện thoại, ngày sinh, giới tính, địa chỉ
 * - Không cho phép tự đổi email và vai trò (chỉ đọc)
 * - Kiểm tra định dạng số điện thoại Việt Nam (10 chữ số, đầu số hợp lệ)
 */
@WebServlet(urlPatterns = {"/api/profile", "/api/user/profile"})
public class UserProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        Long userId = resolveUserId(request);
        String emailParam = request.getParameter("email");

        try {
            Map<String, Object> profile = null;
            if (userId != null && userId > 0) {
                profile = userDAO.getUserProfile(userId);
            } else if (emailParam != null && !emailParam.trim().isEmpty()) {
                profile = userDAO.getUserProfileByEmail(emailParam.trim());
            }

            if (profile == null) {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Không tìm thấy thông tin hồ sơ người dùng.", "Quay lại trang chủ", "/index.html");
                return;
            }

            ApiResponse.success(response, "PROFILE_FETCH_SUCCESS", "Lấy thông tin hồ sơ thành công.", profile);
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi truy xuất hồ sơ: " + e.getMessage(), "Thử lại", "/index.html");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleUpdateProfile(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleUpdateProfile(request, response);
    }

    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        Long userId = resolveUserId(request);
        String emailParam = request.getParameter("email");

        JsonObject body;
        try {
            body = GSON.fromJson(request.getReader(), JsonObject.class);
        } catch (Exception e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON",
                    "Dữ liệu JSON không hợp lệ.", "Kiểm tra dữ liệu", null);
            return;
        }

        if (body == null) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "EMPTY_REQUEST",
                    "Dữ liệu cập nhật không được để trống.", "Kiểm tra dữ liệu", null);
            return;
        }

        // Kiểm tra an ninh: Không cho phép tự đổi email hoặc vai trò (S2-02 AC2)
        if (body.has("email") && !body.get("email").isJsonNull()) {
            String submittedEmail = body.get("email").getAsString();
            // Nếu gửi email khác với email hiện tại thì từ chối hoặc cảnh báo
            // Ở đây ghi log và bỏ qua không cho phép thay đổi email
        }

        if (body.has("roles") || body.has("role") || body.has("roleCode")) {
            // Không cho phép tự đổi vai trò
        }

        String fullName = body.has("fullName") && !body.get("fullName").isJsonNull()
                ? body.get("fullName").getAsString() : null;
        if (fullName == null && body.has("name") && !body.get("name").isJsonNull()) {
            fullName = body.get("name").getAsString();
        }

        String phone = body.has("phone") && !body.get("phone").isJsonNull()
                ? body.get("phone").getAsString() : null;
        String dateOfBirth = body.has("dateOfBirth") && !body.get("dateOfBirth").isJsonNull()
                ? body.get("dateOfBirth").getAsString() : null;
        String gender = body.has("gender") && !body.get("gender").isJsonNull()
                ? body.get("gender").getAsString() : "OTHER";
        String address = body.has("address") && !body.get("address").isJsonNull()
                ? body.get("address").getAsString() : null;

        // Bắt buộc kiểm tra định dạng số điện thoại Việt Nam (S2-02 AC3)
        if (phone != null && !phone.trim().isEmpty()) {
            String normalizedPhone = phone.trim().replaceAll("[\\s.-]", "");
            if (!UserDAO.isValidVietnamPhone(normalizedPhone)) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PHONE_FORMAT",
                        "Số điện thoại không đúng định dạng di động Việt Nam (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09).",
                        "Kiểm tra lại số điện thoại", null);
                return;
            }
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "NAME_REQUIRED",
                    "Họ và tên không được để trống.", "Nhập họ và tên", null);
            return;
        }

        try {
            // Xác định target userId
            if (userId == null || userId <= 0) {
                if (emailParam != null && !emailParam.trim().isEmpty()) {
                    Map<String, Object> existing = userDAO.getUserProfileByEmail(emailParam.trim());
                    if (existing != null && existing.get("id") instanceof Long id) {
                        userId = id;
                    }
                }
            }

            if (userId == null || userId <= 0) {
                ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                        "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.", "Đăng nhập lại", "/login.html");
                return;
            }

            Map<String, Object> updated = userDAO.updateUserProfile(userId, fullName, phone, dateOfBirth, gender, address);

            // Cập nhật session nếu có
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("currentUser") instanceof User user) {
                user.setFullName(fullName.trim());
                user.setPhone(phone != null ? phone.trim() : "");
                user.setDateOfBirth(dateOfBirth);
                user.setGender(gender);
                user.setAddress(address);
            }

            ApiResponse.success(response, "PROFILE_UPDATED", "Cập nhật hồ sơ cá nhân thành công.", updated);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra lại thông tin", null);
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu: " + e.getMessage(), "Thử lại", null);
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("authenticatedUserId");
        if (attr instanceof Long l) return l;

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") instanceof User user) {
            return user.getId();
        }

        String userIdParam = request.getParameter("userId");
        if (userIdParam != null) {
            try {
                return Long.parseLong(userIdParam.trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
