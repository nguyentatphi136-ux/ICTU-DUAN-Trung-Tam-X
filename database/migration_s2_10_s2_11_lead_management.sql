-- =====================================================================
-- Migration: S2-10 & S2-11 - Phân công Lead & Tìm kiếm / Lọc Lead đa điều kiện
-- Story S2-10 (IDTTX-165 / Subtask IDTTX-195): [BE] DB + API Phân công lead cho tư vấn viên
--   - Phân công một hoặc nhiều lead cùng lúc
--   - Tư vấn viên chỉ nhìn thấy lead được giao cho mình
--   - Có ghi lịch sử chuyển giao
-- Story S2-11 (IDTTX-166 / Subtask IDTTX-198): [BE] DB + API Tìm kiếm & Lọc lead đa điều kiện
--   - Lọc theo trạng thái, nguồn, người phụ trách, khoảng thời gian
--   - Tìm nhanh theo tên hoặc số điện thoại
-- Author: Nguyen Minh Ngoc (minhngoc678)
-- Date: 2026-10-06
-- =====================================================================

-- 1. Bảng `leads` quản lý khách hàng tiềm năng
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
    INDEX `idx_leads_phone` (`phone`),
    INDEX `idx_leads_status` (`status`),
    INDEX `idx_leads_source` (`source`),
    INDEX `idx_leads_counselor` (`assigned_counselor_id`),
    INDEX `idx_leads_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng thông tin khách hàng tiềm năng (Leads)';

-- 2. Bảng `lead_assignments` ghi lịch sử chuyển giao / phân công lead (S2-10)
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

-- 3. Bảng `lead_interactions` ghi nhật ký tương tác / cuộc gọi / ghi chú (S2-11)
CREATE TABLE IF NOT EXISTS `lead_interactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lead_id` BIGINT NOT NULL COMMENT 'Lead liên quan',
    `counselor_id` BIGINT NOT NULL COMMENT 'Người thực hiện tương tác',
    `interaction_type` ENUM('CALL', 'NOTE', 'MEETING', 'STATUS_CHANGE', 'ASSIGNMENT') DEFAULT 'NOTE',
    `title` VARCHAR(255) NOT NULL COMMENT 'Tiêu đề ngắn gọn tương tác',
    `content` TEXT NULL COMMENT 'Nội dung chi tiết cuộc gọi/trao đổi',
    `duration_seconds` INT NULL COMMENT 'Thời lượng cuộc gọi nếu có',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận',
    CONSTRAINT `fk_lead_interaction_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_lead_interaction_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_lead_interaction_lead_created` (`lead_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký tương tác chăm sóc khách hàng';

-- 4. Seed dữ liệu mẫu cho kiểm thử
INSERT INTO `leads` (`id`, `full_name`, `phone`, `email`, `source`, `course`, `assigned_counselor_id`, `status`, `notes`, `created_at`)
VALUES
(1, 'Vũ Minh Anh', '0988123456', 'minhanh.vu@gmail.com', 'FACEBOOK', 'Lập trình Web Fullstack', 7, 'CONSULTING', 'Quan tâm lộ trình từ cơ bản, hỏi về học phí trả góp', DATE_SUB(NOW(), INTERVAL 35 DAY)),
(2, 'Ngô Quốc Bảo', '0912345679', 'bao.ngo@hotmail.com', 'WEBSITE', 'Java Spring Boot', 7, 'TRIAL_SCHEDULED', 'Hẹn test đầu vào chiều thứ 7', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3, 'Bùi Thu Hà', '0977889900', 'thuha.bui@gmail.com', 'REFERRAL', 'Python & Data Analysis', NULL, 'NEW', 'Bạn học giới thiệu, muốn tư vấn khóa học buổi tối', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 'Trần Minh Đức', '0903456789', 'duc.tran@yahoo.com', 'HOTLINE', 'Lập trình Frontend React', 7, 'WON', 'Đã đóng cọc học phí lớp K24', DATE_SUB(NOW(), INTERVAL 40 DAY)),
(5, 'Đặng Hoàng Nam', '0933221100', 'nam.dang@gmail.com', 'TIKTOK', 'Thiết kế UI/UX', NULL, 'NEW', 'Để lại số điện thoại qua link bio', NOW())
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);
