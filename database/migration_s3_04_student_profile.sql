-- =====================================================================
-- Migration: S3-04 - Quản lý hồ sơ học viên toàn diện
-- Story S3-04 (IDTTX-212 / Subtask IDTTX-241): [BE] Thiết kế CSDL & Viết API quản lý hồ sơ học viên
-- 
-- Yêu cầu nghiệp vụ Story S3-04:
-- 1. Xem và sửa thông tin cá nhân (SĐT, ngày sinh, email, địa chỉ), trình độ đầu vào (điểm thi, xếp loại, nền tảng), ghi chú
-- 2. Trang hồ sơ hiển thị lớp đang học (tiến độ buổi học, sĩ số, phòng học, ca học) và lịch sử các lớp đã học
-- 3. Tìm kiếm theo mã học viên, tên, số điện thoại; lọc theo trạng thái và chương trình đào tạo
-- 4. Phân quyền: Quản lý đào tạo, Admin, Tuyển sinh có toàn quyền; Giảng viên xem chi tiết; Học viên chỉ xem hồ sơ của mình
--
-- Author: Nguyen Minh Ngoc (minhngoc678)
-- Branch: feature/S3-04-ho-so-hoc-vien
-- Date: 2026-10-16
-- =====================================================================

-- 1. Bảng `classes` quản lý thông tin lớp học (phục vụ S3-04 & S3-05 trở đi)
CREATE TABLE IF NOT EXISTS `classes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Khóa chính tự tăng',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã lớp học (ví dụ WEB-K15, DATA-K07)',
    `program` VARCHAR(150) NOT NULL COMMENT 'Chương trình đào tạo của lớp',
    `start_date` DATE NOT NULL COMMENT 'Ngày khai giảng',
    `max_students` INT NOT NULL DEFAULT 30 COMMENT 'Sĩ số tối đa của lớp',
    `mode` ENUM('Trực tiếp', 'Trực tuyến', 'Kết hợp') NOT NULL DEFAULT 'Trực tiếp' COMMENT 'Hình thức học',
    `status` ENUM('Chuẩn bị', 'Đang học', 'Kết thúc', 'Huỷ') NOT NULL DEFAULT 'Chuẩn bị' COMMENT 'Trạng thái lớp',
    `done_sessions` INT NOT NULL DEFAULT 0 COMMENT 'Số buổi học đã hoàn thành',
    `slot` VARCHAR(100) NOT NULL COMMENT 'Khung giờ học (ví dụ: Tối 2-4-6, Chiều 2-4-6)',
    `room` VARCHAR(100) NULL COMMENT 'Phòng học được xếp (ví dụ: P.301, Lab 1)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm mở lớp',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_classes_code` (`code`),
    INDEX `idx_classes_status` (`status`),
    INDEX `idx_classes_program` (`program`),
    INDEX `idx_classes_room` (`room`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh mục lớp học';

-- 2. Bảng `class_subjects` danh sách môn học và phân công giảng dạy của lớp
CREATE TABLE IF NOT EXISTS `class_subjects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_code` VARCHAR(50) NOT NULL COMMENT 'Khóa ngoại tới mã lớp',
    `subject_code` VARCHAR(50) NOT NULL COMMENT 'Mã môn học',
    `subject_name` VARCHAR(150) NOT NULL COMMENT 'Tên môn học',
    `sessions` INT NOT NULL DEFAULT 0 COMMENT 'Số buổi của môn',
    `teacher_name` VARCHAR(150) NULL COMMENT 'Giảng viên chính phụ trách môn',
    `tas` JSON NULL COMMENT 'Danh sách tên trợ giảng (mảng JSON)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_cs_class` (`class_code`),
    INDEX `idx_cs_teacher` (`teacher_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Môn học trong lớp và phân công';

-- 3. Bảng `students` quản lý hồ sơ học viên toàn diện
CREATE TABLE IF NOT EXISTS `students` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) NOT NULL UNIQUE COMMENT 'Mã học viên chuẩn (HV260101)',
    `name` VARCHAR(150) NOT NULL COMMENT 'Họ và tên học viên',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại di động (10 số, đầu 03/05/07/08/09)',
    `email` VARCHAR(150) NOT NULL COMMENT 'Email liên hệ của học viên',
    `login` VARCHAR(150) NOT NULL UNIQUE COMMENT 'Email đăng nhập hệ thống cấp (S25200212xxx@tms.vn)',
    `program` VARCHAR(150) NOT NULL COMMENT 'Chương trình đào tạo theo học',
    `status` ENUM('Mới nhập học', 'Đang học', 'Bảo lưu', 'Đã tốt nghiệp', 'Thôi học') DEFAULT 'Mới nhập học' COMMENT 'Trạng thái học viên',
    `enrolled_at` DATE NOT NULL COMMENT 'Ngày nhập học chính thức',
    `class_code` VARCHAR(50) NULL COMMENT 'Mã lớp hiện tại đang học',
    `birth` DATE NULL COMMENT 'Ngày sinh học viên',
    `address` VARCHAR(255) NULL COMMENT 'Địa chỉ liên hệ',
    `counselor` VARCHAR(100) NULL COMMENT 'Tư vấn viên phụ trách',
    `source` VARCHAR(50) NULL COMMENT 'Nguồn tuyển sinh tiếp cận ban đầu',
    `entry_score` VARCHAR(50) NULL COMMENT 'Kết quả kiểm tra đầu vào (ví dụ: 6,5 / 10)',
    `level` VARCHAR(50) NULL COMMENT 'Xếp loại trình độ (Cơ bản, Nâng cao...)',
    `background` TEXT NULL COMMENT 'Nền tảng học vấn / kinh nghiệm',
    `lead_id` BIGINT NULL COMMENT 'ID Lead gốc nếu chuyển từ tuyển sinh',
    `user_id` BIGINT NULL COMMENT 'ID tài khoản trong bảng users',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_students_code` (`code`),
    INDEX `idx_students_name` (`name`),
    INDEX `idx_students_phone` (`phone`),
    INDEX `idx_students_email` (`email`),
    INDEX `idx_students_login` (`login`),
    INDEX `idx_students_status` (`status`),
    INDEX `idx_students_program` (`program`),
    INDEX `idx_students_class_code` (`class_code`),
    INDEX `idx_students_enrolled_at` (`enrolled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hồ sơ học viên cho Story S3-04';

-- 4. Bảng `student_notes` ghi nhật ký / ghi chú chăm sóc hồ sơ học viên
CREATE TABLE IF NOT EXISTS `student_notes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_code` VARCHAR(20) NOT NULL COMMENT 'Mã học viên',
    `author_name` VARCHAR(100) NOT NULL COMMENT 'Người ghi chú (Quản lý đào tạo, Tư vấn viên)',
    `note_text` TEXT NOT NULL COMMENT 'Nội dung ghi chú',
    `noted_at` DATE NOT NULL COMMENT 'Ngày ghi nhận ghi chú (YYYY-MM-DD)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_student_notes_code` (`student_code`, `noted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ghi chú hồ sơ học viên';

-- 5. Bảng `student_class_history` lưu lịch sử các lớp đã học của học viên
CREATE TABLE IF NOT EXISTS `student_class_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_code` VARCHAR(20) NOT NULL COMMENT 'Mã học viên',
    `class_code` VARCHAR(50) NOT NULL COMMENT 'Mã lớp đã học',
    `program` VARCHAR(150) NOT NULL COMMENT 'Chương trình đào tạo',
    `period` VARCHAR(50) NOT NULL COMMENT 'Khoảng thời gian học (ví dụ: 06/2026 – 07/2026)',
    `result` VARCHAR(100) NOT NULL COMMENT 'Kết quả đánh giá (ví dụ: Hoàn thành · 8,2 hoặc Bảo lưu)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sch_code` (`student_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử lớp học của học viên';

-- =====================================================================
-- Dữ liệu mẫu (Seed Data)
-- =====================================================================

-- Lớp học mẫu
INSERT INTO `classes` (`code`, `program`, `start_date`, `max_students`, `mode`, `status`, `done_sessions`, `slot`, `room`) VALUES
('WEB-K15', 'Lập trình web Full-stack', '2026-08-12', 30, 'Trực tiếp', 'Đang học', 22, 'Tối 2-4-6', 'P.301'),
('DATA-K07', 'Phân tích dữ liệu', '2026-07-20', 25, 'Kết hợp', 'Đang học', 30, 'Tối 3-5-7', 'P.301'),
('UIUX-K09', 'Thiết kế UI/UX', '2026-07-02', 20, 'Trực tiếp', 'Đang học', 26, 'Chiều 2-4-6', 'P.302'),
('JAVA-K12', 'Lập trình Java Backend', '2026-10-15', 30, 'Trực tiếp', 'Chuẩn bị', 0, 'Tối 2-4-6', 'Lab 1'),
('WEB-K16', 'Lập trình web Full-stack', '2026-10-20', 30, 'Kết hợp', 'Chuẩn bị', 0, 'Tối 3-5-7', 'Meet WEB'),
('TEST-K06', 'Kiểm thử phần mềm', '2026-11-02', 25, 'Trực tuyến', 'Chuẩn bị', 0, 'Tối 2-4-6', NULL),
('JAVA-K11', 'Lập trình Java Backend', '2026-03-10', 30, 'Trực tiếp', 'Kết thúc', 56, 'Tối 3-5-7', 'Lab 1'),
('TEST-K05', 'Kiểm thử phần mềm', '2026-05-05', 25, 'Trực tuyến', 'Huỷ', 4, 'Tối 3-5-7', NULL),
('GIT-K03', 'Git và làm việc nhóm', '2026-06-08', 40, 'Trực tuyến', 'Kết thúc', 4, 'Tối 7', NULL)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `done_sessions` = VALUES(`done_sessions`);

-- Hồ sơ học viên mẫu tiêu biểu (NGUYEN MINH ANH - HV260101)
INSERT INTO `students` (
    `code`, `name`, `phone`, `email`, `login`, `program`, `status`,
    `enrolled_at`, `class_code`, `birth`, `address`, `counselor`, `source`,
    `entry_score`, `level`, `background`, `lead_id`, `user_id`
) VALUES (
    'HV260101', 'NGUYEN MINH ANH', '0912 345 678', 'minhanh.ng@gmail.com', 'S25200212101@tms.vn',
    'Lập trình web Full-stack', 'Đang học', '2026-08-12', 'WEB-K15', '2004-03-15',
    '12 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'TRAN THU HA', 'Biểu mẫu web',
    '6,5 / 10', 'Cơ bản', 'Sinh viên năm 3 ngành Kinh tế, đã tự học HTML và CSS.',
    1, 7
)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `phone` = VALUES(`phone`), `address` = VALUES(`address`);

-- Ghi chú cho HV260101
INSERT INTO `student_notes` (`student_code`, `author_name`, `note_text`, `noted_at`) VALUES
('HV260101', 'LE HOANG NAM', 'Xin nghỉ hai buổi vì lịch thi ở trường, đã báo giảng viên.', '2026-09-22'),
('HV260101', 'TRAN THU HA', 'Muốn học lớp tối 2-4-6.', '2026-08-12');

-- Lịch sử lớp đã học cho HV260101
INSERT INTO `student_class_history` (`student_code`, `class_code`, `program`, `period`, `result`) VALUES
('HV260101', 'GIT-K03', 'Git và làm việc nhóm', '06/2026 – 07/2026', 'Hoàn thành · 8,2'),
('HV260101', 'WEB-K12', 'Lập trình web Full-stack', '02/2026 – 04/2026', 'Bảo lưu'),
('HV260101', 'TIN-K20', 'Tin học văn phòng', '09/2025 – 11/2025', 'Hoàn thành · 9,0');
