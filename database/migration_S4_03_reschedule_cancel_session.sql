-- =============================================================================
-- Migration S4-03: Sửa, dời hoặc hủy buổi học đơn lẻ
-- Epic: EP-04 Quản lý lớp học và thời khóa biểu
-- =============================================================================

-- 1. Đảm bảo bảng class_sessions có enum status bao gồm 'RESCHEDULED' và có trường cancel_reason
ALTER TABLE `class_sessions`
    MODIFY COLUMN `status` ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED') DEFAULT 'SCHEDULED';

-- 2. Đảm bảo bảng class_sessions có trường cancel_reason nếu chưa có
-- (MySQL 8.0+ hỗ trợ ADD COLUMN IF NOT EXISTS)
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'class_sessions' 
  AND COLUMN_NAME = 'cancel_reason';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE `class_sessions` ADD COLUMN `cancel_reason` VARCHAR(255) NULL AFTER `status`', 
    'SELECT "Column cancel_reason already exists in class_sessions"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Tạo bảng class_session_change_logs để lưu vết lịch sử sửa, dời và hủy buổi học
CREATE TABLE IF NOT EXISTS `class_session_change_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL COMMENT 'Mã buổi học',
    `action_type` ENUM('UPDATE', 'RESCHEDULE', 'CANCEL') NOT NULL COMMENT 'Loại thao tác',
    `performed_by` BIGINT NOT NULL COMMENT 'Người thực hiện thao tác (User ID)',
    `reason` VARCHAR(255) NULL COMMENT 'Lý do thao tác (bắt buộc khi CANCEL)',
    `old_values` LONGTEXT NULL COMMENT 'Trạng thái dữ liệu JSON trước khi thay đổi',
    `new_values` LONGTEXT NULL COMMENT 'Trạng thái dữ liệu JSON sau khi thay đổi',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_cscl_session` (`session_id`),
    INDEX `idx_cscl_user` (`performed_by`),
    CONSTRAINT `fk_cscl_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cscl_user` FOREIGN KEY (`performed_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng lịch sử chi tiết thay đổi và hủy buổi học';

-- 4. Đảm bảo bảng notifications hỗ trợ loại 'SCHEDULE'
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recipient_id` BIGINT NOT NULL COMMENT 'Người nhận thông báo (users.id)',
    `sender_id` BIGINT NULL COMMENT 'Người gửi hoặc NULL nếu hệ thống sinh',
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `notification_type` ENUM('ASSIGNMENT', 'GRADE', 'SCHEDULE', 'ABSENCE', 'TUITION', 'SYSTEM') DEFAULT 'SYSTEM',
    `link_url` VARCHAR(255) NULL COMMENT 'Đường dẫn chuyển hướng nhanh',
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notif_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notif_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_notif_user_read` (`recipient_id`, `is_read`, `created_at`)
) ENGINE=InnoDB COMMENT='Bảng thông báo hệ thống và học vụ';
