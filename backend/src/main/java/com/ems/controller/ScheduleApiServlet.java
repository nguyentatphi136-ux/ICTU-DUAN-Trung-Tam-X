package com.ems.controller;

import com.ems.config.GsonProvider;
import com.ems.dto.ConflictCheckResult;
import com.ems.dto.RescheduleResponse;
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
import com.google.gson.JsonParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller xử lý API Thời khóa biểu & Quản lý buổi học (Story S4-02 & S4-03 / Sprint 4)
 * Endpoints:
 * - POST   /api/schedules/check-conflicts   : Kiểm tra trùng lịch giảng viên / phòng học
 * - POST   /api/schedules                   : Tạo buổi học mới (chặn xung đột, cho phép QLĐT ghi đè có audit)
 * - GET    /api/schedules                   : Tra cứu danh sách lịch học
 * - GET    /api/schedules/{id}              : Chi tiết buổi học
 * - PUT    /api/schedules/{id}              : Cập nhật / dời buổi học
 * - PATCH  /api/schedules/{id}              : Sửa một phần / dời buổi học (giữ nguyên các trường không gửi)
 * - POST   /api/schedules/{id}/cancel       : Hủy buổi học (yêu cầu lý do hủy bắt buộc)
 * - DELETE /api/schedules/{id}              : Hủy buổi học (yêu cầu lý do hủy bắt buộc)
 *
 * Đồng thời hỗ trợ alias /api/class-sessions/* theo chuẩn RESTful.
 */
@WebServlet(urlPatterns = {"/api/schedules/*", "/api/class-sessions/*"})
public class ScheduleApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = GsonProvider.getGson();
    private static final Pattern ID_PATH = Pattern.compile("^/(\\d+)$");
    private static final Pattern CANCEL_PATH = Pattern.compile("^/(\\d+)/cancel$");

    private final ScheduleConflictService scheduleConflictService;

    public ScheduleApiServlet() {
        this.scheduleConflictService = new ScheduleConflictService();
    }

    public ScheduleApiServlet(ScheduleConflictService scheduleConflictService) {
        this.scheduleConflictService = scheduleConflictService;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String method = request.getMethod();
        if ("PATCH".equalsIgnoreCase(method)) {
            doPatch(request, response);
        } else {
            super.service(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "" : request.getPathInfo();

        // 1. Endpoint: POST /api/schedules/{id}/cancel hoặc /api/class-sessions/{id}/cancel
        Matcher cancelMatcher = CANCEL_PATH.matcher(path);
        if (cancelMatcher.matches()) {
            handleCancelRequest(request, response, Long.parseLong(cancelMatcher.group(1)));
            return;
        }

        try {
            ScheduleRequest scheduleReq = parseRequestBody(request);
            if (scheduleReq == null) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_BODY",
                        "Dữ liệu gửi lên không đúng định dạng JSON.", "Kiểm tra dữ liệu", "/schedules");
                return;
            }

            // 2. Endpoint: POST /api/schedules/check-conflicts
            if ("/check-conflicts".equals(path)) {
                ConflictCheckResult result = scheduleConflictService.checkConflicts(scheduleReq);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");

                java.util.Map<String, Object> respBody = new java.util.LinkedHashMap<>();
                respBody.put("success", true);
                respBody.put("hasConflict", result.isHasConflict());
                respBody.put("conflicts", result.getConflicts());
                respBody.put("message", result.getMessage());
                respBody.put("data", result);
                response.getWriter().write(GSON.toJson(respBody));
                return;
            }

            // 3. Endpoint: POST /api/schedules hoặc POST /api/class-sessions
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
        handleUpdateOrReschedule(request, response);
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handleUpdateOrReschedule(request, response);
    }

    private void handleUpdateOrReschedule(HttpServletRequest request, HttpServletResponse response)
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

            RescheduleResponse rescheduleResp = scheduleConflictService.updateScheduleWithDetails(
                    actorId, actorRoles, scheduleId, scheduleReq
            );

            ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_UPDATED",
                    rescheduleResp.getMessage(), rescheduleResp);
        } catch (ValidationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra lại dữ liệu", "/schedules");
        } catch (ForbiddenAccessException e) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
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
        } catch (Exception e) {
            logError("Lỗi hệ thống khi cập nhật lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                    "Hệ thống tạm thời gặp sự cố. Vui lòng thử lại sau.", "Thử lại", "/schedules");
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
        } catch (Exception e) {
            logError("Lỗi hệ thống khi tra cứu lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                    "Hệ thống tạm thời gặp sự cố. Vui lòng thử lại sau.", "Thử lại", "/schedules");
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

        long scheduleId = Long.parseLong(matcher.group(1));
        handleCancelRequest(request, response, scheduleId);
    }

    private void handleCancelRequest(HttpServletRequest request, HttpServletResponse response, long scheduleId)
            throws IOException {
        try {
            String reason = extractReason(request);

            Long actorId = getActorId(request);
            List<String> actorRoles = getActorRoles(request);

            RescheduleResponse cancelResp = scheduleConflictService.cancelScheduleWithDetails(
                    actorId, actorRoles, scheduleId, reason
            );

            ApiResponse.success(response, HttpServletResponse.SC_OK, "SCHEDULE_CANCELLED",
                    cancelResp.getMessage(), cancelResp);
        } catch (ValidationException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                    e.getMessage(), "Kiểm tra lại dữ liệu", "/schedules");
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
        } catch (Exception e) {
            logError("Lỗi hệ thống khi hủy lịch học", e);
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SYSTEM_ERROR",
                    "Hệ thống tạm thời gặp sự cố. Vui lòng thử lại sau.", "Thử lại", "/schedules");
        }
    }

    private String extractReason(HttpServletRequest request) {
        // 1. Thử đọc từ query parameter
        String reason = request.getParameter("reason");
        if (reason != null && !reason.trim().isEmpty()) {
            return reason.trim();
        }

        // 2. Thử đọc từ body JSON
        try {
            BufferedReader reader = request.getReader();
            if (reader != null) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("reason") && !json.get("reason").isJsonNull()) {
                    return json.get("reason").getAsString();
                }
                if (json.has("cancelReason") && !json.get("cancelReason").isJsonNull()) {
                    return json.get("cancelReason").getAsString();
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback cho method DELETE nếu không có lý do được chỉ định (tương thích backward)
        if ("DELETE".equalsIgnoreCase(request.getMethod())) {
            return "Quản lý đào tạo hủy lịch";
        }

        return reason;
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
