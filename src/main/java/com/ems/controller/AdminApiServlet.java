package com.ems.controller;

import com.ems.dao.AdminDAO;
import com.ems.dao.PermissionDAO;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/api/admin/*")
public class AdminApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Pattern ROLE_PERMISSION_PATH = Pattern.compile("^/roles/([A-Z_]+)/permissions$");
    private static final Pattern USER_ROLE_PATH = Pattern.compile("^/users/(\\d+)/roles$");
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String path = request.getPathInfo() == null ? "" : request.getPathInfo();
            Object data = switch (path) {
                case "/roles" -> permissionDAO.listRoles();
                case "/permissions" -> permissionDAO.listPermissions();
                case "/menus" -> permissionDAO.listMenus();
                case "/users" -> adminDAO.listUsers();
                default -> null;
            };
            if (data == null) {
                ApiResponse.error(response, 404, "API_NOT_FOUND", "Không tìm thấy chức năng.",
                        "Quay lại trang quản trị", "/admin.html");
                return;
            }
            ApiResponse.success(response, "ADMIN_DATA_SUCCESS", "Lấy dữ liệu quản trị thành công.", data);
        } catch (SQLException exception) {
            serviceUnavailable(response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher roleMatcher = ROLE_PERMISSION_PATH.matcher(path);
        Matcher userMatcher = USER_ROLE_PATH.matcher(path);
        try {
            JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
            if (roleMatcher.matches()) {
                Map<String, Object> updated = permissionDAO.replaceRolePermissions(
                        roleMatcher.group(1),
                        stringArray(body, "permissionCodes")
                );
                if (updated == null) {
                    ApiResponse.error(response, 404, "ROLE_NOT_FOUND", "Không tìm thấy vai trò.",
                            "Quay lại phân quyền", "/admin.html#vai-tro");
                    return;
                }
                ApiResponse.success(response, "ROLE_PERMISSIONS_UPDATED", "Cập nhật quyền vai trò thành công.", updated);
                return;
            }
            if (userMatcher.matches()) {
                Object actor = request.getAttribute("authenticatedUserId");
                if (!(actor instanceof Long actorId)) {
                    ApiResponse.error(response, 401, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.",
                            "Đăng nhập lại", "/login.html");
                    return;
                }
                Map<String, Object> updated = adminDAO.replaceUserRoles(
                        actorId,
                        Long.parseLong(userMatcher.group(1)),
                        stringArray(body, "roleCodes")
                );
                if (updated == null) {
                    ApiResponse.error(response, 404, "USER_NOT_FOUND", "Không tìm thấy tài khoản.",
                            "Quay lại quản lý tài khoản", "/admin.html#tai-khoan");
                    return;
                }
                ApiResponse.success(response, "USER_ROLES_UPDATED", "Cập nhật vai trò thành công.", updated);
                return;
            }
            ApiResponse.error(response, 404, "API_NOT_FOUND", "Không tìm thấy chức năng.",
                    "Quay lại trang quản trị", "/admin.html");
        } catch (IllegalArgumentException exception) {
            ApiResponse.error(response, 400, "REQUEST_VALIDATION_ERROR", exception.getMessage(),
                    "Kiểm tra lại lựa chọn", "/admin.html#vai-tro");
        } catch (SQLException exception) {
            serviceUnavailable(response);
        } catch (RuntimeException exception) {
            ApiResponse.error(response, 400, "REQUEST_INVALID_JSON", "Dữ liệu gửi lên không hợp lệ.",
                    "Quay lại trang quản trị", "/admin.html");
        }
    }

    private List<String> stringArray(JsonObject body, String key) {
        JsonElement element = body == null ? null : body.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("Danh sách vai trò hoặc quyền không hợp lệ.");
        }
        List<String> values = new ArrayList<>();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement item : array) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Danh sách vai trò hoặc quyền không hợp lệ.");
            }
            values.add(item.getAsString());
        }
        return values;
    }

    private void serviceUnavailable(HttpServletResponse response) throws IOException {
        ApiResponse.error(response, 503, "SYSTEM_SERVICE_UNAVAILABLE",
                "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.",
                "Thử lại", "/admin.html");
    }
}