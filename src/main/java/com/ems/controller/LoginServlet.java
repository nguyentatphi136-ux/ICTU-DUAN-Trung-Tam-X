package com.ems.controller;

import com.ems.dao.UserDAO;
import com.ems.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Mục 11: Controller xử lý Đăng nhập
 * Xử lý luồng S1-01 & S1-02 (Tạo phiên làm việc)
 */
@WebServlet(urlPatterns = {"/login", "/auth/login"})
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Nếu đã có session hợp lệ -> chuyển tiếp vào dashboard
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard.jsp");
            return;
        }
        // Chưa có -> Mở giao diện login.jsp
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        User user = userDAO.authenticate(email, password);

        if (user != null) {
            // Đăng nhập thành công -> Tạo phiên mới và lưu user vào session (IDTTX-33)
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", user);
            session.setMaxInactiveInterval(8 * 60 * 60); // 8 giờ (IDTTX-36)

            // Điều hướng theo vai trò (Role-based redirect)
            String role = (user.getPrimaryRole() != null) ? user.getPrimaryRole().toLowerCase() : "student";
            resp.sendRedirect(req.getContextPath() + "/" + role + ".jsp");
        } else {
            // Thất bại -> Báo lỗi ra login.jsp
            req.setAttribute("errorMessage", "Email hoặc mật khẩu không chính xác.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
