package com.ems.controller;

import com.ems.dao.AttendanceDAO;
import com.ems.exception.ValidationException;
import com.ems.security.ApiResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S4-07 Điểm danh, quyền ATTENDANCE_MANAGE. Trạng thái: PRESENT, LATE, EXCUSED_ABSENCE, UNEXCUSED_ABSENCE.
 *
 * <pre>
 * GET /api/attendance/sessions?from=&to=     buổi mình dạy (quản lý: mọi buổi) kèm bảng đã lưu: [{ session, sheet }]
 * GET /api/sessions/{id}/attendance          { session, roster, sheet, logs, approved, canEdit }
 * PUT /api/sessions/{id}/attendance          { marks: { mã: trạng thái }, notes: { mã: ghi chú }, reason }
 * </pre>
 */
@WebServlet(name = "AttendanceApiServlet", urlPatterns = {"/api/attendance/sessions", "/api/sessions/*"})
public class AttendanceApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern SHEET = Pattern.compile("^/(\\d+)/attendance$");

    private final transient AttendanceDAO dao = new AttendanceDAO();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if ("/api/attendance/sessions".equals(req.getServletPath())) {
            if (!"GET".equals(method)) {
                notFound(resp);
                return;
            }
            LocalDate to = date(req, "to", LocalDate.now());
            LocalDate from = date(req, "from", to.minusDays(14));
            if (from.isAfter(to) || from.plusDays(120).isBefore(to)) throw new ValidationException("Khoảng ngày không hợp lệ (tối đa 120 ngày).");
            ApiResponse.success(resp, "ATTENDANCE_SESSIONS", "Buổi cần điểm danh.", dao.visibleSessions(MyScheduleApiServlet.currentUser(req), from, to));
            return;
        }
        Matcher m = SHEET.matcher(path);
        if (!m.matches()) {
            notFound(resp);
            return;
        }
        long id = Long.parseLong(m.group(1));
        if ("GET".equals(method)) {
            ApiResponse.success(resp, "ATTENDANCE_SHEET", "Điểm danh buổi học.", dao.get(MyScheduleApiServlet.currentUser(req), id));
        } else if ("PUT".equals(method)) {
            JsonObject b = body(req);
            ApiResponse.success(resp, "ATTENDANCE_SAVED", "Đã lưu điểm danh.", dao.save(MyScheduleApiServlet.currentUser(req), id,
                    strMap(b, "marks"), strMap(b, "notes"), str(b, "reason")));
        } else {
            notFound(resp);
        }
    }

    private static Map<String, String> strMap(JsonObject b, String key) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonElement el = b.get(key);
        if (el == null || el.isJsonNull()) return out;
        if (!el.isJsonObject()) throw new ValidationException("Trường " + key + " phải là đối tượng { mã học viên: giá trị }.");
        for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
            out.put(e.getKey(), e.getValue().isJsonNull() ? null : e.getValue().getAsString());
        }
        return out;
    }

    private static LocalDate date(HttpServletRequest req, String name, LocalDate fallback) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return fallback;
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Tham số " + name + " phải có dạng YYYY-MM-DD.");
        }
    }
}
