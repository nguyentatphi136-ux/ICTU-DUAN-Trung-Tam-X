-- =====================================================================
-- Migration: S2-10 - Phân công lead cho tư vấn viên
-- Story S2-10 (IDTTX-165 / Subtask IDTTX-195): [BE] DB + API Phân công lead cho tư vấn viên
-- 
-- Yêu cầu nghiệp vụ:
-- 1. Phân công một hoặc nhiều lead cùng lúc cho một tư vấn viên
-- 2. Tư vấn viên chỉ nhìn thấy lead được giao cho mình
-- 3. Có ghi lịch sử chuyển giao (lead assignment / transfer history)
-- 
-- Author: Nguyen Minh Ngoc (minhngoc678)
-- Branch: s2-10-minhngoc(BE)
-- Date: 2026-10-06
-- =====================================================================

-- 1. Bảng `leads` quản lý khách hàng tiềm năng & phân công người phụ trách
CREATE TABLE IF NOT EXISTS `leads` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL COMMENT 'Họ và tên khách hàng tiềm năng',
    `phone` VARCHAR(20) NOT NULL COMMENT 'Số điện thoại liên hệ',
    `email` VARCHAR(150) NULL COMMENT 'Email khách hàng (nếu có)',
    `source` VARCHAR(50) DEFAULT 'WEBSITE' COMMENT 'Nguồn: WEBSITE, FACEBOOK, REFERRAL, HOTLINE, EVENT, TIKTOK',
    `course` VARCHAR(100) NULL COMMENT 'Khóa học/chương trình quan tâm',
    `interested_program_id` INT NULL COMMENT 'Khóa ngoại liên kết tới bảng programs (nếu có)',
    `assigned_counselor_id` BIGINT NULL COMMENT 'Tư vấn viên (Admissions) phụ trách hiện tại',
    `status` ENUM('NEW', 'CONTACTED', 'CONSULTING', 'TRIAL_SCHEDULED', 'WON', 'REJECTED') DEFAULT 'NEW' COMMENT 'Trạng thái xử lý Lead',
    `reject_reason` VARCHAR(255) NULL COMMENT 'Lý do từ chối nếu status = REJECTED',
    `notes` TEXT NULL COMMENT 'Nội dung tư vấn / ghi chú ban đầu',
    `last_interaction_at` DATETIME NULL COMMENT 'Thời điểm tương tác gần nhất',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo lead',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật gần nhất',
    CONSTRAINT `fk_leads_counselor` FOREIGN KEY (`assigned_counselor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_leads_phone` (`phone`),
    INDEX `idx_leads_status` (`status`),
    INDEX `idx_leads_counselor` (`assigned_counselor_id`),
    INDEX `idx_leads_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng thông tin khách hàng tiềm năng (Leads)';

-- 2. Bảng `lead_assignments` ghi lịch sử chuyển giao / phân công lead (S2-10: Có ghi lịch sử chuyển giao)
CREATE TABLE IF NOT EXISTS `lead_assignments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lead_id` BIGINT NOT NULL COMMENT 'Lead được phân công',
    `previous_counselor_id` BIGINT NULL COMMENT 'Tư vấn viên trước đó (NULL nếu là phân công lần đầu)',
    `new_counselor_id` BIGINT NOT NULL COMMENT 'Tư vấn viên được phân công mới',
    `assigned_by` BIGINT NOT NULL COMMENT 'Người thực hiện phân công (Quản lý đào tạo hoặc Admin)',
    `note` VARCHAR(500) NULL COMMENT 'Ghi chú phân công / lý do chuyển giao',
    `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm phân công',
    CONSTRAINT `fk_lead_assign_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_lead_assign_prev_counselor` FOREIGN KEY (`previous_counselor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_lead_assign_new_counselor` FOREIGN KEY (`new_counselor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_lead_assign_by` FOREIGN KEY (`assigned_by`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_lead_assign_lead` (`lead_id`),
    INDEX `idx_lead_assign_new_counselor` (`new_counselor_id`),
    INDEX `idx_lead_assign_assigned_at` (`assigned_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử phân công và chuyển giao lead giữa các tư vấn viên';

-- 3. Dữ liệu mẫu (Seed Data) phục vụ kiểm thử phân công
INSERT INTO `leads` (`id`, `full_name`, `phone`, `email`, `source`, `course`, `assigned_counselor_id`, `status`, `notes`, `created_at`)
VALUES
(1, 'Vũ Minh Anh', '0988123456', 'minhanh.vu@gmail.com', 'FACEBOOK', 'Lập trình Web Fullstack', 5, 'CONSULTING', 'Quan tâm lộ trình từ cơ bản', NOW()),
(2, 'Ngô Quốc Bảo', '0912345679', 'bao.ngo@hotmail.com', 'WEBSITE', 'Java Spring Boot', 5, 'TRIAL_SCHEDULED', 'Hẹn test đầu vào', NOW()),
(3, 'Bùi Thu Hà', '0977889900', 'thuha.bui@gmail.com', 'REFERRAL', 'Python & Data Analysis', NULL, 'NEW', 'Chưa phân công tư vấn viên', NOW()),
(4, 'Đặng Hoàng Nam', '0933221100', 'nam.dang@gmail.com', 'TIKTOK', 'Thiết kế UI/UX', NULL, 'NEW', 'Chưa phân công tư vấn viên', NOW()),
(5, 'Trần Minh Đức', '0903456789', 'duc.tran@yahoo.com', 'HOTLINE', 'Lập trình Frontend React', 6, 'WON', 'Đã phân công cho tư vấn viên khác', NOW())
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);
