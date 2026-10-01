package com.ems.controller;

import com.ems.dao.PermissionDAO;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/me/menu")
public class MenuApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User user = (User) request.getSession(false).getAttribute("currentUser");
        try {
            ApiResponse.success(response, "MENU_LIST_SUCCESS", "Lấy danh sách menu thành công.",
                    permissionDAO.menuForUser(user.getId()));
        } catch (SQLException exception) {
            ApiResponse.error(response, 503, "SYSTEM_SERVICE_UNAVAILABLE",
                    "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.",
                    "Thử lại", "/index.html");
        }
    }
}