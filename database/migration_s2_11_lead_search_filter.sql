-- =====================================================================
-- Migration: S2-11 - Tìm kiếm và lọc lead theo nhiều điều kiện
-- Story S2-11 (IDTTX-166 / Subtask IDTTX-198): [BE] DB + API Tìm kiếm và lọc lead
-- 
-- Yêu cầu nghiệp vụ:
-- 1. Lọc theo trạng thái, nguồn, người phụ trách, khoảng thời gian
-- 2. Tìm nhanh theo tên hoặc số điện thoại
-- 3. Tìm lại được cuộc trao đổi từ tháng trước khi khách gọi lại (Nhật ký tương tác)
-- 
-- Author: Nguyen Minh Ngoc (minhngoc678)
-- Branch: s2-11-minhngoc(BE)
-- Date: 2026-10-06
-- =====================================================================

-- 1. Bảng `leads` quản lý khách hàng tiềm năng với đầy đủ trường lọc & tìm kiếm
CREATE TABLE IF NOT EXISTS `leads` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL COMMENT 'Họ và tên khách hàng tiềm năng',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại liên hệ',
    `email` VARCHAR(150) NULL COMMENT 'Email khách hàng (nếu có)',
    `source` VARCHAR(50) DEFAULT 'WEBSITE' COMMENT 'Nguồn: WEBSITE, FACEBOOK, REFERRAL, HOTLINE, EVENT, TIKTOK',
    `course` VARCHAR(100) NULL COMMENT 'Khóa học/chương trình đào tạo quan tâm',
    `interested_program_id` INT NULL COMMENT 'Khóa ngoại liên kết tới bảng programs (nếu có)',
    `assigned_counselor_id` BIGINT NULL COMMENT 'Tư vấn viên (Admissions) phụ trách hiện tại',
    `status` ENUM('NEW', 'CONTACTED', 'CONSULTING', 'TRIAL_SCHEDULED', 'WON', 'REJECTED') DEFAULT 'NEW' COMMENT 'Trạng thái xử lý Lead',
    `reject_reason` VARCHAR(255) NULL COMMENT 'Lý do từ chối nếu status = REJECTED',
    `notes` TEXT NULL COMMENT 'Nội dung tư vấn / ghi chú ban đầu',
    `last_interaction_at` DATETIME NULL COMMENT 'Thời điểm tương tác gần nhất',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo lead',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật gần nhất',
    CONSTRAINT `fk_leads_counselor` FOREIGN KEY (`assigned_counselor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    -- Các chỉ mục tối ưu tìm kiếm và lọc đa điều kiện (S2-11)
    INDEX `idx_leads_phone` (`phone`),
    INDEX `idx_leads_full_name` (`full_name`),
    INDEX `idx_leads_status` (`status`),
    INDEX `idx_leads_source` (`source`),
    INDEX `idx_leads_counselor` (`assigned_counselor_id`),
    INDEX `idx_leads_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng thông tin khách hàng tiềm năng phục vụ tìm kiếm và lọc';

-- 2. Bảng `lead_interactions` ghi nhật ký tương tác / cuộc gọi trao đổi (S2-11)
CREATE TABLE IF NOT EXISTS `lead_interactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lead_id` BIGINT NOT NULL COMMENT 'Lead liên quan',
    `counselor_id` BIGINT NOT NULL COMMENT 'Tư vấn viên thực hiện cuộc gọi/trao đổi',
    `interaction_type` ENUM('CALL', 'NOTE', 'MEETING', 'STATUS_CHANGE', 'ASSIGNMENT') DEFAULT 'NOTE' COMMENT 'Loại tương tác',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề ngắn gọn cuộc trao đổi',
    `content` TEXT NULL COMMENT 'Nội dung chi tiết trao đổi từ tháng trước',
    `duration_seconds` INT NULL COMMENT 'Thời lượng cuộc gọi nếu có (giây)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận',
    CONSTRAINT `fk_lead_interaction_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_lead_interaction_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_lead_interaction_lead_created` (`lead_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký tương tác chăm sóc khách hàng phục vụ tra cứu lịch sử trao đổi';

-- 3. Dữ liệu mẫu (Seed Data) mô phỏng khách hàng và cuộc trao đổi từ tháng trước
INSERT INTO `leads` (`id`, `full_name`, `phone`, `email`, `source`, `course`, `assigned_counselor_id`, `status`, `notes`, `created_at`)
VALUES
(1, 'Vũ Minh Anh', '0988123456', 'minhanh.vu@gmail.com', 'FACEBOOK', 'Lập trình Web Fullstack', 5, 'CONSULTING', 'Quan tâm lộ trình từ cơ bản, hỏi về học phí trả góp 0%', DATE_SUB(NOW(), INTERVAL 35 DAY)),
(2, 'Ngô Quốc Bảo', '0912345679', 'bao.ngo@hotmail.com', 'WEBSITE', 'Java Spring Boot', 5, 'TRIAL_SCHEDULED', 'Hẹn test đầu vào chiều thứ 7 phòng P.102', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3, 'Bùi Thu Hà', '0977889900', 'thuha.bui@gmail.com', 'REFERRAL', 'Python & Data Analysis', NULL, 'NEW', 'Bạn học giới thiệu, muốn tư vấn lớp học ca tối', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 'Đặng Hoàng Nam', '0933221100', 'nam.dang@gmail.com', 'TIKTOK', 'Thiết kế UI/UX', NULL, 'NEW', 'Điền form từ link bio TikTok', NOW()),
(5, 'Trần Minh Đức', '0903456789', 'duc.tran@yahoo.com', 'HOTLINE', 'Lập trình Frontend React', 6, 'WON', 'Đã đóng học phí đầy đủ', DATE_SUB(NOW(), INTERVAL 40 DAY))
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);

-- Nhật ký tương tác từ tháng trước của khách hàng Vũ Minh Anh (Lead 1)
INSERT INTO `lead_interactions` (`lead_id`, `counselor_id`, `interaction_type`, `title`, `content`, `duration_seconds`, `created_at`)
VALUES
(1, 5, 'CALL', 'Cuộc gọi tư vấn học phí tháng trước', 'Khách hàng hỏi chi tiết về chính sách chia nhỏ đợt đóng học phí 3 lần và lộ trình thực tập tại doanh nghiệp.', 250, DATE_SUB(NOW(), INTERVAL 35 DAY)),
(1, 5, 'NOTE', 'Ghi chú nhu cầu', 'Khách hẹn đầu tháng sau sẽ gọi lại để chốt lịch khai giảng.', NULL, DATE_SUB(NOW(), INTERVAL 34 DAY))
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);
