package com.ems.filter;

import com.ems.config.SessionBlacklist;
import com.ems.model.User;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Mục 9 & 11: Filter kiểm soát bảo mật và phiên người dùng (SessionAuthFilter)
 * Phục vụ: IDTTX-33, IDTTX-36, IDTTX-54, IDTTX-55
 */
@WebFilter("/*")
public class SessionAuthFilter implements Filter {

    private static final int SESSION_TIMEOUT_SECONDS = 8 * 60 * 60; // 8 giờ

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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
        // Tài nguyên tĩnh
        return path.startsWith("/assets/") || path.startsWith("/css/") || path.startsWith("/js/")
                || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png")
                || path.endsWith(".jpg") || path.endsWith(".svg") || path.endsWith(".ico");
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp, String errorReason) throws IOException {
        String xRequestedWith = req.getHeader("X-Requested-With");
        String accept = req.getHeader("Accept");

        if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith) || (accept != null && accept.contains("application/json"))) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 401 (IDTTX-54)
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(String.format("{\"status\": 401, \"error\": \"%s\", \"message\": \"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.\"}", errorReason));
        } else {
            // Mục 10: Chuyển hướng sang trang login.jsp kèm query parameter để JSTL hiển thị thông báo
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=" + errorReason);
        }
    }

    @Override
    public void destroy() {
    }
}
