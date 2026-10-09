-- =====================================================================
-- Migration: S2-05 - Quản lý danh mục môn học (Subjects Management)
-- Feature Epic: EP-02 - Quản lý Chương trình đào tạo & Lớp học
-- =====================================================================

CREATE TABLE IF NOT EXISTS `subjects` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã môn học: WEB101, WEB102, DATA101,...',
    `subject_name` VARCHAR(150) NOT NULL COMMENT 'Tên môn học',
    `total_sessions` INT NOT NULL DEFAULT 15 COMMENT 'Số buổi học chuẩn',
    `credit_weight` DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT 'Trọng số môn học',
    `min_pass_score` DECIMAL(4, 2) NOT NULL DEFAULT 5.00 COMMENT 'Điểm đạt tối thiểu (thang 10)',
    `learning_outcomes` TEXT NULL COMMENT 'Chuẩn đầu ra môn học',
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' COMMENT 'Trạng thái môn học',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_subjects_code` (`subject_code`),
    INDEX `idx_subjects_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng danh mục môn học';
