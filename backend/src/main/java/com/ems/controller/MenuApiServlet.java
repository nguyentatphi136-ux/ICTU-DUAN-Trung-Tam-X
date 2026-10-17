package com.ems.controller;

import com.ems.dao.PermissionDAO;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * GET /api/me/menu         menu cũ theo bảng menus (MENU_VIEW).
 * GET /api/me/permissions  quyền theo từng vai trò của người đang đăng nhập; frontend dựng menu bên trái từ đây,
 *                          nên đổi quyền ở S1-05 thì menu đổi theo ở lần mở trang kế tiếp.
 */
@WebServlet({"/api/me/menu", "/api/me/permissions"})
public class MenuApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        User user = (User) request.getSession(false).getAttribute("currentUser");
        try {
            if ("/api/me/permissions".equals(request.getServletPath())) {
                ApiResponse.success(response, "PERMISSION_LIST_SUCCESS", "Quyền của bạn theo từng vai trò.",
                        permissionDAO.permissionsByRoleForUser(user.getId()));
                return;
            }
            ApiResponse.success(response, "MENU_LIST_SUCCESS", "Lấy danh sách menu thành công.",
                    permissionDAO.menuForUser(user.getId()));
        } catch (SQLException exception) {
            ApiResponse.error(response, 503, "SYSTEM_SERVICE_UNAVAILABLE",
                    "Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.",
                    "Thử lại", "/index.html");
        }
    }
}
