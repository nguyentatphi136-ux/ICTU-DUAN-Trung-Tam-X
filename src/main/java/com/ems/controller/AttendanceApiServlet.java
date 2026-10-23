package com.ems.controller;

import com.ems.config.GsonProvider;
import com.ems.constant.RoleConstant;
import com.ems.dao.AttendanceDAO;
import com.ems.dto.AttendanceSheetResponse;
import com.ems.dto.SaveAttendanceRequest;
import com.ems.model.AttendanceAuditLog;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;

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
import java.util.List;

/**
 * Controller Servlet phục vụ REST API Điểm danh học viên (S4-07):
 * AC1: Bốn trạng thái: Có mặt, Đi muộn, Vắng có phép, Vắng không phép
 * AC2: Đánh dấu tất cả có mặt rồi chỉnh ngoại lệ
 * AC3: Mặc định chỉ điểm danh được buổi hôm nay; sửa buổi quá khứ cần quyền Quản lý đào tạo
 * AC4: Sửa điểm danh đã lưu được ghi vào nhật ký
 * AC5: Phản hồi cấu trúc gọn nhẹ, tối ưu cho màn hình di động 360px
 *
 * Endpoints:
 *  - GET  /api/attendance/session/{id}     (hoặc /api/sessions/{id}/attendance)
 *  - POST /api/attendance/session/{id}     (hoặc PUT /api/sessions/{id}/attendance)
 *  - POST /api/attendance/session/{id}/all (đánh dấu tất cả có mặt)
 *  - GET  /api/attendance/session/{id}/logs
 */
@WebServlet(name = "AttendanceApiServlet", urlPatterns = {
        "/api/attendance/*",
        "/api/sessions/*/attendance",
        "/api/sessions/*/attendance/logs"
})
public class AttendanceApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = GsonProvider.getGson();

    private AttendanceDAO attendanceDAO;

    @Override
    public void init() throws ServletException {
        this.attendanceDAO = new AttendanceDAO();
    }

    public void setAttendanceDAO(AttendanceDAO attendanceDAO) {
        this.attendanceDAO = attendanceDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = getCurrentUser(req);
        Long sessionId = extractSessionId(req);

        if (sessionId == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_SESSION_ID", "Mã buổi học không hợp lệ.");
            return;
        }

        String uri = req.getRequestURI();
        try {
            if (uri.endsWith("/logs")) {
                List<AttendanceAuditLog> logs = attendanceDAO.getAuditLogs(sessionId);
                ApiResponse.success(resp, "SUCCESS", "Lấy nhật ký sửa điểm danh thành công", logs);
            } else {
                AttendanceSheetResponse sheet = attendanceDAO.getAttendanceSheet(sessionId, currentUser, LocalDate.now());
                if (sheet == null) {
                    ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy buổi học.");
                    return;
                }
                ApiResponse.success(resp, "SUCCESS", "Lấy chi tiết bảng điểm danh thành công", sheet);
            }
        } catch (SQLException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi truy vấn CSDL: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doSaveOrBatch(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doSaveOrBatch(req, resp);
    }

    private void doSaveOrBatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = getCurrentUser(req);
        Long sessionId = extractSessionId(req);

        if (sessionId == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_SESSION_ID", "Mã buổi học không hợp lệ.");
            return;
        }

        String uri = req.getRequestURI();
        try {
            // AC2: Endpoint đánh dấu tất cả có mặt
            if (uri.endsWith("/all") || uri.endsWith("/all-present")) {
                AttendanceSheetResponse result = attendanceDAO.markAllPresent(sessionId, currentUser, LocalDate.now());
                ApiResponse.success(resp, "SUCCESS", "Đã đánh dấu tất cả học viên có mặt thành công", result);
                return;
            }

            // Đọc body JSON
            String jsonBody = readRequestBody(req);
            SaveAttendanceRequest request;
            if (jsonBody != null && !jsonBody.trim().isEmpty()) {
                request = GSON.fromJson(jsonBody, SaveAttendanceRequest.class);
            } else {
                request = new SaveAttendanceRequest();
            }

            if (request.getSessionId() == null) {
                request.setSessionId(sessionId);
            }

            AttendanceSheetResponse result = attendanceDAO.saveAttendance(request, currentUser, LocalDate.now());
            ApiResponse.success(resp, "SUCCESS", "Lưu bảng điểm danh thành công", result);

        } catch (SecurityException e) {
            // AC3: Bị chặn quyền sửa buổi quá khứ
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            // AC1, AC3: Lỗi validate dữ liệu hoặc trạng thái không hợp lệ
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL khi lưu điểm danh: " + e.getMessage());
        }
    }

    private Long extractSessionId(HttpServletRequest req) {
        String uri = req.getRequestURI();
        // 1. Dạng /api/sessions/{id}/attendance[/logs]
        if (uri.contains("/api/sessions/")) {
            String sub = uri.substring(uri.indexOf("/api/sessions/") + "/api/sessions/".length());
            String idStr = sub.split("/")[0];
            try {
                return Long.parseLong(idStr);
            } catch (NumberFormatException ignored) {}
        }

        // 2. Dạng /api/attendance/session/{id}[/all|logs]
        if (uri.contains("/session/")) {
            String sub = uri.substring(uri.indexOf("/session/") + "/session/".length());
            String idStr = sub.split("/")[0];
            try {
                return Long.parseLong(idStr);
            } catch (NumberFormatException ignored) {}
        }

        // 3. Query param ?sessionId=...
        String param = req.getParameter("sessionId");
        if (param != null && !param.trim().isEmpty()) {
            try {
                return Long.parseLong(param.trim());
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            User u = (User) session.getAttribute("currentUser");
            if (u != null) return u;
            User authUser = (User) session.getAttribute("authUser");
            if (authUser != null) return authUser;
        }

        // Người dùng mặc định nếu không đăng nhập session (Giảng viên dạy lớp)
        User defaultUser = new User(2L, "VU DUC LONG", "long.vd@tms.vn", "ACTIVE", List.of(RoleConstant.INSTRUCTOR));
        defaultUser.setUserCode("GV-002");
        return defaultUser;
    }

    private String readRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
