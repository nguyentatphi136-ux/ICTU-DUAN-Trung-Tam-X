package vn.edu.ictu.ems.controller;

import com.ems.dao.AdminDAO;
import com.ems.security.ApiResponse;
import com.ems.service.UserImportParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S2-01 (IDTTX-31): Nhập người dùng hàng loạt từ tệp Excel hoặc CSV.
 * - AC1: Tải tệp mẫu
 * - AC2: Xem trước, tách ba nhóm hợp lệ / trùng / lỗi theo từng dòng
 * - AC3: Nhập dòng hợp lệ, bỏ qua dòng trùng và dòng lỗi, có báo cáo tổng kết
 *
 * API cho giao diện React (quyền USER_CREATE, xem PermissionPolicy):
 * - GET  /api/admin/users/import/template
 * - POST /api/admin/users/import/preview   multipart "file" (.xlsx, .xls, .csv) hoặc JSON {rows}
 * - POST /api/admin/users/import           JSON {fileName, rows}; không gửi rows thì nhập các dòng vừa xem trước
 * Đường /admin/user-import giữ cho trang JSP cũ.
 */
@WebServlet({"/admin/user-import", "/admin/users/import", "/api/admin/users/import/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 15
)
public class UserImportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final String PENDING_ROWS = "PENDING_IMPORT_ROWS";
    private static final String PENDING_FILE = "PENDING_IMPORT_FILENAME";
    private final AdminDAO adminDAO = new AdminDAO();
    private final com.ems.dao.PermissionDAO permissionDAO = new com.ems.dao.PermissionDAO();

    /** Trang JSP cũ không đi qua kiểm tra quyền /api/ của SessionAuthFilter nên tự kiểm ở đây. */
    private boolean mayImport(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("currentUser") instanceof com.ems.model.User user)) return false;
        try {
            return permissionDAO.hasPermission(user.getId(), "USER_CREATE");
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean isApi(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isApi(request) && !mayImport(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String pathInfo = request.getPathInfo();
        if ("template".equalsIgnoreCase(request.getParameter("action"))
                || (pathInfo != null && pathInfo.contains("template"))) {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung_tms.csv\"");
            response.getWriter().write(UserImportParser.templateCsv());
            return;
        }
        if (isApi(request)) {
            ApiResponse.error(response, 404, "API_NOT_FOUND", "Không tìm thấy chức năng.");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        if (isApi(request)) {
            handleApi(request, response);
        } else if (mayImport(request)) {
            handleJspForm(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void handleApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        HttpSession session = request.getSession();
        try {
            if ("/preview".equals(path)) {
                String fileName;
                List<Map<String, Object>> rows;
                if (isMultipart(request)) {
                    Part part = request.getPart("file");
                    if (part == null || part.getSize() == 0) {
                        ApiResponse.error(response, 400, "IMPORT_FILE_REQUIRED", "Vui lòng chọn tệp .xlsx, .xls hoặc .csv.");
                        return;
                    }
                    fileName = submittedFileName(part);
                    try (InputStream in = part.getInputStream()) {
                        rows = UserImportParser.parse(fileName, in);
                    }
                } else {
                    JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                    fileName = body != null && body.has("fileName") ? body.get("fileName").getAsString() : "import_users.csv";
                    rows = rowsFromJson(body);
                }
                if (rows.isEmpty()) {
                    ApiResponse.error(response, 400, "IMPORT_EMPTY", "Tệp không có dòng dữ liệu nào.");
                    return;
                }
                Map<String, Object> preview = adminDAO.previewUsersBatch(rows);
                preview.put("fileName", fileName);
                session.setAttribute(PENDING_ROWS, rows);
                session.setAttribute(PENDING_FILE, fileName);
                ApiResponse.success(response, "IMPORT_PREVIEW_SUCCESS", "Xem trước thành công.", preview);
                return;
            }
            if (path.isEmpty() || "/".equals(path)) {
                JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
                List<Map<String, Object>> rows = rowsFromJson(body);
                String fileName = body != null && body.has("fileName") ? body.get("fileName").getAsString() : null;
                if (rows.isEmpty()) {
                    rows = pendingRows(session);
                    if (fileName == null) fileName = (String) session.getAttribute(PENDING_FILE);
                }
                if (rows.isEmpty()) {
                    ApiResponse.error(response, 400, "IMPORT_NOTHING_TO_IMPORT", "Chưa có dữ liệu xem trước. Vui lòng tải tệp lên lại.");
                    return;
                }
                long actorId = request.getAttribute("authenticatedUserId") instanceof Long id ? id : 0;
                Map<String, Object> summary = adminDAO.importUsersBatch(actorId, fileName, rows);
                session.removeAttribute(PENDING_ROWS);
                session.removeAttribute(PENDING_FILE);
                ApiResponse.success(response, "IMPORT_BATCH_SUCCESS", "Đã nhập danh sách người dùng.", summary);
                return;
            }
            ApiResponse.error(response, 404, "API_NOT_FOUND", "Không tìm thấy chức năng.");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(response, 400, "IMPORT_INVALID", e.getMessage());
        } catch (IllegalStateException e) {
            ApiResponse.error(response, 413, "IMPORT_FILE_TOO_LARGE", "Tệp vượt quá 10MB.");
        } catch (ServletException e) {
            ApiResponse.error(response, 400, "IMPORT_INVALID", "Không đọc được tệp tải lên.");
        } catch (SQLException e) {
            ApiResponse.error(response, 500, "DATABASE_ERROR", "Lỗi cơ sở dữ liệu khi xử lý tệp nhập.");
        }
    }

    private void handleJspForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String view = "/WEB-INF/views/admin/user-import.jsp";
        try {
            if ("confirmImport".equalsIgnoreCase(request.getParameter("action"))) {
                List<Map<String, Object>> rows = pendingRows(session);
                if (rows.isEmpty()) {
                    request.setAttribute("errorMessage", "Không tìm thấy dữ liệu xem trước cần nhập. Vui lòng tải lại tệp.");
                    request.getRequestDispatcher(view).forward(request, response);
                    return;
                }
                long actorId = 1;
                if (session.getAttribute("currentUser") instanceof com.ems.model.User u) actorId = u.getId();
                Map<String, Object> summary = adminDAO.importUsersBatch(actorId, (String) session.getAttribute(PENDING_FILE), rows);
                session.removeAttribute(PENDING_ROWS);
                session.removeAttribute(PENDING_FILE);
                request.setAttribute("summary", summary);
                request.setAttribute("message", "Đã hoàn thành đợt nhập người dùng. Xem kết quả báo cáo chi tiết bên dưới.");
                request.getRequestDispatcher(view).forward(request, response);
                return;
            }

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("errorMessage", "Vui lòng chọn tệp CSV hoặc Excel hợp lệ.");
                request.getRequestDispatcher(view).forward(request, response);
                return;
            }
            String fileName = submittedFileName(filePart);
            List<Map<String, Object>> rows;
            try (InputStream in = filePart.getInputStream()) {
                rows = UserImportParser.parse(fileName, in);
            }
            if (rows.isEmpty()) {
                request.setAttribute("errorMessage", "Tệp tải lên không có dữ liệu người dùng nào.");
                request.getRequestDispatcher(view).forward(request, response);
                return;
            }
            session.setAttribute(PENDING_ROWS, rows);
            session.setAttribute(PENDING_FILE, fileName);
            request.setAttribute("preview", adminDAO.previewUsersBatch(rows));
            request.setAttribute("uploadedFileName", fileName);
            request.getRequestDispatcher(view).forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("errorMessage", "Lỗi cơ sở dữ liệu khi xử lý: " + e.getMessage());
            request.getRequestDispatcher(view).forward(request, response);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher(view).forward(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> pendingRows(HttpSession session) {
        Object rows = session.getAttribute(PENDING_ROWS);
        return rows instanceof List<?> list ? (List<Map<String, Object>>) list : new ArrayList<>();
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String type = request.getContentType();
        return type != null && type.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/");
    }

    private static String submittedFileName(Part part) {
        String name = part.getSubmittedFileName();
        if (name == null || name.isBlank()) return "import_users.csv";
        return name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
    }

    private static List<Map<String, Object>> rowsFromJson(JsonObject body) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (body == null || !body.has("rows") || !body.get("rows").isJsonArray()) return list;
        body.getAsJsonArray("rows").forEach(el -> {
            if (el.isJsonObject()) {
                Map<String, Object> row = new LinkedHashMap<>();
                el.getAsJsonObject().entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive()) row.put(entry.getKey(), entry.getValue().getAsString());
                });
                list.add(row);
            }
        });
        return list;
    }
}
