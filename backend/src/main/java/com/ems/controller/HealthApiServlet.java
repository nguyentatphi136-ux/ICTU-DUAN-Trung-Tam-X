package com.ems.controller;

import com.ems.config.DBConnection;
import com.ems.security.ApiResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/** Trạng thái backend và kết nối CSDL. "" là gốc http://localhost:PORT/, backend không còn giao diện JSP. */
@WebServlet({"", "/api/health"})
public class HealthApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean database;
        try (Connection conn = DBConnection.getConnection()) {
            database = conn.isValid(2);
        } catch (SQLException e) {
            database = false;
        }
        Map<String, String> data = Map.of("backend", "Đang hoạt động",
                "database", database ? "Đã kết nối" : "Không kết nối được");
        if (database) {
            ApiResponse.success(response, "SYSTEM_OK", "Dịch vụ đang hoạt động.", data);
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new com.google.gson.Gson().toJson(Map.of("success", false,
                    "code", "DATABASE_UNAVAILABLE", "message", "Backend đang chạy nhưng không kết nối được CSDL.", "data", data)));
        }
    }
}
