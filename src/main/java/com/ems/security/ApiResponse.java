package com.ems.security;

import com.google.gson.Gson;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponse {
    private static final Gson GSON = new Gson();

    private ApiResponse() {}

    public static void success(HttpServletResponse response, String code, String message, Object data)
            throws IOException {
        success(response, HttpServletResponse.SC_OK, code, message, data);
    }

    public static void success(HttpServletResponse response, int status, String code, String message, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(new Success(true, code, message, data)));
    }

    public static void created(HttpServletResponse response, String code, String message, Object data)
            throws IOException {
        success(response, HttpServletResponse.SC_CREATED, code, message, data);
    }

    public static void error(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        error(response, status, code, message, "Quay lại", "/index.html");
    }

    public static void error(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            String actionLabel,
            String actionHref
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", code);
        errorBody.put("message", message);
        String label = (actionLabel != null && !actionLabel.isBlank()) ? actionLabel : "Quay lại";
        String href = (actionHref != null && !actionHref.isBlank()) ? actionHref : "/index.html";
        errorBody.put("action", Map.of("label", label, "href", href));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("error", errorBody);
        response.getWriter().write(GSON.toJson(payload));
    }

    private record Success(boolean success, String code, String message, Object data) {}
}