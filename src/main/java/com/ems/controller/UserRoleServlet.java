package com.ems.controller;

import com.ems.constant.RoleConstant;
import com.ems.dao.AdminDAO;
import com.ems.model.User;
import com.ems.service.UserStore;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Tác vụ Jira: IDTTX-24 [BE] Gán và thu hồi vai trò của một người dùng
 * URL Mapping: /admin/users/roles
 * Chức năng:
 *   - Xem danh sách người dùng và các vai trò hiện có
 *   - Gán thêm vai trò (1 người dùng có thể giữ nhiều vai trò cùng lúc)
 *   - Thu hồi vai trò (chặn không thể tự thu hồi vai trò Quản trị của chính mình)
 *   - Thay đổi có hiệu lực ngay ở thao tác kế tiếp
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
@WebServlet(name = "UserRoleServlet", urlPatterns = {"/admin/users/roles"})
public class UserRoleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final List<String> ASSIGNABLE_ROLES = Arrays.asList(
            RoleConstant.ADMIN,
            RoleConstant.TRAINING_MANAGER,
            RoleConstant.ADMISSIONS,
            RoleConstant.INSTRUCTOR,
            RoleConstant.TEACHING_ASSISTANT,
            RoleConstant.ACCOUNTANT,
            RoleConstant.STUDENT
    );

    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || !currentUser.hasRole(RoleConstant.ADMIN)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("errorMessage", "Chức năng này chỉ dành riêng cho Quản trị hệ thống (System Admin).");
            req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, resp);
            return;
        }

        // Chuyển flash message từ session sang request nếu có
        if (session.getAttribute("flashSuccess") != null) {
            req.setAttribute("successMessage", session.getAttribute("flashSuccess"));
            session.removeAttribute("flashSuccess");
        }
        if (session.getAttribute("flashError") != null) {
            req.setAttribute("errorMessage", session.getAttribute("flashError"));
            session.removeAttribute("flashError");
        }

        // Nạp danh sách người dùng: ưu tiên UserStore (đảm bảo đồng bộ ngay lập tức cho IDTTX-24)
        List<User> userList = UserStore.getAllUsers();

        req.setAttribute("users", userList);
        req.setAttribute("assignableRoles", ASSIGNABLE_ROLES);
        req.setAttribute("currentUser", currentUser);

        req.getRequestDispatcher("/WEB-INF/views/admin/user-roles.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || !currentUser.hasRole(RoleConstant.ADMIN)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("errorMessage", "Chức năng này chỉ dành riêng cho Quản trị hệ thống (System Admin).");
            req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, resp);
            return;
        }

        String action = req.getParameter("action");
        String userIdStr = req.getParameter("userId");
        String role = req.getParameter("role");

        try {
            if (userIdStr == null || userIdStr.trim().isEmpty() || role == null || role.trim().isEmpty()) {
                session.setAttribute("flashError", "Thiếu thông tin người dùng hoặc vai trò cần thao tác.");
                resp.sendRedirect(req.getContextPath() + "/admin/users/roles");
                return;
            }

            long targetUserId = Long.parseLong(userIdStr.trim());
            role = role.trim();

            if ("assign".equalsIgnoreCase(action)) {
                UserStore.assignRole(currentUser.getId(), targetUserId, role);
                try {
                    adminDAO.assignRole(currentUser.getId(), targetUserId, role);
                } catch (Exception ignored) {
                }
                session.setAttribute("flashSuccess", "Đã gán vai trò '" + RoleConstant.getLabel(role) + "' (" + role + ") thành công cho người dùng ID " + targetUserId + ".");
            } else if ("revoke".equalsIgnoreCase(action)) {
                UserStore.revokeRole(currentUser.getId(), targetUserId, role);
                try {
                    adminDAO.revokeRole(currentUser.getId(), targetUserId, role);
                } catch (Exception ignored) {
                }
                session.setAttribute("flashSuccess", "Đã thu hồi vai trò '" + RoleConstant.getLabel(role) + "' (" + role + ") thành công khỏi người dùng ID " + targetUserId + ".");
            } else {
                session.setAttribute("flashError", "Hành động '" + action + "' không được hỗ trợ.");
            }
        } catch (SecurityException e) {
            // IDTTX-24: Không thể tự thu hồi vai trò quản trị của chính mình
            session.setAttribute("flashError", "Lỗi bảo mật (CANNOT_REVOKE_OWN_ADMIN): " + e.getMessage());
        } catch (IllegalStateException e) {
            session.setAttribute("flashError", e.getMessage());
        } catch (IllegalArgumentException e) {
            session.setAttribute("flashError", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            session.setAttribute("flashError", "Đã xảy ra lỗi: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/users/roles");
    }
}
