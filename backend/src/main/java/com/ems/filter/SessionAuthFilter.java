package com.ems.filter;

import com.ems.config.SessionBlacklist;
import com.ems.controller.LoginApiServlet;
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

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Đường dẫn đã chuẩn hoá giống cách Tomcat chọn servlet. getRequestURI() còn giữ tham số ";..." nên
        // "/api/admin/users;.json" từng lọt qua kiểm tra đuôi .json mà vẫn tới AdminApiServlet.
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());

        // Backend chỉ cung cấp API. Giao diện là frontend React; "/" chỉ báo trạng thái (HealthApiServlet).
        if (!path.isEmpty() && !"/".equals(path) && !path.startsWith("/api/")) {
            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Backend chỉ cung cấp API tại /api. Giao diện chạy ở frontend.");
            return;
        }

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
            unauthorized(resp, "AUTH_SESSION_REVOKED");
            return;
        }

        // 4. Kiểm tra User trong Session (IDTTX-54): Hết hạn hoặc chưa đăng nhập
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            unauthorized(resp, "AUTH_UNAUTHORIZED");
            return;
        }

        // 5. Tự động gia hạn phiên khi còn hoạt động (IDTTX-36 - Sliding Window)
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

        // Đăng nhập bằng mật khẩu mặc định: chỉ được đổi mật khẩu hoặc đăng xuất.
        if (session.getAttribute(LoginApiServlet.MUST_CHANGE_PASSWORD) != null
                && !path.equals("/api/auth/change-password") && !path.equals("/api/auth/logout")) {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_PASSWORD_CHANGE_REQUIRED",
                    "Bạn cần đổi mật khẩu mặc định trước khi sử dụng hệ thống.", "Đổi mật khẩu", "/doi-mat-khau");
            return;
        }

        String requiredPermission = PermissionPolicy.requiredPermission(req.getMethod(), path);
        if (requiredPermission == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN",
                    "Chức năng này chưa được cấp quyền truy cập.", "Quay lại trang trước", "/index.html");
            return;
        }
        if (!"@authenticated".equals(requiredPermission)) {
            try {
                // "A|B": có một trong các quyền là đủ (ví dụ xem thùng rác: quyền module hoặc TRASH_PURGE).
                boolean allowed = false;
                for (String permission : requiredPermission.split("\\|")) {
                    if (permissionDAO.hasPermission(currentUser.getId(), permission)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
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
        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.isEmpty() || "/".equals(path)
                || "/api/auth/login".equals(path) || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path) || "/api/health".equals(path)
                || path.startsWith("/api/public/") || path.startsWith("/api/docs");
    }

    private static void unauthorized(HttpServletResponse resp, String code) throws IOException {
        ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, code,
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", "Đăng nhập lại", "/login.html");
    }

    @Override
    public void destroy() {
    }
}
