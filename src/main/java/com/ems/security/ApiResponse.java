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

    public static void success(HttpServletResponse response, int status, String code, String message, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(new Success(true, code, message, data)));
    }

    public static void error(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            String actionLabel,
            String actionHref
    ) throws IOException {
        errorWithDetails(response, status, code, message, null, actionLabel, actionHref);
    }

    public static void errorWithDetails(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            Object details,
            String actionLabel,
            String actionHref
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> errorMap = new java.util.LinkedHashMap<>();
        errorMap.put("code", code);
        errorMap.put("message", message);
        if (details != null) {
            errorMap.put("details", details);
        }
        if (actionLabel != null && actionHref != null) {
            errorMap.put("action", Map.of("label", actionLabel, "href", actionHref));
        }
        response.getWriter().write(GSON.toJson(Map.of(
                "success", false,
                "error", errorMap
        )));
    }

    private record Success(boolean success, String code, String message, Object data) {}
}