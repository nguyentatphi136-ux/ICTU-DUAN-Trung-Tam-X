package com.ems.controller;

import com.ems.dto.TrainingProgramPageResponse;
import com.ems.dto.TrainingProgramRequest;
import com.ems.dto.TrainingProgramResponse;
import com.ems.exception.ApiException;
import com.ems.security.ApiResponse;
import com.ems.service.TrainingProgramService;
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
 * REST API Servlet xử lý CRUD Chương trình đào tạo
 * User Story S2-04: Quản lý danh mục chương trình đào tạo
 *
 * Endpoints:
 * - GET    /api/training-programs                 : Lấy danh sách (phân trang, tìm kiếm)
 * - GET    /api/training-programs/{id}            : Xem chi tiết chương trình
 * - POST   /api/training-programs                 : Tạo mới chương trình
 * - PUT    /api/training-programs/{id}            : Cập nhật chương trình
 * - DELETE /api/training-programs/{id}            : Xoá chương trình (chặn khi có lớp đang chạy)
 * - PATCH  /api/training-programs/{id}/deactivate : Ngừng áp dụng chương trình
 */
@WebServlet(name = "TrainingProgramApiServlet", urlPatterns = {"/api/training-programs", "/api/training-programs/*"})
public class TrainingProgramApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private static final Pattern ID_PATTERN = Pattern.compile("^/(\\d+)$");
    private static final Pattern DEACTIVATE_PATTERN = Pattern.compile("^/(\\d+)/deactivate$");

    private TrainingProgramService trainingProgramService;

    @Override
    public void init() throws ServletException {
        this.trainingProgramService = new TrainingProgramService();
    }

    public void setTrainingProgramService(TrainingProgramService trainingProgramService) {
        this.trainingProgramService = trainingProgramService;
    }

    /**
     * GET: Lấy danh sách hoặc xem chi tiết
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);

        try {
            // Case 1: GET /api/training-programs/{id}
            Matcher idMatcher = ID_PATTERN.matcher(pathInfo);
            if (idMatcher.matches()) {
                int id = Integer.parseInt(idMatcher.group(1));
                TrainingProgramResponse detail = trainingProgramService.getById(id);
                ApiResponse.success(response, "PROGRAM_DETAIL_SUCCESS",
                        "Lấy chi tiết chương trình đào tạo thành công.", detail);
                return;
            }

            // Case 2: GET /api/training-programs (List & Search)
            if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
                String keyword = request.getParameter("keyword");
                if (keyword == null) keyword = request.getParameter("q");
                if (keyword == null) keyword = request.getParameter("search");

                String status = request.getParameter("status");

                int page = 1;
                int pageSize = 10;
                try {
                    if (request.getParameter("page") != null) {
                        page = Integer.parseInt(request.getParameter("page"));
                    }
                    if (request.getParameter("size") != null) {
                        pageSize = Integer.parseInt(request.getParameter("size"));
                    } else if (request.getParameter("pageSize") != null) {
                        pageSize = Integer.parseInt(request.getParameter("pageSize"));
                    } else if (request.getParameter("limit") != null) {
                        pageSize = Integer.parseInt(request.getParameter("limit"));
                    }
                } catch (NumberFormatException ignored) {}

                TrainingProgramPageResponse result = trainingProgramService.getList(keyword, status, page, pageSize);
                ApiResponse.success(response, "PROGRAM_LIST_SUCCESS",
                        "Lấy danh sách chương trình đào tạo thành công.", result);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Đường dẫn API không tồn tại.");
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Lỗi máy chủ ngoài dự kiến: " + e.getMessage());
        }
    }

    /**
     * POST: Tạo mới chương trình hoặc Deactivate
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);

        try {
            // Hỗ trợ POST /api/training-programs/{id}/deactivate (cho client không hỗ trợ PATCH)
            Matcher deactivateMatcher = DEACTIVATE_PATTERN.matcher(pathInfo);
            if (deactivateMatcher.matches()) {
                int id = Integer.parseInt(deactivateMatcher.group(1));
                TrainingProgramResponse res = trainingProgramService.deactivate(id);
                ApiResponse.success(response, "PROGRAM_DEACTIVATED",
                        "Ngừng áp dụng chương trình đào tạo thành công.", res);
                return;
            }

            // POST /api/training-programs (Tạo mới)
            if (pathInfo.isEmpty() || "/".equals(pathInfo)) {
                TrainingProgramRequest reqBody = parseRequestBody(request);
                TrainingProgramResponse created = trainingProgramService.create(reqBody);
                ApiResponse.created(response, "PROGRAM_CREATED",
                        "Tạo mới chương trình đào tạo thành công.", created);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Đường dẫn API không tồn tại.");
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            // Bắt thêm UNIQUE constraint vi phạm từ DB nếu có
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                ApiResponse.error(response, HttpServletResponse.SC_CONFLICT,
                        "PROGRAM_CODE_CONFLICT", "Mã chương trình đào tạo đã tồn tại trong cơ sở dữ liệu.");
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
            }
        } catch (Exception e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Lỗi máy chủ: " + e.getMessage());
        }
    }

    /**
     * PUT: Cập nhật thông tin chương trình
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);

        try {
            Matcher idMatcher = ID_PATTERN.matcher(pathInfo);
            if (idMatcher.matches()) {
                int id = Integer.parseInt(idMatcher.group(1));
                TrainingProgramRequest reqBody = parseRequestBody(request);
                TrainingProgramResponse updated = trainingProgramService.update(id, reqBody);
                ApiResponse.success(response, "PROGRAM_UPDATED",
                        "Cập nhật chương trình đào tạo thành công.", updated);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Đường dẫn cập nhật không hợp lệ.");
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                ApiResponse.error(response, HttpServletResponse.SC_CONFLICT,
                        "PROGRAM_CODE_CONFLICT", "Mã chương trình đào tạo đã tồn tại trong cơ sở dữ liệu.");
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
            }
        } catch (Exception e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Lỗi máy chủ: " + e.getMessage());
        }
    }

    /**
     * DELETE: Xoá chương trình đào tạo (bảo vệ khi có lớp đang chạy)
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);

        try {
            Matcher idMatcher = ID_PATTERN.matcher(pathInfo);
            if (idMatcher.matches()) {
                int id = Integer.parseInt(idMatcher.group(1));
                trainingProgramService.delete(id);
                ApiResponse.success(response, "PROGRAM_DELETED",
                        "Xoá chương trình đào tạo thành công.", null);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Đường dẫn xoá không hợp lệ.");
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Lỗi máy chủ: " + e.getMessage());
        }
    }

    /**
     * Hỗ trợ HTTP PATCH: Ngừng áp dụng chương trình
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = getCleanPathInfo(request);

        try {
            Matcher deactivateMatcher = DEACTIVATE_PATTERN.matcher(pathInfo);
            if (deactivateMatcher.matches()) {
                int id = Integer.parseInt(deactivateMatcher.group(1));
                TrainingProgramResponse res = trainingProgramService.deactivate(id);
                ApiResponse.success(response, "PROGRAM_DEACTIVATED",
                        "Ngừng áp dụng chương trình đào tạo thành công.", res);
                return;
            }

            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Đường dẫn PATCH không hợp lệ.");
        } catch (ApiException e) {
            ApiResponse.error(response, e.getStatusCode(), e.getErrorCode(), e.getMessage());
        } catch (SQLException e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "DATABASE_ERROR", "Lỗi cơ sở dữ liệu: " + e.getMessage());
        } catch (Exception e) {
            ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Lỗi máy chủ: " + e.getMessage());
        }
    }

    private String getCleanPathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? "" : pathInfo.trim();
    }

    private TrainingProgramRequest parseRequestBody(HttpServletRequest request) {
        try {
            TrainingProgramRequest req = GSON.fromJson(request.getReader(), TrainingProgramRequest.class);
            if (req == null) {
                throw new com.ems.exception.ValidationException("Nội dung yêu cầu JSON không được để trống.");
            }
            return req;
        } catch (JsonSyntaxException e) {
            throw new com.ems.exception.ValidationException("Định dạng dữ liệu JSON không hợp lệ: " + e.getMessage());
        } catch (IOException e) {
            throw new com.ems.exception.ValidationException("Không thể đọc dữ liệu gửi lên: " + e.getMessage());
        }
    }
}
