package com.ems.controller;

import com.ems.dto.ClassSessionResponse;
import com.ems.exception.ApiException;
import com.ems.security.ApiResponse;
import com.ems.service.ClassSessionService;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * REST API Servlet phục vụ thời khóa biểu tuần / tháng
 * EP-04 – S4-04: Quản lý đào tạo
 *
 * Endpoints:
 *  GET /api/schedules/week?date=yyyy-MM-dd[&classId=&lecturerId=&roomId=]
 *  GET /api/schedules/month?year=yyyy&month=M[&classId=&lecturerId=&roomId=]
 *  GET /api/schedules/conflicts?startDate=&endDate=
 *  GET /api/schedules/filters/classes
 *  GET /api/schedules/filters/instructors
 *  GET /api/schedules/filters/classrooms
 *
 * Danh sách theo khoảng ngày và chi tiết một buổi (GET /api/schedules, /api/schedules/{id}) thuộc
 * ScheduleApiServlet của S4-03. Đường dẫn khớp chính xác và tiền tố dài hơn được ưu tiên hơn /api/schedules/*,
 * nên hai servlet cùng chạy được.
 */
@WebServlet(name = "ScheduleCalendarApiServlet", urlPatterns = {
        "/api/schedules/week", "/api/schedules/month", "/api/schedules/conflicts", "/api/schedules/filters/*"
})
public class ScheduleCalendarApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private ClassSessionService classSessionService;

    @Override
    public void init() throws ServletException {
        this.classSessionService = new ClassSessionService();
    }

    /** Cho phép inject service từ test. */
    public void setClassSessionService(ClassSessionService classSessionService) {
        this.classSessionService = classSessionService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = getCleanPathInfo(req);

        try {
            // ----------------------------------------------------------------
            // GET /api/schedules/week
            // ----------------------------------------------------------------
            if ("/week".equals(pathInfo)) {
                String dateParam   = req.getParameter("date");
                Long classId       = parseLong(req.getParameter("classId"));
                Long lecturerId    = parseLong(req.getParameter("lecturerId"));
                Integer roomId     = parseInteger(req.getParameter("roomId"));

                // Mặc định về tuần hiện tại nếu không truyền date
                if (dateParam == null || dateParam.isBlank()) {
                    dateParam = java.time.LocalDate.now().toString();
                }

                List<ClassSessionResponse> sessions =
                        classSessionService.getWeeklySchedule(dateParam, classId, lecturerId, roomId);

                ApiResponse.success(resp, "SCHEDULE_WEEK_SUCCESS",
                        "Lấy thời khóa biểu theo tuần thành công.", sessions);
                return;
            }

            // ----------------------------------------------------------------
            // GET /api/schedules/month
            // ----------------------------------------------------------------
            if ("/month".equals(pathInfo)) {
                String yearParam  = req.getParameter("year");
                String monthParam = req.getParameter("month");

                if (yearParam == null || monthParam == null) {
                    throw new com.ems.exception.ValidationException(
                            "Tham số 'year' và 'month' là bắt buộc.");
                }

                int year;
                int month;
                try {
                    year  = Integer.parseInt(yearParam.trim());
                    month = Integer.parseInt(monthParam.trim());
                } catch (NumberFormatException e) {
                    throw new com.ems.exception.ValidationException(
                            "Tham số 'year' và 'month' phải là số nguyên.");
                }

                Long classId    = parseLong(req.getParameter("classId"));
                Long lecturerId = parseLong(req.getParameter("lecturerId"));
                Integer roomId  = parseInteger(req.getParameter("roomId"));

                List<ClassSessionResponse> sessions =
                        classSessionService.getMonthlySchedule(year, month, classId, lecturerId, roomId);

                ApiResponse.success(resp, "SCHEDULE_MONTH_SUCCESS",
                        "Lấy thời khóa biểu theo tháng thành công.", sessions);
                return;
            }

            // ----------------------------------------------------------------
            // GET /api/schedules/conflicts
            // ----------------------------------------------------------------
            if ("/conflicts".equals(pathInfo)) {
                String startDate = req.getParameter("startDate");
                String endDate   = req.getParameter("endDate");

                if (startDate == null || endDate == null) {
                    throw new com.ems.exception.ValidationException(
                            "Tham số 'startDate' và 'endDate' là bắt buộc.");
                }

                List<Map<String, Object>> conflicts =
                        classSessionService.detectConflicts(startDate, endDate);

                ApiResponse.success(resp, "SCHEDULE_CONFLICTS_SUCCESS",
                        "Phát hiện " + conflicts.size() + " cặp buổi học bị trùng.", conflicts);
                return;
            }

            // ----------------------------------------------------------------
            // GET /api/schedules/filters/*
            // ----------------------------------------------------------------
            if ("/filters/classes".equals(pathInfo)) {
                ApiResponse.success(resp, "FILTER_CLASSES_SUCCESS",
                        "Danh sách lớp học.", classSessionService.getFilterClasses());
                return;
            }
            if ("/filters/instructors".equals(pathInfo)) {
                ApiResponse.success(resp, "FILTER_INSTRUCTORS_SUCCESS",
                        "Danh sách giảng viên.", classSessionService.getFilterInstructors());
                return;
            }
            if ("/filters/classrooms".equals(pathInfo)) {
                ApiResponse.success(resp, "FILTER_CLASSROOMS_SUCCESS",
                        "Danh sách phòng học.", classSessionService.getFilterClassrooms());
                return;
            }

            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND,
                    "ENDPOINT_NOT_FOUND", "Đường dẫn API không tồn tại.");

        } catch (ApiException e) {
            ApiResponse.error(resp, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Lỗi máy chủ: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Phần sau /api/schedules, ví dụ /week hoặc /filters/classes (đường dẫn khớp chính xác có pathInfo null). */
    private String getCleanPathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String path = request.getServletPath() + (pathInfo == null ? "" : pathInfo.trim());
        return path.startsWith("/api/schedules") ? path.substring("/api/schedules".length()) : path;
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new com.ems.exception.ValidationException(
                    "Tham số ID không hợp lệ: '" + s + "'. Phải là số nguyên.");
        }
    }

    private Integer parseInteger(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new com.ems.exception.ValidationException(
                    "Tham số ID không hợp lệ: '" + s + "'. Phải là số nguyên.");
        }
    }
}
