package com.ems.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Servlet cung cấp tài liệu Swagger OpenAPI 3.0 & Swagger UI
 * URL: /api/swagger.json, /api/docs, /api/docs/*
 */
@WebServlet(name = "SwaggerApiServlet", urlPatterns = {"/api/swagger.json", "/api/docs", "/api/docs/*"})
public class SwaggerApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri.endsWith("/swagger.json")) {
            response.setContentType("application/json;charset=UTF-8");
            try (InputStream is = getServletContext().getResourceAsStream("/api-docs/swagger.json")) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    response.getOutputStream().write(bytes);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"Swagger specification not found.\"}");
                }
            }
            return;
        }

        // Chuyển hướng tới trang Swagger UI
        String contextPath = request.getContextPath();
        response.sendRedirect(contextPath + "/api-docs/index.html");
    }
}
