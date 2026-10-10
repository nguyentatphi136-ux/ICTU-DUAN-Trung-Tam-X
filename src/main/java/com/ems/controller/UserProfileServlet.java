package com.ems.controller;

import com.ems.dao.UserDAO;
import com.ems.exception.ApiException;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * S2-02: Xem và cập nhật hồ sơ cá nhân của người đang đăng nhập.
 * - Sửa được họ tên, số điện thoại, ngày sinh, giới tính, địa chỉ; email và vai trò chỉ đọc.
 * - Số điện thoại phải đúng định dạng di động Việt Nam và chưa được tài khoản khác dùng (409 PHONE_DUPLICATE).
 * Chỉ thao tác trên tài khoản trong phiên đăng nhập, không nhận userId hay email từ tham số.
 */
@WebServlet(urlPatterns = {"/api/profile", "/api/user/profile"})
public class UserProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = currentUserId(request);
        if (userId == null) {
            unauthorized(response);
            return;
        }
        try {
            Map<String, Object> profile = userDAO.getUserProfile(userId);
            if (profile == null) {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "PROFILE_NOT_FOUND",
                        "Không tìm thấy thông tin hồ sơ người dùng.");
                return;
            }
            ApiResponse.success(response, "PROFILE_FETCH_SUCCESS", "Lấy thông tin hồ sơ thành công.", profile);
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi truy xuất hồ sơ.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleUpdateProfile(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handleUpdateProfile(request, response);
    }

    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = currentUserId(request);
        if (userId == null) {
            unauthorized(response);
            return;
        }

        JsonObject body;
        try {
            body = GSON.fromJson(request.getReader(), JsonObject.class);
        } catch (RuntimeException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_JSON", "Dữ liệu JSON không hợp lệ.");
            return;
        }
        if (body == null) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "EMPTY_REQUEST",
                    "Dữ liệu cập nhật không được để trống.");
            return;
        }

        // Email và vai trò trong body (nếu có) bị bỏ qua: người dùng không tự đổi được (S2-02 AC2).
        String fullName = text(body, "fullName");
        if (fullName == null) fullName = text(body, "name");
        String phone = text(body, "phone");
        String dateOfBirth = text(body, "dateOfBirth");
        String gender = text(body, "gender");
        String address = text(body, "address");

        try {
            Map<String, Object> updated = userDAO.updateUserProfile(userId, fullName, phone, dateOfBirth, gender, address);

            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("currentUser") instanceof User user && updated != null) {
                user.setFullName(String.valueOf(updated.get("fullName")));
                user.setPhone(String.valueOf(updated.get("phone")));
                user.setDateOfBirth(String.valueOf(updated.get("dateOfBirth")));
                user.setGender(String.valueOf(updated.get("gender")));
                user.setAddress(String.valueOf(updated.get("address")));
            }
            ApiResponse.success(response, "PROFILE_UPDATED", "Cập nhật hồ sơ cá nhân thành công.", updated);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (IllegalStateException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "PROFILE_NOT_FOUND", e.getMessage());
        } catch (IllegalArgumentException e) {
            String code = e.getMessage() != null && e.getMessage().startsWith("Số điện thoại")
                    ? "INVALID_PHONE_FORMAT" : "VALIDATION_ERROR";
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, code, e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi lưu hồ sơ.");
        }
    }

    private static String text(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString() : null;
    }

    /** Người đang đăng nhập: SessionAuthFilter gán authenticatedUserId cho mọi đường /api/. */
    private static Long currentUserId(HttpServletRequest request) {
        if (request.getAttribute("authenticatedUserId") instanceof Long id) return id;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") instanceof User user) return user.getId();
        return null;
    }

    private static void unauthorized(HttpServletResponse response) throws IOException {
        ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.", "Đăng nhập lại", "/dang-nhap");
    }
}
