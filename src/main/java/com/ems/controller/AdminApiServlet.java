package com.ems.controller;

import com.ems.dao.AdminDAO;
import com.ems.dao.PermissionDAO;
import com.ems.security.ApiResponse;
import com.ems.service.EmailService;
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
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/api/admin/*")
public class AdminApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Pattern ROLE_PERMISSION_PATH = Pattern.compile("^/roles/([A-Z_]+)/permissions$");
    private static final Pattern USER_ROLE_PATH = Pattern.compile("^/users/(\\d+)/roles$");
    private static final Pattern USER_ROLE_REVOKE_PATH = Pattern.compile("^/users/(\\d+)/roles/([A-Za-z0-9_]+)$");
    private static final Pattern USER_STATUS_PATH = Pattern.compile("^/users/(\\d+)/status$");
    private static final Pattern USER_DETAIL_PATH = Pattern.compile("^/users/(\\d+)$");
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String path = request.getPathInfo() == null ? "" : request.getPathInfo();
            if ("/users/import/template".equals(path)) {
                response.setContentType("text/csv; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung_tms.csv\"");
                String csv = "\uFEFFHọ và tên,Email,Số điện thoại,Vai trò,Ngày sinh,Giới tính,Địa chỉ\n"
                        + "Nguyễn Văn An,an.nguyen@tms.vn,0912345678,HOC_VIEN,2003-05-15,Nam,Hà Nội\n"
                        + "Trần Thị Bình,binh.tran@tms.vn,0987654321,HOC_VIEN,2002-11-20,Nữ,Thái Nguyên\n"
                        + "Lê Hoàng Cường,cuong.le@tms.vn,0903112233,GIANG_VIEN,1990-08-10,Nam,Đà Nẵng\n";
                response.getWriter().write(csv);
                return;
            }
            Object data = switch (path) {
                case "/roles" -> permissionDAO.listRoles();
                case "/permissions" -> permissionDAO.listPermissions();
                case "/menus" -> permissionDAO.listMenus();
                case "/users" -> {
                    String q = request.getParameter("q");
                    if (q == null) q = request.getParameter("keyword");
                    String role = request.getParameter("role");
                    if (role == null) role = request.getParameter("roleCode");
                    String status = request.getParameter("status");
                    int page = 1;
                    int pageSize = 20;
                    try {
                        if (request.getParameter("page") != null) page = Integer.parseInt(request.getParameter("page"));
                        if (request.getParameter("pageSize") != null) pageSize = Integer.parseInt(request.getParameter("pageSize"));
                        if (request.getParameter("limit") != null) pageSize = Integer.parseInt(request.getParameter("limit"));
                    } catch (NumberFormatException ignored) {}
                    yield adminDAO.searchUsers(q, role, status, page, pageSize);
                }
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
        Matcher statusMatcher = USER_STATUS_PATH.matcher(path);
        Matcher detailMatcher = USER_DETAIL_PATH.matcher(path);
        try {
            JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
            if (detailMatcher.matches()) {
                Object actor = request.getAttribute("authenticatedUserId");
                if (!(actor instanceof Long actorId)) {
                    ApiResponse.error(response, 401, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.",
                            "Đăng nhập lại", "/login.html");
                    return;
                }
                long targetId = Long.parseLong(detailMatcher.group(1));
                String fullName = body != null && body.has("fullName") ? body.get("fullName").getAsString()
                        : (body != null && body.has("name") ? body.get("name").getAsString() : null);
                String phone = body != null && body.has("phone") ? body.get("phone").getAsString() : null;
                Map<String, Object> updated = adminDAO.updateUserDetails(actorId, targetId, fullName, phone);
                if (updated == null) {
                    ApiResponse.error(response, 404, "USER_NOT_FOUND", "Không tìm thấy tài khoản.",
                            "Quay lại quản lý tài khoản", "/admin.html#tai-khoan");
                    return;
                }
                ApiResponse.success(response, "USER_UPDATED", "Cập nhật thông tin tài khoản thành công.", updated);
                return;
            }
            if (statusMatcher.matches()) {
                Object actor = request.getAttribute("authenticatedUserId");
                if (!(actor instanceof Long actorId)) {
                    ApiResponse.error(response, 401, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.",
                            "Đăng nhập lại", "/login.html");
                    return;
                }
                String status = body != null && body.has("status") ? body.get("status").getAsString() : null;
                String lockedReason = body != null && body.has("lockedReason") ? body.get("lockedReason").getAsString()
                        : (body != null && body.has("reason") ? body.get("reason").getAsString() : null);

                long targetId = Long.parseLong(statusMatcher.group(1));
                Map<String, Object> updated = adminDAO.updateUserStatus(actorId, targetId, status, lockedReason);
                if (updated == null) {
                    ApiResponse.error(response, 404, "USER_NOT_FOUND", "Không tìm thấy tài khoản.",
                            "Quay lại quản lý tài khoản", "/admin.html#tai-khoan");
                    return;
                }
                String code = "LOCKED".equalsIgnoreCase(status) ? "USER_LOCKED" : "USER_UNLOCKED";
                String msg = "LOCKED".equalsIgnoreCase(status)
                        ? "Khoá tài khoản thành công. Toàn bộ phiên đăng nhập đã được thu hồi."
                        : "Mở khoá tài khoản thành công.";
                ApiResponse.success(response, code, msg, updated);
                return;
            }
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher userMatcher = USER_ROLE_PATH.matcher(path);
        try {
            if ("/users/import/preview".equals(path)) {
                JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                List<Map<String, Object>> rows = parseRowsFromJson(body);
                Map<String, Object> preview = adminDAO.previewUsersBatch(rows);
                ApiResponse.success(response, "IMPORT_PREVIEW_SUCCESS", "Xem trước danh sách người dùng thành công.", preview);
                return;
            }
            if ("/users/import".equals(path)) {
                Object actor = request.getAttribute("authenticatedUserId");
                long actorId = (actor instanceof Long l) ? l : 0;
                JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                String fileName = body != null && body.has("fileName") ? body.get("fileName").getAsString() : "import_users.xlsx";
                List<Map<String, Object>> rows = parseRowsFromJson(body);
                Map<String, Object> summary = adminDAO.importUsersBatch(actorId, fileName, rows);
                ApiResponse.success(response, "IMPORT_BATCH_SUCCESS", "Nhập danh sách người dùng thành công.", summary);
                return;
            }
            if ("/users".equals(path) || "/users/".equals(path)) {
                Object actor = request.getAttribute("authenticatedUserId");
                if (!(actor instanceof Long actorId)) {
                    ApiResponse.error(response, 401, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.",
                            "Đăng nhập lại", "/login.html");
                    return;
                }
                JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                String email = body != null && body.has("email") ? body.get("email").getAsString() : null;
                String fullName = body != null && body.has("fullName") ? body.get("fullName").getAsString()
                        : (body != null && body.has("name") ? body.get("name").getAsString() : null);
                String phone = body != null && body.has("phone") ? body.get("phone").getAsString() : null;
                String userCode = body != null && body.has("userCode") ? body.get("userCode").getAsString() : null;
                String tempPassword = body != null && body.has("tempPassword") ? body.get("tempPassword").getAsString()
                        : (body != null && body.has("password") ? body.get("password").getAsString() : null);
                List<String> roleCodes = stringArray(body, "roles");
                if (roleCodes.isEmpty()) {
                    String singleRole = body != null && body.has("role") ? body.get("role").getAsString() : null;
                    if (singleRole != null && !singleRole.isBlank()) roleCodes = List.of(singleRole);
                }

                try {
                    Map<String, Object> created = adminDAO.createUser(actorId, userCode, email, fullName, phone, roleCodes, tempPassword);
                    final String userEmail = (String) created.get("email");
                    final String userName = (String) created.get("fullName");
                    final String userTempPass = (String) created.get("tempPassword");
                    String appBase = System.getenv("APP_BASE_URL");
                    if (appBase == null || appBase.isBlank()) appBase = "http://localhost:5173";
                    final String loginLink = appBase + "/login.html";
                    java.util.concurrent.CompletableFuture.runAsync(() ->
                            EmailService.sendActivationEmail(userEmail, userName, userTempPass, loginLink)
                    );

                    ApiResponse.success(response, "USER_CREATED", "Tạo tài khoản thành công. Email kích hoạt kèm mật khẩu tạm đã được gửi.", created);
                    return;
                } catch (IllegalArgumentException ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("Email này đã tồn tại")) {
                        ApiResponse.error(response, 409, "EMAIL_ALREADY_EXISTS", ex.getMessage(),
                                "Nhập email khác", "/admin.html#tai-khoan");
                        return;
                    }
                    throw ex;
                }
            }
            if (userMatcher.matches()) {
                Object actor = request.getAttribute("authenticatedUserId");
                if (!(actor instanceof Long actorId)) {
                    ApiResponse.error(response, 401, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.",
                            "Đăng nhập lại", "/login.html");
                    return;
                }
                JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                String role = body != null && body.has("role") ? body.get("role").getAsString()
                        : (body != null && body.has("roleCode") ? body.get("roleCode").getAsString() : null);
                if (role == null || role.trim().isEmpty()) {
                    ApiResponse.error(response, 400, "REQUEST_VALIDATION_ERROR", "Vui lòng cung cấp vai trò cần gán.",
                            "Kiểm tra lại lựa chọn", "/admin.html#vai-tro");
                    return;
                }
                Map<String, Object> updated = adminDAO.assignRole(actorId, Long.parseLong(userMatcher.group(1)), role);
                if (updated == null) {
                    ApiResponse.error(response, 404, "USER_NOT_FOUND", "Không tìm thấy tài khoản.",
                            "Quay lại quản lý tài khoản", "/admin.html#tai-khoan");
                    return;
                }
                ApiResponse.success(response, "USER_ROLE_ASSIGNED", "Gán vai trò thành công.", updated);
                return;
            }
            ApiResponse.error(response, 404, "API_NOT_FOUND", "Không tìm thấy chức năng.",
                    "Quay lại trang quản trị", "/admin.html");
        } catch (IllegalStateException exception) {
            ApiResponse.error(response, 409, "ROLE_ALREADY_ASSIGNED", exception.getMessage(),
                    "Kiểm tra lại lựa chọn", "/admin.html#vai-tro");
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

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher revokeMatcher = USER_ROLE_REVOKE_PATH.matcher(path);
        try {
            if (revokeMatcher.matches()) {
                Object actor = request.getAttribute("authenticatedUserId");
                if (!(actor instanceof Long actorId)) {
                    ApiResponse.error(response, 401, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.",
                            "Đăng nhập lại", "/login.html");
                    return;
                }
                long targetId = Long.parseLong(revokeMatcher.group(1));
                String roleCode = revokeMatcher.group(2);
                Map<String, Object> updated = adminDAO.revokeRole(actorId, targetId, roleCode);
                if (updated == null) {
                    ApiResponse.error(response, 404, "USER_NOT_FOUND", "Không tìm thấy tài khoản.",
                            "Quay lại quản lý tài khoản", "/admin.html#tai-khoan");
                    return;
                }
                ApiResponse.success(response, "USER_ROLE_REVOKED", "Thu hồi vai trò thành công.", updated);
                return;
            }
            ApiResponse.error(response, 404, "API_NOT_FOUND", "Không tìm thấy chức năng.",
                    "Quay lại trang quản trị", "/admin.html");
        } catch (IllegalStateException exception) {
            ApiResponse.error(response, 404, "ROLE_NOT_ASSIGNED", exception.getMessage(),
                    "Kiểm tra lại lựa chọn", "/admin.html#vai-tro");
        } catch (IllegalArgumentException exception) {
            ApiResponse.error(response, 409, "CANNOT_REVOKE_OWN_ADMIN", exception.getMessage(),
                    "Kiểm tra lại lựa chọn", "/admin.html#vai-tro");
        } catch (SQLException exception) {
            serviceUnavailable(response);
        }
    }

    private List<Map<String, Object>> parseRowsFromJson(JsonObject body) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (body != null && body.has("rows") && body.get("rows").isJsonArray()) {
            JsonArray arr = body.getAsJsonArray("rows");
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    Map<String, Object> row = new LinkedHashMap<>();
                    if (obj.has("fullName")) row.put("fullName", obj.get("fullName").getAsString());
                    else if (obj.has("name")) row.put("fullName", obj.get("name").getAsString());
                    if (obj.has("email")) row.put("email", obj.get("email").getAsString());
                    if (obj.has("phone")) row.put("phone", obj.get("phone").getAsString());
                    if (obj.has("role")) row.put("role", obj.get("role").getAsString());
                    if (obj.has("dateOfBirth")) row.put("dateOfBirth", obj.get("dateOfBirth").getAsString());
                    if (obj.has("gender")) row.put("gender", obj.get("gender").getAsString());
                    if (obj.has("address")) row.put("address", obj.get("address").getAsString());
                    rows.add(row);
                }
            }
        }
        return rows;
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