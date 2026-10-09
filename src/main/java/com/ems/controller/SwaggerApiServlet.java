package com.ems.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Servlet cung cấp tài liệu Swagger/OpenAPI JSON
 * Endpoint: /api/docs/swagger.json và chuyển hướng /api/docs sang Swagger UI
 */
@WebServlet(name = "SwaggerApiServlet", urlPatterns = {"/api/docs", "/api/docs/*"})
public class SwaggerApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo) || "".equals(pathInfo)) {
            response.sendRedirect(request.getContextPath() + "/api-docs/index.html");
            return;
        }

        if ("/swagger.json".equalsIgnoreCase(pathInfo)) {
            response.setContentType("application/json;charset=UTF-8");
            try (InputStream is = getServletContext().getResourceAsStream("/api-docs/swagger.json")) {
                if (is == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"error\": \"swagger.json not found\"}");
                    return;
                }
                OutputStream os = response.getOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return;
        }

        response.sendRedirect(request.getContextPath() + "/api-docs/index.html");
    }
}
