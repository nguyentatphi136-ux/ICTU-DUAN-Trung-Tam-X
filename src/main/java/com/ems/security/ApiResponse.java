package com.ems.security;

import com.google.gson.Gson;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public final class ApiResponse {
    private static final Gson GSON = new Gson();

    private ApiResponse() {}

    public static void success(HttpServletResponse response, String code, String message, Object data)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(new Success(true, code, message, data)));
    }

    public static void created(HttpServletResponse response, String code, String message, Object data)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(new Success(true, code, message, data)));
    }

    public static void error(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        error(response, status, code, message, null, null);
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
        java.util.Map<String, Object> errorBody = new java.util.LinkedHashMap<>();
        errorBody.put("code", code);
        errorBody.put("message", message);
        if (actionLabel != null && actionHref != null) {
            errorBody.put("action", Map.of("label", actionLabel, "href", actionHref));
        }
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("success", false);
        payload.put("error", errorBody);
        response.getWriter().write(GSON.toJson(payload));
    }

    private record Success(boolean success, String code, String message, Object data) {}
}