package com.ems.controller;

import com.ems.config.SessionBlacklist;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Mục 11: Controller xử lý Đăng xuất
 * Đáp ứng IDTTX-34, IDTTX-35, IDTTX-55
 */
@WebServlet(urlPatterns = {"/logout", "/auth/logout"})
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processLogout(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processLogout(req, resp);
    }

    private void processLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            SessionBlacklist.add(sessionId); // Thu hồi ngay phía server
            session.removeAttribute("currentUser");
            session.invalidate();
        }

        // Xóa cookie JSESSIONID
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        resp.addCookie(cookie);

        // Chuyển hướng về login.jsp kèm thông báo
        resp.sendRedirect(req.getContextPath() + "/login.jsp?message=logged_out");
    }
}
