package com.ems.controller;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.dto.ClassResponse;
import com.ems.dto.CreateClassRequest;
import com.ems.exception.BusinessRuleException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.ems.service.ClassService;
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
import java.util.List;

/**
 * Controller REST API xử lý nghiệp vụ Quản lý lớp học (User Story S3-05)
 * Endpoint: POST /api/classes (Mở lớp học mới)
 * Endpoint: GET /api/classes (Danh sách lớp học)
 * Endpoint: GET /api/classes/{id} (Chi tiết lớp học)
 */
@WebServlet(name = "ClassApiServlet", urlPatterns = {"/api/classes", "/api/classes/*"})
public class ClassApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private final ClassService classService;

    public ClassApiServlet() {
        this.classService = new ClassService();
    }

    public ClassApiServlet(ClassService classService) {
        this.classService = classService;
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra xác thực người dùng (Authentication)
        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện chức năng.", "Đăng nhập", "/login.html");
            return;
        }

        // 2. Kiểm tra phân quyền: Chỉ Training Manager (hoặc Admin) mới được mở lớp
        if (!isAuthorizedToManageClass(currentUser)) {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chỉ người dùng có vai trò Quản lý đào tạo (Training Manager) mới được phép mở lớp.",
                    "Quay lại", "/index.html");
            return;
        }

        // 3. Đọc dữ liệu JSON Request Body
        CreateClassRequest createRequest;
        try {
            createRequest = GSON.fromJson(req.getReader(), CreateClassRequest.class);
            if (createRequest == null) {
                ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_BODY_EMPTY",
                        "Dữ liệu gửi lên không được để trống.");
                return;
            }
        } catch (JsonSyntaxException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "REQUEST_INVALID_JSON",
                    "Định dạng JSON gửi lên không hợp lệ.");
            return;
        }

        // 4. Xử lý mở lớp qua ClassService
        try {
            ClassResponse response = classService.openClass(createRequest);
            ApiResponse.created(resp, "CLASS_OPENED_SUCCESS", "Mở lớp học thành công.", response);
        } catch (ValidationException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
        } catch (DuplicateResourceException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_CONFLICT, "CLASS_CODE_ALREADY_EXISTS", e.getMessage());
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "PROGRAM_NOT_FOUND", e.getMessage());
        } catch (BusinessRuleException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "PROGRAM_HAS_NO_SUBJECTS", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Lỗi CSDL khi mở lớp học: " + e.getMessage(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi lưu trữ dữ liệu. Giao dịch mở lớp đã được hoàn tác an toàn.");
        } catch (Exception e) {
            getServletContext().log("Lỗi hệ thống khi mở lớp học: " + e.getMessage(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                    "Đã xảy ra lỗi hệ thống trong quá trình xử lý.");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện chức năng.", "Đăng nhập", "/login.html");
            return;
        }

        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                List<ClassResponse> classes = classService.listClasses();
                ApiResponse.success(resp, "CLASS_LIST_SUCCESS", "Lấy danh sách lớp học thành công.", classes);
            } else {
                String idStr = pathInfo.substring(1);
                try {
                    long classId = Long.parseLong(idStr);
                    ClassResponse classDetail = classService.getById(classId);
                    ApiResponse.success(resp, "CLASS_DETAIL_SUCCESS", "Lấy chi tiết lớp học thành công.", classDetail);
                } catch (NumberFormatException e) {
                    ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_CLASS_ID",
                            "Mã định danh (ID) lớp học không hợp lệ.");
                } catch (ResourceNotFoundException e) {
                    ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "CLASS_NOT_FOUND", e.getMessage());
                }
            }
        } catch (SQLException e) {
            getServletContext().log("Lỗi CSDL khi truy vấn lớp học: " + e.getMessage(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Không thể kết nối cơ sở dữ liệu để lấy danh sách lớp.");
        }
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object userObj = session.getAttribute("currentUser");
        return (userObj instanceof User) ? (User) userObj : null;
    }

    private boolean isAuthorizedToManageClass(User user) {
        if (user == null) return false;
        List<String> roles = user.getRoles();
        if (roles != null) {
            for (String role : roles) {
                if (RoleConstant.TRAINING_MANAGER.equalsIgnoreCase(role)
                        || RoleConstant.ADMIN.equalsIgnoreCase(role)
                        || "TRAINING_MANAGER".equalsIgnoreCase(role)
                        || "ADMIN".equalsIgnoreCase(role)) {
                    return true;
                }
            }
        }
        if (user.getPrimaryRole() != null) {
            String primary = user.getPrimaryRole();
            if (RoleConstant.TRAINING_MANAGER.equalsIgnoreCase(primary)
                    || RoleConstant.ADMIN.equalsIgnoreCase(primary)
                    || "TRAINING_MANAGER".equalsIgnoreCase(primary)
                    || "ADMIN".equalsIgnoreCase(primary)) {
                return true;
            }
        }
        return PermissionConstant.hasPermission(roles, PermissionConstant.CLASS_MANAGE);
    }
}
