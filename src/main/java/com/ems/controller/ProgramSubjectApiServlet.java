package com.ems.controller;

import com.ems.dto.ProgramSubjectRequest;
import com.ems.dto.ProgramSubjectResponse;
import com.ems.dto.ReorderSubjectsRequest;
import com.ems.exception.ApiException;
import com.ems.security.ApiResponse;
import com.ems.service.ProgramSubjectService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
 * REST API Servlet xử lý Gắn môn học vào chương trình đào tạo & Sắp xếp thứ tự
 * User Story S2-06: Gắn môn học vào chương trình và sắp thứ tự học
 *
 * Endpoints:
 * - GET    /api/programs/{programId}/subjects                       : Danh sách môn trong chương trình
 * - POST   /api/programs/{programId}/subjects                       : Thêm môn vào chương trình
 * - PUT    /api/programs/{programId}/subjects/reorder               : Đổi thứ tự học các môn (kéo thả)
 * - PUT    /api/programs/{programId}/subjects/{subjectId}/prereq    : Gán môn tiên quyết
 * - DELETE /api/programs/{programId}/subjects/{subjectId}           : Gỡ môn khỏi chương trình
 */
@WebServlet(name = "ProgramSubjectApiServlet", urlPatterns = {"/api/programs/*/subjects", "/api/programs/*/subjects/*"})
public class ProgramSubjectApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/programs/(\\d+)/subjects(?:/(.*))?$");

    private ProgramSubjectService programSubjectService;

    @Override
    public void init() throws ServletException {
        this.programSubjectService = new ProgramSubjectService();
    }

    public void setProgramSubjectService(ProgramSubjectService programSubjectService) {
        this.programSubjectService = programSubjectService;
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

        int programId = Integer.parseInt(matcher.group(1));
        try {
            List<ProgramSubjectResponse> curriculum = programSubjectService.getCurriculum(programId);
            ApiResponse.success(response, "CURRICULUM_GET_SUCCESS", "Lấy lộ trình môn học thành công.", curriculum);
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

        int programId = Integer.parseInt(matcher.group(1));
        try {
            ProgramSubjectRequest req = GSON.fromJson(request.getReader(), ProgramSubjectRequest.class);
            List<ProgramSubjectResponse> updated = programSubjectService.addSubject(programId, req);
            ApiResponse.created(response, "SUBJECT_ADDED_SUCCESS", "Gắn môn học vào chương trình thành công.", updated);
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

        int programId = Integer.parseInt(matcher.group(1));
        String subPath = matcher.group(2);

        try {
            // Case 1: /reorder
            if ("reorder".equalsIgnoreCase(subPath)) {
                ReorderSubjectsRequest req = GSON.fromJson(request.getReader(), ReorderSubjectsRequest.class);
                List<ProgramSubjectResponse> updated = programSubjectService.reorder(programId, req);
                ApiResponse.success(response, "SUBJECT_REORDER_SUCCESS", "Sắp xếp lại thứ tự học thành công.", updated);
                return;
            }

            // Case 2: /{subjectId}/prereq
            if (subPath != null && subPath.matches("^\\d+/prereq$")) {
                int subjectId = Integer.parseInt(subPath.split("/")[0]);
                ProgramSubjectRequest req = GSON.fromJson(request.getReader(), ProgramSubjectRequest.class);
                List<ProgramSubjectResponse> updated = programSubjectService.updatePrerequisite(
                        programId, subjectId, req.getPrerequisiteSubjectId());
                ApiResponse.success(response, "PREREQ_UPDATE_SUCCESS", "Cập nhật môn tiên quyết thành công.", updated);
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
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        Matcher matcher = PATH_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Đường dẫn không hợp lệ.");
            return;
        }

        int programId = Integer.parseInt(matcher.group(1));
        String subPath = matcher.group(2);
        if (subPath == null || !subPath.matches("^\\d+$")) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_PATH", "Cần truyền ID môn học để gỡ khỏi chương trình.");
            return;
        }

        try {
            int subjectId = Integer.parseInt(subPath);
            List<ProgramSubjectResponse> updated = programSubjectService.removeSubject(programId, subjectId);
            ApiResponse.success(response, "SUBJECT_REMOVED_SUCCESS", "Gỡ môn học khỏi chương trình thành công.", updated);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }
}
