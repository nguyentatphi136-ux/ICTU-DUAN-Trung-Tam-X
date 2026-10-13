-- =====================================================================
-- Migration: S2-08 - Đăng ký tư vấn công khai dành cho khách truy cập
-- Feature Epic: EP-03 - Tuyển sinh & Quản lý Lead
-- Author: Backend Team (S2-08)
-- Date: 2026-10-04
-- =====================================================================

-- 1. Đảm bảo bảng `leads` tồn tại và đầy đủ các trường theo đặc tả
CREATE TABLE IF NOT EXISTS `leads` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL COMMENT 'Họ và tên khách đăng ký',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại liên hệ',
    `email` VARCHAR(150) NULL COMMENT 'Email khách hàng (nếu có)',
    `source` VARCHAR(50) DEFAULT 'WEBSITE' COMMENT 'Nguồn: WEBSITE, FACEBOOK, REFERRAL, EVENT,...',
    `course` VARCHAR(100) NULL COMMENT 'Khoá học/chương trình quan tâm dạng text',
    `interested_program_id` INT NULL COMMENT 'Khóa ngoại liên kết tới bảng programs (nếu có)',
    `assigned_counselor_id` BIGINT NULL COMMENT 'Tư vấn viên phụ trách',
    `status` ENUM('NEW', 'CONTACTED', 'CONSULTING', 'TRIAL_SCHEDULED', 'WON', 'REJECTED') DEFAULT 'NEW' COMMENT 'Trạng thái xử lý Lead',
    `reject_reason` VARCHAR(255) NULL COMMENT 'Lý do từ chối (nếu status = REJECTED)',
    `notes` TEXT NULL COMMENT 'Nội dung cần tư vấn / ghi chú của khách',
    `preferred_time` VARCHAR(100) NULL COMMENT 'Khung giờ thuận tiện liên hệ',
    `converted_student_id` BIGINT NULL COMMENT 'Liên kết sang học viên khi chốt nhập học',
    `ip_address` VARCHAR(45) NULL COMMENT 'IP gửi biểu mẫu để audit và chống spam',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm gửi đăng ký',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật gần nhất',
    CONSTRAINT `fk_leads_program` FOREIGN KEY (`interested_program_id`) REFERENCES `programs` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_leads_counselor` FOREIGN KEY (`assigned_counselor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_leads_phone` (`phone`),
    INDEX `idx_leads_status` (`status`),
    INDEX `idx_leads_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng phễu khách hàng tiềm năng & đăng ký tư vấn';

-- 2. Thêm quyền cho vai trò GUEST nếu chưa có
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`)
VALUES ('CONSULTATION_CREATE', 'Gửi yêu cầu tư vấn', 'PUBLIC', 'Đăng ký tư vấn công khai dành cho khách vãng lai')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- 3. Gán quyền CONSULTATION_CREATE cho role GUEST (nếu bảng roles có role GUEST)
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.permission_code = 'CONSULTATION_CREATE'
WHERE r.role_code = 'GUEST'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
