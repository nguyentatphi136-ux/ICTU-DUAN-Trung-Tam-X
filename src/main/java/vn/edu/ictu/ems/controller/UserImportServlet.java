package vn.edu.ictu.ems.controller;

import com.ems.dao.AdminDAO;
import com.ems.security.ApiResponse;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý Chức năng S2-01 (IDTTX-31):
 * Nhập danh sách người dùng hàng loạt từ tệp Excel (Bulk User Import from Excel).
 * - AC1: Tải được tệp mẫu chuẩn
 * - AC2: Xem trước và báo lỗi theo từng dòng trước khi nhập
 * - AC3: Dòng lỗi bị bỏ qua, dòng hợp lệ vẫn được nhập, có báo cáo tổng kết
 */
@WebServlet({"/admin/user-import", "/admin/users/import"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 15
)
public class UserImportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String pathInfo = request.getPathInfo();

        // 1. Tải tệp mẫu (S2-01 AC1)
        if ("template".equalsIgnoreCase(action) || (pathInfo != null && pathInfo.contains("template"))) {
            downloadTemplate(response);
            return;
        }

        // 2. Hiển thị trang JSP Nhập người dùng từ Excel
        request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String contentType = request.getContentType();
        String action = request.getParameter("action");
        HttpSession session = request.getSession();

        // Hỗ trợ REST API JSON từ Frontend fetch
        if (contentType != null && contentType.contains("application/json")) {
            handleJsonApi(request, response);
            return;
        }

        // Hỗ trợ Form Web JSP (Multipart Form / Confirm Import)
        try {
            if ("confirmImport".equalsIgnoreCase(action)) {
                // AC3: Tiến hành nhập các dòng hợp lệ, bỏ qua các dòng lỗi
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> evaluatedRows = (List<Map<String, Object>>) session.getAttribute("PENDING_IMPORT_ROWS");
                String fileName = request.getParameter("fileName");
                if (fileName == null || fileName.isBlank()) {
                    fileName = (String) session.getAttribute("PENDING_IMPORT_FILENAME");
                }
                if (fileName == null || fileName.isBlank()) {
                    fileName = "import_users.xlsx";
                }

                if (evaluatedRows == null || evaluatedRows.isEmpty()) {
                    request.setAttribute("errorMessage", "Không tìm thấy dữ liệu xem trước cần nhập. Vui lòng tải lại tệp.");
                    request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
                    return;
                }

                long actorId = 1; // Default Admin
                Object currentObj = session.getAttribute("currentUser");
                if (currentObj instanceof com.ems.model.User u) {
                    actorId = u.getId();
                }

                Map<String, Object> summary = adminDAO.importUsersBatch(actorId, fileName, evaluatedRows);
                session.removeAttribute("PENDING_IMPORT_ROWS");
                session.removeAttribute("PENDING_IMPORT_FILENAME");

                request.setAttribute("summary", summary);
                request.setAttribute("message", "Đã hoàn thành đợt nhập người dùng. Xem kết quả báo cáo chi tiết bên dưới.");
                request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
                return;
            }

            // AC2: Xem trước và báo lỗi theo từng dòng từ file tải lên
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("errorMessage", "Vui lòng chọn tệp CSV hoặc Excel hợp lệ.");
                request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
                return;
            }

            String fileName = getSubmittedFileName(filePart);
            List<Map<String, Object>> parsedRows = parseCsvStream(filePart);

            if (parsedRows.isEmpty()) {
                request.setAttribute("errorMessage", "Tệp tải lên không có dữ liệu người dùng nào.");
                request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
                return;
            }

            Map<String, Object> preview = adminDAO.previewUsersBatch(parsedRows);
            session.setAttribute("PENDING_IMPORT_ROWS", parsedRows);
            session.setAttribute("PENDING_IMPORT_FILENAME", fileName);

            request.setAttribute("preview", preview);
            request.setAttribute("uploadedFileName", fileName);
            request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);

        } catch (SQLException e) {
            request.setAttribute("errorMessage", "Lỗi cơ sở dữ liệu khi xử lý: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Đã xảy ra lỗi: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/admin/user-import.jsp").forward(request, response);
        }
    }

    /**
     * Tải tệp mẫu Excel/CSV chuẩn UTF-8 có BOM (AC1)
     */
    private void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung_tms.csv\"");
        String csv = "\uFEFFHọ và tên,Email,Số điện thoại,Vai trò,Ngày sinh,Giới tính,Địa chỉ\r\n"
                + "Nguyễn Văn An,an.nguyen@tms.vn,0912345678,student,2003-05-15,Nam,Thái Nguyên\r\n"
                + "Trần Thị Bình,binh.tran@tms.vn,0987654321,student,2002-11-20,Nữ,Hà Nội\r\n"
                + "Lê Hoàng Cường,cuong.le@tms.vn,0903112233,instructor,1990-08-10,Nam,Đà Nẵng\r\n"
                + "Phạm Thu Dung,dung.pt@tms.vn,0356789123,ta,1998-04-25,Nữ,Bắc Ninh\r\n";
        response.getWriter().write(csv);
    }

    /**
     * Đọc tệp CSV và bóc tách các dòng thành danh sách Map
     */
    private List<Map<String, Object>> parseCsvStream(Part filePart) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(filePart.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return list;

            String[] headers = splitCsvLine(headerLine);
            int nameCol = -1, emailCol = -1, phoneCol = -1, roleCol = -1, dobCol = -1, genderCol = -1, addrCol = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].toLowerCase().replaceAll("[\\s_]", "");
                if (h.contains("họ") || h.contains("name") || h.contains("tên")) nameCol = i;
                else if (h.contains("email") || h.contains("mail")) emailCol = i;
                else if (h.contains("thoại") || h.contains("phone") || h.contains("sđt") || h.contains("sdt")) phoneCol = i;
                else if (h.contains("trò") || h.contains("role") || h.contains("vaitro")) roleCol = i;
                else if (h.contains("sinh") || h.contains("dob") || h.contains("birth")) dobCol = i;
                else if (h.contains("tính") || h.contains("gender")) genderCol = i;
                else if (h.contains("chỉ") || h.contains("address") || h.contains("diachi")) addrCol = i;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsvLine(line);
                if (cols.length == 0) continue;

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("fullName", nameCol >= 0 && nameCol < cols.length ? cols[nameCol] : (cols.length > 0 ? cols[0] : ""));
                map.put("email", emailCol >= 0 && emailCol < cols.length ? cols[emailCol] : (cols.length > 1 ? cols[1] : ""));
                map.put("phone", phoneCol >= 0 && phoneCol < cols.length ? cols[phoneCol] : (cols.length > 2 ? cols[2] : ""));
                map.put("role", roleCol >= 0 && roleCol < cols.length ? cols[roleCol] : (cols.length > 3 ? cols[3] : "student"));
                map.put("dateOfBirth", dobCol >= 0 && dobCol < cols.length ? cols[dobCol] : (cols.length > 4 ? cols[4] : ""));
                map.put("gender", genderCol >= 0 && genderCol < cols.length ? cols[genderCol] : (cols.length > 5 ? cols[5] : ""));
                map.put("address", addrCol >= 0 && addrCol < cols.length ? cols[addrCol] : (cols.length > 6 ? cols[6] : ""));
                list.add(map);
            }
        }
        return list;
    }

    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        char delimiter = line.contains(";") && !line.contains(",") ? ';' : ',';

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private String getSubmittedFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1);
            }
        }
        return "import_users.xlsx";
    }

    private void handleJsonApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonObject body = GSON.fromJson(request.getReader(), JsonObject.class);
            String path = request.getPathInfo() == null ? "" : request.getPathInfo();
            if ("/preview".equals(path) || (body != null && body.has("previewOnly"))) {
                List<Map<String, Object>> rows = parseRowsFromJson(body);
                Map<String, Object> preview = adminDAO.previewUsersBatch(rows);
                ApiResponse.success(response, "IMPORT_PREVIEW_SUCCESS", "Xem trước thành công.", preview);
                return;
            }

            long actorId = 1;
            String fileName = body != null && body.has("fileName") ? body.get("fileName").getAsString() : "import_users.xlsx";
            List<Map<String, Object>> rows = parseRowsFromJson(body);
            Map<String, Object> summary = adminDAO.importUsersBatch(actorId, fileName, rows);
            ApiResponse.success(response, "IMPORT_BATCH_SUCCESS", "Nhập danh sách thành công.", summary);
        } catch (SQLException e) {
            ApiResponse.error(response, 500, "DATABASE_ERROR", e.getMessage(), "Thử lại", "/admin/user-import");
        }
    }

    private List<Map<String, Object>> parseRowsFromJson(JsonObject body) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (body == null || !body.has("rows") || !body.get("rows").isJsonArray()) return list;
        body.getAsJsonArray("rows").forEach(el -> {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                Map<String, Object> m = new LinkedHashMap<>();
                obj.entrySet().forEach(entry -> {
                    if (!entry.getValue().isJsonNull()) {
                        m.put(entry.getKey(), entry.getValue().getAsString());
                    }
                });
                list.add(m);
            }
        });
        return list;
    }
}
