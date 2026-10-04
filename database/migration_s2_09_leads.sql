-- =============================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EMS)
-- SPRINT 2 - USER STORY S2-09 [IDTTX-45]: [BE] DB + API CRUD LEAD
-- Database: MySQL 8.0+
-- Migration: Tạo bảng Leads, Phân quyền Lead (RBAC) & Dữ liệu mẫu
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `ems_database` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ems_database`;

-- 1. Tạo bảng Quản lý Khách hàng tiềm năng (Leads)
CREATE TABLE IF NOT EXISTS `leads` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL COMMENT 'Họ và tên khách hàng tiềm năng',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại liên hệ (có kiểm tra trùng lặp)',
    `email` VARCHAR(150) NULL COMMENT 'Email liên hệ',
    `source` VARCHAR(50) NOT NULL DEFAULT 'WEBSITE' COMMENT 'Nguồn: WEBSITE, FACEBOOK, REFERRAL, DIRECT, ADS, EVENT, OTHER',
    `program_id` INT NULL COMMENT 'Chương trình đào tạo quan tâm (FK tới programs.id)',
    `program_interest` VARCHAR(150) NULL COMMENT 'Tên chương trình quan tâm dự phòng',
    `status` ENUM('NEW', 'CONTACTED', 'CONSULTING', 'TRIAL', 'WON', 'LOST', 'ENROLLED', 'TRIAL_SCHEDULED', 'REJECTED') NOT NULL DEFAULT 'NEW' COMMENT 'Trạng thái xử lý',
    `notes` TEXT NULL COMMENT 'Ghi chú nhu cầu tư vấn',
    `assigned_to` BIGINT NULL COMMENT 'Tư vấn viên (Admissions) phụ trách',
    `created_by` BIGINT NULL COMMENT 'Người tạo lead (NULL nếu khách gửi công khai qua website)',
    `updated_by` BIGINT NULL COMMENT 'Người cập nhật thông tin gần nhất',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_leads_phone` (`phone`),
    INDEX `idx_leads_email` (`email`),
    INDEX `idx_leads_status` (`status`),
    INDEX `idx_leads_assigned` (`assigned_to`),
    INDEX `idx_leads_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng quản lý khách hàng tiềm năng (Leads)';

-- Đảm bảo tương thích trường nếu bảng đã tồn tại từ schema trước
SET @dbname = DATABASE();
SET @tablename = "leads";

-- Kiểm tra và bổ sung cột program_interest nếu chưa có
SET @precheck = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'program_interest');
SET @sql = IF(@precheck = 0, 'ALTER TABLE `leads` ADD COLUMN `program_interest` VARCHAR(150) NULL AFTER `program_id`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Kiểm tra và bổ sung cột program_id nếu chưa có
SET @precheck = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'program_id');
SET @sql = IF(@precheck = 0, 'ALTER TABLE `leads` ADD COLUMN `program_id` INT NULL AFTER `source`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Kiểm tra và bổ sung cột assigned_to nếu chưa có
SET @precheck = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'assigned_to');
SET @sql = IF(@precheck = 0, 'ALTER TABLE `leads` ADD COLUMN `assigned_to` BIGINT NULL AFTER `notes`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Kiểm tra và bổ sung cột created_by nếu chưa có
SET @precheck = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'created_by');
SET @sql = IF(@precheck = 0, 'ALTER TABLE `leads` ADD COLUMN `created_by` BIGINT NULL AFTER `assigned_to`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Kiểm tra và bổ sung cột updated_by nếu chưa có
SET @precheck = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = 'updated_by');
SET @sql = IF(@precheck = 0, 'ALTER TABLE `leads` ADD COLUMN `updated_by` BIGINT NULL AFTER `created_by`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Đăng ký các quyền hạn liên quan đến Lead (Permissions)
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`)
VALUES
    ('LEAD_VIEW', 'Xem danh sách và chi tiết khách hàng tiềm năng', 'LEAD', 'Quyền tra cứu phễu khách hàng tiềm năng'),
    ('LEAD_CREATE', 'Tạo mới khách hàng tiềm năng', 'LEAD', 'Quyền thêm khách hàng tiềm năng mới vào hệ thống'),
    ('LEAD_UPDATE', 'Cập nhật thông tin khách hàng tiềm năng', 'LEAD', 'Quyền cập nhật tiến trình tư vấn và thông tin lead'),
    ('LEAD_DELETE', 'Xóa khách hàng tiềm năng', 'LEAD', 'Quyền xóa lead khỏi hệ thống (Chỉ Quản lý đào tạo & Admin)'),
    ('LEAD_MANAGE', 'Quản trị toàn diện khách hàng tiềm năng', 'LEAD', 'Toàn quyền phân công và xử lý phễu lead')
