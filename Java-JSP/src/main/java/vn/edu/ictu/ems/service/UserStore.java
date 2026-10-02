package vn.edu.ictu.ems.service;

import vn.edu.ictu.ems.constant.RoleConstant;
import vn.edu.ictu.ems.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho lưu trữ và quản lý người dùng & vai trò (In-Memory Data Store)
 * Tác vụ Jira: IDTTX-24 [BE] Gán và thu hồi vai trò của một người dùng
 * Đặc tả:
 *   1. Một người dùng có thể giữ nhiều vai trò cùng lúc (vd: vừa là Giảng viên vừa là Quản lý đào tạo).
 *   2. Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại.
 *   3. Không thể tự thu hồi vai trò quản trị (Admin) của chính mình.
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class UserStore {

    private static final Map<Integer, User> usersById = new ConcurrentHashMap<>();
    private static final Map<String, Integer> idByEmail = new ConcurrentHashMap<>();

    static {
        resetToDefault();
    }

    public static synchronized void resetToDefault() {
        usersById.clear();
        idByEmail.clear();

        initUser(new User(1, "admin@example.com", "Quản trị viên Hệ thống", "active",
                new ArrayList<>(Collections.singletonList(RoleConstant.ADMIN))));

        initUser(new User(2, "instructor@example.com", "Giảng viên Nguyễn Văn A", "active",
                new ArrayList<>(Collections.singletonList(RoleConstant.INSTRUCTOR))));

        initUser(new User(3, "student@example.com", "Học viên Trần Thị B", "active",
                new ArrayList<>(Collections.singletonList(RoleConstant.STUDENT))));

        initUser(new User(4, "accountant@example.com", "Kế toán Lê Thị C", "active",
                new ArrayList<>(Collections.singletonList(RoleConstant.ACCOUNTANT))));

        initUser(new User(5, "training@example.com", "Quản lý đào tạo Hoàng D", "active",
                new ArrayList<>(Collections.singletonList(RoleConstant.TRAINING_MANAGER))));
    }

    private static void initUser(User user) {
        usersById.put(user.getId(), user);
        idByEmail.put(user.getEmail().toLowerCase(), user.getId());
    }

    public static List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }

    public static User findById(int id) {
        return usersById.get(id);
    }

    public static User findByEmail(String email) {
        if (email == null) return null;
        Integer id = idByEmail.get(email.trim().toLowerCase());
        return id != null ? usersById.get(id) : null;
    }

    /**
     * Gán vai trò cho người dùng (IDTTX-24)
     * Cho phép một người dùng giữ nhiều vai trò cùng lúc.
     * Có hiệu lực ngay lập tức.
     */
    public static synchronized void assignRole(int adminUserId, int targetUserId, String role) {
        if (!RoleConstant.isValidRole(role)) {
            throw new IllegalArgumentException("Mã vai trò '" + role + "' không hợp lệ trong hệ thống.");
        }

        User targetUser = usersById.get(targetUserId);
        if (targetUser == null) {
            throw new NoSuchElementException("Không tìm thấy người dùng có ID: " + targetUserId);
        }

        List<String> roles = targetUser.getRoles();
        if (roles == null) {
            roles = new ArrayList<>();
            targetUser.setRoles(roles);
        }

        if (roles.contains(role)) {
            throw new IllegalStateException("ROLE_ALREADY_ASSIGNED: Người dùng đã sở hữu vai trò " + role);
        }

        roles.add(role);
    }

    /**
     * Thu hồi vai trò của người dùng (IDTTX-24)
     * Ràng buộc nghiệp vụ: Không thể tự thu hồi vai trò quản trị (Admin) của chính mình.
     * Có hiệu lực ngay lập tức.
     */
    public static synchronized void revokeRole(int adminUserId, int targetUserId, String role) {
        if (!RoleConstant.isValidRole(role)) {
            throw new IllegalArgumentException("Mã vai trò '" + role + "' không hợp lệ trong hệ thống.");
        }

        User targetUser = usersById.get(targetUserId);
        if (targetUser == null) {
            throw new NoSuchElementException("Không tìm thấy người dùng có ID: " + targetUserId);
        }

        // Kiểm tra ràng buộc bảo vệ: Admin không thể tự tước quyền Admin của chính mình
        if (adminUserId == targetUserId && RoleConstant.ADMIN.equalsIgnoreCase(role)) {
            throw new SecurityException("CANNOT_REVOKE_OWN_ADMIN: Quản trị viên không thể tự thu hồi vai trò quản trị của chính mình.");
        }

        List<String> roles = targetUser.getRoles();
        if (roles == null || !roles.contains(role)) {
            throw new IllegalStateException("ROLE_NOT_ASSIGNED: Người dùng hiện không nắm giữ vai trò " + role);
        }

        roles.remove(role);
    }
}
