package com.ems.service;

import com.ems.constant.RoleConstant;
import com.ems.dao.LeadDAO;
import com.ems.model.Lead;
import com.ems.model.User;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service xử lý nghiệp vụ cho Lead (S2-09 [IDTTX-45])
 * Bao gồm kiểm tra ràng buộc (Validation), xử lý trùng số điện thoại, và kiểm tra quyền RBAC.
 */
public class LeadService {

    // Regex kiểm tra số điện thoại Việt Nam hợp lệ: 10 chữ số bắt đầu bằng 0 hoặc +84 (đầu số 3, 5, 7, 8, 9)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+84|0)(?:3[2-9]|5[6|8|9]|7[0|6-9]|8[1-9]|9[0-9])[0-9]{7}$");
    // Regex định dạng email tiêu chuẩn
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final LeadDAO leadDAO;

    public LeadService() {
        this.leadDAO = new LeadDAO();
    }

    public LeadService(LeadDAO leadDAO) {
        this.leadDAO = leadDAO;
    }

    /**
     * Xác thực thông tin Lead
     */
    public List<String> validateLead(Lead lead, boolean isUpdate) {
        List<String> errors = new ArrayList<>();

        if (lead == null) {
            errors.add("Dữ liệu khách hàng tiềm năng không được để trống.");
            return errors;
        }

        // 1. Kiểm tra Họ và tên
        if (lead.getFullName() == null || lead.getFullName().trim().isBlank()) {
            errors.add("Họ và tên không được để trống.");
        } else {
            String name = lead.getFullName().trim();
            if (name.length() < 2 || name.length() > 100) {
                errors.add("Họ và tên phải có độ dài từ 2 đến 100 ký tự.");
            }
        }

        // 2. Kiểm tra Số điện thoại
        if (lead.getPhone() == null || lead.getPhone().trim().isBlank()) {
            errors.add("Số điện thoại không được để trống.");
        } else {
            String cleanPhone = lead.getPhone().replaceAll("[\\s.-]", "");
            if (!isValidPhoneNumber(cleanPhone)) {
                errors.add("Số điện thoại không đúng định dạng di động Việt Nam hợp lệ (ví dụ: 0912345678 hoặc +84912345678).");
            }
        }

        // 3. Kiểm tra Email (nếu có)
        if (lead.getEmail() != null && !lead.getEmail().trim().isBlank()) {
            String email = lead.getEmail().trim();
            if (email.length() > 150) {
                errors.add("Email không được vượt quá 150 ký tự.");
            } else if (!isValidEmail(email)) {
                errors.add("Email không đúng định dạng hợp lệ (ví dụ: example@domain.com).");
            }
        }

        // 4. Kiểm tra Trạng thái
        if (lead.getStatus() != null && !lead.getStatus().trim().isBlank()) {
            if (!Lead.isValidStatus(lead.getStatus())) {
                errors.add("Trạng thái không hợp lệ. Danh sách hợp lệ: " + String.join(", ", Lead.VALID_STATUSES));
            }
        }

        // 5. Kiểm tra Nguồn
        if (lead.getSource() != null && !lead.getSource().trim().isBlank()) {
            if (!Lead.isValidSource(lead.getSource())) {
                errors.add("Nguồn khách hàng không hợp lệ. Danh sách hợp lệ: " + String.join(", ", Lead.VALID_SOURCES));
            }
        }

        return errors;
    }

    /**
     * Kiểm tra số điện thoại có hợp lệ theo chuẩn không
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        String cleanPhone = phone.replaceAll("[\\s.-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Kiểm tra email có hợp lệ không
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Xử lý kiểm tra trùng lặp số điện thoại
     * Trả về thông tin lead đã tồn tại nếu trùng, hoặc null nếu không trùng
     */
    public Lead checkDuplicatePhone(String phone, Long excludeId) throws SQLException {
        if (phone == null || phone.isBlank()) return null;
        String cleanPhone = phone.replaceAll("[\\s.-]", "");

        Lead existing = leadDAO.findByPhone(cleanPhone);
        if (existing != null) {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                return existing;
            }
        }
        return null;
    }

    /**
     * Kiểm tra quyền tạo Lead: Admissions, TrainingManager, Admin
     */
    public static boolean canCreateLead(User user) {
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) return false;
        return user.hasRole(RoleConstant.ADMIN)
                || user.hasRole(RoleConstant.TRAINING_MANAGER)
                || user.hasRole(RoleConstant.ADMISSIONS);
    }

    /**
     * Kiểm tra quyền xem Lead: Admissions, TrainingManager, Admin
     */
    public static boolean canReadLead(User user) {
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) return false;
        return user.hasRole(RoleConstant.ADMIN)
                || user.hasRole(RoleConstant.TRAINING_MANAGER)
                || user.hasRole(RoleConstant.ADMISSIONS);
    }

    /**
     * Kiểm tra quyền sửa Lead: Admissions, TrainingManager, Admin
     */
    public static boolean canUpdateLead(User user) {
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) return false;
        return user.hasRole(RoleConstant.ADMIN)
                || user.hasRole(RoleConstant.TRAINING_MANAGER)
                || user.hasRole(RoleConstant.ADMISSIONS);
    }

    /**
     * Kiểm tra quyền xóa Lead: CHỈ Training Manager và Admin được xóa.
     * Admissions TUYỆT ĐỐI KHÔNG được xóa!
     */
    public static boolean canDeleteLead(User user) {
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) return false;
        return user.hasRole(RoleConstant.ADMIN)
                || user.hasRole(RoleConstant.TRAINING_MANAGER);
    }

    public Lead findById(long id) throws SQLException {
        return leadDAO.findById(id);
    }

    public Lead findByPhone(String phone) throws SQLException {
        return leadDAO.findByPhone(phone);
    }

    public long createLead(Lead lead) throws SQLException {
        return leadDAO.create(lead);
    }

    public boolean updateLead(Lead lead) throws SQLException {
        return leadDAO.update(lead);
    }

    public boolean deleteLead(long id) throws SQLException {
        return leadDAO.delete(id);
    }

    public Map<String, Object> searchLeads(String keyword, String status, String source,
                                           Long assignedTo, String startDate, String endDate,
                                           int page, int pageSize) throws SQLException {
        return leadDAO.searchLeads(keyword, status, source, assignedTo, startDate, endDate, page, pageSize);
    }
}
