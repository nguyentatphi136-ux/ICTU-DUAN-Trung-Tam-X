-- =============================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EDUCATION MANAGEMENT SYSTEM - EMS)
-- MIGRATION SCRIPT CHO USER STORY S6-01 (Jira: IDTTX-273, Subtask: IDTTX-280 [BE])
-- Mô tả: Chấm điểm và viết nhận xét bài nộp của giảng viên;
--        hỗ trợ chấm liên tục 25 bài/buổi tối, xem trước tệp nộp, mở repo tab mới,
--        điểm chỉ hiện khi công bố, và ghi nhật ký sửa điểm.
-- =============================================================================

USE `ems_database`;

-- 1. Bảng bài tập (Assignments)
CREATE TABLE IF NOT EXISTS `assignments` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `class_code` VARCHAR(50) NOT NULL COMMENT 'Mã lớp học (VD: WEB-K15)',
    `subject_name` VARCHAR(150) NOT NULL COMMENT 'Tên môn học',
    `session_code` VARCHAR(50) NULL COMMENT 'Mã buổi học trong TKB (VD: B14)',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề bài tập',
    `description` TEXT NULL COMMENT 'Mô tả, yêu cầu bài tập',
    `due_date` DATETIME NOT NULL COMMENT 'Hạn nộp bài',
    `max_score` DECIMAL(4, 1) NOT NULL DEFAULT 10.0 COMMENT 'Điểm tối đa',
    `late_policy` ENUM('Cho nộp', 'Chặn') DEFAULT 'Cho nộp' COMMENT 'Chính sách nộp muộn',
    `created_by` VARCHAR(100) NOT NULL DEFAULT 'VU DUC LONG',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_asg_class` (`class_code`),
    INDEX `idx_asg_due` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Danh mục bài tập được giao S6-01';

-- 2. Bảng bài nộp của học viên (Assignment Submissions)
CREATE TABLE IF NOT EXISTS `assignment_submissions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `assignment_id` INT NOT NULL COMMENT 'Mã bài tập',
    `student_code` VARCHAR(50) NOT NULL COMMENT 'Mã học viên (HV...)',
    `student_name` VARCHAR(100) NOT NULL COMMENT 'Họ tên học viên',
    `student_email` VARCHAR(150) NULL,
    `version_no` INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản nộp (Bản 1, Bản 2...)',
    `submitted_at` DATETIME NOT NULL COMMENT 'Thời điểm nộp bài',
    `is_late` TINYINT(1) DEFAULT 0 COMMENT '1: Nộp muộn, 0: Đúng hạn',
    `late_hours` DECIMAL(5, 1) DEFAULT 0.0 COMMENT 'Số giờ trễ hạn',
    `repo_url` VARCHAR(500) NULL COMMENT 'Đường dẫn Repository (GitHub/GitLab)',
    `student_note` TEXT NULL COMMENT 'Ghi chú của học viên khi nộp bài',
    `state` ENUM('Chưa nộp', 'Chưa chấm', 'Đã chấm - Nháp', 'Đã công bố') DEFAULT 'Chưa chấm',
    INDEX `idx_sub_asg_st` (`assignment_id`, `student_code`),
    INDEX `idx_sub_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bài nộp của học viên S6-01';

-- 3. Bảng tệp đính kèm bài nộp (Submission Files)
CREATE TABLE IF NOT EXISTS `submission_files` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `submission_id` INT NOT NULL COMMENT 'Mã bài nộp',
    `file_name` VARCHAR(255) NOT NULL,
    `file_size_bytes` BIGINT NOT NULL DEFAULT 0,
    `formatted_size` VARCHAR(50) NOT NULL DEFAULT '0 KB',
    `file_type` VARCHAR(20) NOT NULL DEFAULT 'PDF',
    `preview_content` LONGTEXT NULL COMMENT 'Nội dung xem trước (text, code, trích xuất báo cáo)',
    INDEX `idx_sf_sub` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tệp tài liệu đính kèm bài nộp S6-01';

-- 4. Bảng điểm số và nhận xét (Submission Grades)
CREATE TABLE IF NOT EXISTS `submission_grades` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `submission_id` INT NOT NULL UNIQUE COMMENT 'Mã bài nộp',
    `score` DECIMAL(4, 1) NOT NULL COMMENT 'Điểm số từ 0 đến max_score',
    `comment` TEXT NOT NULL COMMENT 'Nhận xét của giảng viên',
    `graded_by` VARCHAR(100) NOT NULL COMMENT 'Tên giảng viên chấm',
    `graded_at` DATETIME NOT NULL COMMENT 'Thời điểm chấm',
    `is_published` TINYINT(1) DEFAULT 0 COMMENT '0: Nháp, 1: Đã công bố cho học viên',
    `published_at` DATETIME NULL COMMENT 'Thời điểm công bố',
    `published_by` VARCHAR(100) NULL COMMENT 'Người thực hiện công bố',
    INDEX `idx_sg_sub` (`submission_id`),
    INDEX `idx_sg_published` (`is_published`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Điểm và nhận xét bài nộp S6-01';

-- 5. Bảng nhật ký sửa điểm (Grade Audit Logs)
CREATE TABLE IF NOT EXISTS `grade_audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `submission_id` INT NOT NULL COMMENT 'Mã bài nộp',
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm thao tác',
    `actor_name` VARCHAR(100) NOT NULL COMMENT 'Người sửa / chấm / công bố',
    `action` ENUM('Chấm lần đầu', 'Sửa điểm', 'Công bố điểm', 'Cập nhật nhận xét') NOT NULL,
    `score_before` DECIMAL(4, 1) NULL COMMENT 'Điểm trước khi sửa (null nếu chấm lần đầu)',
    `score_after` DECIMAL(4, 1) NOT NULL COMMENT 'Điểm sau khi sửa',
    `reason` VARCHAR(255) NULL COMMENT 'Lý do sửa điểm (bắt buộc khi sửa điểm đã công bố)',
    `log_text` TEXT NOT NULL COMMENT 'Nội dung tóm tắt hiển thị',
    INDEX `idx_gal_sub` (`submission_id`),
    INDEX `idx_gal_time` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký sửa điểm S6-01';

