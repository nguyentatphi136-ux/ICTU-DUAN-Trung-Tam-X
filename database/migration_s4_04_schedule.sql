Select-String -Path .\database\migration_s4_04_schedule.sql -Pattern 'DROP\s+TABLE|TRUNCATE|DELETE\s+FROM|ALTER\s+TABLE|CREATE\s+TABLE' -CaseSensitive:$f-- =============================================================================
-- MIGRATION S4-04: Hỗ trợ EP-04 – Thời khóa biểu tuần và tháng
-- Áp dụng cho CSDL ems_database (MySQL 8.0+)
-- Chạy một lần; các bảng đã dùng IF NOT EXISTS / IF NOT EXISTS / IGNORE
-- =============================================================================

USE `ems_database`;

-- Bảng phòng học (nếu chưa tồn tại)
CREATE TABLE IF NOT EXISTS `classrooms` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `room_code`   VARCHAR(50)  NOT NULL UNIQUE,
    `room_name`   VARCHAR(100) NOT NULL,
    `capacity`    INT          NOT NULL DEFAULT 30,
    `room_type`   ENUM('PHYSICAL','ONLINE') DEFAULT 'PHYSICAL',
    `meeting_url` VARCHAR(255) NULL,
    `status`      ENUM('AVAILABLE','MAINTENANCE') DEFAULT 'AVAILABLE'
) ENGINE=InnoDB COMMENT='Danh mục phòng học';

-- Bảng lớp học (nếu chưa tồn tại)
CREATE TABLE IF NOT EXISTS `classes` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_code`          VARCHAR(50)  NOT NULL UNIQUE,
    `class_name`          VARCHAR(150) NOT NULL,
    `program_id`          INT NOT NULL,
    `start_date`          DATE NOT NULL,
    `end_date`            DATE NULL,
    `max_capacity`        INT NOT NULL DEFAULT 30,
    `default_classroom_id` INT NULL,
    `study_mode`          ENUM('OFFLINE','ONLINE','HYBRID') DEFAULT 'OFFLINE',
    `status`              ENUM('PLANNING','IN_PROGRESS','FINISHED','CANCELLED') DEFAULT 'PLANNING',
    `created_at`          DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Bảng phân công giảng viên theo môn của lớp (nếu chưa tồn tại)
CREATE TABLE IF NOT EXISTS `class_subjects` (
    `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id`             BIGINT NOT NULL,
    `subject_id`           INT    NOT NULL,
    `primary_instructor_id` BIGINT NOT NULL,
    `status`               ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED') DEFAULT 'NOT_STARTED',
    UNIQUE KEY `uk_class_subject` (`class_id`, `subject_id`)
) ENGINE=InnoDB;

-- Bảng thời khóa biểu chi tiết từng buổi học (nếu chưa tồn tại)
CREATE TABLE IF NOT EXISTS `class_sessions` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id`         BIGINT  NOT NULL,
    `class_subject_id` BIGINT  NOT NULL,
    `subject_lesson_id` INT    NULL COMMENT 'Bài học giáo trình tương ứng (tùy chọn)',
    `session_number`   INT     NOT NULL COMMENT 'Buổi số mấy của môn',
    `session_date`     DATE    NOT NULL,
    `start_time`       TIME    NOT NULL,
    `end_time`         TIME    NOT NULL,
    `classroom_id`     INT     NOT NULL,
    `instructor_id`    BIGINT  NOT NULL,
    `status`           ENUM('SCHEDULED','COMPLETED','CANCELLED','RESCHEDULED') DEFAULT 'SCHEDULED',
    `cancel_reason`    VARCHAR(255) NULL,
    `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_csess_class`       FOREIGN KEY (`class_id`)         REFERENCES `classes`        (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_csess_subject`     FOREIGN KEY (`class_subject_id`) REFERENCES `class_subjects` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_csess_room`        FOREIGN KEY (`classroom_id`)     REFERENCES `classrooms`     (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_csess_instructor`  FOREIGN KEY (`instructor_id`)    REFERENCES `users`          (`id`) ON DELETE RESTRICT,
    INDEX `idx_session_schedule`  (`session_date`, `start_time`, `end_time`),
    INDEX `idx_session_instructor` (`instructor_id`, `session_date`),
    INDEX `idx_session_room`      (`classroom_id`,  `session_date`)
) ENGINE=InnoDB COMMENT='Bảng thời khóa biểu chi tiết từng buổi học';

-- =============================================================================
-- DỮ LIỆU MẪU (tuỳ chọn, dùng để kiểm tra giao diện)
-- Xóa block này nếu CSDL đã có dữ liệu thật
-- =============================================================================
/*
-- Phòng học mẫu
INSERT IGNORE INTO classrooms (room_code, room_name, capacity) VALUES
    ('A101','Phòng A101',40),
    ('A102','Phòng A102',35),
    ('B201','Phòng B201',30);

-- Lớp học mẫu (cần program_id hợp lệ)
INSERT IGNORE INTO classes (class_code, class_name, program_id, start_date, status) VALUES
    ('JAVA-K01','Lập trình Java Khóa 1',1,'2026-09-01','IN_PROGRESS'),
    ('WEB-K01', 'Lập trình Web Khóa 1', 1,'2026-09-01','IN_PROGRESS');

-- Môn học theo lớp (cần subject_id, instructor_id hợp lệ)
INSERT IGNORE INTO class_subjects (class_id, subject_id, primary_instructor_id) VALUES
    (1,1,1),(1,2,2),(2,1,1);

-- Buổi học mẫu tuần 2026-10-19 ~ 2026-10-25
INSERT IGNORE INTO class_sessions
    (class_id, class_subject_id, session_number, session_date, start_time, end_time, classroom_id, instructor_id)
VALUES
    (1,1,1,'2026-10-20','08:00','10:00',1,1),
    (1,1,2,'2026-10-22','08:00','10:00',1,1),
    (2,3,1,'2026-10-21','13:00','15:00',2,1);
*/
