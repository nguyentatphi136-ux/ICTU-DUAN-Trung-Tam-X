package com.ems.controller;

import com.ems.dto.SubjectPageResponse;
import com.ems.dto.SubjectRequest;
import com.ems.dto.SubjectResponse;
import com.ems.exception.ApiException;
import com.ems.security.ApiResponse;
import com.ems.service.SubjectService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST API Servlet xử lý CRUD Môn học
 * User Story S2-05: Quản lý danh mục môn học
 *
 * Endpoints:
 * - GET    /api/subjects        : Lấy danh sách (phân trang, tìm kiếm)
 * - GET    /api/subjects/{id}   : Chi tiết môn học
 * - POST   /api/subjects        : Tạo mới môn học
 * - PUT    /api/subjects/{id}   : Cập nhật môn học
 * - DELETE /api/subjects/{id}   : Xoá môn học (chặn khi có lớp đã mở)
 */
@WebServlet(name = "SubjectApiServlet", urlPatterns = {"/api/subjects", "/api/subjects/*"})
public class SubjectApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Pattern ID_PATTERN = Pattern.compile("^/(\\d+)$");

    private SubjectService subjectService;

    @Override
    public void init() throws ServletException {
        this.subjectService = new SubjectService();
    }

    public void setSubjectService(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);

        try {
            Matcher idMatcher = ID_PATTERN.matcher(pathInfo);
            if (idMatcher.matches()) {
                int id = Integer.parseInt(idMatcher.group(1));
                SubjectResponse detail = subjectService.getById(id);
                ApiResponse.success(response, "SUBJECT_DETAIL_SUCCESS", "Lấy chi tiết môn học thành công.", detail);
                return;
            }

            if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
                String keyword = request.getParameter("keyword");
                if (keyword == null) keyword = request.getParameter("q");
                if (keyword == null) keyword = request.getParameter("search");

                int page = 1;
                int pageSize = 10;
                try {
                    if (request.getParameter("page") != null) page = Integer.parseInt(request.getParameter("page"));
                    if (request.getParameter("pageSize") != null) pageSize = Integer.parseInt(request.getParameter("pageSize"));
                    else if (request.getParameter("size") != null) pageSize = Integer.parseInt(request.getParameter("size"));
                } catch (NumberFormatException ignored) {}

                SubjectPageResponse result = subjectService.getList(keyword, page, pageSize);
                ApiResponse.success(response, "SUBJECT_LIST_SUCCESS", "Lấy danh sách môn học thành công.", result);
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);
        if (!pathInfo.isEmpty() && !"/".equals(pathInfo)) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint không tồn tại.");
            return;
        }

        try {
            SubjectRequest reqBody = parseRequestBody(request);
            SubjectResponse created = subjectService.create(reqBody);
            ApiResponse.created(response, "SUBJECT_CREATE_SUCCESS", "Tạo mới môn học thành công.", created);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);
        Matcher idMatcher = ID_PATTERN.matcher(pathInfo);
        if (!idMatcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", "Đường dẫn không hợp lệ.");
            return;
        }

        try {
            int id = Integer.parseInt(idMatcher.group(1));
            SubjectRequest reqBody = parseRequestBody(request);
            SubjectResponse updated = subjectService.update(id, reqBody);
            ApiResponse.success(response, "SUBJECT_UPDATE_SUCCESS", "Cập nhật môn học thành công.", updated);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);
        Matcher idMatcher = ID_PATTERN.matcher(pathInfo);
        if (!idMatcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", "Đường dẫn không hợp lệ.");
            return;
        }

        try {
            int id = Integer.parseInt(idMatcher.group(1));
            subjectService.delete(id);
            ApiResponse.success(response, "SUBJECT_DELETE_SUCCESS", "Xoá môn học thành công.", null);
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL: " + e.getMessage());
        }
    }

    private String getCleanPathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return (pathInfo == null) ? "" : pathInfo.trim();
    }

    private SubjectRequest parseRequestBody(HttpServletRequest request) throws IOException {
        try {
            return GSON.fromJson(request.getReader(), SubjectRequest.class);
        } catch (JsonSyntaxException e) {
            throw new com.ems.exception.ValidationException("Định dạng JSON yêu cầu không hợp lệ: " + e.getMessage());
        }
    }
}
