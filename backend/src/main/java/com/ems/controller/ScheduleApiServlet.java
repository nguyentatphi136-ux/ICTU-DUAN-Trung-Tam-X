package com.ems.controller;

import com.ems.config.GsonProvider;
import com.ems.dto.ConflictCheckResult;
import com.ems.dto.ScheduleRequest;
import com.ems.exception.ForbiddenAccessException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ScheduleConflictException;
import com.ems.exception.ValidationException;
import com.ems.model.ClassSession;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.ems.service.ScheduleConflictService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller xß╗¡ l├╜ API Thß╗¥i kh├│a biß╗âu & Kiß╗âm tra xung ─æß╗Öt (Story S4-02 / Sprint 4)
 * Endpoints:
 * - POST /api/schedules/check-conflicts : Kiß╗âm tra tr├╣ng lß╗ïch giß║úng vi├¬n / ph├▓ng hß╗ìc
 * - POST /api/schedules                 : Tß║ío buß╗òi hß╗ìc mß╗¢i (chß║╖n xung ─æß╗Öt, cho ph├⌐p QL─ÉT ghi ─æ├¿ c├│ audit)
 * - GET  /api/schedules                 : Tra cß╗⌐u danh s├ích lß╗ïch hß╗ìc
 * - GET  /api/schedules/{id}            : Chi tiß║┐t buß╗òi hß╗ìc
 * - PUT  /api/schedules/{id}            : Cß║¡p nhß║¡t buß╗òi hß╗ìc (kiß╗âm tra xung ─æß╗Öt loß║íi trß╗½ ch├¡nh n├│)
 * - DELETE /api/schedules/{id}          : Hß╗ºy buß╗òi hß╗ìc
 */