ON DUPLICATE KEY UPDATE
    `permission_name` = VALUES(`permission_name`),
    `module` = VALUES(`module`),
    `description` = VALUES(`description`);

-- 3. Phân quyền Lead cho các vai trò theo ma trận RBAC:
-- 3.1. Quản trị hệ thống (Admin): Có đầy đủ các quyền
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r, `permissions` p
WHERE r.role_code IN ('Admin', 'ADMIN')
  AND p.permission_code IN ('LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_DELETE', 'LEAD_MANAGE')
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 3.2. Quản lý đào tạo (Training Manager): Được phép Create/Read/Update/Delete và Manage
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r, `permissions` p
WHERE r.role_code IN ('TrainingManager', 'TRAINING_MANAGER')
  AND p.permission_code IN ('LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_DELETE', 'LEAD_MANAGE')
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- 3.3. Tư vấn tuyển sinh (Admissions): Được phép Create/Read/Update (TUYỆT ĐỐI KHÔNG CÓ LEAD_DELETE)
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r, `permissions` p
WHERE r.role_code IN ('Admissions', 'ADMISSIONS')
  AND p.permission_code IN ('LEAD_VIEW', 'LEAD_CREATE', 'LEAD_UPDATE', 'LEAD_MANAGE')
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

-- Đảm bảo Admissions không có quyền xóa Lead
DELETE rp FROM `role_permissions` rp
JOIN `roles` r ON r.id = rp.role_id
JOIN `permissions` p ON p.id = rp.permission_id
WHERE r.role_code IN ('Admissions', 'ADMISSIONS') AND p.permission_code = 'LEAD_DELETE';

-- 4. Dữ liệu mẫu (Seed Data) phục vụ kiểm thử
INSERT INTO `leads` (`id`, `full_name`, `phone`, `email`, `source`, `program_interest`, `status`, `notes`)
VALUES
    (1, 'Trần Minh Quang', '0912345678', 'quang.tran@gmail.com', 'WEBSITE', 'Khóa học Lập trình Web Fullstack Java', 'NEW', 'Quan tâm lớp học tối, cần tư vấn lộ trình học từ con số 0'),
    (2, 'Lê Hoàng Yến', '0987654321', 'yen.le@yahoo.com', 'FACEBOOK', 'Khóa học Lập trình Frontend React', 'CONTACTED', 'Đã gọi lần 1, khách xin gửi tài liệu qua Zalo'),
    (3, 'Phạm Quốc Bảo', '0905123456', 'bao.pham@outlook.com', 'REFERRAL', 'Khóa học Tester / QA Pro', 'CONSULTING', 'Bạn cũ giới thiệu, hẹn gọi lại sau 17h'),
    (4, 'Đỗ Thùy Trang', '0934567890', 'trang.do@gmail.com', 'ADS', 'Khóa học Lập trình Python & AI', 'TRIAL', 'Đã hẹn tham gia buổi học thử ngày thứ Bảy')
ON DUPLICATE KEY UPDATE
    `full_name` = VALUES(`full_name`),
    `phone` = VALUES(`phone`);
