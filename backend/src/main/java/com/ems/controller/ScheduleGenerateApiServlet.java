package com.ems.controller;

import com.ems.exception.ValidationException;
import com.ems.model.Holiday;
import com.ems.model.PlannedSession;
import com.ems.model.ScheduleGenerationResult;
import com.ems.model.SchedulePattern;
import com.ems.model.SkippedDate;
import com.ems.security.ApiResponse;
import com.ems.service.HolidayStore;
import com.ems.service.ScheduleService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RESTful API sinh thời khoá biểu tự động (Sprint 4: S4-01). Quyền do SessionAuthFilter kiểm theo PermissionPolicy:
 * GET cần SCHEDULE_READ, POST cần CLASS_MANAGE.
 *
 * <pre>
 * POST /api/v1/classes/{code}/schedule/preview   { days: [1,3,5], start, end, from, count, room } sinh lịch, xem trước
 * POST /api/v1/classes/{code}/schedule           như trên, lưu vào class_sessions (thay lịch cũ)
 * GET  /api/v1/classes/{code}/schedule           lịch đã lưu của lớp
 * GET  /api/v1/holidays                          [{ id, name, from, to }]
 * POST /api/v1/holidays                          { name, from, to }
 * </pre>
 * days theo Date.getDay() của trình duyệt: Chủ nhật 0, Thứ Hai 1 ... Thứ Bảy 6.
 *
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
// Servlet không cho phép dấu * ở giữa đường dẫn, nên bắt cả /api/v1/classes/* rồi tách mã lớp bằng regex bên dưới.
@WebServlet(name = "ScheduleGenerateApiServlet", urlPatterns = {
        "/api/v1/classes/*",
        "/api/v1/holidays"
})
public class ScheduleGenerateApiServlet extends JsonApiServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern SCHEDULE = Pattern.compile("^/([A-Za-z0-9_-]+)/schedule(/preview)?$");

    private final transient ScheduleService scheduleService = new ScheduleService();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            if ("/api/v1/holidays".equals(req.getServletPath())) {
                if ("GET".equals(method)) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (Holiday h : HolidayStore.getAllHolidays()) {
                        list.add(Map.of("id", h.getId(), "name", h.getName(), "from", h.getFromDate().toString(), "to", h.getToDate().toString()));
                    }
                    ApiResponse.success(resp, "HOLIDAY_LIST", "Danh sách ngày nghỉ lễ.", list);
                } else if ("POST".equals(method)) {
                    JsonObject b = body(req);
                    HolidayStore.addHoliday(str(b, "name"), str(b, "from"), str(b, "to"));
                    ApiResponse.created(resp, "HOLIDAY_CREATED", "Đã thêm ngày nghỉ lễ.", null);
                } else {
                    notFound(resp);
                }
                return;
            }
            Matcher m = SCHEDULE.matcher(path);
            if (!m.matches()) {
                notFound(resp);
                return;
            }
            String classCode = m.group(1);
            boolean preview = m.group(2) != null;
            if ("GET".equals(method) && !preview) {
                ApiResponse.success(resp, "SCHEDULE_OF_CLASS", "Lịch học của lớp.", scheduleService.getSessionsByClass(classCode));
            } else if ("POST".equals(method)) {
                SchedulePattern p = pattern(classCode, body(req));
                ScheduleGenerationResult result = scheduleService.generateSchedule(p);
                if (preview) {
                    ApiResponse.success(resp, "SCHEDULE_PREVIEW", "Xem trước thời khoá biểu.", toMap(result));
                } else {
                    scheduleService.saveSchedule(classCode, result.getSessions(), p);
                    ApiResponse.success(resp, "SCHEDULE_SAVED", "Đã lưu thời khoá biểu lớp " + classCode + ".",
                            Map.of("sessionsCount", result.getSessions().size()));
                }
            } else {
                notFound(resp);
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (IllegalStateException e) {
            // ClassStore, HolidayStore, ScheduleService báo lỗi CSDL bằng IllegalStateException.
            throw new SQLException(e.getMessage(), e);
        }
    }

    /** Mẫu lặp từ JSON; thiếu trường thì để ScheduleService báo lỗi, không tự điền giá trị mặc định. */
    private static SchedulePattern pattern(String classCode, JsonObject b) {
        List<Integer> days = new ArrayList<>();
        JsonElement el = b.get("days");
        if (el != null && el.isJsonArray()) {
            for (JsonElement d : el.getAsJsonArray()) {
                int day = d.getAsInt();
                if (day < 0 || day > 6) throw new ValidationException("Thứ trong tuần phải từ 0 (Chủ nhật) đến 6 (Thứ Bảy).");
                days.add(day);
            }
        }
        int count = b.has("count") && !b.get("count").isJsonNull() ? b.get("count").getAsInt() : 0;
        return new SchedulePattern(classCode, days, str(b, "start"), str(b, "end"), str(b, "from"), count, str(b, "room"));
    }

    private static Map<String, Object> toMap(ScheduleGenerationResult r) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("firstDate", r.getFirstDate());
        out.put("lastDate", r.getLastDate());
        out.put("sessionsPerWeek", r.getSessionsPerWeek());
        out.put("plainWeeks", r.getPlainWeeks());
        out.put("actualWeeks", r.getActualWeeks());
        out.put("weeksDelayed", r.getWeeksDelayed());
        List<PlannedSession> sessions = r.getSessions();
        out.put("sessions", sessions);
        List<Map<String, String>> skipped = new ArrayList<>();
        for (SkippedDate s : r.getSkipped()) skipped.add(Map.of("date", s.getDate(), "reason", s.getReason()));
        out.put("skipped", skipped);
        return out;
    }
}
