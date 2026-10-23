-- =====================================================================
-- Migration: S3-03 - Chuyển lead đã chốt thành hồ sơ học viên
-- Story S3-03 (IDTTX-211 / Subtask IDTTX-240): [BE] Thiết kế CSDL & Viết API chuyển lead thành học viên
-- 
-- Yêu cầu nghiệp vụ Story S3-03:
-- 1. Dữ liệu lead được chuyển sang hồ sơ học viên, không phải nhập lại
-- 2. Hệ thống sinh mã học viên tự động (định dạng HV26xxxx cho năm 2026)
-- 3. Tài khoản đăng nhập cho học viên được tạo (định dạng S25200212xxx@tms.vn) và gửi email kích hoạt
-- 4. Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa
--
-- Author: Nguyen Minh Ngoc (minhngoc678)
-- Branch: feature/S3-03-chuyen-hoc-vien
-- Date: 2026-10-16
-- =====================================================================

-- 1. Cập nhật bảng `leads`: Bổ sung các cờ và trường quản lý chuyển đổi học viên
ALTER TABLE `leads` 
    ADD COLUMN IF NOT EXISTS `student_code` VARCHAR(20) NULL COMMENT 'Mã học viên được cấp sau khi chuyển đổi (HV26xxxx)',
    ADD COLUMN IF NOT EXISTS `is_converted` BOOLEAN DEFAULT FALSE COMMENT 'Đánh dấu lead đã chuyển thành học viên chưa',
    ADD COLUMN IF NOT EXISTS `is_locked` BOOLEAN DEFAULT FALSE COMMENT 'Khóa lead: không cho kéo thẻ, sửa thông tin hay thêm nhật ký',
    ADD COLUMN IF NOT EXISTS `converted_at` DATETIME NULL COMMENT 'Thời điểm chuyển đổi thành học viên',
    ADD INDEX IF NOT EXISTS `idx_leads_student_code` (`student_code`),
    ADD INDEX IF NOT EXISTS `idx_leads_is_converted` (`is_converted`);

-- 2. Bảng `students` quản lý hồ sơ học viên toàn diện
CREATE TABLE IF NOT EXISTS `students` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính tự tăng',
    `code` VARCHAR(20) NOT NULL UNIQUE COMMENT 'Mã học viên chuẩn (HV260143)',
    `name` VARCHAR(150) NOT NULL COMMENT 'Họ và tên học viên (sao chép từ Lead)',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại liên hệ (sao chép từ Lead)',
    `email` VARCHAR(150) NOT NULL COMMENT 'Email liên hệ nhận thông tin kích hoạt',
    `login` VARCHAR(150) NOT NULL UNIQUE COMMENT 'Email đăng nhập hệ thống cấp (S25200212xxx@tms.vn)',
    `program` VARCHAR(150) NOT NULL COMMENT 'Chương trình đào tạo nhập học',
    `status` ENUM('Mới nhập học', 'Đang học', 'Bảo lưu', 'Đã tốt nghiệp', 'Thôi học') DEFAULT 'Mới nhập học' COMMENT 'Trạng thái học tập',
    `enrolled_at` DATE NOT NULL COMMENT 'Ngày nhập học chính thức (YYYY-MM-DD)',
    `class_code` VARCHAR(50) NULL COMMENT 'Mã lớp đang theo học nếu đã xếp lớp',
    `birth` DATE NULL COMMENT 'Ngày sinh của học viên',
    `address` VARCHAR(255) NULL COMMENT 'Địa chỉ thường trú / liên hệ',
    `counselor` VARCHAR(100) NULL COMMENT 'Tư vấn viên phụ trách lead gốc',
    `source` VARCHAR(50) NULL COMMENT 'Nguồn tuyển sinh tiếp cận ban đầu',
    `entry_score` VARCHAR(50) NULL COMMENT 'Điểm thi đầu vào / xếp lớp',
    `level` VARCHAR(50) NULL COMMENT 'Trình độ ban đầu (Cơ bản, Nâng cao...)',
    `background` TEXT NULL COMMENT 'Lý lịch học vấn / công việc',
    `lead_id` BIGINT NULL COMMENT 'Khóa ngoại liên kết tới lead gốc',
    `user_id` BIGINT NULL COMMENT 'Khóa ngoại liên kết tới tài khoản đăng nhập users',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo hồ sơ',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật gần nhất',
    CONSTRAINT `fk_students_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE SET NULL,
    INDEX `idx_students_code` (`code`),
    INDEX `idx_students_name` (`name`),
    INDEX `idx_students_phone` (`phone`),
    INDEX `idx_students_email` (`email`),
    INDEX `idx_students_login` (`login`),
    INDEX `idx_students_status` (`status`),
    INDEX `idx_students_program` (`program`),
    INDEX `idx_students_class_code` (`class_code`),
    INDEX `idx_students_enrolled_at` (`enrolled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hồ sơ học viên cho Story S3-03 trở đi';

