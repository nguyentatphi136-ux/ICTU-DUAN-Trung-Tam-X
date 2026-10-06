package com.ems.controller;

import com.ems.constant.RoleConstant;
import com.ems.dao.LeadDAO;
import com.ems.model.Lead;
import com.ems.model.User;
import com.ems.service.LeadService;

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

/**
 * Controller JSP & Servlet quản lý Khách hàng tiềm năng (Leads)
 * Đáp ứng đầy đủ tiêu chí chấp nhận S2-09 – EP-03 – Tư vấn tuyển sinh:
 * • Tạo và sửa lead với họ tên, số điện thoại, email, nguồn, chương trình quan tâm
 * • Cảnh báo khi số điện thoại trùng với lead đã có
 * • Chỉ Quản lý đào tạo (và Admin) được xoá lead (Tư vấn tuyển sinh TUYỆT ĐỐI không được xoá)
 */
@WebServlet(name = "LeadServlet", urlPatterns = {"/lead", "/lead/list", "/lead/add", "/lead/edit", "/lead/save", "/lead/delete"})
public class LeadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final LeadService leadService;

    public LeadServlet() {
        this.leadService = new LeadService(new LeadDAO());
    }

    public LeadServlet(LeadService leadService) {
        this.leadService = leadService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            req.setAttribute("errorMessage", "Vui lòng đăng nhập để tiếp tục.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        String uri = req.getRequestURI();

        // 1. Điều hướng Xóa Lead
        if (uri.endsWith("/lead/delete")) {
            handleDeleteLead(req, resp, currentUser);
            return;
        }

        // 2. Điều hướng Thêm mới Lead
        if (uri.endsWith("/lead/add")) {
            if (!LeadService.canCreateLead(currentUser)) {
                denyAccess(req, resp, "Bạn không có quyền tạo khách hàng tiềm năng mới.");
                return;
            }
            Lead lead = new Lead();
            lead.setSource(Lead.SOURCE_WEBSITE);
            lead.setStatus(Lead.STATUS_NEW);
            req.setAttribute("lead", lead);
            req.setAttribute("action", "add");
            req.setAttribute("validSources", Lead.VALID_SOURCES);
            req.setAttribute("validStatuses", Lead.VALID_STATUSES);
            req.getRequestDispatcher("/WEB-INF/views/lead/form-lead.jsp").forward(req, resp);
            return;
        }

        // 3. Điều hướng Sửa Lead
        if (uri.endsWith("/lead/edit")) {
            if (!LeadService.canUpdateLead(currentUser)) {
                denyAccess(req, resp, "Bạn không có quyền chỉnh sửa thông tin khách hàng tiềm năng.");
                return;
            }
            String idStr = req.getParameter("id");
            if (idStr == null || idStr.isBlank()) {
                resp.sendRedirect(req.getContextPath() + "/lead/list");
                return;
            }
            try {
                long leadId = Long.parseLong(idStr.trim());
                Lead lead = leadService.findById(leadId);
                if (lead == null) {
                    req.getSession().setAttribute("errorMessage", "Không tìm thấy khách hàng tiềm năng với mã #" + leadId);
                    resp.sendRedirect(req.getContextPath() + "/lead/list");
                    return;
                }
                req.setAttribute("lead", lead);
                req.setAttribute("action", "edit");
                req.setAttribute("validSources", Lead.VALID_SOURCES);
                req.setAttribute("validStatuses", Lead.VALID_STATUSES);
                req.getRequestDispatcher("/WEB-INF/views/lead/form-lead.jsp").forward(req, resp);
                return;
            } catch (NumberFormatException | SQLException e) {
                req.getSession().setAttribute("errorMessage", "Mã khách hàng không hợp lệ hoặc lỗi CSDL.");
                resp.sendRedirect(req.getContextPath() + "/lead/list");
                return;
            }
        }

        // 4. Mặc định: Danh sách Leads (/lead hoặc /lead/list)
        handleListLeads(req, resp, currentUser);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = getCurrentUser(req);
        if (currentUser == null) {
            req.setAttribute("errorMessage", "Vui lòng đăng nhập để tiếp tục.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        String uri = req.getRequestURI();

        if (uri.endsWith("/lead/delete")) {
            handleDeleteLead(req, resp, currentUser);
            return;
        }

        if (uri.endsWith("/lead/save")) {
            handleSaveLead(req, resp, currentUser);
            return;
        }

        handleListLeads(req, resp, currentUser);
    }

    /**
     * Xử lý lưu (Thêm mới hoặc Cập nhật) Lead
     */
    private void handleSaveLead(HttpServletRequest req, HttpServletResponse resp, User currentUser)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        boolean isUpdate = idStr != null && !idStr.isBlank() && !idStr.equals("0");
        Long leadId = isUpdate ? Long.parseLong(idStr.trim()) : null;

        // Kiểm tra quyền tạo/sửa
        if (isUpdate && !LeadService.canUpdateLead(currentUser)) {
            denyAccess(req, resp, "Bạn không có quyền chỉnh sửa thông tin khách hàng tiềm năng.");
            return;
        } else if (!isUpdate && !LeadService.canCreateLead(currentUser)) {
            denyAccess(req, resp, "Bạn không có quyền tạo khách hàng tiềm năng mới.");
            return;
        }

        String fullName = req.getParameter("fullName");
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        String source = req.getParameter("source");
        String programInterest = req.getParameter("programInterest");
        String status = req.getParameter("status");
        String notes = req.getParameter("notes");
        boolean forceDuplicate = "true".equalsIgnoreCase(req.getParameter("forceDuplicate"));

        Lead lead = new Lead();
        if (isUpdate) {
            lead.setId(leadId);
        }
        lead.setFullName(fullName != null ? fullName.trim() : "");
        lead.setPhone(phone != null ? phone.trim() : "");
        lead.setEmail((email != null && !email.isBlank()) ? email.trim() : null);
        lead.setSource((source != null && !source.isBlank()) ? source.trim() : Lead.SOURCE_WEBSITE);
        lead.setProgramInterest((programInterest != null && !programInterest.isBlank()) ? programInterest.trim() : null);
        lead.setStatus((status != null && !status.isBlank()) ? status.trim() : Lead.STATUS_NEW);
        lead.setNotes(notes != null ? notes.trim() : null);

        // 1. Kiểm tra validation các trường
        List<String> errors = leadService.validateLead(lead, isUpdate);
        if (!errors.isEmpty()) {
            req.setAttribute("errorMessage", String.join("<br>", errors));
            req.setAttribute("lead", lead);
            req.setAttribute("action", isUpdate ? "edit" : "add");
            req.setAttribute("validSources", Lead.VALID_SOURCES);
            req.setAttribute("validStatuses", Lead.VALID_STATUSES);
            req.getRequestDispatcher("/WEB-INF/views/lead/form-lead.jsp").forward(req, resp);
            return;
        }

        try {
            // 2. CẢNH BÁO TRÙNG LẶP SỐ ĐIỆN THOẠI (S2-09)
            Lead duplicateLead = leadService.checkDuplicatePhone(lead.getPhone(), leadId);
            if (duplicateLead != null && !forceDuplicate) {
                String warningMessage = "CẢNH BÁO TRÙNG SỐ ĐIỆN THOẠI: Số điện thoại '" + lead.getPhone()
                        + "' đã tồn tại trong hệ thống (Lead #" + duplicateLead.getId()
                        + " - " + duplicateLead.getFullName()
                        + ", Trạng thái: " + duplicateLead.getStatus() + ").";

                req.setAttribute("duplicateWarning", true);
                req.setAttribute("duplicateLead", duplicateLead);
                req.setAttribute("warningMessage", warningMessage);
                req.setAttribute("lead", lead);
                req.setAttribute("action", isUpdate ? "edit" : "add");
                req.setAttribute("validSources", Lead.VALID_SOURCES);
                req.setAttribute("validStatuses", Lead.VALID_STATUSES);
                req.getRequestDispatcher("/WEB-INF/views/lead/form-lead.jsp").forward(req, resp);
                return;
            }

            // 3. Thực hiện lưu CSDL
            lead.setUpdatedBy(currentUser.getId());
            if (!isUpdate) {
                lead.setCreatedBy(currentUser.getId());
                long newId = leadService.createLead(lead);
                req.getSession().setAttribute("successMessage", "Thêm mới khách hàng tiềm năng thành công (Mã #" + newId + ")!");
            } else {
                leadService.updateLead(lead);
                req.getSession().setAttribute("successMessage", "Cập nhật thông tin khách hàng tiềm năng #" + lead.getId() + " thành công!");
            }

            resp.sendRedirect(req.getContextPath() + "/lead/list");
        } catch (SQLException e) {
            getServletContext().log("Lỗi lưu khách hàng tiềm năng", e);
            req.setAttribute("errorMessage", "Lỗi cơ sở dữ liệu: " + e.getMessage());
            req.setAttribute("lead", lead);
            req.setAttribute("action", isUpdate ? "edit" : "add");
            req.setAttribute("validSources", Lead.VALID_SOURCES);
            req.setAttribute("validStatuses", Lead.VALID_STATUSES);
            req.getRequestDispatcher("/WEB-INF/views/lead/form-lead.jsp").forward(req, resp);
        }
    }

    /**
     * Xử lý Xóa Lead
     * TIÊU CHÍ S2-09: CHỈ Quản lý đào tạo (và Admin) được xóa lead. Tư vấn tuyển sinh TUYỆT ĐỐI không được xóa!
     */
    private void handleDeleteLead(HttpServletRequest req, HttpServletResponse resp, User currentUser)
            throws ServletException, IOException {
        // Kiểm tra nghiêm ngặt: Tư vấn tuyển sinh không có quyền xóa
        if (currentUser.hasRole(RoleConstant.ADMISSIONS)
                && !currentUser.hasRole(RoleConstant.TRAINING_MANAGER)
                && !currentUser.hasRole(RoleConstant.ADMIN)) {
            denyAccess(req, resp, "Tư vấn tuyển sinh không có quyền xóa khách hàng tiềm năng. Chỉ Quản lý đào tạo hoặc Quản trị viên mới được phép xóa.");
            return;
        }

        if (!LeadService.canDeleteLead(currentUser)) {
            denyAccess(req, resp, "Bạn không có quyền xóa khách hàng tiềm năng. Chỉ Quản lý đào tạo hoặc Quản trị viên mới có quyền này.");
            return;
        }

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/lead/list");
            return;
        }

        try {
            long leadId = Long.parseLong(idStr.trim());
            Lead lead = leadService.findById(leadId);
            if (lead == null) {
                req.getSession().setAttribute("errorMessage", "Không tìm thấy khách hàng cần xóa với mã #" + leadId);
                resp.sendRedirect(req.getContextPath() + "/lead/list");
                return;
            }

            boolean deleted = leadService.deleteLead(leadId);
            if (deleted) {
                req.getSession().setAttribute("successMessage", "Đã xóa thành công khách hàng tiềm năng: " + lead.getFullName() + " (Mã #" + leadId + ").");
            } else {
                req.getSession().setAttribute("errorMessage", "Không thể xóa khách hàng tiềm năng #" + leadId + ". Vui lòng thử lại.");
            }
            resp.sendRedirect(req.getContextPath() + "/lead/list");
        } catch (NumberFormatException | SQLException e) {
            getServletContext().log("Lỗi xóa khách hàng tiềm năng", e);
            req.getSession().setAttribute("errorMessage", "Lỗi cơ sở dữ liệu khi xóa lead: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/lead/list");
        }
    }

    /**
     * Xử lý tra cứu danh sách Lead
     */
    private void handleListLeads(HttpServletRequest req, HttpServletResponse resp, User currentUser)
            throws ServletException, IOException {
        String keyword = req.getParameter("keyword");
        if (keyword == null) keyword = req.getParameter("search");
        if (keyword == null) keyword = req.getParameter("q");

        String status = req.getParameter("status");
        String source = req.getParameter("source");

        int page = 1;
        int pageSize = 10;
        try {
            if (req.getParameter("page") != null) page = Integer.parseInt(req.getParameter("page"));
            if (req.getParameter("pageSize") != null) pageSize = Integer.parseInt(req.getParameter("pageSize"));
        } catch (NumberFormatException ignored) {}

        try {
            Map<String, Object> result = leadService.searchLeads(keyword, status, source, null, null, null, page, pageSize);

            req.setAttribute("leadList", result.get("items"));
            req.setAttribute("currentPage", result.get("page"));
            req.setAttribute("pageSize", result.get("pageSize"));
            req.setAttribute("totalItems", result.get("totalItems"));
            req.setAttribute("totalPages", result.get("totalPages"));
            req.setAttribute("keyword", keyword != null ? keyword.trim() : "");
            req.setAttribute("status", status != null ? status.trim() : "ALL");
            req.setAttribute("source", source != null ? source.trim() : "ALL");

            // Cờ phân quyền hiển thị trên giao diện JSP
            req.setAttribute("canCreate", LeadService.canCreateLead(currentUser));
            req.setAttribute("canUpdate", LeadService.canUpdateLead(currentUser));
            req.setAttribute("canDelete", LeadService.canDeleteLead(currentUser));
            req.setAttribute("isAdmissionsOnly", currentUser.hasRole(RoleConstant.ADMISSIONS)
                    && !currentUser.hasRole(RoleConstant.TRAINING_MANAGER)
                    && !currentUser.hasRole(RoleConstant.ADMIN));

            // Chuyển session message sang request attribute nếu có
            HttpSession session = req.getSession(false);
            if (session != null) {
                if (session.getAttribute("successMessage") != null) {
                    req.setAttribute("successMessage", session.getAttribute("successMessage"));
                    session.removeAttribute("successMessage");
                }
                if (session.getAttribute("errorMessage") != null) {
                    req.setAttribute("errorMessage", session.getAttribute("errorMessage"));
                    session.removeAttribute("errorMessage");
                }
            }

            req.getRequestDispatcher("/WEB-INF/views/lead/list-lead.jsp").forward(req, resp);
        } catch (SQLException e) {
            getServletContext().log("Lỗi tra cứu danh sách lead", e);
            req.setAttribute("errorMessage", "Lỗi cơ sở dữ liệu khi tải danh sách khách hàng tiềm năng: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/lead/list-lead.jsp").forward(req, resp);
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

    private void denyAccess(HttpServletRequest req, HttpServletResponse res, String message)
            throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute("forbiddenMessage", message);
        req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, res);
    }
}
