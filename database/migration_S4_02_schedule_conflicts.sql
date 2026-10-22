-- =============================================================================
-- MIGRATION SCRIPT CHO USER STORY S4-02:
-- [BE] Database + API kiểm tra xung đột thời khóa biểu
-- Phân hệ 4 & 5: Lớp học & Thời khóa biểu (EP-04)
-- =============================================================================

USE `ems_database`;

-- 1. Điều chỉnh bảng class_sessions:
-- Đảm bảo class_subject_id là NULLABLE (hỗ trợ xếp lịch linh hoạt) và session_number có DEFAULT 1
ALTER TABLE `class_sessions`
    MODIFY COLUMN `class_subject_id` BIGINT NULL COMMENT 'Môn học tương ứng của lớp (tùy chọn)',
    MODIFY COLUMN `session_number` INT NOT NULL DEFAULT 1 COMMENT 'Buổi số mấy của môn';

-- 2. Đảm bảo các chỉ mục tối ưu truy vấn kiểm tra giao nhau khung giờ (start_time, end_time)
-- idx_session_schedule (session_date, start_time, end_time)
-- idx_session_instructor (instructor_id, session_date)
-- idx_session_room (classroom_id, session_date)
-- Đã được định nghĩa trong schema.sql. Nếu chưa có thì bổ sung:
CREATE INDEX IF NOT EXISTS `idx_session_schedule` ON `class_sessions` (`session_date`, `start_time`, `end_time`);
CREATE INDEX IF NOT EXISTS `idx_session_instructor` ON `class_sessions` (`instructor_id`, `session_date`);
CREATE INDEX IF NOT EXISTS `idx_session_room` ON `class_sessions` (`classroom_id`, `session_date`);

-- 3. Bổ sung phòng học & lớp học mẫu phục vụ kiểm thử
INSERT INTO `programs` (`id`, `program_code`, `program_name`, `total_duration_hours`, `tuition_fee`, `status`) VALUES
(1, 'PROG-JAVA-FULL', 'Lập trình Java Fullstack', 360, 25000000.00, 'ACTIVE'),
(2, 'PROG-WEB-FE', 'Lập trình Frontend Chuyên nghiệp', 180, 15000000.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE `program_name` = VALUES(`program_name`);

INSERT INTO `classrooms` (`id`, `room_code`, `room_name`, `capacity`, `room_type`, `meeting_url`, `status`) VALUES
(1, 'PH-101', 'Phòng lý thuyết 101 (Tầng 1)', 40, 'PHYSICAL', NULL, 'AVAILABLE'),
(2, 'LAB-202', 'Phòng máy Lab 202 (Tầng 2)', 35, 'PHYSICAL', NULL, 'AVAILABLE'),
(3, 'ONLINE-01', 'Phòng học Google Meet 01', 100, 'ONLINE', 'https://meet.google.com/ems-s4-02', 'AVAILABLE')
ON DUPLICATE KEY UPDATE `room_name` = VALUES(`room_name`);

INSERT INTO `classes` (`id`, `class_code`, `class_name`, `program_id`, `start_date`, `end_date`, `max_capacity`, `default_classroom_id`, `study_mode`, `status`) VALUES
(1, 'CNTT-K23-01', 'Lớp Lập trình Java K23-01', 1, '2026-11-01', '2027-04-30', 30, 1, 'OFFLINE', 'IN_PROGRESS'),
(2, 'CNTT-K23-02', 'Lớp Web Fullstack K23-02', 1, '2026-11-01', '2027-04-30', 30, 2, 'OFFLINE', 'IN_PROGRESS')
ON DUPLICATE KEY UPDATE `class_name` = VALUES(`class_name`);

-- Buổi học mẫu (ID 15) của lớp CNTT-K23-02:
-- Giảng viên 3 (Trần Thị Giảng), Phòng 1 (PH-101), ngày 2026-11-01 từ 09:00:00 đến 11:00:00
INSERT INTO `class_sessions` (`id`, `class_id`, `class_subject_id`, `session_number`, `session_date`, `start_time`, `end_time`, `classroom_id`, `instructor_id`, `status`) VALUES
(15, 2, NULL, 1, '2026-11-01', '09:00:00', '11:00:00', 1, 3, 'SCHEDULED')
ON DUPLICATE KEY UPDATE `session_number` = VALUES(`session_number`);

-- 4. Bổ sung quyền hạn cho phân hệ thời khóa biểu (nếu chưa có)
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`) VALUES
('CLASS_MANAGE', 'Quản lý lớp học', 'CLASS', 'Mở lớp, xếp lịch và phân công'),
('SCHEDULE_READ', 'Xem thời khóa biểu', 'SCHEDULE', 'Tra cứu lịch học và xung đột')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- Gán quyền CLASS_MANAGE và SCHEDULE_READ cho ADMIN và TRAINING_MANAGER
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN ('CLASS_MANAGE', 'SCHEDULE_READ')
WHERE r.role_code IN ('ADMIN', 'TRAINING_MANAGER')
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

-- Gán quyền SCHEDULE_READ cho INSTRUCTOR, TA, STUDENT
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code = 'SCHEDULE_READ'
WHERE r.role_code IN ('INSTRUCTOR', 'TA', 'STUDENT')
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

