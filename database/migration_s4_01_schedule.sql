-- =============================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EDUCATION MANAGEMENT SYSTEM - EMS)
-- MIGRATION SCRIPT CHO USER STORY S4-01 (Jira: IDTTX-247 / Subtask: IDTTX-263 [BE])
-- Người thực hiện: Nguyễn Trung Kiên (NK)
-- Mô tả: Sinh thời khoá biểu tự động theo mẫu lặp cho một lớp (60 buổi),
--        tự động bỏ qua ngày nghỉ lễ và gắn bài học chuẩn theo môn.
-- =============================================================================

USE `ems_database`;

-- 1. Bảng danh mục ngày nghỉ lễ hệ thống (Holidays)
CREATE TABLE IF NOT EXISTS `holidays` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL COMMENT 'Tên ngày nghỉ lễ (20/11, Tết Nguyên đán,...)',
    `from_date` DATE NOT NULL COMMENT 'Từ ngày',
    `to_date` DATE NOT NULL COMMENT 'Đến ngày',
    `description` VARCHAR(255) NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh mục ngày nghỉ lễ hệ thống S4-01';

-- Nạp các ngày nghỉ lễ mẫu
INSERT IGNORE INTO `holidays` (`id`, `name`, `from_date`, `to_date`, `description`)
VALUES 
(1, 'Ngày Nhà giáo Việt Nam', '2026-11-20', '2026-11-20', 'Nghỉ lễ tri ân thầy cô'),
(2, 'Tết Dương lịch', '2027-01-01', '2027-01-01', 'Nghỉ Tết Dương lịch'),
(3, 'Nghỉ Tết Nguyên đán', '2027-02-05', '2027-02-14', 'Nghỉ Tết Nguyên đán cổ truyền'),
(4, 'Giỗ Tổ Hùng Vương', '2027-04-26', '2027-04-26', 'Nghỉ lễ Giỗ Tổ'),
(5, 'Giải phóng miền Nam & Quốc tế Lao động', '2027-04-30', '2027-05-01', 'Nghỉ lễ 30/4 - 1/5')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 2. Đảm bảo lớp mẫu JAVA-K15 (chuẩn 60 buổi theo đề bài) có trong CSDL
INSERT IGNORE INTO `programs` (`id`, `program_code`, `program_name`, `duration`, `standard_tuition`, `status`)
VALUES (2, 'JAVA-BACKEND', 'Lập trình Java Doanh nghiệp', 60, 16000000, 'ACTIVE')
ON DUPLICATE KEY UPDATE `duration` = 60;

INSERT IGNORE INTO `classes` (`id`, `class_code`, `class_name`, `program_id`, `start_date`, `max_capacity`, `status`)
VALUES (5, 'JAVA-K15', 'Lớp Java Doanh nghiệp K15 (60 buổi)', 2, '2026-11-02', 30, 'PLANNING')
ON DUPLICATE KEY UPDATE `class_name` = VALUES(`class_name`);
