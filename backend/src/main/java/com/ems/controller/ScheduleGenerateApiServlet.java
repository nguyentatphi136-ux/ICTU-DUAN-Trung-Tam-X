package com.ems.controller;

import com.ems.model.*;
import com.ems.service.HolidayStore;
import com.ems.service.ScheduleService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RESTful API Servlet cho module Sinh thời khoá biểu tự động (Sprint 4: S4-01)
 * URL Patterns:
 *   - POST /api/v1/classes/{code}/schedule/preview : Sinh lịch và xem trước
 *   - POST /api/v1/classes/{code}/schedule         : Lưu lịch chính thức
 *   - GET  /api/v1/classes/{code}/schedule         : Lấy lịch học của lớp
 *   - GET  /api/v1/holidays                        : Lấy danh sách ngày nghỉ lễ
 *   - POST /api/v1/holidays                        : Thêm ngày nghỉ lễ mới
 * 
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
// Servlet không cho phép dấu * ở giữa đường dẫn, nên bắt cả /api/v1/classes/* rồi tách mã lớp bằng regex bên dưới.
@WebServlet(name = "ScheduleGenerateApiServlet", urlPatterns = {
        "/api/v1/classes/*",
        "/api/v1/holidays"
})
public class ScheduleGenerateApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ScheduleService scheduleService = new ScheduleService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            handleGet(req, resp);
        } catch (RuntimeException e) {
            writeError(resp, e);
        }
    }

    private void handleGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/api/v1/holidays")) {
            List<Holiday> holidays = HolidayStore.getAllHolidays();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < holidays.size(); i++) {
                Holiday h = holidays.get(i);
                if (i > 0) json.append(",");
                json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}",
                        h.getId(), escapeJson(h.getName()), h.getFromDate(), h.getToDate()));
            }
            json.append("]");
            resp.getWriter().write(json.toString());
            return;
        }

        // GET /api/v1/classes/{code}/schedule
        Pattern pattern = Pattern.compile(".*/api/v1/classes/([^/]+)/schedule$");
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            String classCode = matcher.group(1);
            List<PlannedSession> sessions = scheduleService.getSessionsByClass(classCode);
            resp.getWriter().write(sessionsToJson(sessions));
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.getWriter().write("{\"error\":\"Endpoint không tồn tại\"}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String uri = req.getRequestURI();

        // Quyền ghi (CLASS_MANAGE) do SessionAuthFilter kiểm tra theo PermissionPolicy trước khi tới đây.

        // POST /api/v1/holidays
        if (uri.endsWith("/api/v1/holidays")) {
            String body = readBody(req);
            String name = extractJsonField(body, "name");
            String from = extractJsonField(body, "from");
            String to = extractJsonField(body, "to");
            try {
                HolidayStore.addHoliday(name, from, to);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                out.write("{\"success\":true,\"message\":\"Đã thêm ngày nghỉ lễ\"}");
            } catch (RuntimeException e) {
                writeError(resp, e);
            }
            return;
        }

        // POST /api/v1/classes/{code}/schedule/preview
        Pattern previewPattern = Pattern.compile(".*/api/v1/classes/([^/]+)/schedule/preview$");
        Matcher previewMatcher = previewPattern.matcher(uri);
        if (previewMatcher.find()) {
            String classCode = previewMatcher.group(1);
            String body = readBody(req);
            try {
                SchedulePattern p = parsePatternJson(classCode, body);
                ScheduleGenerationResult result = scheduleService.generateSchedule(p);
                out.write(resultToJson(result));
            } catch (RuntimeException e) {
                writeError(resp, e);
            }
            return;
        }

        // POST /api/v1/classes/{code}/schedule (Lưu)
        Pattern savePattern = Pattern.compile(".*/api/v1/classes/([^/]+)/schedule$");
        Matcher saveMatcher = savePattern.matcher(uri);
        if (saveMatcher.find()) {
            String classCode = saveMatcher.group(1);
            String body = readBody(req);
            try {
                SchedulePattern p = parsePatternJson(classCode, body);
                ScheduleGenerationResult result = scheduleService.generateSchedule(p);
                scheduleService.saveSchedule(classCode, result.getSessions(), p);
                out.write(String.format("{\"success\":true,\"message\":\"Đã lưu thời khoá biểu lớp %s\",\"sessionsCount\":%d}",
                        escapeJson(classCode), result.getSessions().size()));
            } catch (RuntimeException e) {
                writeError(resp, e);
            }
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.write("{\"error\":\"Endpoint không tồn tại\"}");
    }

    /** Dữ liệu gửi sai: 400 kèm lý do; lỗi CSDL: 500. */
    private void writeError(HttpServletResponse resp, RuntimeException e) throws IOException {
        boolean badInput = e instanceof IllegalArgumentException || e instanceof java.time.format.DateTimeParseException;
        resp.setStatus(badInput ? HttpServletResponse.SC_BAD_REQUEST : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (!badInput) log("Lỗi API sinh thời khoá biểu", e);
        String message = badInput ? e.getMessage() : "Lỗi máy chủ khi xử lý thời khoá biểu. Vui lòng thử lại sau.";
        resp.getWriter().write(String.format("{\"error\":\"%s\"}", escapeJson(message)));
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private SchedulePattern parsePatternJson(String classCode, String json) {
        String start = extractJsonField(json, "start");
        if (start.isBlank()) start = "18:00";
        String end = extractJsonField(json, "end");
        if (end.isBlank()) end = "20:30";
        String from = extractJsonField(json, "from");
        if (from.isBlank()) from = "2026-10-20";
        String room = extractJsonField(json, "room");
        String countStr = extractJsonField(json, "count");
        int count = 60;
        if (!countStr.isBlank()) {
            try { count = Integer.parseInt(countStr); } catch (Exception ignored) {}
        }

        List<Integer> days = new ArrayList<>();
        Pattern dayPattern = Pattern.compile("\"days\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher m = dayPattern.matcher(json);
        if (m.find()) {
            String[] nums = m.group(1).split(",");
            for (String n : nums) {
                try { days.add(Integer.parseInt(n.trim())); } catch (Exception ignored) {}
            }
        }
        if (days.isEmpty()) {
            days.addAll(List.of(1, 3, 5)); // T2, T4, T6
        }

        return new SchedulePattern(classCode, days, start, end, from, count, room);
    }

    private String extractJsonField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"?([^,\"}]+)\"?");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1).trim() : "";
    }

    private String resultToJson(ScheduleGenerationResult result) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"firstDate\":\"").append(result.getFirstDate()).append("\",");
        sb.append("\"lastDate\":\"").append(result.getLastDate()).append("\",");
        sb.append("\"sessionsPerWeek\":").append(result.getSessionsPerWeek()).append(",");
        sb.append("\"plainWeeks\":").append(result.getPlainWeeks()).append(",");
        sb.append("\"actualWeeks\":").append(result.getActualWeeks()).append(",");
        sb.append("\"weeksDelayed\":").append(result.getWeeksDelayed()).append(",");

        // Sessions
        sb.append("\"sessions\":[");
        for (int i = 0; i < result.getSessions().size(); i++) {
            PlannedSession s = result.getSessions().get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":\"%s\",\"cls\":\"%s\",\"no\":%d,\"date\":\"%s\",\"start\":\"%s\",\"end\":\"%s\",\"subject\":\"%s\",\"lesson\":%d,\"topic\":\"%s\",\"teacher\":%s,\"room\":%s}",
                    s.getId(), s.getCls(), s.getNo(), s.getDate(), s.getStart(), s.getEnd(), s.getSubject(), s.getLesson(),
                    escapeJson(s.getTopic()),
                    s.getTeacher() != null ? "\"" + escapeJson(s.getTeacher()) + "\"" : "null",
                    s.getRoom() != null ? "\"" + escapeJson(s.getRoom()) + "\"" : "null"));
        }
        sb.append("],");

        // Skipped
        sb.append("\"skipped\":[");
        for (int i = 0; i < result.getSkipped().size(); i++) {
            SkippedDate sk = result.getSkipped().get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"date\":\"%s\",\"reason\":\"%s\",\"dayOfWeek\":\"%s\"}",
                    sk.getDate(), escapeJson(sk.getReason()), sk.getDayOfWeek()));
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private String sessionsToJson(List<PlannedSession> sessions) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sessions.size(); i++) {
            PlannedSession s = sessions.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"id\":\"%s\",\"cls\":\"%s\",\"no\":%d,\"date\":\"%s\",\"start\":\"%s\",\"end\":\"%s\",\"subject\":\"%s\",\"lesson\":%d,\"topic\":\"%s\",\"teacher\":%s,\"room\":%s}",
                    s.getId(), s.getCls(), s.getNo(), s.getDate(), s.getStart(), s.getEnd(), s.getSubject(), s.getLesson(),
                    escapeJson(s.getTopic()),
                    s.getTeacher() != null ? "\"" + escapeJson(s.getTeacher()) + "\"" : "null",
                    s.getRoom() != null ? "\"" + escapeJson(s.getRoom()) + "\"" : "null"));
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
