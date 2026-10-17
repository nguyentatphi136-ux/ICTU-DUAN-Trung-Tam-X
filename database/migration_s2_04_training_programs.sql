-- =============================================================================
-- MIGRATION S2-04: QUẢN LÝ DANH MỤC CHƯƠNG TRÌNH ĐÀO TẠO (TRAINING PROGRAMS)
-- User Story: S2-04 (EP-02)
-- =============================================================================

-- 1. Tạo bảng danh mục chương trình đào tạo (programs)
CREATE TABLE IF NOT EXISTS `programs` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `program_code` VARCHAR(50) NOT NULL UNIQUE,
    `program_name` VARCHAR(150) NOT NULL,
    `description` TEXT NULL,
    `duration` INT NOT NULL DEFAULT 60 COMMENT 'Tổng thời lượng (giờ hoặc buổi > 0)',
    `standard_tuition` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Học phí chuẩn (>= 0)',
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' COMMENT 'ACTIVE: đang áp dụng, INACTIVE: ngừng áp dụng',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_programs_code` (`program_code`),
    INDEX `idx_programs_status` (`status`)
) ENGINE=InnoDB COMMENT='Bảng danh mục chương trình đào tạo';

-- 2. Tạo bảng lớp học (classes) nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS `classes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_code` VARCHAR(50) NOT NULL UNIQUE,
    `class_name` VARCHAR(150) NOT NULL,
    `program_id` INT NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NULL,
    `max_capacity` INT NOT NULL DEFAULT 30,
    `default_classroom_id` INT NULL,
    `study_mode` ENUM('OFFLINE', 'ONLINE', 'HYBRID') DEFAULT 'OFFLINE',
    `status` ENUM('PLANNING', 'IN_PROGRESS', 'FINISHED', 'CANCELLED') DEFAULT 'PLANNING',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_classes_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE RESTRICT,
    INDEX `idx_classes_program_status` (`program_id`, `status`)
) ENGINE=InnoDB COMMENT='Bảng quản lý lớp học';

-- 3. Đăng ký quyền PROGRAM_MANAGE vào bảng permissions
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`)
VALUES ('PROGRAM_MANAGE', 'Quản lý chương trình', 'PROGRAM', 'Quản lý danh mục chương trình đào tạo')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- 4. Gán quyền PROGRAM_MANAGE cho Quản lý đào tạo (TRAINING_MANAGER / TrainingManager)
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code = 'PROGRAM_MANAGE'
WHERE r.role_code IN ('TRAINING_MANAGER', 'TrainingManager')
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

-- 5. Gán quyền PROGRAM_MANAGE cho Quản trị viên (ADMIN / Admin)
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code = 'PROGRAM_MANAGE'
WHERE r.role_code IN ('ADMIN', 'Admin')
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

-- 6. Dữ liệu mẫu (Seed Data)
INSERT INTO `programs` (`id`, `program_code`, `program_name`, `description`, `duration`, `standard_tuition`, `status`) VALUES
(1, 'JAVA-WEB', 'Chương trình Java Web Chuyên nghiệp', 'Đào tạo Java Core, Servlet, JSP, Spring Boot và Microservices', 240, 15000000.00, 'ACTIVE'),
(2, 'REACT-FRONTEND', 'Khóa học Lập trình Frontend React', 'Đào tạo HTML/CSS, JavaScript ES6+, TypeScript, ReactJS & Redux', 180, 12000000.00, 'ACTIVE'),
(3, 'PYTHON-DATA', 'Lập trình Python & Phân tích Dữ liệu', 'Đào tạo Python cơ bản, Pandas, NumPy và Machine Learning ứng dụng', 200, 14000000.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE `program_name` = VALUES(`program_name`);

-- 7. Lớp học mẫu để kiểm tra ràng buộc business rule khi xoá
INSERT INTO `classes` (`id`, `class_code`, `class_name`, `program_id`, `start_date`, `end_date`, `status`) VALUES
(1, 'JAVA-K15', 'Lớp Java Web K15 (Đang chạy)', 1, '2026-02-01', '2026-08-01', 'IN_PROGRESS'),
(2, 'REACT-K01', 'Lớp React K01 (Đã hoàn thành)', 2, '2025-06-01', '2025-12-01', 'FINISHED')
ON DUPLICATE KEY UPDATE `class_name` = VALUES(`class_name`);
