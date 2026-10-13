-- =====================================================================
-- Migration: S2-07 - Khai báo các buổi học trong một môn (Subject Lessons)
-- Feature Epic: EP-02 - Quản lý Chương trình đào tạo & Lớp học
-- =====================================================================

CREATE TABLE IF NOT EXISTS `subject_lessons` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL COMMENT 'Liên kết môn học',
    `session_order` INT NOT NULL COMMENT 'Thứ tự buổi học (Buổi 1, Buổi 2,...)',
    `topic` VARCHAR(255) NOT NULL COMMENT 'Chủ đề buổi học',
    `objectives` TEXT NULL COMMENT 'Mục tiêu buổi học',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_subject_session` (`subject_id`, `session_order`),
    INDEX `idx_sl_subject_order` (`subject_id`, `session_order`),
    CONSTRAINT `fk_sl_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng danh mục các buổi học theo môn';
