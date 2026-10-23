package com.ems.controller;

import com.ems.security.ApiResponse;
import com.ems.service.ClassService;
import com.ems.service.ClassService.ClassInput;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S3-05 đến S3-08: lớp học, ghi danh, phân công. Quyền CLASS_MANAGE, riêng /mine chỉ cần đăng nhập (PermissionPolicy).
 *
 * <pre>
 * GET  /api/classes?q=&status=IN_PROGRESS&program=WEB-FS&instructorId=3&from=2026-10-01&to=2026-12-31&page=1&size=20
 * GET  /api/classes/staff                               giảng viên và trợ giảng để chọn
 * GET  /api/classes/mine                                lớp của giảng viên, trợ giảng đang đăng nhập
 * POST /api/classes                                     { code, name, programCode, startDate, maxCapacity, studyMode, roomId }
 * GET  /api/classes/{code}                              chi tiết kèm môn và người phụ trách
 * PUT  /api/classes/{code}/status                       { status }
 * PUT  /api/classes/{code}/room                         { roomId }  (S3-09)
 * GET  /api/classes/{code}/enrollments
 * POST /api/classes/{code}/enrollments                  { studentCodes: ["HV260001"] }
 * PUT  /api/classes/{code}/subjects/{subjectCode}       { instructorId, assistantIds: [7, 8] }
 * </pre>
 */
@WebServlet(name = "ClassApiServlet", urlPatterns = {"/api/classes", "/api/classes/*"})
public class ClassApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final String CODE = "([A-Za-z0-9_-]+)";
    private static final Pattern ITEM = Pattern.compile("^/" + CODE + "$");
    private static final Pattern STATUS = Pattern.compile("^/" + CODE + "/status$");
    private static final Pattern ROOM = Pattern.compile("^/" + CODE + "/room$");
    private static final Pattern ENROLLMENTS = Pattern.compile("^/" + CODE + "/enrollments$");
    private static final Pattern SUBJECT = Pattern.compile("^/" + CODE + "/subjects/" + CODE + "$");

    private transient ClassService service = new ClassService();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long actor = actorId(req);
        Matcher m;
        if ("GET".equals(method)) {
            if (path.isEmpty()) {
                ApiResponse.success(resp, "CLASS_LIST_SUCCESS", "Danh sách lớp.", service.search(req.getParameter("q"), req.getParameter("status"),
                        req.getParameter("program"), req.getParameter("instructorId"), req.getParameter("from"), req.getParameter("to"),
                        intParam(req, "page", 1), intParam(req, "size", 20)));
            } else if ("/staff".equals(path)) {
                ApiResponse.success(resp, "CLASS_STAFF_SUCCESS", "Giảng viên và trợ giảng.", service.staff());
            } else if ("/mine".equals(path)) {
                ApiResponse.success(resp, "CLASS_MINE_SUCCESS", "Lớp của tôi.", service.mine(actor));
            } else if ((m = ENROLLMENTS.matcher(path)).matches()) {
                ApiResponse.success(resp, "ENROLLMENT_LIST_SUCCESS", "Danh sách học viên của lớp.", service.enrollments(m.group(1)));
            } else if ((m = ITEM.matcher(path)).matches()) {
                ApiResponse.success(resp, "CLASS_DETAIL_SUCCESS", "Chi tiết lớp.", service.get(m.group(1)));
            } else {
                notFound(resp);
            }
        } else if ("POST".equals(method)) {
            if (path.isEmpty()) {
                JsonObject b = body(req);
                ApiResponse.created(resp, "CLASS_OPENED", "Đã mở lớp.", service.open(new ClassInput(str(b, "code"), str(b, "name"),
                        str(b, "programCode"), str(b, "startDate"), integer(b, "maxCapacity"), str(b, "studyMode"), integer(b, "roomId"))));
            } else if ((m = ENROLLMENTS.matcher(path)).matches()) {
                java.util.List<String> codes = strList(body(req), "studentCodes");
                ApiResponse.created(resp, "ENROLLED", "Đã ghi danh.", service.enroll(m.group(1), codes, actor));
            } else {
                notFound(resp);
            }
        } else if ("PUT".equals(method)) {
            if ((m = STATUS.matcher(path)).matches()) {
                ApiResponse.success(resp, "CLASS_STATUS_CHANGED", "Đã đổi trạng thái lớp.", service.changeStatus(m.group(1), str(body(req), "status")));
            } else if ((m = ROOM.matcher(path)).matches()) {
                ApiResponse.success(resp, "CLASS_ROOM_SET", "Đã xếp phòng cho lớp.", service.setRoom(m.group(1), integer(body(req), "roomId")));
            } else if ((m = SUBJECT.matcher(path)).matches()) {
                JsonObject b = body(req);
                Integer instructor = integer(b, "instructorId");
                ApiResponse.success(resp, "CLASS_SUBJECT_ASSIGNED", "Đã phân công.", service.assign(m.group(1), m.group(2),
                        instructor == null ? null : instructor.longValue(), ClassService.ids(strList(b, "assistantIds"))));
            } else {
                notFound(resp);
            }
        } else {
            ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
        }
    }

    static Integer integer(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el == null || el.isJsonNull() || el.getAsString().isBlank() ? null : el.getAsInt();
    }
}
