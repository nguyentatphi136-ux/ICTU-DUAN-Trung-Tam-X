package com.ems.controller;

import com.ems.security.ApiResponse;
import com.ems.service.StudentService;
import com.ems.service.StudentService.StudentInput;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S3-04 Hồ sơ học viên, quyền CLASS_MANAGE. Học viên được tạo từ lead: POST /api/leads/{id}/convert (S3-03).
 *
 * <pre>
 * GET /api/students?q=&status=STUDYING&program=WEB-FS&page=1&size=20   q tìm theo mã, họ tên, số điện thoại
 * GET /api/students/{code}             hồ sơ, currentClasses, history
 * PUT /api/students/{code}             { name, phone, email, birth, address, level, notes }
 * </pre>
 */
@WebServlet(name = "StudentApiServlet", urlPatterns = {"/api/students", "/api/students/*"})
public class StudentApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern ITEM = Pattern.compile("^/([A-Za-z0-9_-]+)$");

    private transient StudentService service = new StudentService();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Matcher m = ITEM.matcher(path);
        if ("GET".equals(method) && path.isEmpty()) {
            ApiResponse.success(resp, "STUDENT_LIST_SUCCESS", "Danh sách học viên.", service.search(req.getParameter("q"),
                    req.getParameter("status"), req.getParameter("program"), intParam(req, "page", 1), intParam(req, "size", 20)));
        } else if ("GET".equals(method) && m.matches()) {
            ApiResponse.success(resp, "STUDENT_DETAIL_SUCCESS", "Hồ sơ học viên.", service.get(m.group(1)));
        } else if ("PUT".equals(method) && m.matches()) {
            JsonObject b = body(req);
            ApiResponse.success(resp, "STUDENT_UPDATED", "Đã lưu hồ sơ học viên.", service.update(m.group(1), new StudentInput(
                    str(b, "name"), str(b, "phone"), str(b, "email"), str(b, "birth"), str(b, "address"), str(b, "level"), str(b, "notes"))));
        } else {
            notFound(resp);
        }
    }
}