-- 3. Bảng `student_notes` ghi nhận nhật ký / ghi chú chăm sóc học viên
CREATE TABLE IF NOT EXISTS `student_notes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT 'Học viên liên quan',
    `student_code` VARCHAR(20) NOT NULL COMMENT 'Mã học viên',
    `author_name` VARCHAR(100) NOT NULL COMMENT 'Người ghi chú (Tư vấn viên, Quản lý đào tạo)',
    `note_text` TEXT NOT NULL COMMENT 'Nội dung ghi chú',
    `noted_at` DATE NOT NULL COMMENT 'Ngày ghi nhận',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_student_notes_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    INDEX `idx_student_notes_student` (`student_id`, `noted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký ghi chú học viên';

-- 4. Bảng `student_class_history` lịch sử lớp học cũ của học viên
CREATE TABLE IF NOT EXISTS `student_class_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL COMMENT 'Học viên liên quan',
    `student_code` VARCHAR(20) NOT NULL COMMENT 'Mã học viên',
    `class_code` VARCHAR(50) NOT NULL COMMENT 'Mã lớp đã học',
    `program` VARCHAR(150) NOT NULL COMMENT 'Tên chương trình đào tạo của lớp',
    `period` VARCHAR(50) NOT NULL COMMENT 'Thời gian học (vd 06/2026 - 07/2026)',
    `result` VARCHAR(100) NOT NULL COMMENT 'Kết quả học tập (vd Hoàn thành · 8,2 hoặc Bảo lưu)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_student_history_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    INDEX `idx_student_history_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử các lớp học cũ của học viên';

-- 5. Bảng `student_activation_emails` ghi log email kích hoạt tài khoản đã gửi
CREATE TABLE IF NOT EXISTS `student_activation_emails` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_code` VARCHAR(20) NOT NULL COMMENT 'Mã học viên',
    `recipient_email` VARCHAR(150) NOT NULL COMMENT 'Email nhận thông tin kích hoạt',
    `login_username` VARCHAR(150) NOT NULL COMMENT 'Tên tài khoản đăng nhập được cấp',
    `temp_password` VARCHAR(100) NOT NULL COMMENT 'Mật khẩu tạm thời gửi kèm',
    `status` ENUM('SENT', 'FAILED', 'PENDING') DEFAULT 'SENT' COMMENT 'Trạng thái gửi mail',
    `sent_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm gửi email',
    INDEX `idx_activation_student` (`student_code`),
    INDEX `idx_activation_recipient` (`recipient_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử gửi email kích hoạt tài khoản cho học viên mới';

-- 6. Dữ liệu mẫu (Seed Data) học viên ban đầu đồng bộ với frontend
INSERT INTO `students` (`code`, `name`, `phone`, `email`, `login`, `program`, `status`, `enrolled_at`, `counselor`, `source`, `lead_id`)
VALUES
('HV260142', 'ĐỖ TUẤN KIỆT', '0971 846 320', 'kiet.dt@gmail.com', 'S25200212142@tms.vn', 'Lập trình web Full-stack', 'Mới nhập học', '2026-10-05', 'TRẦN THU HÀ', 'Biểu mẫu web', 5),
('HV260140', 'LÊ MINH KHÔI', '0909 518 264', 'khoi.lm@gmail.com', 'S25200212140@tms.vn', 'Lập trình web Full-stack', 'Mới nhập học', '2026-10-03', 'HOÀNG VĂN TƯ', 'Biểu mẫu web', NULL),
('HV260139', 'PHẠM NGỌC HÂN', '0933 704 186', 'han.pn@gmail.com', 'S25200212139@tms.vn', 'Lập trình web Full-stack', 'Mới nhập học', '2026-10-02', 'LÊ THỊ THU HÀ', 'Hotline', NULL),
('HV260101', 'NGUYỄN MINH ANH', '0912 345 678', 'minhanh.ng@gmail.com', 'S25200212101@tms.vn', 'Lập trình web Full-stack', 'Đang học', '2026-08-12', 'TRẦN THU HÀ', 'Biểu mẫu web', 1)
ON DUPLICATE KEY UPDATE `updated_at` = NOW();
