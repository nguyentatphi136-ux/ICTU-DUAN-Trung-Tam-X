-- =====================================================================
-- Migration: S2-06 - Quản lý chương trình đào tạo & Môn học tiên quyết
-- Feature Epic: EP-02 - Quản lý Chương trình đào tạo & Lớp học
-- Author: Backend Team (S2-06)
-- Date: 2026-10-04
-- =====================================================================

-- 1. Bảng danh mục chương trình đào tạo (Programs)
CREATE TABLE IF NOT EXISTS `programs` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `program_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã chương trình: FE-PRO, IELTS-ALL,...',
    `program_name` VARCHAR(150) NOT NULL COMMENT 'Tên chương trình đào tạo',
    `description` TEXT NULL COMMENT 'Mô tả mục tiêu, đối tượng đào tạo',
    `duration_months` INT NOT NULL DEFAULT 6 COMMENT 'Thời lượng học (tháng)',
    `standard_tuition` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Học phí chuẩn',
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' COMMENT 'Trạng thái hoạt động',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_programs_code` (`program_code`),
    INDEX `idx_programs_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng danh mục chương trình đào tạo';

-- 2. Bảng danh mục môn học (Subjects / Courses)
CREATE TABLE IF NOT EXISTS `subjects` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã môn học: WEB101, WEB102, DATA101,...',
    `subject_name` VARCHAR(150) NOT NULL COMMENT 'Tên môn học',
    `total_sessions` INT NOT NULL DEFAULT 15 COMMENT 'Số buổi học chuẩn',
    `credit_weight` DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT 'Trọng số tín chỉ',
    `min_pass_score` DECIMAL(4, 2) NOT NULL DEFAULT 5.00 COMMENT 'Điểm đạt tối thiểu (thang 10)',
    `learning_outcomes` TEXT NULL COMMENT 'Chuẩn đầu ra môn học',
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' COMMENT 'Trạng thái môn học',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_subjects_code` (`subject_code`),
    INDEX `idx_subjects_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng danh mục môn học';

-- 3. Bảng Ánh xạ Môn học trong Chương trình (Program Subjects & Prerequisite)
CREATE TABLE IF NOT EXISTS `program_subjects` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `program_id` INT NOT NULL COMMENT 'Liên kết chương trình đào tạo',
    `subject_id` INT NOT NULL COMMENT 'Liên kết môn học',
    `order_index` INT NOT NULL DEFAULT 1 COMMENT 'Thứ tự môn trong lộ trình học',
    `prerequisite_subject_id` INT NULL COMMENT 'Môn học tiên quyết trong cùng chương trình (nếu có)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_program_subject` (`program_id`, `subject_id`),
    INDEX `idx_ps_program_order` (`program_id`, `order_index`),
    CONSTRAINT `fk_ps_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ps_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ps_prereq` FOREIGN KEY (`prerequisite_subject_id`) REFERENCES `subjects` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng môn học thuộc chương trình và lộ trình tiên quyết';

-- 4. Bổ sung quyền quản lý chương trình (PROGRAM_MANAGE)
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`)
VALUES ('PROGRAM_MANAGE', 'Quản lý chương trình đào tạo', 'PROGRAM', 'Gắn môn, đổi thứ tự và khai báo môn tiên quyết')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- 5. Gán quyền PROGRAM_MANAGE cho role TRAINING_MANAGER và ADMIN
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `roles` r
JOIN `permissions` p ON p.permission_code = 'PROGRAM_MANAGE'
WHERE r.role_code IN ('TRAINING_MANAGER', 'ADMIN')
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