-- 6. Khởi tạo dữ liệu mẫu bài tập 14 (chuẩn Jira IDTTX-273) cho lớp WEB-K15
INSERT INTO `assignments` (`id`, `class_code`, `subject_name`, `session_code`, `title`, `description`, `due_date`, `max_score`, `late_policy`, `created_by`)
VALUES (
    14,
    'WEB-K15',
    'WEB-02: Lập trình JavaScript & Web Frontend',
    'B14',
    'Bài 14: Gọi API danh sách sản phẩm',
    'Dùng fetch gọi API danh sách sản phẩm và hiển thị dạng lưới thẻ. Có trạng thái loading và bắt lỗi.',
    '2026-10-23 23:59:00',
    10.0,
    'Cho nộp',
    'VU DUC LONG'
) ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 7. Khởi tạo dữ liệu mẫu bài nộp học viên HV260101 (Đã công bố) và HV260102 (Đang lưu nháp)
INSERT INTO `assignment_submissions` (`id`, `assignment_id`, `student_code`, `student_name`, `student_email`, `version_no`, `submitted_at`, `is_late`, `late_hours`, `repo_url`, `student_note`, `state`)
VALUES 
(1001, 14, 'HV260101', 'NGUYEN MINH ANH', 'minhanh.ng@gmail.com', 1, '2026-10-23 20:00:00', 0, 0.0, 'https://github.com/minhanh-ng/bai-14', 'Em đã bổ sung thêm tính năng tìm kiếm sản phẩm và loading.', 'Đã công bố'),
(1002, 14, 'HV260102', 'HOANG YEN NHI', 'nhi.hy@gmail.com', 1, '2026-10-23 20:45:00', 0, 0.0, 'https://github.com/hoangyennhi/bai-14', 'Em gửi thầy bài nộp đúng hạn.', 'Đã chấm - Nháp')
ON DUPLICATE KEY UPDATE `student_name` = VALUES(`student_name`);

INSERT INTO `submission_grades` (`submission_id`, `score`, `comment`, `graded_by`, `graded_at`, `is_published`, `published_at`, `published_by`)
VALUES 
(1001, 9.0, 'Làm đủ yêu cầu, code sạch, có xử lý lỗi mạng và spinner tải rất tốt.', 'VU DUC LONG', '2026-10-23 20:30:00', 1, '2026-10-23 20:35:00', 'VU DUC LONG'),
(1002, 8.5, 'Đúng yêu cầu, cần thêm kiểm thử cho trường hợp biên danh sách rỗng.', 'VU DUC LONG', '2026-10-23 21:00:00', 0, NULL, NULL)
ON DUPLICATE KEY UPDATE `score` = VALUES(`score`);

INSERT INTO `grade_audit_logs` (`submission_id`, `timestamp`, `actor_name`, `action`, `score_before`, `score_after`, `reason`, `log_text`)
VALUES 
(1001, '2026-10-23 20:30:00', 'VU DUC LONG', 'Chấm lần đầu', NULL, 9.0, NULL, 'Chấm lần đầu: 9.'),
(1001, '2026-10-23 20:35:00', 'VU DUC LONG', 'Công bố điểm', 9.0, 9.0, NULL, 'Công bố điểm 9.'),
(1002, '2026-10-23 21:00:00', 'VU DUC LONG', 'Chấm lần đầu', NULL, 8.5, NULL, 'Chấm lần đầu: 8,5.');
