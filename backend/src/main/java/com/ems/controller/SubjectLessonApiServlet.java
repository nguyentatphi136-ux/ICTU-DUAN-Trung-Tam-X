package com.ems.controller;

import com.ems.dto.CloneLessonsRequest;
import com.ems.dto.SubjectLessonRequest;
import com.ems.dto.SubjectLessonResponse;
import com.ems.exception.ApiException;
import com.ems.security.ApiResponse;
import com.ems.service.SubjectLessonService;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API Servlet xử lý Khai báo Buổi học & Nhân bản buổi học trong một môn
 * User Story S2-07: Khai báo các buổi học trong một môn
 *
 * Endpoints:
 * - GET    /api/subjects/{subjectId}/sessions              : Danh sách buổi học
 * - POST   /api/subjects/{subjectId}/sessions              : Thêm buổi học (giới hạn không quá số buổi)
 * - PUT    /api/subjects/{subjectId}/sessions/{sessionId}  : Cập nhật buổi học
 * - DELETE /api/subjects/{subjectId}/sessions/{sessionId}  : Xoá buổi học
 * - POST   /api/subjects/{subjectId}/sessions/clone        : Nhân bản danh sách buổi từ môn khác
 */
@WebServlet(name = "SubjectLessonApiServlet", urlPatterns = {"/api/subjects/*/sessions", "/api/subjects/*/sessions/*"})
public class SubjectLessonApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/subjects/(\\d+)/sessions(?:/(.*))?$");

    private SubjectLessonService subjectLessonService;

    @Override
    public void init() throws ServletException {
        this.subjectLessonService = new SubjectLessonService();
    }

    public void setSubjectLessonService(SubjectLessonService subjectLessonService) {
        this.subjectLessonService = subjectLessonService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        Matcher matcher = PATH_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Đường dẫn không hợp lệ.");
            return;
        }

        int subjectId = Integer.parseInt(matcher.group(1));
        try {
            List<SubjectLessonResponse> list = subjectLessonService.getLessons(subjectId);
            ApiResponse.success(response, "LESSONS_GET_SUCCESS", "Lấy danh sách buổi học thành công.", list);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        Matcher matcher = PATH_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Đường dẫn không hợp lệ.");
            return;
        }

        int subjectId = Integer.parseInt(matcher.group(1));
        String subPath = matcher.group(2);

        try {
            // Case 1: Nhân bản từ môn khác /clone
            if ("clone".equalsIgnoreCase(subPath)) {
                CloneLessonsRequest req = GSON.fromJson(request.getReader(), CloneLessonsRequest.class);
                List<SubjectLessonResponse> cloned = subjectLessonService.cloneLessons(subjectId, req);
                ApiResponse.success(response, "LESSONS_CLONE_SUCCESS", "Nhân bản buổi học thành công.", cloned);
                return;
            }

            // Case 2: Thêm mới buổi học
            if (subPath == null || subPath.isEmpty()) {
                SubjectLessonRequest req = GSON.fromJson(request.getReader(), SubjectLessonRequest.class);
                SubjectLessonResponse created = subjectLessonService.addLesson(subjectId, req);
                ApiResponse.created(response, "LESSON_CREATE_SUCCESS", "Thêm buổi học thành công.", created);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint không tồn tại.");
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        Matcher matcher = PATH_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Đường dẫn không hợp lệ.");
            return;
        }

        String subPath = matcher.group(2);
        if (subPath == null || !subPath.matches("^\\d+$")) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Cần truyền ID buổi học cần sửa.");
            return;
        }

        int lessonId = Integer.parseInt(subPath);
        try {
            SubjectLessonRequest req = GSON.fromJson(request.getReader(), SubjectLessonRequest.class);
            SubjectLessonResponse updated = subjectLessonService.updateLesson(lessonId, req);
            ApiResponse.success(response, "LESSON_UPDATE_SUCCESS", "Cập nhật buổi học thành công.", updated);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        Matcher matcher = PATH_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Đường dẫn không hợp lệ.");
            return;
        }

        String subPath = matcher.group(2);
        if (subPath == null || !subPath.matches("^\\d+$")) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Cần truyền ID buổi học cần xoá.");
            return;
        }

        int lessonId = Integer.parseInt(subPath);
        try {
            subjectLessonService.deleteLesson(lessonId);
            ApiResponse.success(response, "LESSON_DELETE_SUCCESS", "Xoá buổi học thành công.", null);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }
}
