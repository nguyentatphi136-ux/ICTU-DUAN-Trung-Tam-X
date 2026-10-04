package com.ems.filter;

import com.ems.config.SessionBlacklist;
import com.ems.dao.PermissionDAO;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.ems.security.PermissionPolicy;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Mục 9 & 11: Filter kiểm soát bảo mật và phiên người dùng (SessionAuthFilter)
 * Phục vụ: IDTTX-33, IDTTX-36, IDTTX-54, IDTTX-55
 */
@WebFilter("/*")
public class SessionAuthFilter implements Filter {

    private static final int SESSION_TIMEOUT_SECONDS = 8 * 60 * 60; // 8 giờ
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(contextPath.length());

        // 1. Cho phép truy cập tài nguyên công khai không cần đăng nhập
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Lấy session hiện tại
        HttpSession session = req.getSession(false);
        String sessionId = (session != null) ? session.getId() : null;

        // 3. Kiểm tra Session Blacklist (IDTTX-55): Đã đăng xuất thì chặn ngay
        if (sessionId != null && SessionBlacklist.isBlacklisted(sessionId)) {
            redirectToLogin(req, resp, "session_revoked");
            return;
        }

        // 4. Kiểm tra User trong Session (IDTTX-54): Hết hạn hoặc chưa đăng nhập
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            redirectToLogin(req, resp, "session_expired");
            return;
        }

        // 5. Tự động gia hạn phiên khi còn hoạt động (IDTTX-36 - Sliding Window)
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

        if (path.startsWith("/api/")) {
            String requiredPermission = PermissionPolicy.requiredPermission(req.getMethod(), path);
            if (requiredPermission == null) {
                ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN",
                        "Chức năng này chưa được cấp quyền truy cập.", "Quay lại trang trước", "/index.html");
                return;
            }
            if (!"@authenticated".equals(requiredPermission)) {
                try {
                    if (!permissionDAO.hasPermission(currentUser.getId(), requiredPermission)) {
                        ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN",
                                "Bạn không có quyền thực hiện chức năng này.", "Về trang làm việc", "/index.html");
                        return;
                    }
                } catch (SQLException exception) {
                    servletContext.log("Không thể xác minh quyền hiện tại.", exception);
                    ApiResponse.error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                            "SYSTEM_SERVICE_UNAVAILABLE", "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.",
                            "Thử lại", "/index.html");
                    return;
                }
            }
            req.setAttribute("authenticatedUserId", currentUser.getId());
        }

        // Cho phép đi tiếp vào trang nghiệp vụ
        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path) || "/index.jsp".equals(path)) {
            return true;
        }
        if (path.equals("/login") || path.equals("/login.jsp")
                || path.equals("/logout") || path.equals("/auth/logout")
                || path.equals("/forgot-password.jsp") || path.equals("/reset-password.jsp")) {
            return true;
        }
        if ("/api/auth/login".equals(path) || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path) || "/api/health".equals(path)
                || "/api/swagger.json".equals(path) || path.startsWith("/api/docs") || path.startsWith("/api-docs")) {
            return true;
        }
        // Tài nguyên tĩnh
        return path.startsWith("/assets/") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png")
                || path.endsWith(".jpg") || path.endsWith(".svg") || path.endsWith(".ico")
                || path.endsWith(".json");
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp, String errorReason) throws IOException {
        String xRequestedWith = req.getHeader("X-Requested-With");
        String accept = req.getHeader("Accept");

        if (req.getRequestURI().contains("/api/") || "XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
            || (accept != null && accept.contains("application/json"))) {
            String code = "session_revoked".equals(errorReason) ? "AUTH_SESSION_REVOKED" : "AUTH_UNAUTHORIZED";
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, code,
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", "Đăng nhập lại", "/login.html");
        } else {
            // Mục 10: Chuyển hướng sang trang login.jsp kèm query parameter để JSTL hiển thị thông báo
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=" + errorReason);
        }
    }

    @Override
    public void destroy() {
    }
}
