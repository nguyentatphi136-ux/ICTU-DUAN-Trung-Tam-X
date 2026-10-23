package com.ems.controller;

import com.ems.exception.ApiException;
import com.ems.exception.ValidationException;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Khung chung cho các API JSON của S2 (môn học, buổi học, lead, đăng ký tư vấn).
 * Mỗi servlet chỉ viết phần định tuyến trong {@link #handle}; lỗi nghiệp vụ, lỗi JSON và lỗi CSDL
 * được đổi thành phản hồi { success: false, error } giống các API cũ.
 */
public abstract class JsonApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected static final Gson GSON = new Gson();

    /** path là phần sau đường dẫn servlet, không có dấu / ở cuối, ví dụ "" hoặc "/WEB-01/lessons". */
    protected abstract void handle(String method, String path, HttpServletRequest request, HttpServletResponse response)
            throws Exception;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo().trim();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        try {
            handle(request.getMethod().toUpperCase(java.util.Locale.ROOT), path, request, response);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (IllegalStateException e) {
            // Các DAO bọc SQLException trong IllegalStateException: đó là lỗi CSDL, không phải JSON sai.
            if (e.getCause() instanceof SQLException) {
                log("Lỗi CSDL ở " + request.getMethod() + " " + request.getRequestURI(), e);
                ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                        "Lỗi cơ sở dữ liệu. Vui lòng thử lại sau.");
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Dữ liệu gửi lên không đúng định dạng JSON hoặc sai kiểu.");
            }
        } catch (JsonParseException | ClassCastException | NumberFormatException
                 | ArithmeticException | UnsupportedOperationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    "Dữ liệu gửi lên không đúng định dạng JSON hoặc sai kiểu.");
        } catch (SQLIntegrityConstraintViolationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_CONFLICT, "DATA_CONFLICT",
                    "Dữ liệu bị trùng hoặc đang được dùng ở nơi khác. Hãy tải lại trang rồi thử lại.");
        } catch (SQLException e) {
            log("Lỗi CSDL ở " + request.getMethod() + " " + request.getRequestURI(), e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu. Vui lòng thử lại sau.");
        } catch (Exception e) {
            log("Lỗi máy chủ ở " + request.getMethod() + " " + request.getRequestURI(), e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                    "Lỗi máy chủ ngoài dự kiến.");
        }
    }

    protected static void notFound(HttpServletResponse response) throws IOException {
        ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND", "Đường dẫn API không tồn tại.");
    }

    /** Thân yêu cầu JSON; rỗng thì trả về object rỗng để phần kiểm tra báo thiếu trường. */
    protected static JsonObject body(HttpServletRequest request) throws IOException {
        JsonElement el = GSON.fromJson(request.getReader(), JsonElement.class);
        if (el == null || el.isJsonNull()) return new JsonObject();
        if (!el.isJsonObject()) throw new ValidationException("Nội dung yêu cầu phải là một đối tượng JSON.");
        return el.getAsJsonObject();
    }

    protected static String str(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el == null || el.isJsonNull() ? null : el.getAsString().trim();
    }

    /** Mảng chuỗi; trường không có thì trả null để phân biệt với mảng rỗng. */
    protected static List<String> strList(JsonObject o, String key) {
        JsonElement el = o.get(key);
        if (el == null || el.isJsonNull()) return null;
        if (!el.isJsonArray()) throw new ValidationException("Trường " + key + " phải là một mảng.");
        List<String> out = new ArrayList<>();
        for (JsonElement x : (JsonArray) el) out.add(x.getAsString().trim());
        return out;
    }

    protected static int intParam(HttpServletRequest request, String name, int fallback) {
        String v = request.getParameter(name);
        if (v == null || v.isBlank()) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Tham số " + name + " phải là số nguyên.");
        }
    }

    /** Id người đang đăng nhập, SessionAuthFilter gắn vào request sau khi kiểm quyền. */
    protected static long actorId(HttpServletRequest request) {
        Object id = request.getAttribute("authenticatedUserId");
        if (id instanceof Long l) return l;
        throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.");
    }

    protected static boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("currentUser") instanceof User user
                && user.getRoles() != null && user.getRoles().contains("ADMIN");
    }
}
