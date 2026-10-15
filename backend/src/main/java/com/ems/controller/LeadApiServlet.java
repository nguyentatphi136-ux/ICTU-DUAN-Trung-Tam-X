package com.ems.controller;

import com.ems.dao.PermissionDAO;
import com.ems.security.ApiResponse;
import com.ems.service.LeadService;
import com.ems.service.LeadService.LeadInput;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S2-09 Lead, S2-10 Phân công lead, S2-11 Tìm kiếm và lọc lead.
 * Quyền LEAD_MANAGE; phân công, xoá và thùng rác cần thêm LEAD_ASSIGN (PermissionPolicy).
 *
 * <pre>
 * GET    /api/leads?q=&status=NEW&source=WEBSITE&owner=12|none&from=2026-09-01&to=2026-09-30&page=1&size=20
 * GET    /api/leads/{id}                        chi tiết kèm lịch sử phân công
 * GET    /api/leads/check-phone?phone=&excludeId=   lead khác cùng số điện thoại
 * POST   /api/leads                             { name, phone, email, source, programCode, status, note }
 * PUT    /api/leads/{id}                        như POST
 * GET    /api/leads/counselors                  tư vấn viên và số lead đang giữ
 * POST   /api/leads/assign                      { leadIds: [1, 2], counselorId, note }
 * DELETE /api/leads/{id}                        chuyển vào thùng rác
 * GET    /api/leads/trash, POST /api/leads/{id}/restore, DELETE /api/leads/{id}/purge (chỉ ADMIN)
 * </pre>
 */
@WebServlet(name = "LeadApiServlet", urlPatterns = {"/api/leads", "/api/leads/*"})
public class LeadApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern ITEM = Pattern.compile("^/(\\d+)$");
    private static final Pattern RESTORE = Pattern.compile("^/(\\d+)/restore$");
    private static final Pattern PURGE = Pattern.compile("^/(\\d+)/purge$");

    private transient LeadService service = new LeadService();
    private transient PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long actor = actorId(req);
        Matcher m;
        switch (method) {
            case "GET" -> {
                if (path.isEmpty()) {
                    ApiResponse.success(resp, "LEAD_LIST_SUCCESS", "Danh sách lead.", service.search(req.getParameter("q"),
                            req.getParameter("status"), req.getParameter("source"), req.getParameter("owner"),
                            req.getParameter("from"), req.getParameter("to"), intParam(req, "page", 1), intParam(req, "size", 20),
                            actor, seeAll(actor)));
                } else if ("/check-phone".equals(path)) {
                    String exclude = req.getParameter("excludeId");
                    ApiResponse.success(resp, "LEAD_PHONE_CHECKED", "Lead cùng số điện thoại.", service.samePhone(
                            req.getParameter("phone"), exclude == null || exclude.isBlank() ? null : Long.parseLong(exclude.trim())));
                } else if ("/counselors".equals(path)) {
                    ApiResponse.success(resp, "COUNSELOR_LIST_SUCCESS", "Tư vấn viên.", service.counselors());
                } else if ("/trash".equals(path)) {
                    ApiResponse.success(resp, "LEAD_TRASH_SUCCESS", "Lead trong thùng rác.", service.listTrash());
                } else if ((m = ITEM.matcher(path)).matches()) {
                    ApiResponse.success(resp, "LEAD_DETAIL_SUCCESS", "Chi tiết lead.",
                            service.get(Long.parseLong(m.group(1)), actor, seeAll(actor)));
                } else {
                    notFound(resp);
                }
            }
            case "POST" -> {
                if (path.isEmpty()) {
                    ApiResponse.created(resp, "LEAD_CREATED", "Đã thêm lead.", service.create(input(body(req)), actor, seeAll(actor)));
                } else if ("/assign".equals(path)) {
                    JsonObject b = body(req);
                    List<Long> ids = new ArrayList<>();
                    if (b.has("leadIds") && b.get("leadIds").isJsonArray()) {
                        for (JsonElement el : b.getAsJsonArray("leadIds")) ids.add(el.getAsLong());
                    }
                    Long counselor = b.has("counselorId") && !b.get("counselorId").isJsonNull() ? b.get("counselorId").getAsLong() : null;
                    ApiResponse.success(resp, "LEAD_ASSIGNED", "Đã phân công lead.", service.assign(ids, counselor, str(b, "note"), actor));
                } else if ((m = RESTORE.matcher(path)).matches()) {
                    service.restore(Long.parseLong(m.group(1)));
                    ApiResponse.success(resp, "LEAD_RESTORED", "Đã khôi phục lead.", null);
                } else {
                    notFound(resp);
                }
            }
            case "PUT" -> {
                if ((m = ITEM.matcher(path)).matches()) {
                    ApiResponse.success(resp, "LEAD_UPDATED", "Đã lưu lead.",
                            service.update(Long.parseLong(m.group(1)), input(body(req)), actor, seeAll(actor)));
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
                    service.purge(Long.parseLong(m.group(1)));
                    ApiResponse.success(resp, "LEAD_PURGED", "Đã xoá vĩnh viễn lead.", null);
                } else if ((m = ITEM.matcher(path)).matches()) {
                    service.trash(Long.parseLong(m.group(1)), actor);
                    ApiResponse.success(resp, "LEAD_TRASHED", "Đã chuyển lead vào thùng rác.", null);
                } else {
                    notFound(resp);
                }
            }
            default -> ApiResponse.error(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ.");
        }
    }

    /** Có LEAD_ASSIGN thì thấy mọi lead; không có thì chỉ lead được giao cho mình. */
    private boolean seeAll(long actorId) throws SQLException {
        return permissionDAO.hasPermission(actorId, "LEAD_ASSIGN");
    }

    private static LeadInput input(JsonObject b) {
        return new LeadInput(str(b, "name"), str(b, "phone"), str(b, "email"), str(b, "source"),
                str(b, "programCode"), str(b, "status"), str(b, "note"));
    }
}
