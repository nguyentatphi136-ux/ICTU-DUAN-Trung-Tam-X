package vn.edu.ictu.ems.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Danh mục 8 vai trò nghiệp vụ của hệ thống Quản lý Đào tạo Trung tâm (EMS)
 * Đặc tả theo User Story IDTTX-20 & Tài liệu thiết kế dự án ICTU
 * Người thực hiện: Nguyễn Minh Ngọc (MN)
 */
public class RoleConstant {

    public static final String ADMIN = "Admin";
    public static final String TRAINING_MANAGER = "TrainingManager";
    public static final String ADMISSIONS = "Admissions";
    public static final String INSTRUCTOR = "Instructor";
    public static final String TEACHING_ASSISTANT = "TeachingAssistant";
    public static final String ACCOUNTANT = "Accountant";
    public static final String STUDENT = "Student";
    public static final String GUEST = "Guest";

    private static final Map<String, String> ROLE_LABELS = new HashMap<>();

    static {
        ROLE_LABELS.put(ADMIN, "Quản trị hệ thống");
        ROLE_LABELS.put(TRAINING_MANAGER, "Quản lý đào tạo");
        ROLE_LABELS.put(ADMISSIONS, "Tư vấn tuyển sinh");
        ROLE_LABELS.put(INSTRUCTOR, "Giảng viên");
        ROLE_LABELS.put(TEACHING_ASSISTANT, "Trợ giảng");
        ROLE_LABELS.put(ACCOUNTANT, "Kế toán");
        ROLE_LABELS.put(STUDENT, "Học viên");
        ROLE_LABELS.put(GUEST, "Khách truy cập");
    }

    public static String getLabel(String roleCode) {
        return ROLE_LABELS.getOrDefault(roleCode, roleCode);
    }

    public static boolean isValidRole(String roleCode) {
        return ROLE_LABELS.containsKey(roleCode);
    }
}