@WebServlet("/api/schedules/*")
public class ScheduleApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = GsonProvider.getGson();
    private static final Pattern ID_PATH = Pattern.compile("^/(\\d+)$");

    private final ScheduleConflictService scheduleConflictService;

    public ScheduleApiServlet() {
        this.scheduleConflictService = new ScheduleConflictService();
    }

    public ScheduleApiServlet(ScheduleConflictService scheduleConflictService) {
        this.scheduleConflictService = scheduleConflictService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();

        try {
            ScheduleRequest scheduleReq = parseRequestBody(request);
            if (scheduleReq == null) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_BODY",
                        "Dß╗» liß╗çu gß╗¡i l├¬n kh├┤ng ─æ├║ng ─æß╗ïnh dß║íng JSON.", "Kiß╗âm tra dß╗» liß╗çu", "/schedules");
                return;
            }

            // 1. Endpoint: POST /api/schedules/check-conflicts
            if ("/check-conflicts".equals(path)) {
                ConflictCheckResult result = scheduleConflictService.checkConflicts(scheduleReq);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");

                // Trß║ú vß╗ü ─æß╗ïnh dß║íng chß╗⌐a ─æß║ºy ─æß╗º tr╞░ß╗¥ng theo y├¬u cß║ºu prompt & ApiResponse chuß║⌐n
                java.util.Map<String, Object> respBody = new java.util.LinkedHashMap<>();
                respBody.put("success", true);
                respBody.put("hasConflict", result.isHasConflict());
                respBody.put("conflicts", result.getConflicts());
                respBody.put("message", result.getMessage());
                respBody.put("data", result);
                response.getWriter().write(GSON.toJson(respBody));
                return;
            }

            // 2. Endpoint: POST /api/schedules
            if (path.isEmpty() || "/".equals(path)) {
                Long actorId = getActorId(request);
                List<String> actorRoles = getActorRoles(request);

                ClassSession created = scheduleConflictService.createSchedule(actorId, actorRoles, scheduleReq);
                ApiResponse.success(response, HttpServletResponse.SC_CREATED, "SCHEDULE_CREATED",
                        "Tß║ío lß╗ïch hß╗ìc th├ánh c├┤ng.", created);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Kh├┤ng t├¼m thß║Ñy chß╗⌐c n─âng.", "Quay lß║íi", "/schedules");
        } catch (ValidationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiß╗âm tra lß║íi dß╗» liß╗çu", "/schedules");
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN_OVERRIDE",
                    e.getMessage(), "Y├¬u cß║ºu quyß╗ün Quß║ún l├╜ ─æ├áo tß║ío", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lß║íi", "/schedules");
        } catch (ScheduleConflictException e) {
            ApiResponse.conflict(response, "SCHEDULE_CONFLICT", e.getMessage(), e.getConflictResult());
        } catch (SQLException e) {
            logError("Lß╗ùi c╞í sß╗ƒ dß╗» liß╗çu khi thao t├íc lß╗ïch hß╗ìc", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "─É├ú xß║úy ra lß╗ùi m├íy chß╗º khi l╞░u thß╗¥i kh├│a biß╗âu. Vui l├▓ng thß╗¡ lß║íi sau.", "Thß╗¡ lß║íi", "/schedules");
        } catch (Exception e) {
            logError("Lß╗ùi hß╗ç thß╗æng khi thao t├íc lß╗ïch hß╗ìc", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                    "Hß╗ç thß╗æng tß║ím thß╗¥i gß║╖p sß╗▒ cß╗æ. Vui l├▓ng thß╗¡ lß║íi sau.", "Thß╗¡ lß║íi", "/schedules");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher matcher = ID_PATH.matcher(path);

        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "─É╞░ß╗¥ng dß║½n cß║¡p nhß║¡t kh├┤ng hß╗úp lß╗ç.", "Quay lß║íi", "/schedules");
            return;
        }

        try {
            long scheduleId = Long.parseLong(matcher.group(1));
            ScheduleRequest scheduleReq = parseRequestBody(request);
            if (scheduleReq == null) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_BODY",
                        "Dß╗» liß╗çu gß╗¡i l├¬n kh├┤ng ─æ├║ng ─æß╗ïnh dß║íng JSON.", "Kiß╗âm tra dß╗» liß╗çu", "/schedules");
                return;
            }

            Long actorId = getActorId(request);
            List<String> actorRoles = getActorRoles(request);

            ClassSession updated = scheduleConflictService.updateSchedule(actorId, actorRoles, scheduleId, scheduleReq);
            ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_UPDATED",
                    "Cß║¡p nhß║¡t lß╗ïch hß╗ìc th├ánh c├┤ng.", updated);
        } catch (ValidationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiß╗âm tra lß║íi dß╗» liß╗çu", "/schedules");
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN_OVERRIDE",
                    e.getMessage(), "Y├¬u cß║ºu quyß╗ün Quß║ún l├╜ ─æ├áo tß║ío", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lß║íi", "/schedules");
        } catch (ScheduleConflictException e) {
            ApiResponse.conflict(response, "SCHEDULE_CONFLICT", e.getMessage(), e.getConflictResult());
        } catch (SQLException e) {
            logError("Lß╗ùi c╞í sß╗ƒ dß╗» liß╗çu khi cß║¡p nhß║¡t lß╗ïch hß╗ìc", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "─É├ú xß║úy ra lß╗ùi m├íy chß╗º khi cß║¡p nhß║¡t lß╗ïch hß╗ìc.", "Thß╗¡ lß║íi", "/schedules");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher matcher = ID_PATH.matcher(path);

        try {
            if (matcher.matches()) {
                long scheduleId = Long.parseLong(matcher.group(1));
                ClassSession session = scheduleConflictService.getScheduleById(scheduleId);
                ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_FOUND",
                        "Lß║Ñy th├┤ng tin buß╗òi hß╗ìc th├ánh c├┤ng.", session);
                return;
            }

            if (path.isEmpty() || "/".equals(path)) {
                Long classId = parseLongParam(request.getParameter("classId"));
                Long instructorId = parseLongParam(request.getParameter("instructorId"));
                Integer roomId = parseIntParam(request.getParameter("roomId"));
                if (roomId == null) roomId = parseIntParam(request.getParameter("classroomId"));

                LocalDate fromDate = parseDateParam(request.getParameter("fromDate"));
                LocalDate toDate = parseDateParam(request.getParameter("toDate"));

                List<ClassSession> list = scheduleConflictService.listSchedules(
                        classId, instructorId, roomId, fromDate, toDate
                );
                ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_LIST_SUCCESS",
                        "Lß║Ñy danh s├ích thß╗¥i kh├│a biß╗âu th├ánh c├┤ng.", list);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Kh├┤ng t├¼m thß║Ñy chß╗⌐c n─âng.", "Quay lß║íi", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lß║íi", "/schedules");
        } catch (SQLException e) {
            logError("Lß╗ùi c╞í sß╗ƒ dß╗» liß╗çu khi tra cß╗⌐u lß╗ïch hß╗ìc", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lß╗ùi m├íy chß╗º khi tra cß╗⌐u lß╗ïch hß╗ìc.", "Thß╗¡ lß║íi", "/schedules");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher matcher = ID_PATH.matcher(path);

        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "─É╞░ß╗¥ng dß║½n hß╗ºy lß╗ïch kh├┤ng hß╗úp lß╗ç.", "Quay lß║íi", "/schedules");
            return;
        }

        try {
            long scheduleId = Long.parseLong(matcher.group(1));
            String reason = request.getParameter("reason");
            if (reason == null) reason = "Quß║ún l├╜ ─æ├áo tß║ío hß╗ºy lß╗ïch";

            Long actorId = getActorId(request);
            List<String> actorRoles = getActorRoles(request);

            boolean ok = scheduleConflictService.cancelSchedule(actorId, actorRoles, scheduleId, reason);
            if (ok) {
                ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_CANCELLED",
                        "Hß╗ºy buß╗òi hß╗ìc th├ánh c├┤ng.", java.util.Map.of("id", scheduleId, "status", "CANCELLED"));
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Kh├┤ng t├¼m thß║Ñy buß╗òi hß╗ìc ─æß╗â hß╗ºy.", "Quay lß║íi", "/schedules");
            }
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                    e.getMessage(), "Y├¬u cß║ºu quyß╗ün Quß║ún l├╜ ─æ├áo tß║ío", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lß║íi", "/schedules");
        } catch (SQLException e) {
            logError("Lß╗ùi c╞í sß╗ƒ dß╗» liß╗çu khi hß╗ºy lß╗ïch hß╗ìc", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lß╗ùi m├íy chß╗º khi hß╗ºy lß╗ïch hß╗ìc.", "Thß╗¡ lß║íi", "/schedules");
        }
    }

    private ScheduleRequest parseRequestBody(HttpServletRequest request) {
        try {
            return GSON.fromJson(request.getReader(), ScheduleRequest.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getActorId(HttpServletRequest request) {
        Object attr = request.getAttribute("authenticatedUserId");
        if (attr instanceof Long id) return id;
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) return user.getId();
        }
        return 0L;
    }

    private List<String> getActorRoles(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null && user.getRoles() != null) return user.getRoles();
            if (user != null && user.getPrimaryRole() != null) return List.of(user.getPrimaryRole());
        }
        return List.of();
    }

    private Long parseLongParam(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseIntParam(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate parseDateParam(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void logError(String msg, Throwable t) {
        try {
            if (getServletContext() != null) {
                getServletContext().log(msg, t);
                return;
            }
        } catch (Exception ignored) {}
        System.err.println(msg + ": " + (t != null ? t.getMessage() : ""));
    }
}
