package com.ems.filter;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Tác vụ Jira: IDTTX-81 (S1-05) - Xây dựng middleware kiểm tra quyền ở tầng server, mặc định từ chối
 * Áp dụng mô hình Java Servlet / JSP Filter
 * Đảm bảo: Giảng viên không sửa được học phí, Kế toán không sửa được điểm
 */
@WebFilter(filterName = "AuthorizationFilter", urlPatterns = {"/grade/*", "/tuition/*", "/user/*"})
public class AuthorizationFilter implements Filter {

    // Danh sách các đường dẫn công khai (không cần kiểm tra quyền)
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/login",
            "/login.jsp",
            "/logout",
            "/index.html",
            "/index.jsp",
            "/css/",
            "/js/",
            "/images/",
            "/error"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI().substring(contextPath.length());

        // 1. Kiểm tra tài nguyên công khai
        for (String publicPath : PUBLIC_PATHS) {
            if (uri.equals(publicPath) || uri.startsWith(publicPath)) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        // 2. Kiểm tra xác thực (Session Login)
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            req.setAttribute("errorMessage", "Vui lòng đăng nhập để tiếp tục");
            req.getRequestDispatcher("/login.jsp").forward(req, res);
            return;
        }

        // Tác vụ Jira IDTTX-24: Đảm bảo thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp mà không cần đăng nhập lại
        try {
            User freshUser = new com.ems.dao.UserDAO().findById(currentUser.getId());
            if (freshUser != null) {
                currentUser = freshUser;
                session.setAttribute("currentUser", freshUser);
            }
        } catch (Exception ignored) {
        }

        // Kiểm tra trạng thái tài khoản
        if ("locked".equalsIgnoreCase(currentUser.getStatus()) || "inactive".equalsIgnoreCase(currentUser.getStatus())) {
            denyAccess(req, res, "Tài khoản của bạn đã bị khoá hoặc ngừng hoạt động.");
            return;
        }

        List<String> roles = currentUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            if (currentUser.getPrimaryRole() != null) {
                roles = Arrays.asList(currentUser.getPrimaryRole());
            }
        }

        // 3. KIỂM QUYỀN ĐIỂM SỐ (Grades)
        // Đặc tả: Giảng viên sửa được điểm, Kế toán TUYỆT ĐỐI KHÔNG sửa được điểm
        if (uri.startsWith("/grade/edit") || uri.startsWith("/grade/update") || uri.startsWith("/grade/delete")) {
            boolean isAccountantOnly = roles != null && roles.contains(RoleConstant.ACCOUNTANT) &&
                    !roles.contains(RoleConstant.ADMIN) &&
                    !roles.contains(RoleConstant.INSTRUCTOR) &&
                    !roles.contains(RoleConstant.TRAINING_MANAGER);

            if (isAccountantOnly) {
                denyAccess(req, res, "Kế toán không có quyền chỉnh sửa điểm số học viên.");
                return;
            }

            if (!PermissionConstant.hasPermission(roles, PermissionConstant.GRADE_EDIT)) {
                denyAccess(req, res, "Bạn không có quyền chỉnh sửa điểm số học viên.");
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (uri.startsWith("/grade/list") || uri.startsWith("/grade/view")) {
            if (!PermissionConstant.hasPermission(roles, PermissionConstant.GRADE_VIEW)) {
                denyAccess(req, res, "Bạn không có quyền xem thông tin điểm số.");
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 4. KIỂM QUYỀN HỌC PHÍ (Tuition)
        // Đặc tả: Kế toán sửa được học phí, Giảng viên TUYỆT ĐỐI KHÔNG sửa được học phí
        if (uri.startsWith("/tuition/edit") || uri.startsWith("/tuition/update") || uri.startsWith("/tuition/delete")) {
            boolean isInstructorOnly = roles != null && roles.contains(RoleConstant.INSTRUCTOR) &&
                    !roles.contains(RoleConstant.ADMIN) &&
                    !roles.contains(RoleConstant.ACCOUNTANT);

            if (isInstructorOnly) {
                denyAccess(req, res, "Giảng viên không có quyền chỉnh sửa thông tin học phí.");
                return;
            }

            if (!PermissionConstant.hasPermission(roles, PermissionConstant.TUITION_EDIT)) {
                denyAccess(req, res, "Bạn không có quyền chỉnh sửa thông tin học phí.");
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (uri.startsWith("/tuition/list") || uri.startsWith("/tuition/view")) {
            if (!PermissionConstant.hasPermission(roles, PermissionConstant.TUITION_VIEW)) {
                denyAccess(req, res, "Bạn không có quyền xem thông tin học phí.");
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 5. NGUYÊN TẮC MẶC ĐỊNH TỪ CHỐI (DEFAULT-DENY)
        if (!PermissionConstant.hasPermission(roles, PermissionConstant.PUBLIC_VIEW)) {
            denyAccess(req, res, "Bạn không có quyền truy cập chức năng này.");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse res, String message) throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN); // HTTP 403 Forbidden
        req.setAttribute("forbiddenMessage", message);
        req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, res);
    }

    @Override
    public void destroy() {
    }
}
