package com.ems.filter;

import com.ems.config.SessionBlacklist;
import com.ems.model.User;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Bộ lọc xác thực phiên đăng nhập (Session Authentication Filter)
 * Đáp ứng các Task:
 * - IDTTX-33: Cơ chế xác thực qua Session / Token
 * - IDTTX-36: Gia hạn phiên tự động (Sliding Window) khi người dùng có thao tác gửi request
 * - IDTTX-54: Xử lý phiên hết hạn hoặc không hợp lệ -> Trả về HTTP 401 Unauthorized kèm thông báo rõ ràng
 * - IDTTX-55: Chặn đứng các session nằm trong Blacklist (đã logout)
 */
@WebFilter("/*")
public class SessionAuthFilter implements Filter {

    // Thời gian timeout của phiên: 8 giờ = 28800 giây (như công bố trên giao diện hệ thống)
    private static final int SESSION_TIMEOUT_SECONDS = 8 * 60 * 60;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Khởi tạo bộ lọc
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Bật UTF-8 cho toàn bộ request/response
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        String path = uri.substring(contextPath.length());

        // ---------------------------------------------------------------------
        // 1. Danh sách ngoại lệ công khai (Bypass Whitelist)
        // ---------------------------------------------------------------------
        if (isPublicResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // ---------------------------------------------------------------------
        // 2. Kiểm tra Session hiện tại (IDTTX-33)
        // ---------------------------------------------------------------------
        HttpSession session = req.getSession(false);
        String sessionId = (session != null) ? session.getId() : null;

        // Nếu request có gửi kèm JSESSIONID qua Cookie mà session server đã bị null
        if (sessionId == null) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("JSESSIONID".equalsIgnoreCase(c.getName())) {
                        sessionId = c.getValue();
                        break;
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // 3. Kiểm tra Session Blacklist (IDTTX-55)
        // Nếu phiên đã bị thu hồi do Đăng xuất -> Chặn ngay lập tức
        // ---------------------------------------------------------------------
        if (sessionId != null && SessionBlacklist.isBlacklisted(sessionId)) {
            sendUnauthorizedResponse(req, resp, "Phiên đăng nhập đã bị thu hồi do bạn đã đăng xuất. Vui lòng đăng nhập lại.");
            return;
        }

        // ---------------------------------------------------------------------
        // 4. Kiểm tra phiên hợp lệ và thông tin người dùng (IDTTX-54)
        // ---------------------------------------------------------------------
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
        }

        if (currentUser == null) {
            // Phiên hết hạn hoặc chưa đăng nhập -> Trả về mã lỗi 401
            sendUnauthorizedResponse(req, resp, "Phiên đăng nhập đã hết hạn hoặc không tồn tại. Vui lòng đăng nhập lại.");
            return;
        }

        // ---------------------------------------------------------------------
        // 5. Gia hạn phiên tự động khi có request hoạt động (IDTTX-36)
        // Servlet Container tự động cập nhật lastAccessedTime mỗi khi có request.
        // Thiết lập MaxInactiveInterval đảm bảo sliding window kéo dài thêm 8 giờ.
        // ---------------------------------------------------------------------
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);

        // Cho phép request tiếp tục đi tới Servlet hoặc Resource đích
        chain.doFilter(request, response);
    }

    /**
     * Kiểm tra tài nguyên tĩnh hoặc endpoint công khai
     */
    private boolean isPublicResource(String path) {
        if (path == null || path.isEmpty() || "/".equals(path) || "/index.html".equals(path)) {
            return true;
        }

        // Các trang & API xác thực không cần đăng nhập
        if (path.equals("/login") || path.equals("/login.html")
                || path.equals("/auth/login") || path.equals("/auth/logout")
                || path.equals("/ForgotPasswordForm.html") || path.equals("/ResetPasswordForm.html")
                || path.startsWith("/api/auth/google")
                || path.startsWith("/api/public/")) {
            return true;
        }

        // Tài nguyên tĩnh: CSS, JS, hình ảnh, fonts
        return path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".map")
                || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".svg") || path.endsWith(".ico") || path.endsWith(".webp")
                || path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf")
                || path.startsWith("/assets/") || path.startsWith("/src/");
    }

    /**
     * Trả về phản hồi 401 Unauthorized theo chuẩn Acceptance Criteria (IDTTX-54)
     * - Trả về JSON cho Client gọi API (Fetch / AJAX / Axios)
     * - Redirect kèm thông báo cho trình duyệt thông thường
     */
    private void sendUnauthorizedResponse(HttpServletRequest req, HttpServletResponse resp, String message) throws IOException {
        String xRequestedWith = req.getHeader("X-Requested-With");
        String accept = req.getHeader("Accept");
        String contentType = req.getContentType();

        boolean isApiRequest = "XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"))
                || req.getRequestURI().contains("/api/");

        if (isApiRequest) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 401
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(String.format(
                "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"%s\", \"redirectUrl\": \"%s/login.html?error=session_expired\"}",
                escapeJson(message),
                req.getContextPath()
            ));
        } else {
            // Trình duyệt thông thường -> Điều hướng về trang login kèm tham số thông báo
            resp.sendRedirect(req.getContextPath() + "/login.html?error=session_expired");
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public void destroy() {
        // Hủy filter khi container dừng
    }
}