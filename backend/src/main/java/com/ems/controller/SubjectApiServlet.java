package com.ems.controller;

import com.ems.security.ApiResponse;
import com.ems.service.SubjectService;
import com.ems.service.SubjectService.SubjectInput;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S2-05 Môn học và S2-07 Buổi học của môn. Quyền PROGRAM_MANAGE (PermissionPolicy).
 *
 * <pre>
 * GET    /api/subjects?q=&program=WEB-FS&page=&size=   danh sách (không gửi size thì trả hết)
 * GET    /api/subjects/trash                           môn trong thùng rác
 * GET    /api/subjects/{code}                          chi tiết
 * POST   /api/subjects                                 { code, name, sessions, weight, outcome, programCodes }
 * PUT    /api/subjects/{code}                          như POST, đổi được mã
 * DELETE /api/subjects/{code}                          chuyển vào thùng rác (409 SUBJECT_HAS_CLASSES)
 * POST   /api/subjects/{id}/restore                    khôi phục
 * DELETE /api/subjects/{id}/purge                      xoá vĩnh viễn, chỉ ADMIN
 * GET    /api/subjects/{code}/lessons                  [{ order, topic, goal }]
 * POST   /api/subjects/{code}/lessons                  { topic, goal } thêm vào cuối (409 LESSON_LIMIT_REACHED)
 * PUT    /api/subjects/{code}/lessons/{order}          { topic, goal }
 * DELETE /api/subjects/{code}/lessons/{order}          xoá và dồn số thứ tự
 * POST   /api/subjects/{code}/lessons/clone            { fromSubjectCode, overwrite } (409 LESSONS_EXIST)
 * </pre>
 */
@WebServlet(name = "SubjectApiServlet", urlPatterns = {"/api/subjects", "/api/subjects/*"})
public class SubjectApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final String CODE = "([A-Za-z0-9._-]+)";
    private static final Pattern ITEM = Pattern.compile("^/" + CODE + "$");
    private static final Pattern RESTORE = Pattern.compile("^/(\\d+)/restore$");
    private static final Pattern PURGE = Pattern.compile("^/(\\d+)/purge$");
    private static final Pattern LESSONS = Pattern.compile("^/" + CODE + "/lessons$");
    private static final Pattern LESSON = Pattern.compile("^/" + CODE + "/lessons/(\\d+)$");
    private static final Pattern CLONE = Pattern.compile("^/" + CODE + "/lessons/clone$");

    private transient SubjectService service = new SubjectService();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Matcher m;
        switch (method) {
            case "GET" -> {
                if (path.isEmpty()) {
                    String q = req.getParameter("q") != null ? req.getParameter("q") : req.getParameter("keyword");
                    ApiResponse.success(resp, "SUBJECT_LIST_SUCCESS", "Danh sách môn học.", service.search(q,
                            req.getParameter("program"), intParam(req, "page", 1), intParam(req, "size", 0)));
                } else if ("/trash".equals(path)) {
                    ApiResponse.success(resp, "SUBJECT_TRASH_SUCCESS", "Môn học trong thùng rác.", service.listTrash());
                } else if ((m = LESSONS.matcher(path)).matches()) {
                    ApiResponse.success(resp, "LESSON_LIST_SUCCESS", "Buổi học của môn.", service.lessons(m.group(1)));
                } else if ((m = ITEM.matcher(path)).matches()) {
                    ApiResponse.success(resp, "SUBJECT_DETAIL_SUCCESS", "Chi tiết môn học.", service.get(m.group(1)));
                } else {
                    notFound(resp);
                }
            }
            case "POST" -> {
                if (path.isEmpty()) {
                    ApiResponse.created(resp, "SUBJECT_CREATED", "Đã thêm môn học.", service.create(input(body(req))));
                } else if ((m = RESTORE.matcher(path)).matches()) {
                    service.restore(Integer.parseInt(m.group(1)));
                    ApiResponse.success(resp, "SUBJECT_RESTORED", "Đã khôi phục môn học.", null);
                } else if ((m = CLONE.matcher(path)).matches()) {
                    JsonObject b = body(req);
                    boolean overwrite = b.has("overwrite") && b.get("overwrite").getAsBoolean();
                    ApiResponse.success(resp, "LESSONS_CLONED", "Đã nhân bản buổi học.",
                            service.cloneLessons(m.group(1), str(b, "fromSubjectCode"), overwrite));
                } else if ((m = LESSONS.matcher(path)).matches()) {
                    JsonObject b = body(req);
                    ApiResponse.created(resp, "LESSON_CREATED", "Đã thêm buổi học.",
                            service.addLesson(m.group(1), str(b, "topic"), str(b, "goal")));
                } else {
                    notFound(resp);
                }
            }
            case "PUT" -> {
                if ((m = LESSON.matcher(path)).matches()) {
                    JsonObject b = body(req);
                    ApiResponse.success(resp, "LESSON_UPDATED", "Đã lưu buổi học.",
                            service.updateLesson(m.group(1), Integer.parseInt(m.group(2)), str(b, "topic"), str(b, "goal")));
                } else if ((m = ITEM.matcher(path)).matches()) {
                    ApiResponse.success(resp, "SUBJECT_UPDATED", "Đã lưu môn học.", service.update(m.group(1), input(body(req))));
                } else {
                    notFound(resp);
                }
            }
            case "DELETE" -> {
                if ((m = PURGE.matcher(path)).matches()) {
                    if (!isAdmin(req)) {
                        ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Chỉ Quản trị hệ thống được xoá vĩnh viễn.");
                        return;
                    }
                    service.purge(Integer.parseInt(m.group(1)));
                    ApiResponse.success(resp, "SUBJECT_PURGED", "Đã xoá vĩnh viễn môn học.", null);
                } else if ((m = LESSON.matcher(path)).matches()) {
                    ApiResponse.success(resp, "LESSON_DELETED", "Đã xoá buổi học.",
                            service.deleteLesson(m.group(1), Integer.parseInt(m.group(2))));
                } else if ((m = ITEM.matcher(path)).matches()) {
                    service.trash(m.group(1), actorId(req));
                    ApiResponse.success(resp, "SUBJECT_TRASHED", "Đã chuyển môn học vào thùng rác.", null);
                } else {
                    notFound(resp);
                }
            }
            default -> ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
        }
    }

    private static SubjectInput input(JsonObject b) {
        JsonElement sessions = b.get("sessions");
        JsonElement weight = b.get("weight");
        return new SubjectInput(str(b, "code"), str(b, "name"),
                sessions == null || sessions.isJsonNull() ? null : sessions.getAsBigDecimal().intValueExact(),
                weight == null || weight.isJsonNull() ? null : weight.getAsBigDecimal(),
                str(b, "outcome"), strList(b, "programCodes"));
    }
}
