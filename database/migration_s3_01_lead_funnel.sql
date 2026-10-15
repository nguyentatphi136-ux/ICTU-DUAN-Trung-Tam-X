-- =====================================================================
-- Migration: S3-01 - Phễu lead dạng Kanban và lý do từ chối
-- Story S3-01 (IDTTX-209 / Subtask IDTTX-222): [BE] Thiết kế CSDL và API Phễu lead
-- 
-- Yêu cầu nghiệp vụ Story S3-01:
-- 1. 6 cột trạng thái phễu: Mới → Đã liên hệ → Đang tư vấn → Hẹn học thử → Chốt → Từ chối
-- 2. Kéo thả để đổi trạng thái, mỗi lần đổi trạng thái đều được ghi nhật ký (Audit log)
-- 3. Chuyển sang "Từ chối" bắt buộc chọn lý do từ danh mục chuẩn (Học phí cao, Lịch học không phù hợp, v.v.)
--    Nếu chọn "Lý do khác" thì bắt buộc có ghi chú
-- 4. Mỗi cột hiển thị số lượng lead; hỗ trợ cảnh báo lead quá hạn gọi lại (callback date)
-- 
-- Author: Nguyen Minh Ngoc (minhngoc678)
-- Branch: features/s3-01-BE-NMN
-- Date: 2026-10-10
-- =====================================================================

-- 1. Cập nhật bảng `leads` hỗ trợ các trường cho Phễu tuyển sinh Kanban (S3-01)
ALTER TABLE `leads`
    ADD COLUMN IF NOT EXISTS `stage` ENUM('Mới', 'Đã liên hệ', 'Đang tư vấn', 'Hẹn học thử', 'Chốt', 'Từ chối') 
        DEFAULT 'Mới' COMMENT 'Giai đoạn trong phễu tuyển sinh (S3-01)',
    ADD COLUMN IF NOT EXISTS `callback_date` DATE NULL 
        COMMENT 'Ngày hẹn gọi lại (yyyy-mm-dd), phục vụ đánh dấu quá hạn gọi lại',
    ADD COLUMN IF NOT EXISTS `trial_date` DATE NULL 
        COMMENT 'Ngày hẹn học thử (yyyy-mm-dd)',
    ADD COLUMN IF NOT EXISTS `reject_reason` VARCHAR(150) NULL 
        COMMENT 'Lý do từ chối bắt buộc khi stage = Từ chối',
    ADD COLUMN IF NOT EXISTS `reject_note` TEXT NULL 
        COMMENT 'Ghi chú chi tiết khi chọn lý do khác',
    ADD COLUMN IF NOT EXISTS `student_code` VARCHAR(50) NULL 
        COMMENT 'Mã học viên sinh ra sau khi chốt nhập học (S3-03)';

-- Đánh chỉ mục tối ưu truy vấn phễu theo giai đoạn và ngày hẹn gọi lại
CREATE INDEX IF NOT EXISTS `idx_leads_stage` ON `leads` (`stage`);
CREATE INDEX IF NOT EXISTS `idx_leads_callback_date` ON `leads` (`callback_date`);

