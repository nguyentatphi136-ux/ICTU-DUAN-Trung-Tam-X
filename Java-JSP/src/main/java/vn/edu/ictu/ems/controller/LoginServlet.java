package vn.edu.ictu.ems.controller;

import vn.edu.ictu.ems.constant.RoleConstant;
import vn.edu.ictu.ems.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private static final Map<String, User> demoUsers = new HashMap<>();

    static {
        demoUsers.put("admin@example.com", new User(1, "admin@example.com", "Quản trị viên", "active", Collections.singletonList(RoleConstant.ADMIN)));
        demoUsers.put("instructor@example.com", new User(2, "instructor@example.com", "Giảng viên Nguyễn Văn A", "active", Collections.singletonList(RoleConstant.INSTRUCTOR)));
        demoUsers.put("student@example.com", new User(3, "student@example.com", "Học viên Trần Thị B", "active", Collections.singletonList(RoleConstant.STUDENT)));
        demoUsers.put("accountant@example.com", new User(4, "accountant@example.com", "Kế toán Lê Thị C", "active", Collections.singletonList(RoleConstant.ACCOUNTANT)));
        demoUsers.put("training@example.com", new User(5, "training@example.com", "Quản lý đào tạo Hoàng D", "active", Collections.singletonList(RoleConstant.TRAINING_MANAGER)));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (uri.endsWith("/logout")) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        User user = demoUsers.get(email != null ? email.trim().toLowerCase() : "");
        if (user != null && password != null && !password.isEmpty()) {
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", user);

            // Chuyển hướng theo vai trò chính
            if (user.hasRole(RoleConstant.INSTRUCTOR)) {
                resp.sendRedirect(req.getContextPath() + "/grade/list");
            } else if (user.hasRole(RoleConstant.ACCOUNTANT)) {
                resp.sendRedirect(req.getContextPath() + "/tuition/list");
            } else if (user.hasRole(RoleConstant.ADMIN)) {
                resp.sendRedirect(req.getContextPath() + "/grade/list");
            } else {
                resp.sendRedirect(req.getContextPath() + "/grade/list");
            }
            return;
        }

        req.setAttribute("errorMessage", "Email hoặc mật khẩu không chính xác!");
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }
}
