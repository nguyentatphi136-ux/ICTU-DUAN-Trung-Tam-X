package com.ems.security;

import com.ems.config.GsonProvider;
import com.google.gson.Gson;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public final class ApiResponse {
    private static final Gson GSON = GsonProvider.getGson();

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

    public static void conflict(
            HttpServletResponse response,
            String code,
            String message,
            Object conflictData
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json;charset=UTF-8");
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        body.put("hasConflict", true);
        body.put("data", conflictData);
        body.put("error", java.util.Map.of(
                "code", code,
                "message", message,
                "conflictDetails", conflictData
        ));
        response.getWriter().write(GSON.toJson(body));
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
        response.getWriter().write(GSON.toJson(Map.of(
                "success", false,
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "action", Map.of("label", actionLabel != null ? actionLabel : "Quay lại",
                                         "href", actionHref != null ? actionHref : "/index.html")
                )
        )));
    }

    private record Success(boolean success, String code, String message, Object data) {}
}