-- 2. Bảng `lead_stage_logs`: Ghi nhận nhật ký mỗi lần chuyển trạng thái/giai đoạn (S3-01 Audit Log)
CREATE TABLE IF NOT EXISTS `lead_stage_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lead_id` BIGINT NOT NULL COMMENT 'Lead được chuyển giai đoạn',
    `from_stage` VARCHAR(50) NOT NULL COMMENT 'Giai đoạn trước khi đổi',
    `to_stage` VARCHAR(50) NOT NULL COMMENT 'Giai đoạn mới sau khi đổi',
    `reason` VARCHAR(150) NULL COMMENT 'Lý do từ chối nếu chuyển sang Từ chối',
    `note` TEXT NULL COMMENT 'Ghi chú bổ sung',
    `changed_by_user_id` BIGINT NOT NULL COMMENT 'Người thực hiện thao tác kéo thả',
    `changed_by_name` VARCHAR(100) NOT NULL COMMENT 'Họ tên người đổi',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm chuyển giai đoạn',
    CONSTRAINT `fk_lsl_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_lsl_user` FOREIGN KEY (`changed_by_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_lsl_lead_created` (`lead_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký chuyển giai đoạn phễu lead (Audit Log S3-01)';

-- 3. Bảng `lead_care_logs`: Nhật ký chăm sóc khách hàng (Gọi, Nhắn, Gặp, Đổi trạng thái) phục vụ S3-01 & S3-02
CREATE TABLE IF NOT EXISTS `lead_care_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lead_id` BIGINT NOT NULL COMMENT 'Lead liên quan',
    `kind` ENUM('Gọi', 'Nhắn', 'Gặp', 'Đổi trạng thái', 'Phân công', 'Chuyển thành học viên') NOT NULL COMMENT 'Loại tương tác',
    `author_name` VARCHAR(100) NOT NULL COMMENT 'Người thực hiện',
    `content` TEXT NOT NULL COMMENT 'Nội dung chi tiết nhật ký',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm ghi nhận',
    CONSTRAINT `fk_lcl_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE CASCADE,
    INDEX `idx_lcl_lead_created` (`lead_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký chăm sóc tư vấn lead (S3-01 & S3-02)';

-- 4. Dữ liệu mẫu (Seed Data) mô phỏng đầy đủ 6 cột phễu, lead quá hạn và lý do từ chối
INSERT INTO `leads` (`id`, `full_name`, `phone`, `email`, `course`, `source`, `assigned_counselor_id`, `status`, `stage`, `callback_date`, `trial_date`, `reject_reason`, `created_at`)
VALUES
-- Cột 1: Mới
(101, 'Phạm Thị Hạnh', '0912480227', 'hanh.pt@gmail.com', 'Lập trình web Full-stack', 'WEBSITE', 5, 'NEW', 'Mới', NULL, NULL, NULL, NOW()),
(102, 'Đỗ Quang Minh', '0987115630', 'minh.dq@gmail.com', 'Phân tích dữ liệu', 'FACEBOOK', 5, 'NEW', 'Mới', NULL, NULL, NULL, NOW()),
(103, 'Lê Ngọc Anh', '0938772041', 'anh.ln@gmail.com', 'Thiết kế UI/UX', 'WEBSITE', 5, 'NEW', 'Mới', NULL, NULL, NULL, NOW()),

-- Cột 2: Đã liên hệ (có lead quá hạn gọi lại: ngày 2026-10-04 < mốc 2026-10-06)
(104, 'Trần Văn Hùng', '0903664518', 'hung.tv@gmail.com', 'Lập trình web Full-stack', 'REFERRAL', 5, 'CONTACTED', 'Đã liên hệ', '2026-10-04', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(105, 'Nguyễn Thu Trang', '0976220148', 'trang.nt@gmail.com', 'Kiểm thử phần mềm', 'WEBSITE', 5, 'CONTACTED', 'Đã liên hệ', '2026-10-08', NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- Cột 3: Đang tư vấn (có lead quá hạn gọi lại)
(106, 'Nguyễn Bảo Châu', '0965209384', 'chau.nb@gmail.com', 'Lập trình Java Backend', 'HOTLINE', 5, 'CONSULTING', 'Đang tư vấn', '2026-10-05', NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(107, 'Bùi Đức Anh', '0944861357', 'anh.bd@gmail.com', 'Thiết kế UI/UX', 'REFERRAL', 5, 'CONSULTING', 'Đang tư vấn', '2026-10-06', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Cột 4: Hẹn học thử
(108, 'Lê Minh Khôi', '0909518264', 'khoi.lm@gmail.com', 'Lập trình web Full-stack', 'WEBSITE', 5, 'TRIAL_SCHEDULED', 'Hẹn học thử', NULL, '2026-10-10', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(109, 'Võ Thành Nhân', '0962381457', 'nhan.vt@gmail.com', 'Phân tích dữ liệu', 'WEBSITE', 5, 'TRIAL_SCHEDULED', 'Hẹn học thử', NULL, '2026-10-11', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Cột 5: Chốt
(110, 'Vũ Thành Đạt', '0919530276', 'dat.vt@gmail.com', 'Kiểm thử phần mềm', 'FACEBOOK', 5, 'WON', 'Chốt', NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 12 DAY)),

-- Cột 6: Từ chối (Bắt buộc kèm lý do từ chối)
(111, 'Hoàng Mai Lan', '0977418902', 'lan.hm@gmail.com', 'Phân tích dữ liệu', 'WEBSITE', 5, 'REJECTED', 'Từ chối', NULL, NULL, 'Học phí cao', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(112, 'Kiều Văn Sơn', '0988257613', 'son.kv@gmail.com', 'Lập trình Java Backend', 'WEBSITE', 5, 'REJECTED', 'Từ chối', NULL, NULL, 'Đã chọn trung tâm khác', DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE `stage` = VALUES(`stage`), `reject_reason` = VALUES(`reject_reason`);

-- Nhật ký đổi trạng thái mẫu (S3-01)
INSERT INTO `lead_stage_logs` (`lead_id`, `from_stage`, `to_stage`, `reason`, `note`, `changed_by_user_id`, `changed_by_name`, `created_at`)
VALUES
(106, 'Mới', 'Đã liên hệ', NULL, 'Đã gọi điện lần đầu cho khách', 5, 'Lê Thị Thu Hà', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(106, 'Đã liên hệ', 'Đang tư vấn', NULL, 'Khách quan tâm lộ trình Java chuyên sâu', 5, 'Lê Thị Thu Hà', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(111, 'Đang tư vấn', 'Từ chối', 'Học phí cao', 'Khách thấy học phí chưa phù hợp với ngân sách cá nhân', 5, 'Lê Thị Thu Hà', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE `note` = VALUES(`note`);
