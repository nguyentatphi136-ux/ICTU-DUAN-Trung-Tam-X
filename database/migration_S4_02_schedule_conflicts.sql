-- =============================================================================
-- MIGRATION SCRIPT CHO USER STORY S4-02:
-- [BE] Database + API kiß╗âm tra xung ─æß╗Öt thß╗¥i kh├│a biß╗âu
-- Ph├ón hß╗ç 4 & 5: Lß╗¢p hß╗ìc & Thß╗¥i kh├│a biß╗âu (EP-04)
-- =============================================================================

USE `ems_database`;

-- 1. ─Éiß╗üu chß╗ënh bß║úng class_sessions:
-- ─Éß║úm bß║úo class_subject_id l├á NULLABLE (hß╗ù trß╗ú xß║┐p lß╗ïch linh hoß║ít) v├á session_number c├│ DEFAULT 1
ALTER TABLE `class_sessions`
    MODIFY COLUMN `class_subject_id` BIGINT NULL COMMENT 'M├┤n hß╗ìc t╞░╞íng ß╗⌐ng cß╗ºa lß╗¢p (t├╣y chß╗ìn)',
    MODIFY COLUMN `session_number` INT NOT NULL DEFAULT 1 COMMENT 'Buß╗òi sß╗æ mß║Ñy cß╗ºa m├┤n';

-- 2. ─Éß║úm bß║úo c├íc chß╗ë mß╗Ñc tß╗æi ╞░u truy vß║Ñn kiß╗âm tra giao nhau khung giß╗¥ (start_time, end_time)
-- idx_session_schedule (session_date, start_time, end_time)
-- idx_session_instructor (instructor_id, session_date)
-- idx_session_room (classroom_id, session_date)
-- ─É├ú ─æ╞░ß╗úc ─æß╗ïnh ngh─⌐a trong schema.sql. Nß║┐u ch╞░a c├│ th├¼ bß╗ò sung:
CREATE INDEX IF NOT EXISTS `idx_session_schedule` ON `class_sessions` (`session_date`, `start_time`, `end_time`);
CREATE INDEX IF NOT EXISTS `idx_session_instructor` ON `class_sessions` (`instructor_id`, `session_date`);
CREATE INDEX IF NOT EXISTS `idx_session_room` ON `class_sessions` (`classroom_id`, `session_date`);

-- 3. Bß╗ò sung ph├▓ng hß╗ìc & lß╗¢p hß╗ìc mß║½u phß╗Ñc vß╗Ñ kiß╗âm thß╗¡
INSERT INTO `programs` (`id`, `program_code`, `program_name`, `total_duration_hours`, `tuition_fee`, `status`) VALUES
(1, 'PROG-JAVA-FULL', 'Lß║¡p tr├¼nh Java Fullstack', 360, 25000000.00, 'ACTIVE'),
(2, 'PROG-WEB-FE', 'Lß║¡p tr├¼nh Frontend Chuy├¬n nghiß╗çp', 180, 15000000.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE `program_name` = VALUES(`program_name`);

INSERT INTO `classrooms` (`id`, `room_code`, `room_name`, `capacity`, `room_type`, `meeting_url`, `status`) VALUES
(1, 'PH-101', 'Ph├▓ng l├╜ thuyß║┐t 101 (Tß║ºng 1)', 40, 'PHYSICAL', NULL, 'AVAILABLE'),
(2, 'LAB-202', 'Ph├▓ng m├íy Lab 202 (Tß║ºng 2)', 35, 'PHYSICAL', NULL, 'AVAILABLE'),
(3, 'ONLINE-01', 'Ph├▓ng hß╗ìc Google Meet 01', 100, 'ONLINE', 'https://meet.google.com/ems-s4-02', 'AVAILABLE')
ON DUPLICATE KEY UPDATE `room_name` = VALUES(`room_name`);

INSERT INTO `classes` (`id`, `class_code`, `class_name`, `program_id`, `start_date`, `end_date`, `max_capacity`, `default_classroom_id`, `study_mode`, `status`) VALUES
(1, 'CNTT-K23-01', 'Lß╗¢p Lß║¡p tr├¼nh Java K23-01', 1, '2026-11-01', '2027-04-30', 30, 1, 'OFFLINE', 'IN_PROGRESS'),
(2, 'CNTT-K23-02', 'Lß╗¢p Web Fullstack K23-02', 1, '2026-11-01', '2027-04-30', 30, 2, 'OFFLINE', 'IN_PROGRESS')
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

