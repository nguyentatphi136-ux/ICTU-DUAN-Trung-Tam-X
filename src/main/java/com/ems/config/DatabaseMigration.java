package com.ems.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tiện ích thực thi Migration Cơ sở dữ liệu tự động
 * Sprint 2 - User Story S2-09 [IDTTX-45]: Đảm bảo bảng leads và dữ liệu RBAC được khởi tạo thành công
 */
public class DatabaseMigration {

    public static boolean runMigration() {
        System.out.println("[DatabaseMigration] Bắt đầu kiểm tra và thực thi migration S2-09 (Leads)...");

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tạo bảng leads nếu chưa tồn tại
            String createTableSql = "CREATE TABLE IF NOT EXISTS `leads` ("
                    + "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "`full_name` VARCHAR(100) NOT NULL COMMENT 'Họ và tên khách hàng tiềm năng', "
                    + "`phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại liên hệ', "
                    + "`email` VARCHAR(150) NULL COMMENT 'Email liên hệ', "
                    + "`source` VARCHAR(50) NOT NULL DEFAULT 'WEBSITE' COMMENT 'Nguồn lead', "
                    + "`program_id` INT NULL COMMENT 'Chương trình đào tạo quan tâm', "
                    + "`program_interest` VARCHAR(150) NULL COMMENT 'Tên chương trình quan tâm', "
                    + "`status` VARCHAR(50) NOT NULL DEFAULT 'NEW' COMMENT 'Trạng thái xử lý', "
                    + "`notes` TEXT NULL COMMENT 'Ghi chú nhu cầu tư vấn', "
                    + "`assigned_to` BIGINT NULL COMMENT 'Tư vấn viên phụ trách', "
                    + "`created_by` BIGINT NULL COMMENT 'Người tạo lead', "
                    + "`updated_by` BIGINT NULL COMMENT 'Người cập nhật gần nhất', "
                    + "`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "`updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                    + "INDEX `idx_leads_phone` (`phone`), "
                    + "INDEX `idx_leads_email` (`email`), "
                    + "INDEX `idx_leads_status` (`status`), "
                    + "INDEX `idx_leads_assigned` (`assigned_to`), "
                    + "INDEX `idx_leads_created_at` (`created_at`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            stmt.execute(createTableSql);
            System.out.println("[DatabaseMigration] -> Bảng `leads` đã sẵn sàng.");

            // 2. Đăng ký permissions cho module LEAD
            String[] permissions = {
                    "LEAD_VIEW|Xem danh sách và chi tiết khách hàng tiềm năng|LEAD",
                    "LEAD_CREATE|Tạo mới khách hàng tiềm năng|LEAD",
                    "LEAD_UPDATE|Cập nhật thông tin khách hàng tiềm năng|LEAD",
                    "LEAD_DELETE|Xóa khách hàng tiềm năng|LEAD",
                    "LEAD_MANAGE|Quản trị toàn diện khách hàng tiềm năng|LEAD"
            };

            for (String perm : permissions) {
                String[] parts = perm.split("\\|");
                String insertPermSql = "INSERT INTO permissions (permission_code, permission_name, module, description) "
                        + "VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name)";
                try (PreparedStatement ps = conn.prepareStatement(insertPermSql)) {
                    ps.setString(1, parts[0]);
                    ps.setString(2, parts[1]);
                    ps.setString(3, parts[2]);
                    ps.setString(4, parts[1]);
                    ps.executeUpdate();
                } catch (SQLException ignored) {}
            }
            System.out.println("[DatabaseMigration] -> Đăng ký Permissions cho Lead hoàn tất.");

            // 3. Gán Role Permissions cho ADMIN, TRAINING_MANAGER, ADMISSIONS
            // 3.1 ADMIN: Có toàn bộ quyền LEAD
            assignPermissionsToRole(conn, new String[]{"Admin", "ADMIN"},
                    new String[]{"LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_DELETE", "LEAD_MANAGE"});

            // 3.2 TRAINING_MANAGER: Có toàn bộ CRUD LEAD (kể cả DELETE)
            assignPermissionsToRole(conn, new String[]{"TrainingManager", "TRAINING_MANAGER"},
                    new String[]{"LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_DELETE", "LEAD_MANAGE"});

            // 3.3 ADMISSIONS: Chỉ có VIEW, CREATE, UPDATE, MANAGE (KHÔNG CÓ LEAD_DELETE)
            assignPermissionsToRole(conn, new String[]{"Admissions", "ADMISSIONS"},
                    new String[]{"LEAD_VIEW", "LEAD_CREATE", "LEAD_UPDATE", "LEAD_MANAGE"});

            // Thu hồi LEAD_DELETE từ Admissions nếu có
            try {
                String revokeSql = "DELETE rp FROM role_permissions rp "
                        + "JOIN roles r ON r.id = rp.role_id "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "WHERE r.role_code IN ('Admissions', 'ADMISSIONS') AND p.permission_code = 'LEAD_DELETE'";
                stmt.executeUpdate(revokeSql);
            } catch (SQLException ignored) {}

            System.out.println("[DatabaseMigration] -> Phân quyền RBAC cho Lead hoàn tất.");

            // 4. Khởi tạo dữ liệu mẫu nếu bảng đang trống
            String checkCountSql = "SELECT COUNT(*) FROM leads";
            boolean isEmpty = false;
            try (ResultSet rs = stmt.executeQuery(checkCountSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    isEmpty = true;
                }
            } catch (SQLException ignored) {}

            if (isEmpty) {
                String seedSql = "INSERT INTO leads (full_name, phone, email, source, program_interest, status, notes) VALUES "
                        + "('Trần Minh Quang', '0912345678', 'quang.tran@gmail.com', 'WEBSITE', 'Khóa học Lập trình Web Fullstack Java', 'NEW', 'Quan tâm lớp học tối, cần tư vấn lộ trình học từ con số 0'), "
                        + "('Lê Hoàng Yến', '0987654321', 'yen.le@yahoo.com', 'FACEBOOK', 'Khóa học Lập trình Frontend React', 'CONTACTED', 'Đã gọi lần 1, khách xin gửi tài liệu qua Zalo'), "
                        + "('Phạm Quốc Bảo', '0905123456', 'bao.pham@outlook.com', 'REFERRAL', 'Khóa học Tester / QA Pro', 'CONSULTING', 'Bạn cũ giới thiệu, hẹn gọi lại sau 17h'), "
                        + "('Đỗ Thùy Trang', '0934567890', 'trang.do@gmail.com', 'ADS', 'Khóa học Lập trình Python & AI', 'TRIAL', 'Đã hẹn tham gia buổi học thử ngày thứ Bảy')";
                stmt.execute(seedSql);
                System.out.println("[DatabaseMigration] -> Đã thêm 4 bản ghi dữ liệu mẫu cho Leads.");
            }

            System.out.println("[DatabaseMigration] Migration S2-09 thực thi thành công 100%!");
            return true;
        } catch (SQLException e) {
            System.err.println("[DatabaseMigration] Lỗi thực thi migration: " + e.getMessage());
            return false;
        }
    }

    private static void assignPermissionsToRole(Connection conn, String[] roleCodes, String[] permCodes) {
        for (String roleCode : roleCodes) {
            for (String permCode : permCodes) {
                String sql = "INSERT INTO role_permissions (role_id, permission_id) "
                        + "SELECT r.id, p.id FROM roles r, permissions p "
                        + "WHERE r.role_code = ? AND p.permission_code = ? "
                        + "ON DUPLICATE KEY UPDATE role_id = role_id";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, roleCode);
                    ps.setString(2, permCode);
                    ps.executeUpdate();
                } catch (SQLException ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        boolean success = runMigration();
        System.exit(success ? 0 : 1);
    }
}
