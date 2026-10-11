-- =============================================================================
-- MIGRATION: V20261011_01__create_class_and_class_subjects.sql
-- User Story: S3-05 (Epic EP-03) - [BE] Thiết kế Database + API mở lớp học
-- Hệ thống quản lý đào tạo (EMS) - Chuẩn hóa 3NF, MySQL 8.0+
-- =============================================================================

USE `ems_database`;

-- 1. Bảng Lớp học (Classes)
CREATE TABLE IF NOT EXISTS `classes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã lớp học duy nhất (VD: CNTT-K23-01)',
    `class_name` VARCHAR(150) NOT NULL COMMENT 'Tên lớp học',
    `program_id` INT NOT NULL COMMENT 'Chương trình đào tạo tương ứng',
    `start_date` DATE NOT NULL COMMENT 'Ngày khai giảng dự kiến (ExpectedStartDate)',
    `end_date` DATE NULL COMMENT 'Ngày kết thúc dự kiến',
    `max_capacity` INT NOT NULL DEFAULT 30 COMMENT 'Sĩ số tối đa (MaxStudents)',
    `default_classroom_id` INT NULL COMMENT 'Phòng học mặc định (nếu có)',
    `study_mode` ENUM('OFFLINE', 'ONLINE', 'HYBRID') NOT NULL DEFAULT 'OFFLINE' COMMENT 'Hình thức học',
    `status` ENUM('PLANNING', 'IN_PROGRESS', 'FINISHED', 'CANCELLED') NOT NULL DEFAULT 'PLANNING' COMMENT 'Trạng thái lớp (Khởi tạo: PLANNING - Chuẩn bị)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_classes_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_classes_room` FOREIGN KEY (`default_classroom_id`) REFERENCES `classrooms` (`id`) ON DELETE SET NULL,
    INDEX `idx_classes_program` (`program_id`),
    INDEX `idx_classes_status` (`status`),
    INDEX `idx_classes_start_date` (`start_date`)
) ENGINE=InnoDB COMMENT='Bảng thông tin lớp học theo chương trình đào tạo';

-- 2. Bảng Môn học của Lớp (Class Subjects - Sao chép từ Program Subjects khi mở lớp)
CREATE TABLE IF NOT EXISTS `class_subjects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL COMMENT 'Lớp học',
    `subject_id` INT NOT NULL COMMENT 'Môn học thuộc lớp',
    `order_index` INT NOT NULL DEFAULT 1 COMMENT 'Thứ tự môn học trong lộ trình đào tạo của lớp',
    `primary_instructor_id` BIGINT NULL COMMENT 'Giảng viên chính phụ trách môn (NULL khi mới mở lớp)',
    `status` ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'Trạng thái môn học',
    UNIQUE KEY `uk_class_subject` (`class_id`, `subject_id`),
    CONSTRAINT `fk_cs_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cs_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_cs_instructor` FOREIGN KEY (`primary_instructor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_cs_class` (`class_id`),
    INDEX `idx_cs_subject` (`subject_id`)
) ENGINE=InnoDB COMMENT='Bảng liên kết danh sách môn học của lớp và phân công giảng viên';

-- =============================================================================
-- 3. Seed dữ liệu mẫu Chương trình đào tạo & Môn học phục vụ nghiệp vụ mở lớp
-- =============================================================================

-- Danh mục chương trình đào tạo mẫu
INSERT INTO `programs` (`id`, `program_code`, `program_name`, `description`, `duration_months`, `standard_tuition`, `status`) VALUES
(1, 'CNTT-FULLSTACK', 'Lập trình viên Fullstack Java Web', 'Chương trình đào tạo chuyên sâu Fullstack từ cơ bản đến nâng cao', 6, 25000000.00, 'ACTIVE'),
(2, 'DATA-ANALYTICS', 'Phân tích dữ liệu & Trí tuệ nhân tạo', 'Chương trình đào tạo Data Analysis, SQL, Python, BI', 4, 18000000.00, 'ACTIVE'),
(3, 'FRONTEND-REACT', 'Phát triển Frontend Hiện đại với React & TypeScript', 'Chương trình chuyên sâu Frontend Web', 3, 12000000.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE `program_name` = VALUES(`program_name`);

-- Danh mục môn học mẫu
INSERT INTO `subjects` (`id`, `subject_code`, `subject_name`, `total_sessions`, `credit_weight`, `min_pass_score`, `learning_outcomes`, `status`) VALUES
(1, 'CS101', 'Nhập môn Lập trình và Thuật toán', 12, 2.00, 5.00, 'Nắm vững tư duy logic và thuật toán cơ bản', 'ACTIVE'),
(2, 'DB101', 'Cơ sở dữ liệu Quan hệ & MySQL', 15, 2.50, 5.00, 'Thiết kế chuẩn hóa CSDL và truy vấn SQL phức tạp', 'ACTIVE'),
(3, 'JAVA101', 'Lập trình Java Căn bản & OOP', 18, 3.00, 5.00, 'Lập trình hướng đối tượng với Java', 'ACTIVE'),
(4, 'WEB101', 'Phát triển Ứng dụng Web với Servlet & JSP', 20, 3.50, 5.00, 'Xây dựng Web Enterprise với MVC, Servlet, JSP', 'ACTIVE'),
(5, 'DA101', 'Python cho Khoa học Dữ liệu', 16, 2.50, 5.00, 'Xử lý và trực quan hóa dữ liệu với Python', 'ACTIVE'),
(6, 'BI101', 'Trực quan hóa Dữ liệu với Power BI', 14, 2.00, 5.00, 'Xây dựng Dashboard phân tích báo cáo', 'ACTIVE')
ON DUPLICATE KEY UPDATE `subject_name` = VALUES(`subject_name`);

-- Ánh xạ môn học vào chương trình đào tạo (Program Subjects)
-- Chương trình 1: CNTT-FULLSTACK có 4 môn
INSERT INTO `program_subjects` (`program_id`, `subject_id`, `order_index`, `prerequisite_subject_id`) VALUES
(1, 1, 1, NULL),
(1, 2, 2, 1),
(1, 3, 3, 1),
(1, 4, 4, 3)
ON DUPLICATE KEY UPDATE `order_index` = VALUES(`order_index`);

-- Chương trình 2: DATA-ANALYTICS có 3 môn
INSERT INTO `program_subjects` (`program_id`, `subject_id`, `order_index`, `prerequisite_subject_id`) VALUES
(2, 1, 1, NULL),
(2, 2, 2, 1),
(2, 5, 3, 1)
ON DUPLICATE KEY UPDATE `order_index` = VALUES(`order_index`);

-- Chương trình 3: FRONTEND-REACT có 2 môn
INSERT INTO `program_subjects` (`program_id`, `subject_id`, `order_index`, `prerequisite_subject_id`) VALUES
(3, 1, 1, NULL),
(3, 2, 2, 1)
ON DUPLICATE KEY UPDATE `order_index` = VALUES(`order_index`);
