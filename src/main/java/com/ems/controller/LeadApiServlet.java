package com.ems.controller;

import com.ems.constant.RoleConstant;
import com.ems.dao.LeadDAO;
import com.ems.model.Lead;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.ems.service.LeadService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller RESTful API xử lý CRUD cho Khách hàng tiềm năng (Leads)
 * Sprint 2 - User Story S2-09 [IDTTX-45]
 * URL Patterns: /api/leads, /api/leads/*
 */
@WebServlet(name = "LeadApiServlet", urlPatterns = {"/api/leads", "/api/leads/*"})
public class LeadApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Pattern LEAD_ID_PATTERN = Pattern.compile("^/(\\d+)$");

    private final LeadService leadService;

    public LeadApiServlet() {
        this.leadService = new LeadService(new LeadDAO());
    }

    public LeadApiServlet(LeadService leadService) {
        this.leadService = leadService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để truy cập danh sách khách hàng tiềm năng.", "Đăng nhập", "/login.html");
            return;
        }

        if (!LeadService.canReadLead(currentUser)) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN",
                    "Bạn không có quyền xem thông tin khách hàng tiềm năng.", "Về trang chủ", "/index.html");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            handleListLeads(request, response);
            return;
        }

        if ("/check-phone".equalsIgnoreCase(pathInfo)) {
            handleCheckPhone(request, response);
            return;
        }

        Matcher matcher = LEAD_ID_PATTERN.matcher(pathInfo);
        if (matcher.matches()) {
            long leadId = Long.parseLong(matcher.group(1));
            handleGetLeadDetail(leadId, response);
            return;
        }

        ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                "Đường dẫn API không tồn tại.", "Quay lại", "/index.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện chức năng này.", "Đăng nhập", "/login.html");
            return;
        }

        // Quyền tạo lead: Admissions, TrainingManager, Admin
        if (!LeadService.canCreateLead(currentUser)) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "LEAD_CREATE_FORBIDDEN",
                    "Bạn không có quyền tạo khách hàng tiềm năng mới.", "Về trang chủ", "/index.html");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.isEmpty()) {
            ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND",
                    "Đường dẫn API không hợp lệ cho phương thức POST.", "Quay lại", "/index.html");
            return;
        }

        JsonObject body;
        try {
            body = GSON.fromJson(request.getReader(), JsonObject.class);
            if (body == null) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_INVALID_JSON",
                        "Dữ liệu JSON gửi lên không được để trống.", "Thử lại", "/admissions.html");
                return;
            }
        } catch (JsonSyntaxException e) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_INVALID_JSON",
                    "Định dạng JSON gửi lên không hợp lệ.", "Thử lại", "/admissions.html");
            return;
        }

        Lead lead = new Lead();
        if (body.has("fullName") && !body.get("fullName").isJsonNull()) {
            lead.setFullName(body.get("fullName").getAsString());
        }
        if (body.has("phone") && !body.get("phone").isJsonNull()) {
            lead.setPhone(body.get("phone").getAsString());
        }
        if (body.has("email") && !body.get("email").isJsonNull()) {
            lead.setEmail(body.get("email").getAsString());
        }
        if (body.has("source") && !body.get("source").isJsonNull()) {
            lead.setSource(body.get("source").getAsString());
        }
        if (body.has("programId") && !body.get("programId").isJsonNull()) {
            lead.setProgramId(body.get("programId").getAsInt());
        }
        if (body.has("programInterest") && !body.get("programInterest").isJsonNull()) {
            lead.setProgramInterest(body.get("programInterest").getAsString());
        }
        if (body.has("status") && !body.get("status").isJsonNull()) {
            lead.setStatus(body.get("status").getAsString());
        }
        if (body.has("notes") && !body.get("notes").isJsonNull()) {
            lead.setNotes(body.get("notes").getAsString());
        }
        if (body.has("assignedTo") && !body.get("assignedTo").isJsonNull()) {
            lead.setAssignedTo(body.get("assignedTo").getAsLong());
        }

        boolean forceDuplicate = body.has("forceDuplicate") && body.get("forceDuplicate").getAsBoolean();

        // 1. Kiểm tra validation
        List<String> errors = leadService.validateLead(lead, false);
        if (!errors.isEmpty()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_VALIDATION_ERROR",
                    String.join(" ", errors), "Sửa thông tin", "/admissions.html");
            return;
        }

        // 2. Xử lý trùng lặp số điện thoại (Duplicate phone)
        try {
            Lead existingDuplicate = leadService.checkDuplicatePhone(lead.getPhone(), null);
            if (existingDuplicate != null && !forceDuplicate) {
                String warningMsg = "Số điện thoại '" + lead.getPhone() + "' đã tồn tại trong hệ thống (Lead #"
                        + existingDuplicate.getId() + " - " + existingDuplicate.getFullName() + ").";

                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(GSON.toJson(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "DUPLICATE_PHONE",
                                "message", warningMsg,
                                "duplicateLead", existingDuplicate,
                                "action", Map.of("label", "Xem lead trùng", "href", "/admissions.html?id=" + existingDuplicate.getId())
                        )
                )));
                return;
            }

            lead.setCreatedBy(currentUser.getId());
            lead.setUpdatedBy(currentUser.getId());

            long newId = leadService.createLead(lead);
            Lead createdLead = leadService.findById(newId);

            ApiResponse.success(response, HttpServletResponse.SC_CREATED, "LEAD_CREATED",
                    "Thêm khách hàng tiềm năng thành công.", createdLead);
        } catch (SQLException e) {
            getServletContext().log("Lỗi tạo mới lead", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi lưu khách hàng tiềm năng: " + e.getMessage(), "Thử lại sau", "/admissions.html");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện chức năng này.", "Đăng nhập", "/login.html");
            return;
        }

        // Quyền sửa lead: Admissions, TrainingManager, Admin
        if (!LeadService.canUpdateLead(currentUser)) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "LEAD_UPDATE_FORBIDDEN",
                    "Bạn không có quyền cập nhật thông tin khách hàng tiềm năng.", "Về trang chủ", "/index.html");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) pathInfo = "";
        Matcher matcher = LEAD_ID_PATTERN.matcher(pathInfo);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_INVALID_ID",
                    "ID khách hàng tiềm năng không hợp lệ trên đường dẫn.", "Quay lại", "/admissions.html");
            return;
        }

        long leadId = Long.parseLong(matcher.group(1));

        try {
            Lead existingLead = leadService.findById(leadId);
            if (existingLead == null) {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "LEAD_NOT_FOUND",
                        "Không tìm thấy khách hàng tiềm năng với ID: " + leadId, "Quay lại", "/admissions.html");
                return;
            }

            JsonObject body;
            try {
                body = GSON.fromJson(request.getReader(), JsonObject.class);
                if (body == null) {
                    ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_INVALID_JSON",
                            "Dữ liệu cập nhật không được để trống.", "Thử lại", "/admissions.html");
                    return;
                }
            } catch (JsonSyntaxException e) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_INVALID_JSON",
                        "Định dạng JSON gửi lên không hợp lệ.", "Thử lại", "/admissions.html");
                return;
            }

            if (body.has("fullName") && !body.get("fullName").isJsonNull()) {
                existingLead.setFullName(body.get("fullName").getAsString());
            }
            if (body.has("phone") && !body.get("phone").isJsonNull()) {
                existingLead.setPhone(body.get("phone").getAsString());
            }
            if (body.has("email")) {
                existingLead.setEmail(body.get("email").isJsonNull() ? null : body.get("email").getAsString());
            }
            if (body.has("source") && !body.get("source").isJsonNull()) {
                existingLead.setSource(body.get("source").getAsString());
            }
            if (body.has("programId")) {
                existingLead.setProgramId(body.get("programId").isJsonNull() ? null : body.get("programId").getAsInt());
            }
            if (body.has("programInterest")) {
                existingLead.setProgramInterest(body.get("programInterest").isJsonNull() ? null : body.get("programInterest").getAsString());
            }
            if (body.has("status") && !body.get("status").isJsonNull()) {
                existingLead.setStatus(body.get("status").getAsString());
            }
            if (body.has("notes")) {
                existingLead.setNotes(body.get("notes").isJsonNull() ? null : body.get("notes").getAsString());
            }
            if (body.has("assignedTo")) {
                existingLead.setAssignedTo(body.get("assignedTo").isJsonNull() ? null : body.get("assignedTo").getAsLong());
            }

            boolean forceDuplicate = body.has("forceDuplicate") && body.get("forceDuplicate").getAsBoolean();

            // 1. Kiểm tra validation
            List<String> errors = leadService.validateLead(existingLead, true);
            if (!errors.isEmpty()) {
                ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_VALIDATION_ERROR",
                        String.join(" ", errors), "Sửa thông tin", "/admissions.html");
                return;
            }

            // 2. Kiểm tra trùng số điện thoại với lead khác
            Lead existingDuplicate = leadService.checkDuplicatePhone(existingLead.getPhone(), leadId);
            if (existingDuplicate != null && !forceDuplicate) {
                String warningMsg = "Số điện thoại '" + existingLead.getPhone() + "' đã được sử dụng bởi Lead #"
                        + existingDuplicate.getId() + " (" + existingDuplicate.getFullName() + ").";

                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(GSON.toJson(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "DUPLICATE_PHONE",
                                "message", warningMsg,
                                "duplicateLead", existingDuplicate,
                                "action", Map.of("label", "Xem lead trùng", "href", "/admissions.html?id=" + existingDuplicate.getId())
                        )
                )));
                return;
            }

            existingLead.setUpdatedBy(currentUser.getId());
            boolean updated = leadService.updateLead(existingLead);

            if (updated) {
                Lead refreshed = leadService.findById(leadId);
                ApiResponse.success(response, "LEAD_UPDATED",
                        "Cập nhật thông tin khách hàng tiềm năng thành công.", refreshed);
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LEAD_UPDATE_FAILED",
                        "Cập nhật không thành công. Vui lòng thử lại sau.", "Thử lại", "/admissions.html");
            }
        } catch (SQLException e) {
            getServletContext().log("Lỗi cập nhật lead", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi cập nhật khách hàng tiềm năng: " + e.getMessage(), "Thử lại sau", "/admissions.html");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            ApiResponse.error(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện chức năng này.", "Đăng nhập", "/login.html");
            return;
        }

        // TIÊU CHÍ ĐẶC TẢ S2-09: "Chỉ Quản lý đào tạo được xoá lead" (Training Manager & Admin)
        // Tư vấn tuyển sinh (Admissions) TUYỆT ĐỐI KHÔNG có quyền xóa lead!
        if (currentUser.hasRole(RoleConstant.ADMISSIONS)
                && !currentUser.hasRole(RoleConstant.TRAINING_MANAGER)
                && !currentUser.hasRole(RoleConstant.ADMIN)) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "LEAD_DELETE_FORBIDDEN",
                    "Tư vấn tuyển sinh không có quyền xóa khách hàng tiềm năng. Chỉ Quản lý đào tạo hoặc Quản trị viên mới được phép xóa.",
                    "Về danh sách lead", "/admissions.html");
            return;
        }

        if (!LeadService.canDeleteLead(currentUser)) {
            ApiResponse.error(response, HttpServletResponse.SC_FORBIDDEN, "LEAD_DELETE_FORBIDDEN",
                    "Bạn không có quyền xóa khách hàng tiềm năng. Chỉ Quản lý đào tạo hoặc Quản trị viên mới có quyền này.",
                    "Về danh sách lead", "/admissions.html");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) pathInfo = "";
        Matcher matcher = LEAD_ID_PATTERN.matcher(pathInfo);
        if (!matcher.matches()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "LEAD_INVALID_ID",
                    "ID khách hàng tiềm năng không hợp lệ trên đường dẫn.", "Quay lại", "/admissions.html");
            return;
        }

        long leadId = Long.parseLong(matcher.group(1));

        try {
            Lead existingLead = leadService.findById(leadId);
            if (existingLead == null) {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "LEAD_NOT_FOUND",
                        "Không tìm thấy khách hàng tiềm năng với ID: " + leadId, "Quay lại", "/admissions.html");
                return;
            }

            boolean deleted = leadService.deleteLead(leadId);
            if (deleted) {
                ApiResponse.success(response, "LEAD_DELETED",
                        "Xóa khách hàng tiềm năng thành công.", Map.of("id", leadId, "fullName", existingLead.getFullName()));
            } else {
                ApiResponse.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LEAD_DELETE_FAILED",
                        "Xóa không thành công. Vui lòng thử lại sau.", "Thử lại", "/admissions.html");
            }
        } catch (SQLException e) {
            getServletContext().log("Lỗi xóa lead", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi xóa khách hàng tiềm năng: " + e.getMessage(), "Thử lại sau", "/admissions.html");
        }
    }

    private void handleListLeads(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String keyword = request.getParameter("search");
        if (keyword == null) keyword = request.getParameter("q");
        if (keyword == null) keyword = request.getParameter("keyword");

        String status = request.getParameter("status");
        String source = request.getParameter("source");

        Long assignedTo = null;
        if (request.getParameter("assignedTo") != null && !request.getParameter("assignedTo").isBlank()) {
            try {
                assignedTo = Long.parseLong(request.getParameter("assignedTo"));
            } catch (NumberFormatException ignored) {}
        }

        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) page = Integer.parseInt(request.getParameter("page"));
            if (request.getParameter("pageSize") != null) pageSize = Integer.parseInt(request.getParameter("pageSize"));
            if (request.getParameter("limit") != null) pageSize = Integer.parseInt(request.getParameter("limit"));
        } catch (NumberFormatException ignored) {}

        try {
            Map<String, Object> result = leadService.searchLeads(keyword, status, source, assignedTo, startDate, endDate, page, pageSize);
            ApiResponse.success(response, "LEAD_LIST_SUCCESS",
                    "Lấy danh sách khách hàng tiềm năng thành công.", result);
        } catch (SQLException e) {
            getServletContext().log("Lỗi tra cứu danh sách lead", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi tra cứu danh sách khách hàng: " + e.getMessage(), "Thử lại sau", "/admissions.html");
        }
    }

    private void handleGetLeadDetail(long leadId, HttpServletResponse response)
            throws IOException {
        try {
            Lead lead = leadService.findById(leadId);
            if (lead == null) {
                ApiResponse.error(response, HttpServletResponse.SC_NOT_FOUND, "LEAD_NOT_FOUND",
                        "Không tìm thấy khách hàng tiềm năng với ID: " + leadId, "Quay lại danh sách", "/admissions.html");
                return;
            }
            ApiResponse.success(response, "LEAD_DETAIL_SUCCESS",
                    "Lấy thông tin chi tiết khách hàng tiềm năng thành công.", lead);
        } catch (SQLException e) {
            getServletContext().log("Lỗi lấy chi tiết lead", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi lấy chi tiết khách hàng: " + e.getMessage(), "Thử lại sau", "/admissions.html");
        }
    }

    private void handleCheckPhone(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String phone = request.getParameter("phone");
        if (phone == null || phone.isBlank()) {
            ApiResponse.error(response, HttpServletResponse.SC_BAD_REQUEST, "PHONE_REQUIRED",
                    "Vui lòng cung cấp tham số 'phone' để kiểm tra.", "Kiểm tra lại", "/admissions.html");
            return;
        }

        Long excludeId = null;
        if (request.getParameter("excludeId") != null && !request.getParameter("excludeId").isBlank()) {
            try {
                excludeId = Long.parseLong(request.getParameter("excludeId"));
            } catch (NumberFormatException ignored) {}
        }

        try {
            Lead duplicate = leadService.checkDuplicatePhone(phone, excludeId);
            boolean isDuplicate = (duplicate != null);

            Map<String, Object> data = Map.of(
                    "phone", phone.trim(),
                    "isDuplicate", isDuplicate,
                    "duplicateLead", isDuplicate ? duplicate : Map.of()
            );

            ApiResponse.success(response, "PHONE_CHECK_SUCCESS",
                    isDuplicate ? "Số điện thoại đã tồn tại trong hệ thống." : "Số điện thoại chưa tồn tại, có thể sử dụng.",
                    data);
        } catch (SQLException e) {
            getServletContext().log("Lỗi kiểm tra số điện thoại", e);
            ApiResponse.error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "DATABASE_ERROR",
                    "Lỗi cơ sở dữ liệu khi kiểm tra số điện thoại: " + e.getMessage(), "Thử lại sau", "/admissions.html");
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userObj = session.getAttribute("currentUser");
            if (userObj instanceof User u) {
                return u;
            }
        }
        return null;
    }
}
