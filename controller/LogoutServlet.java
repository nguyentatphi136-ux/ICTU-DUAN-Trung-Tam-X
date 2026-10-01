package com.ems.controller;

import com.ems.config.SessionBlacklist;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Servlet xử lý đăng xuất hệ thống
 * Phục vụ các Task:
 * - IDTTX-34: Thiết kế API và xử lý logic đăng xuất
 * - IDTTX-35: API POST /auth/logout — thu hồi session/token ngay phía server
 * - IDTTX-55: Lưu trữ session vào blacklist để đảm bảo logout có hiệu lực tức thì
 */
@WebServlet(urlPatterns = {"/auth/logout", "/api/auth/logout"})
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * API POST /auth/logout (IDTTX-35)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processLogout(req, resp);
    }

    /**
     * Hỗ trợ GET cho trường hợp người dùng bấm đường dẫn đăng xuất trực tiếp trên thẻ <a>
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processLogout(req, resp);
    }

    private void processLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Thiết lập mã hóa UTF-8
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // 1. Lấy session hiện tại (nếu có)
        HttpSession session = req.getSession(false);

        if (session != null) {
            String sessionId = session.getId();

            // 2. Thu hồi session phía server: Đưa vào Blacklist ngay lập tức (IDTTX-35, IDTTX-55)
            SessionBlacklist.add(sessionId);

            // 3. Xóa dữ liệu người dùng khỏi session và hủy bỏ (invalidate)
            session.removeAttribute("currentUser");
            session.invalidate();
        }

        // 4. Xóa Cookie JSESSIONID trên trình duyệt của client
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0); // Thời gian sống = 0 để trình duyệt xóa ngay
        cookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        resp.addCookie(cookie);

        // 5. Kiểm tra loại request: AJAX/Fetch (API) hay Trình duyệt thông thường (IDTTX-34)
        String acceptHeader = req.getHeader("Accept");
        String xRequestedWith = req.getHeader("X-Requested-With");
        String uri = req.getRequestURI();

        boolean isApi = "XMLHttpRequest".equalsIgnoreCase(xRequestedWith)
                || (acceptHeader != null && acceptHeader.contains("application/json"))
                || uri.contains("/api/");

        if (isApi) {
            // Trả về JSON 200 OK thành công cho Frontend xử lý điều hướng
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"status\": 200, \"message\": \"Đăng xuất thành công. Phiên đã bị thu hồi ngay lập tức.\", \"redirectUrl\": \"/login.html?message=logged_out\"}");
        } else {
            // Nếu gửi Form hoặc bấm link trực tiếp -> Redirect về trang login
            resp.sendRedirect(req.getContextPath() + "/login.html?message=logged_out");
        }
    }
}