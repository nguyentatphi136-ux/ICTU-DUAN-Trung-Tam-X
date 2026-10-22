-- =============================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EDUCATION MANAGEMENT SYSTEM - EMS)
-- MIGRATION SCRIPT CHO USER STORY S4-06 (Jira: IDTTX-252, Subtask: IDTTX-268 [BE])
-- Mô tả: Xử lý bảo lưu, chuyển lớp và thôi học của học viên; ghi log lịch sử
--        và kiểm soát danh sách điểm danh phản ánh đúng theo ngày hiệu lực.
-- =============================================================================

USE `ems_database`;

-- 1. Bảng lưu trữ chi tiết lịch sử thay đổi tình trạng học vụ (Student Status Logs)
CREATE TABLE IF NOT EXISTS `student_status_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_code` VARCHAR(50) NOT NULL COMMENT 'Mã học viên (HV...)',
    `action_type` ENUM('Bảo lưu', 'Chuyển lớp', 'Thôi học') NOT NULL COMMENT 'Loại thao tác',
    `from_class` VARCHAR(50) NULL COMMENT 'Mã lớp trước khi thay đổi',
    `to_class` VARCHAR(50) NULL COMMENT 'Mã lớp chuyển đến (nếu chuyển lớp)',
    `effective_date` DATE NOT NULL COMMENT 'Ngày bắt đầu có hiệu lực',
    `reason` VARCHAR(255) NOT NULL COMMENT 'Lý do chính thức',
    `notes` TEXT NULL COMMENT 'Ghi chú bổ sung',
    `actor_name` VARCHAR(100) NOT NULL COMMENT 'Họ tên cán bộ thực hiện (Quản lý đào tạo)',
    `actor_id` BIGINT NULL COMMENT 'ID tài khoản thực hiện',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận hệ thống',
    INDEX `idx_ssl_student` (`student_code`),
    INDEX `idx_ssl_effective` (`effective_date`),
    INDEX `idx_ssl_action` (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký biến động học vụ S4-06';

-- 2. Đảm bảo bảng classes có các lớp phục vụ kiểm thử chuyển lớp cùng chương trình
INSERT IGNORE INTO `programs` (`id`, `program_code`, `program_name`, `duration`, `standard_tuition`, `status`)
VALUES 
(1, 'WEB-FULLSTACK', 'Lập trình web Full-stack', 72, 18500000, 'ACTIVE'),
(2, 'JAVA-BACKEND', 'Lập trình Java Backend', 60, 16000000, 'ACTIVE'),
(3, 'DATA-ANALYTICS', 'Phân tích dữ liệu', 48, 14000000, 'ACTIVE')
ON DUPLICATE KEY UPDATE `status` = 'ACTIVE';

INSERT IGNORE INTO `classes` (`id`, `class_code`, `class_name`, `program_id`, `start_date`, `max_capacity`, `status`)
VALUES 
(1, 'WEB-K15', 'Lớp Web Fullstack K15', 1, '2026-08-15', 30, 'IN_PROGRESS'),
(2, 'WEB-K16', 'Lớp Web Fullstack K16', 1, '2026-09-20', 30, 'IN_PROGRESS'),
(3, 'JAVA-K12', 'Lớp Java Backend K12', 2, '2026-09-01', 25, 'IN_PROGRESS'),
(4, 'DATA-K07', 'Lớp Data Analytics K07', 3, '2026-07-10', 25, 'IN_PROGRESS')
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

-- 3. Chèn bản ghi lịch sử mẫu cho học viên HV260101 (để kiểm tra hiển thị lịch sử)
INSERT INTO `student_status_logs` (`student_code`, `action_type`, `from_class`, `to_class`, `effective_date`, `reason`, `notes`, `actor_name`, `created_at`)
VALUES 
('HV260101', 'Chuyển lớp', 'WEB-K12', 'WEB-K15', '2026-08-15', 'Đổi sang lịch học phù hợp hơn', 'Chuyển sang lớp học tối 2-4-6 do lịch học trường thay đổi', 'Nguyễn Minh Ngọc', '2026-08-14 10:30:00'),
('HV260101', 'Bảo lưu', 'WEB-K12', NULL, '2026-04-10', 'Lý do sức khoẻ', 'Bảo lưu 1 khóa do phải điều trị tại bệnh viện', 'Nguyễn Tất Phi', '2026-04-09 14:15:00');
