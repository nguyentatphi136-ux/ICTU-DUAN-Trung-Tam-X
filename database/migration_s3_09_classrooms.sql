-- =============================================================================
-- MIGRATION SCRIPT: S3-09 - [BE] Thiết kế Database + API CRUD phòng học
-- Epic: EP-04 - Lớp học & Thời khóa biểu
-- Vai trò: Training Manager (Quản lý đào tạo)
-- Hệ cơ sở dữ liệu: MySQL 8.0+, InnoDB, UTF8MB4
-- =============================================================================

USE `ems_database`;

-- 1. Tạo bảng classrooms nếu chưa tồn tại
CREATE TABLE IF NOT EXISTS `classrooms` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `room_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã phòng học, duy nhất: VD: PH-101, OL-Zoom01',
    `room_name` VARCHAR(100) NOT NULL COMMENT 'Tên phòng học',
    `capacity` INT NULL DEFAULT 30 COMMENT 'Sức chứa tối đa (bắt buộc > 0 với phòng vật lý)',
    `room_type` ENUM('PHYSICAL', 'ONLINE') NOT NULL DEFAULT 'PHYSICAL' COMMENT 'Loại phòng: PHYSICAL hoặc ONLINE',
    `location` VARCHAR(255) NULL COMMENT 'Vị trí phòng vật lý (tòa nhà, tầng...)',
    `meeting_url` VARCHAR(255) NULL COMMENT 'Google Meet / Zoom URL nếu là phòng online',
    `description` TEXT NULL COMMENT 'Mô tả cơ sở vật chất, ghi chú',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Trạng thái hoạt động (true: hoạt động, false: ngừng dùng)',
    `status` ENUM('AVAILABLE', 'MAINTENANCE') DEFAULT 'AVAILABLE' COMMENT 'Trạng thái sẵn sàng',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_room_type` (`room_type`),
    INDEX `idx_room_active` (`is_active`)
) ENGINE=InnoDB COMMENT='Bảng danh mục phòng học vật lý và trực tuyến';

-- 2. Đảm bảo các cột mở rộng tồn tại nếu bảng đã được tạo từ schema cũ
SET @dbname = DATABASE();
SET @tablename = "classrooms";

-- Bổ sung location
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = "location") > 0,
  "SELECT 1",
  "ALTER TABLE classrooms ADD COLUMN location VARCHAR(255) NULL COMMENT 'Vị trí phòng vật lý' AFTER room_type"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Bổ sung description
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = "description") > 0,
  "SELECT 1",
  "ALTER TABLE classrooms ADD COLUMN description TEXT NULL COMMENT 'Mô tả phòng' AFTER meeting_url"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Bổ sung is_active
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = "is_active") > 0,
  "SELECT 1",
  "ALTER TABLE classrooms ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Trạng thái hoạt động' AFTER description"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Bổ sung created_at
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = "created_at") > 0,
  "SELECT 1",
  "ALTER TABLE classrooms ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP AFTER status"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Bổ sung updated_at
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = "updated_at") > 0,
  "SELECT 1",
  "ALTER TABLE classrooms ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. Bổ sung Permission cho Phân hệ Quản lý phòng học
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`) VALUES
('ROOM_VIEW', 'Xem phòng học', 'ROOM', 'Đọc danh sách và chi tiết phòng học'),
('ROOM_MANAGE', 'Quản lý phòng học', 'ROOM', 'Thêm, sửa, xóa và kiểm tra sức chứa phòng học')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- 4. Gán quyền cho ADMIN và TRAINING_MANAGER
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN ('ROOM_VIEW', 'ROOM_MANAGE')
WHERE r.role_code IN ('ADMIN', 'TRAINING_MANAGER')
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

-- 5. Seed dữ liệu mẫu phòng học
INSERT INTO `classrooms` (`room_code`, `room_name`, `room_type`, `capacity`, `location`, `meeting_url`, `description`, `is_active`, `status`) VALUES
('PH-101', 'Phòng lý thuyết 101', 'PHYSICAL', 20, 'Tầng 1 - Tòa A', NULL, 'Trang bị máy chiếu, điều hòa, bảng từ', TRUE, 'AVAILABLE'),
('PH-102', 'Phòng thực hành máy tính 102', 'PHYSICAL', 30, 'Tầng 1 - Tòa B', NULL, '30 bộ máy tính cấu hình Core i7, 16GB RAM', TRUE, 'AVAILABLE'),
('PH-201', 'Hội trường lớn 201', 'PHYSICAL', 50, 'Tầng 2 - Tòa A', NULL, 'Âm thanh vòm, máy chiếu 4K', TRUE, 'AVAILABLE'),
('OL-MEET-01', 'Phòng học trực tuyến Google Meet 01', 'ONLINE', NULL, NULL, 'https://meet.google.com/abc-defg-hij', 'Phòng học Google Meet cho các lớp đào tạo từ xa', TRUE, 'AVAILABLE'),
('OL-ZOOM-01', 'Phòng học trực tuyến Zoom Pro 01', 'ONLINE', NULL, NULL, 'https://zoom.us/j/1234567890', 'Phòng học Zoom bản quyền 300 người', TRUE, 'AVAILABLE')
ON DUPLICATE KEY UPDATE
`room_name` = VALUES(`room_name`),
`capacity` = VALUES(`capacity`),
`room_type` = VALUES(`room_type`),
`location` = VALUES(`location`),
`meeting_url` = VALUES(`meeting_url`),
`is_active` = VALUES(`is_active`);
