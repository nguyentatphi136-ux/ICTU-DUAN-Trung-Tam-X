package com.ems.controller;

import com.ems.security.ApiResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/health")
public class HealthApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiResponse.success(response, "SYSTEM_OK", "Dịch vụ đang hoạt động.", null);
    }
}