package vn.edu.ictu.ems.service;

import vn.edu.ictu.ems.constant.RoleConstant;
import com.ems.model.User;

import java.util.*;

/**
 * Kho lưu trữ người dùng và vai trò theo package ICTU (vn.edu.ictu.ems)
 * Tác vụ Jira: IDTTX-24 [BE] Gán và thu hồi vai trò của một người dùng
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class UserStore {

    public static List<User> getAllUsers() {
        return com.ems.service.UserStore.getAllUsers();
    }

    public static User findById(int id) {
        return com.ems.service.UserStore.findById((long) id);
    }

    public static User findById(long id) {
        return com.ems.service.UserStore.findById(id);
    }

    public static User findByEmail(String email) {
        return com.ems.service.UserStore.findByEmail(email);
    }

    public static void assignRole(int adminUserId, int targetUserId, String role) {
        com.ems.service.UserStore.assignRole((long) adminUserId, (long) targetUserId, role);
    }

    public static void assignRole(long adminUserId, long targetUserId, String role) {
        com.ems.service.UserStore.assignRole(adminUserId, targetUserId, role);
    }

    public static void revokeRole(int adminUserId, int targetUserId, String role) {
        com.ems.service.UserStore.revokeRole((long) adminUserId, (long) targetUserId, role);
    }

    public static void revokeRole(long adminUserId, long targetUserId, String role) {
        com.ems.service.UserStore.revokeRole(adminUserId, targetUserId, role);
    }

    public static void resetToDefault() {
        com.ems.service.UserStore.resetToDefault();
    }

    public static User authenticate(String email, String password) {
        return com.ems.service.UserStore.authenticate(email, password);
    }
}
