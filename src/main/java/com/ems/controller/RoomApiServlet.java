package com.ems.controller;

import com.ems.dao.PermissionDAO;
import com.ems.dto.CreateRoomRequest;
import com.ems.dto.RoomCapacityCheckRequest;
import com.ems.dto.RoomCapacityValidationResult;
import com.ems.dto.RoomResponse;
import com.ems.dto.UpdateRoomRequest;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.RoomCapacityException;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.ems.service.RoomService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API Servlet Quản lý phòng học (User Story S3-09, Epic EP-04)
 * Endpoints:
 * - GET    /api/rooms               : Lấy danh sách phòng học (lọc, phân trang)
 * - GET    /api/rooms/{id}          : Lấy chi tiết phòng học
 * - GET    /api/rooms/{id}/validate-capacity : Kiểm tra sức chứa phòng khi xếp lớp
 * - POST   /api/rooms               : Tạo phòng học mới
 * - POST   /api/rooms/{id}/validate-capacity : Kiểm tra sức chứa phòng khi xếp lớp
 * - PUT    /api/rooms/{id}          : Cập nhật phòng học
 * - DELETE /api/rooms/{id}          : Xóa / Ngừng hoạt động phòng học
 */
@WebServlet(urlPatterns = {"/api/rooms", "/api/rooms/*"})
public class RoomApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^/(\\d+)$");
    private static final Pattern VALIDATE_CAPACITY_PATTERN = Pattern.compile("^/(\\d+)/validate-capacity$");

    private final RoomService roomService;
    private final PermissionDAO permissionDAO;

    public RoomApiServlet() {
        this.roomService = new RoomService();
        this.permissionDAO = new PermissionDAO();
    }

    public RoomApiServlet(RoomService roomService, PermissionDAO permissionDAO) {
        this.roomService = roomService != null ? roomService : new RoomService();
        this.permissionDAO = permissionDAO != null ? permissionDAO : new PermissionDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (!isAuthorized(request, response, "ROOM_VIEW")) {
            return;
        }

        try {
            // 1. GET /api/rooms/{id}/validate-capacity?studentCount=30
            Matcher validateMatcher = pathInfo != null ? VALIDATE_CAPACITY_PATTERN.matcher(pathInfo) : null;
            if (validateMatcher != null && validateMatcher.matches()) {
                long roomId = Long.parseLong(validateMatcher.group(1));
                String countParam = request.getParameter("studentCount");
                if (countParam == null || countParam.trim().isEmpty()) {
                    ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                            "Tham số 'studentCount' là bắt buộc.", "Nhập sĩ số lớp", "/training-manager.html#phong-hoc");
                    return;
                }
                int studentCount;
                try {
                    studentCount = Integer.parseInt(countParam.trim());
                } catch (NumberFormatException e) {
                    ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                            "Sĩ số lớp phải là số nguyên dương.", "Kiểm tra lại", "/training-manager.html#phong-hoc");
                    return;
                }

                RoomCapacityValidationResult result = roomService.validateCapacity(roomId, studentCount);
                if (result.isValid()) {
                    ApiResponse.success(response, HttpServletResponse.SC_OK, result.getCode(), result.getMessage(), result.toDetailsMap());
                } else {
                    ApiResponse.errorWithDetails(response, HttpServletResponse.SC_BAD_REQUEST, result.getCode(),
                            result.getMessage(), result.toDetailsMap(), "Chọn phòng khác", "/training-manager.html#phong-hoc");
                }
                return;
            }

            // 2. GET /api/rooms/{id}
            Matcher idMatcher = pathInfo != null ? ROOM_ID_PATTERN.matcher(pathInfo) : null;
            if (idMatcher != null && idMatcher.matches()) {
                long roomId = Long.parseLong(idMatcher.group(1));
                RoomResponse room = roomService.getRoomById(roomId);
                ApiResponse.success(response, HttpServletResponse.SC_OK, "ROOM_DETAIL_SUCCESS", "Lấy chi tiết phòng học thành công.", room);
                return;
            }

            // 3. GET /api/rooms
            if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
                String keyword = request.getParameter("q");
                if (keyword == null) keyword = request.getParameter("keyword");
                String roomType = request.getParameter("roomType");
                String activeParam = request.getParameter("isActive");
                Boolean isActive = activeParam != null && !activeParam.isBlank() ? Boolean.parseBoolean(activeParam) : null;

                int page = 1;
                int pageSize = 20;
                try {
                    if (request.getParameter("page") != null) page = Integer.parseInt(request.getParameter("page"));
                    if (request.getParameter("pageSize") != null) pageSize = Integer.parseInt(request.getParameter("pageSize"));
                    if (request.getParameter("limit") != null) pageSize = Integer.parseInt(request.getParameter("limit"));
                } catch (NumberFormatException ignored) {}

                Map<String, Object> data = roomService.getRooms(keyword, roomType, isActive, page, pageSize);
                ApiResponse.success(response, HttpServletResponse.SC_OK, "ROOM_LIST_SUCCESS", "Lấy danh sách phòng học thành công.", data);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Không tìm thấy endpoint yêu cầu.", "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ROOM_NOT_FOUND",
                    e.getMessage(), "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra dữ liệu", "/training-manager.html#phong-hoc");
        } catch (SQLException e) {
            handleSqlException(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        try {
            // 1. POST /api/rooms/{id}/validate-capacity
            Matcher validateMatcher = pathInfo != null ? VALIDATE_CAPACITY_PATTERN.matcher(pathInfo) : null;
            if (validateMatcher != null && validateMatcher.matches()) {
                if (!isAuthorized(request, response, "ROOM_VIEW")) return;
                long roomId = Long.parseLong(validateMatcher.group(1));
                RoomCapacityCheckRequest reqBody = GSON.fromJson(request.getReader(), RoomCapacityCheckRequest.class);
                if (reqBody == null || reqBody.getStudentCount() == null) {
                    ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                            "Dữ liệu kiểm tra yêu cầu 'studentCount'.", "Nhập sĩ số", "/training-manager.html#phong-hoc");
                    return;
                }

                RoomCapacityValidationResult result = roomService.validateCapacity(roomId, reqBody.getStudentCount());
                if (result.isValid()) {
                    ApiResponse.success(response, HttpServletResponse.SC_OK, result.getCode(), result.getMessage(), result.toDetailsMap());
                } else {
                    ApiResponse.errorWithDetails(response, HttpServletResponse.SC_BAD_REQUEST, result.getCode(),
                            result.getMessage(), result.toDetailsMap(), "Chọn phòng khác", "/training-manager.html#phong-hoc");
                }
                return;
            }

            // 2. POST /api/rooms
            if (pathInfo == null || "/".equals(pathInfo) || pathInfo.isEmpty()) {
                if (!isAuthorized(request, response, "ROOM_MANAGE")) return;
                CreateRoomRequest createReq = GSON.fromJson(request.getReader(), CreateRoomRequest.class);
                RoomResponse created = roomService.createRoom(createReq);
                ApiResponse.success(response, HttpServletResponse.SC_CREATED, "ROOM_CREATED",
                        "Tạo phòng học mới thành công.", created);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Không tìm thấy endpoint yêu cầu.", "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (DuplicateResourceException e) {
            ApiResponse.error(response, HttpServletResponse.SC_CONFLICT, "ROOM_CODE_ALREADY_EXISTS",
                    e.getMessage(), "Đổi mã phòng", "/training-manager.html#phong-hoc");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra dữ liệu", "/training-manager.html#phong-hoc");
        } catch (JsonSyntaxException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_INVALID_JSON",
                    "Định dạng JSON gửi lên không hợp lệ.", "Kiểm tra lại", "/training-manager.html#phong-hoc");
        } catch (SQLException e) {
            handleSqlException(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (!isAuthorized(request, response, "ROOM_MANAGE")) return;

        try {
            Matcher idMatcher = pathInfo != null ? ROOM_ID_PATTERN.matcher(pathInfo) : null;
            if (idMatcher != null && idMatcher.matches()) {
                long roomId = Long.parseLong(idMatcher.group(1));
                UpdateRoomRequest updateReq = GSON.fromJson(request.getReader(), UpdateRoomRequest.class);
                RoomResponse updated = roomService.updateRoom(roomId, updateReq);
                ApiResponse.success(response, HttpServletResponse.SC_OK, "ROOM_UPDATED",
                        "Cập nhật thông tin phòng học thành công.", updated);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Không tìm thấy endpoint yêu cầu.", "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ROOM_NOT_FOUND",
                    e.getMessage(), "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (DuplicateResourceException e) {
            ApiResponse.error(response, HttpServletResponse.SC_CONFLICT, "ROOM_CODE_ALREADY_EXISTS",
                    e.getMessage(), "Đổi mã phòng", "/training-manager.html#phong-hoc");
        } catch (RoomCapacityException e) {
            Object details = e.getValidationResult() != null ? e.getValidationResult().toDetailsMap() : null;
            ApiResponse.errorWithDetails(response, HttpServletResponse.SC_CONFLICT, e.getCode(),
                    e.getMessage(), details, "Điều chỉnh sĩ số lớp trước", "/training-manager.html#phong-hoc");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra dữ liệu", "/training-manager.html#phong-hoc");
        } catch (JsonSyntaxException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_INVALID_JSON",
                    "Định dạng JSON gửi lên không hợp lệ.", "Kiểm tra lại", "/training-manager.html#phong-hoc");
        } catch (SQLException e) {
            handleSqlException(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (!isAuthorized(request, response, "ROOM_MANAGE")) return;

        try {
            Matcher idMatcher = pathInfo != null ? ROOM_ID_PATTERN.matcher(pathInfo) : null;
            if (idMatcher != null && idMatcher.matches()) {
                long roomId = Long.parseLong(idMatcher.group(1));
                Map<String, Object> result = roomService.deleteRoom(roomId);
                String code = "DEACTIVATED".equals(result.get("action")) ? "ROOM_DEACTIVATED" : "ROOM_DELETED";
                ApiResponse.success(response, HttpServletResponse.SC_OK, code, (String) result.get("message"), result);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Không tìm thấy endpoint yêu cầu.", "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ROOM_NOT_FOUND",
                    e.getMessage(), "Về danh sách phòng", "/training-manager.html#phong-hoc");
        } catch (SQLException e) {
            handleSqlException(response, e);
        }
    }

    /**
     * Kiểm tra quyền người dùng đối với các thao tác Room (Bảo mật 2 lớp cùng SessionAuthFilter)
     */
    private boolean isAuthorized(HttpServletRequest request, HttpServletResponse response, String requiredPermission)
            throws IOException {
        Object authAttr = request.getAttribute("authenticatedUserId");
        HttpSession session = request.getSession(false);
        User currentUser = session != null ? (User) session.getAttribute("currentUser") : null;

        if (authAttr == null && currentUser == null) {
            ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để tiếp tục thao tác.", "Đăng nhập", "/login.html");
            return false;
        }

        long userId = authAttr instanceof Long ? (Long) authAttr : (currentUser != null ? currentUser.getId() : -1L);
        if (currentUser != null && (currentUser.hasRole("Admin") || currentUser.hasRole("TrainingManager"))) {
            return true;
        }

        try {
            if (!permissionDAO.hasPermission(userId, requiredPermission)) {
                ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN",
                        "Bạn không có quyền thực hiện chức năng quản lý phòng học.", "Quay lại", "/index.html");
                return false;
            }
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SYSTEM_SERVICE_UNAVAILABLE",
                    "Dịch vụ xác thực tạm thời không khả dụng.", "Thử lại", "/index.html");
            return false;
        }
        return true;
    }

    private void handleSqlException(HttpServletResponse response, SQLException e) throws IOException {
        ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SYSTEM_SERVICE_UNAVAILABLE",
                "Hệ thống cơ sở dữ liệu tạm thời gián đoạn. Vui lòng thử lại sau.", "Thử lại", "/training-manager.html#phong-hoc");
    }
}
