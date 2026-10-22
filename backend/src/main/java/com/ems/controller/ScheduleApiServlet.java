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
                        "Dữ liệu gửi lên không đúng định dạng JSON.", "Kiểm tra dữ liệu", "/schedules");
                return;
            }

            // 1. Endpoint: POST /api/schedules/check-conflicts
            if ("/check-conflicts".equals(path)) {
                ConflictCheckResult result = scheduleConflictService.checkConflicts(scheduleReq);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");

                // Trả về định dạng chứa đầy đủ trường theo yêu cầu prompt & ApiResponse chuẩn
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
                        "Tạo lịch học thành công.", created);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Không tìm thấy chức năng.", "Quay lại", "/schedules");
        } catch (ValidationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra lại dữ liệu", "/schedules");
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN_OVERRIDE",
                    e.getMessage(), "Yêu cầu quyền Quản lý đào tạo", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lại", "/schedules");
        } catch (ScheduleConflictException e) {
            ApiResponse.conflict(response, "SCHEDULE_CONFLICT", e.getMessage(), e.getConflictResult());
        } catch (SQLException e) {
            logError("Lỗi cơ sở dữ liệu khi thao tác lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Đã xảy ra lỗi máy chủ khi lưu thời khóa biểu. Vui lòng thử lại sau.", "Thử lại", "/schedules");
        } catch (Exception e) {
            logError("Lỗi hệ thống khi thao tác lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                    "Hệ thống tạm thời gặp sự cố. Vui lòng thử lại sau.", "Thử lại", "/schedules");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher matcher = ID_PATH.matcher(path);

        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Đường dẫn cập nhật không hợp lệ.", "Quay lại", "/schedules");
            return;
        }

        try {
            long scheduleId = Long.parseLong(matcher.group(1));
            ScheduleRequest scheduleReq = parseRequestBody(request);
            if (scheduleReq == null) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_BODY",
                        "Dữ liệu gửi lên không đúng định dạng JSON.", "Kiểm tra dữ liệu", "/schedules");
                return;
            }

            Long actorId = getActorId(request);
            List<String> actorRoles = getActorRoles(request);

            ClassSession updated = scheduleConflictService.updateSchedule(actorId, actorRoles, scheduleId, scheduleReq);
            ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_UPDATED",
                    "Cập nhật lịch học thành công.", updated);
        } catch (ValidationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra lại dữ liệu", "/schedules");
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN_OVERRIDE",
                    e.getMessage(), "Yêu cầu quyền Quản lý đào tạo", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lại", "/schedules");
        } catch (ScheduleConflictException e) {
            ApiResponse.conflict(response, "SCHEDULE_CONFLICT", e.getMessage(), e.getConflictResult());
        } catch (SQLException e) {
            logError("Lỗi cơ sở dữ liệu khi cập nhật lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Đã xảy ra lỗi máy chủ khi cập nhật lịch học.", "Thử lại", "/schedules");
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
                        "Lấy thông tin buổi học thành công.", session);
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
                        "Lấy danh sách thời khóa biểu thành công.", list);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Không tìm thấy chức năng.", "Quay lại", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lại", "/schedules");
        } catch (SQLException e) {
            logError("Lỗi cơ sở dữ liệu khi tra cứu lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi máy chủ khi tra cứu lịch học.", "Thử lại", "/schedules");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();
        Matcher matcher = ID_PATH.matcher(path);

        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Đường dẫn hủy lịch không hợp lệ.", "Quay lại", "/schedules");
            return;
        }

        try {
            long scheduleId = Long.parseLong(matcher.group(1));
            String reason = request.getParameter("reason");
            if (reason == null) reason = "Quản lý đào tạo hủy lịch";

            Long actorId = getActorId(request);
            List<String> actorRoles = getActorRoles(request);

            boolean ok = scheduleConflictService.cancelSchedule(actorId, actorRoles, scheduleId, reason);
            if (ok) {
                ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_CANCELLED",
                        "Hủy buổi học thành công.", java.util.Map.of("id", scheduleId, "status", "CANCELLED"));
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                        "Không tìm thấy buổi học để hủy.", "Quay lại", "/schedules");
            }
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                    e.getMessage(), "Yêu cầu quyền Quản lý đào tạo", "/schedules");
        } catch (ResourceNotFoundException e) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND",
                    e.getMessage(), "Quay lại", "/schedules");
        } catch (SQLException e) {
            logError("Lỗi cơ sở dữ liệu khi hủy lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi máy chủ khi hủy lịch học.", "Thử lại", "/schedules");
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
