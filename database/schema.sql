-- =============================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EDUCATION MANAGEMENT SYSTEM - EMS)
-- Database: MySQL 8.0+
-- Thiết kế chuẩn hóa 3NF (Third Normal Form), UTF8MB4, InnoDB
-- Bao quát toàn bộ 8 Sprints / 75 User Stories theo Product Backlog
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `ems_database` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ems_database`;

-- =============================================================================
-- PHÂN HỆ 1: QUẢN TRỊ NGƯỜI DÙNG, PHÂN QUYỀN (RBAC) & BẢO MẬT (Sprint 1 & 2)
-- =============================================================================

-- 1.1 Bảng Vai trò (Roles)
CREATE TABLE IF NOT EXISTS `roles` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `role_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã vai trò: ADMIN, TRAINING_MANAGER, ADMISSIONS, INSTRUCTOR, TA, STUDENT, ACCOUNTANT, GUEST',
    `role_name` VARCHAR(100) NOT NULL COMMENT 'Tên hiển thị: Quản trị viên, Giảng viên,...',
    `description` VARCHAR(255) NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Bảng lưu trữ danh mục vai trò người dùng';

-- 1.2 Bảng Tài khoản người dùng (Users)
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã nhân sự / tài khoản',
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'Mật khẩu mã hóa BCrypt/Argon2',
    `full_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NULL,
    `date_of_birth` DATE NULL,
    `gender` ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER',
    `address` VARCHAR(255) NULL,
    `avatar_url` VARCHAR(255) NULL,
    `status` ENUM('ACTIVE', 'LOCKED', 'PENDING') DEFAULT 'ACTIVE' COMMENT 'Trạng thái hoạt động',
    `locked_reason` VARCHAR(255) NULL COMMENT 'Lý do khóa tài khoản',
    `failed_login_attempts` INT NOT NULL DEFAULT 0,
    `locked_until` DATETIME NULL,
    `deleted_at` DATETIME NULL COMMENT 'Xoá mềm: thời điểm chuyển vào thùng rác, NULL là đang dùng',
    `deleted_by` BIGINT NULL COMMENT 'Người chuyển vào thùng rác',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_email` (`email`),
    INDEX `idx_users_phone` (`phone`),
    INDEX `idx_users_fullname` (`full_name`),
    INDEX `idx_users_status` (`status`)
) ENGINE=InnoDB COMMENT='Bảng thông tin tài khoản người dùng';

-- 1.3 Bảng Gán Vai trò cho Người dùng (User Roles - Hỗ trợ một người nhiều vai trò)
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` INT NOT NULL,
    `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Bảng trung gian phân quyền đa vai trò cho người dùng';

-- 1.4 Bảng Quyền hạn chi tiết (Permissions)
CREATE TABLE IF NOT EXISTS `permissions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `permission_code` VARCHAR(100) NOT NULL UNIQUE COMMENT 'VD: USER_CREATE, CLASS_MANAGE, ATTENDANCE_CHECK, GRADE_POST',
    `permission_name` VARCHAR(150) NOT NULL,
    `module` VARCHAR(50) NOT NULL COMMENT 'Phân nhóm module: AUTH, USER, CLASS, ATTENDANCE, GRADE, FINANCE,...',
    `description` VARCHAR(255) NULL
) ENGINE=InnoDB COMMENT='Bảng danh mục quyền hạn hệ thống';

-- 1.5 Bảng Phân quyền cho Vai trò (Role Permissions)
CREATE TABLE IF NOT EXISTS `role_permissions` (
    `role_id` INT NOT NULL,
    `permission_id` INT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_permissions_perm` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Bảng ánh xạ quyền cho từng vai trò';

-- 1.5a Danh sách menu và quyền cần để hiển thị menu
CREATE TABLE IF NOT EXISTS `menus` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `menu_code` VARCHAR(64) NOT NULL UNIQUE,
    `title` VARCHAR(120) NOT NULL,
    `href` VARCHAR(255) NOT NULL,
    `icon` VARCHAR(64) NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='Danh sách mục điều hướng hệ thống';

CREATE TABLE IF NOT EXISTS `menu_permissions` (
    `menu_id` INT NOT NULL,
    `permission_id` INT NOT NULL,
    PRIMARY KEY (`menu_id`, `permission_id`),
    CONSTRAINT `fk_menu_permissions_menu` FOREIGN KEY (`menu_id`) REFERENCES `menus` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_menu_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Quyền cần có để nhận một mục menu';

-- 1.6 Bảng Đặt lại mật khẩu (Password Resets)
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(255) NOT NULL UNIQUE,
    `expires_at` DATETIME NOT NULL,
    `used_at` DATETIME NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_password_resets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_token_lookup` (`token_hash`, `expires_at`)
) ENGINE=InnoDB COMMENT='Bảng lưu token lấy lại mật khẩu qua email';

-- 1.7 Bảng Danh sách đen Session (Session Blacklist phục vụ thu hồi tức thì khi Logout)
CREATE TABLE IF NOT EXISTS `session_blacklist` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` VARCHAR(128) NOT NULL UNIQUE,
    `user_id` BIGINT NULL,
    `revoked_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `expires_at` DATETIME NOT NULL,
    CONSTRAINT `fk_session_blacklist_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_session_lookup` (`session_id`)
) ENGINE=InnoDB COMMENT='Bảng thu hồi phiên đăng nhập';

-- 1.8 Bảng Nhật ký thao tác nhạy cảm (Audit Logs)
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NULL,
    `action` VARCHAR(100) NOT NULL COMMENT 'Hành động: LOGIN, LOGOUT, UPDATE_GRADE, UPDATE_TUITION, ASSIGN_ROLE,...',
    `entity_type` VARCHAR(50) NOT NULL COMMENT 'Đối tượng: USER, GRADE, TUITION, CLASS,...',
    `entity_id` VARCHAR(50) NULL COMMENT 'Khóa chính bản ghi bị tác động',
    `old_values` JSON NULL COMMENT 'Giá trị trước thay đổi',
    `new_values` JSON NULL COMMENT 'Giá trị sau thay đổi',
    `ip_address` VARCHAR(45) NULL,
    `user_agent` TEXT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_audit_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_audit_entity` (`entity_type`, `entity_id`),
    INDEX `idx_audit_user` (`user_id`, `created_at`)
) ENGINE=InnoDB COMMENT='Bảng nhật ký kiểm toán hệ thống';

-- 1.9 Bảng Lô nhập người dùng hàng loạt từ Excel (Sprint 2 S2-01)
CREATE TABLE IF NOT EXISTS `user_import_batches` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `batch_code` VARCHAR(64) NOT NULL UNIQUE COMMENT 'Mã lô nhập, VD: IMP-20261006-001',
    `actor_id` BIGINT NULL COMMENT 'Người thực hiện nhập (Admin)',
    `file_name` VARCHAR(255) NOT NULL COMMENT 'Tên tệp Excel/CSV đã tải lên',
    `total_rows` INT NOT NULL DEFAULT 0,
    `success_rows` INT NOT NULL DEFAULT 0,
    `failed_rows` INT NOT NULL DEFAULT 0,
    `summary_note` TEXT NULL COMMENT 'Báo cáo tổng kết đợt nhập',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_import_batches_actor` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_import_batch_created` (`created_at`)
) ENGINE=InnoDB COMMENT='Bảng lưu lịch sử các đợt nhập người dùng hàng loạt từ Excel';

-- 1.10 Bảng Chi tiết lỗi từng dòng khi nhập Excel (Sprint 2 S2-01)
CREATE TABLE IF NOT EXISTS `user_import_errors` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `batch_id` BIGINT NOT NULL,
    `row_index` INT NOT NULL COMMENT 'Thứ tự dòng trong tệp Excel/CSV',
    `raw_data` JSON NULL COMMENT 'Dữ liệu thô của dòng bị lỗi',
    `error_reason` VARCHAR(255) NOT NULL COMMENT 'Nguyên nhân từ chối / lỗi',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_import_errors_batch` FOREIGN KEY (`batch_id`) REFERENCES `user_import_batches` (`id`) ON DELETE CASCADE,
    INDEX `idx_import_errors_batch` (`batch_id`)
) ENGINE=InnoDB COMMENT='Bảng lưu chi tiết các dòng bị từ chối / lỗi khi nhập Excel';


-- =============================================================================
-- PHÂN HỆ 2: CHƯƠNG TRÌNH ĐÀO TẠO & MÔN HỌC (Sprint 2)
-- =============================================================================

-- 2.1 Bảng Chương trình đào tạo (Training Programs)
CREATE TABLE IF NOT EXISTS `programs` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `program_code` VARCHAR(50) NOT NULL UNIQUE,
    `program_name` VARCHAR(150) NOT NULL,
    `description` TEXT NULL,
    `duration` INT NOT NULL DEFAULT 60 COMMENT 'Tổng thời lượng (giờ hoặc buổi > 0)',
    `standard_tuition` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    `deleted_at` DATETIME NULL COMMENT 'Xoá mềm: thời điểm chuyển vào thùng rác, NULL là đang dùng',
    `deleted_by` BIGINT NULL COMMENT 'Người chuyển vào thùng rác',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Bảng danh mục chương trình đào tạo';

-- 2.2 Bảng Môn học (Subjects / Courses)
CREATE TABLE IF NOT EXISTS `subjects` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_code` VARCHAR(50) NOT NULL UNIQUE,
    `subject_name` VARCHAR(150) NOT NULL,
    `total_sessions` INT NOT NULL DEFAULT 15 COMMENT 'Số buổi học chuẩn',
    `credit_weight` DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT 'Trọng số tín chỉ',
    `min_pass_score` DECIMAL(4, 2) NOT NULL DEFAULT 5.00 COMMENT 'Điểm đạt tối thiểu',
    `learning_outcomes` TEXT NULL COMMENT 'Chuẩn đầu ra môn học',
    `status` ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    `deleted_at` DATETIME NULL COMMENT 'Xoá mềm: thời điểm chuyển vào thùng rác, NULL là đang dùng',
    `deleted_by` BIGINT NULL COMMENT 'Người chuyển vào thùng rác',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Bảng danh mục môn học';

-- 2.3 Bảng Ánh xạ Môn học trong Chương trình (Program Subjects)
CREATE TABLE IF NOT EXISTS `program_subjects` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `program_id` INT NOT NULL,
    `subject_id` INT NOT NULL,
    `order_index` INT NOT NULL DEFAULT 1 COMMENT 'Thứ tự môn trong lộ trình học',
    `prerequisite_subject_id` INT NULL COMMENT 'Môn học tiên quyết (nếu có)',
    UNIQUE KEY `uk_program_subject` (`program_id`, `subject_id`),
    CONSTRAINT `fk_ps_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ps_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ps_prereq` FOREIGN KEY (`prerequisite_subject_id`) REFERENCES `subjects` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Bảng môn học thuộc chương trình và lộ trình tiên quyết';

-- 2.4 Bảng Bài học/Chủ đề chuẩn của Môn (Subject Lessons / Syllabus)
CREATE TABLE IF NOT EXISTS `subject_lessons` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL,
    `session_order` INT NOT NULL COMMENT 'Thứ tự buổi học (Buổi 1, Buổi 2,...)',
    `topic` VARCHAR(255) NOT NULL COMMENT 'Chủ đề buổi học',
    `objectives` TEXT NULL COMMENT 'Mục tiêu buổi học',
    UNIQUE KEY `uk_subject_session` (`subject_id`, `session_order`),
    CONSTRAINT `fk_sl_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Bảng danh mục các buổi học chuẩn theo môn';


-- =============================================================================
-- PHÂN HỆ 3: TUYỂN SINH, PHỄU KHÁCH HÀNG & HỒ SƠ HỌC VIÊN (Sprint 2 & 3)
-- =============================================================================

-- 3.1 Bảng Khách hàng tiềm năng (Leads)
CREATE TABLE IF NOT EXISTS `leads` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `email` VARCHAR(150) NULL,
    `source` VARCHAR(50) DEFAULT 'WEBSITE' COMMENT 'Nguồn: WEBSITE, FACEBOOK, REFERRAL, EVENT,...',
    `interested_program_id` INT NULL,
    `assigned_counselor_id` BIGINT NULL COMMENT 'Tư vấn viên phụ trách',
    `status` ENUM('NEW', 'CONTACTED', 'CONSULTING', 'TRIAL_SCHEDULED', 'WON', 'REJECTED') DEFAULT 'NEW',
    `reject_reason` VARCHAR(255) NULL,
    `notes` TEXT NULL,
    `converted_student_id` BIGINT NULL COMMENT 'Liên kết sang học viên khi chốt nhập học',
    `deleted_at` DATETIME NULL COMMENT 'Xoá mềm: thời điểm chuyển vào thùng rác, NULL là đang dùng',
    `deleted_by` BIGINT NULL COMMENT 'Người chuyển vào thùng rác',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_leads_program` FOREIGN KEY (`interested_program_id`) REFERENCES `programs` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_leads_counselor` FOREIGN KEY (`assigned_counselor_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_leads_phone` (`phone`),
    INDEX `idx_leads_status` (`status`)
) ENGINE=InnoDB COMMENT='Bảng phễu khách hàng tiềm năng';

-- 3.2 Bảng Nhật ký tư vấn chăm sóc Lead (Lead Interactions)
CREATE TABLE IF NOT EXISTS `lead_interactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `lead_id` BIGINT NOT NULL,
    `counselor_id` BIGINT NOT NULL,
    `interaction_type` ENUM('CALL', 'MESSAGE', 'MEETING', 'EMAIL') NOT NULL,
    `content` TEXT NOT NULL,
    `callback_scheduled_at` DATETIME NULL COMMENT 'Lịch hẹn gọi lại',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_li_lead` FOREIGN KEY (`lead_id`) REFERENCES `leads` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_li_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
    INDEX `idx_callback_time` (`callback_scheduled_at`)
) ENGINE=InnoDB COMMENT='Bảng lịch sử chăm sóc và tương tác khách hàng';

-- 3.3 Bảng Hồ sơ Học viên chi tiết (Students Profile)
CREATE TABLE IF NOT EXISTS `students` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT 'Tài khoản đăng nhập hệ thống',
    `student_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã học viên: HV2026-001',
    `entry_level` VARCHAR(100) NULL COMMENT 'Trình độ đầu vào',
    `enrollment_status` ENUM('STUDYING', 'SUSPENDED', 'DROPPED_OUT', 'GRADUATED') DEFAULT 'STUDYING',
    `notes` TEXT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_students_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng hồ sơ học viên chính thức';


-- =============================================================================
-- PHÂN HỆ 4: QUẢN LÝ LỚP HỌC & GHI DANH (Sprint 3 & 4)
-- =============================================================================

-- 4.1 Bảng Phòng học (Classrooms - Vật lý hoặc Trực tuyến)
CREATE TABLE IF NOT EXISTS `classrooms` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `room_code` VARCHAR(50) NOT NULL UNIQUE,
    `room_name` VARCHAR(100) NOT NULL,
    `capacity` INT NOT NULL DEFAULT 30,
    `room_type` ENUM('PHYSICAL', 'ONLINE') DEFAULT 'PHYSICAL',
    `meeting_url` VARCHAR(255) NULL COMMENT 'Google Meet / Zoom URL nếu là phòng online',
    `status` ENUM('AVAILABLE', 'MAINTENANCE') DEFAULT 'AVAILABLE'
) ENGINE=InnoDB COMMENT='Bảng danh mục phòng học';

-- 4.2 Bảng Lớp học (Classes)
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
    CONSTRAINT `fk_classes_room` FOREIGN KEY (`default_classroom_id`) REFERENCES `classrooms` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Bảng quản lý lớp học';

-- 4.3 Bảng Môn học của Lớp & Phân công Giảng viên chính (Class Subjects)
CREATE TABLE IF NOT EXISTS `class_subjects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `subject_id` INT NOT NULL,
    `primary_instructor_id` BIGINT NOT NULL COMMENT 'Giảng viên chính phụ trách môn',
    `status` ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'NOT_STARTED',
    UNIQUE KEY `uk_class_subject` (`class_id`, `subject_id`),
    CONSTRAINT `fk_cs_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cs_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_cs_instructor` FOREIGN KEY (`primary_instructor_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng phân công giảng viên chính theo môn của lớp';

-- 4.4 Bảng Trợ giảng hỗ trợ Môn của Lớp (Class Subject Teaching Assistants)
CREATE TABLE IF NOT EXISTS `class_subject_assistants` (
    `class_subject_id` BIGINT NOT NULL,
    `ta_user_id` BIGINT NOT NULL,
    PRIMARY KEY (`class_subject_id`, `ta_user_id`),
    CONSTRAINT `fk_csa_class_subject` FOREIGN KEY (`class_subject_id`) REFERENCES `class_subjects` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_csa_user` FOREIGN KEY (`ta_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Bảng phân công trợ giảng cho từng môn của lớp';

-- 4.5 Bảng Ghi danh Học viên vào Lớp (Enrollments)
CREATE TABLE IF NOT EXISTS `enrollments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `enrollment_date` DATE NOT NULL,
    `status` ENUM('ENROLLED', 'SUSPENDED', 'TRANSFERRED', 'DROPPED', 'GRADUATED') DEFAULT 'ENROLLED',
    `notes` VARCHAR(255) NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
    CONSTRAINT `fk_enr_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_enr_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng học viên trong lớp';

-- 4.6 Bảng Lịch sử chuyển lớp, bảo lưu, thôi học (Student Status Logs)
CREATE TABLE IF NOT EXISTS `student_status_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enrollment_id` BIGINT NOT NULL,
    `action_type` ENUM('SUSPEND', 'RESUME', 'TRANSFER', 'DROP_OUT') NOT NULL,
    `from_class_id` BIGINT NULL,
    `to_class_id` BIGINT NULL,
    `effective_date` DATE NOT NULL,
    `reason` VARCHAR(255) NOT NULL,
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ssl_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ssl_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Lịch sử biến động học vụ của học viên';


-- =============================================================================
-- PHÂN HỆ 5: THỜI KHÓA BIỂU, ĐIỂM DANH & CHUYÊN CẦN (Sprint 4 & 5)
-- =============================================================================

-- 5.1 Bảng Lịch học từng buổi (Class Sessions / Schedules)
CREATE TABLE IF NOT EXISTS `class_sessions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `class_subject_id` BIGINT NOT NULL,
    `subject_lesson_id` INT NULL COMMENT 'Bài học giáo trình tương ứng',
    `session_number` INT NOT NULL COMMENT 'Buổi số mấy của môn',
    `session_date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `classroom_id` INT NOT NULL,
    `instructor_id` BIGINT NOT NULL COMMENT 'Giảng viên dạy buổi này (có thể dạy thay)',
    `status` ENUM('SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED') DEFAULT 'SCHEDULED',
    `cancel_reason` VARCHAR(255) NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_csess_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_csess_subject` FOREIGN KEY (`class_subject_id`) REFERENCES `class_subjects` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_csess_lesson` FOREIGN KEY (`subject_lesson_id`) REFERENCES `subject_lessons` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_csess_room` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_csess_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
    INDEX `idx_session_schedule` (`session_date`, `start_time`, `end_time`),
    INDEX `idx_session_instructor` (`instructor_id`, `session_date`),
    INDEX `idx_session_room` (`classroom_id`, `session_date`)
) ENGINE=InnoDB COMMENT='Bảng thời khóa biểu chi tiết từng buổi học';

-- 5.2 Bảng Điểm danh học viên (Attendance)
CREATE TABLE IF NOT EXISTS `attendance` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `status` ENUM('PRESENT', 'LATE', 'EXCUSED_ABSENCE', 'UNEXCUSED_ABSENCE') DEFAULT 'PRESENT',
    `notes` VARCHAR(255) NULL,
    `marked_by` BIGINT NOT NULL COMMENT 'Người điểm danh',
    `marked_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT NULL,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_session_student` (`session_id`, `student_id`),
    CONSTRAINT `fk_att_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_att_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_att_marker` FOREIGN KEY (`marked_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng ghi nhận điểm danh theo buổi';

-- 5.3 Bảng Đơn xin phép vắng học (Absence Requests)
CREATE TABLE IF NOT EXISTS `absence_requests` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `session_id` BIGINT NOT NULL,
    `reason` TEXT NOT NULL,
    `proof_file_url` VARCHAR(255) NULL COMMENT 'Minh chứng tệp đính kèm (giấy khám, đơn từ,...)',
    `status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    `reviewed_by` BIGINT NULL,
    `review_note` VARCHAR(255) NULL,
    `reviewed_at` DATETIME NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ar_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ar_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ar_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Bảng đơn xin nghỉ học của học viên';


-- =============================================================================
-- PHÂN HỆ 6: BÀI TẬP, NỘP BÀI, RUBRIC & HỌC LIỆU (Sprint 5 & 6)
-- =============================================================================

-- 6.1 Bảng Bài tập (Assignments)
CREATE TABLE IF NOT EXISTS `assignments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `class_subject_id` BIGINT NOT NULL,
    `session_id` BIGINT NULL COMMENT 'Gắn với buổi học cụ thể nếu có',
    `title` VARCHAR(255) NOT NULL,
    `description` LONGTEXT NULL,
    `max_score` DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    `due_date` DATETIME NOT NULL,
    `allow_late_submission` BOOLEAN DEFAULT TRUE,
    `is_draft` BOOLEAN DEFAULT FALSE COMMENT 'Lưu nháp hay đã phát hành',
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_assign_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_assign_subject` FOREIGN KEY (`class_subject_id`) REFERENCES `class_subjects` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_assign_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_assign_author` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng giao bài tập cho lớp';

-- 6.2 Bảng Tệp đính kèm đề bài (Assignment Attachments)
CREATE TABLE IF NOT EXISTS `assignment_attachments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `assignment_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(255) NOT NULL,
    `file_size_bytes` BIGINT NOT NULL,
    CONSTRAINT `fk_aa_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Tệp đính kèm theo đề bài của giảng viên';

-- 6.3 Bảng Mẫu tiêu chí chấm điểm (Rubrics)
CREATE TABLE IF NOT EXISTS `rubrics` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `assignment_id` BIGINT NOT NULL UNIQUE,
    `rubric_name` VARCHAR(150) NOT NULL,
    `total_weight` DECIMAL(5, 2) NOT NULL DEFAULT 100.00,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_rubrics_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Bảng tiêu chuẩn chấm Rubric';

-- 6.4 Bảng Chi tiết tiêu chí Rubric (Rubric Criteria)
CREATE TABLE IF NOT EXISTS `rubric_criteria` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `rubric_id` BIGINT NOT NULL,
    `criteria_name` VARCHAR(150) NOT NULL,
    `description` TEXT NULL,
    `weight_percent` DECIMAL(5, 2) NOT NULL COMMENT 'Trọng số phần trăm (Tổng = 100%)',
    `max_points` DECIMAL(5, 2) NOT NULL DEFAULT 10.00,
    CONSTRAINT `fk_rc_rubric` FOREIGN KEY (`rubric_id`) REFERENCES `rubrics` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Các tiêu chí thành phần của Rubric';

-- 6.5 Bảng Bài nộp của Học viên (Assignment Submissions - Có phiên bản nộp)
CREATE TABLE IF NOT EXISTS `assignment_submissions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `assignment_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `version` INT NOT NULL DEFAULT 1 COMMENT 'Phiên bản nộp (1, 2, 3...)',
    `repo_url` VARCHAR(255) NULL COMMENT 'Đường dẫn Git/GitHub',
    `submission_note` TEXT NULL,
    `is_latest` BOOLEAN DEFAULT TRUE COMMENT 'Đánh dấu bản nộp mới nhất',
    `is_late` BOOLEAN DEFAULT FALSE COMMENT 'Nộp trễ hạn',
    `status` ENUM('SUBMITTED', 'NEED_RESUBMIT', 'GRADED') DEFAULT 'SUBMITTED',
    `resubmit_reason` TEXT NULL COMMENT 'Lý do yêu cầu làm lại',
    `new_deadline` DATETIME NULL COMMENT 'Hạn nộp mới nếu yêu cầu làm lại',
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_assign_student_version` (`assignment_id`, `student_id`, `version`),
    CONSTRAINT `fk_sub_assign` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sub_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Bảng lưu trữ bài làm nộp của học viên';

-- 6.6 Bảng Tệp đính kèm của bài nộp (Submission Files)
CREATE TABLE IF NOT EXISTS `submission_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `submission_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(255) NOT NULL,
    `file_size_bytes` BIGINT NOT NULL,
    CONSTRAINT `fk_sf_submission` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Các tệp mã nguồn / tài liệu học viên đính kèm bài nộp';

-- 6.7 Bảng Chấm điểm bài nộp (Submission Grades)
CREATE TABLE IF NOT EXISTS `submission_grades` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `submission_id` BIGINT NOT NULL UNIQUE,
    `score` DECIMAL(5, 2) NOT NULL,
    `feedback` TEXT NULL,
    `is_published` BOOLEAN DEFAULT FALSE COMMENT 'Đã công bố cho học viên thấy hay chưa',
    `graded_by` BIGINT NOT NULL,
    `graded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` BIGINT NULL,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sg_submission` FOREIGN KEY (`submission_id`) REFERENCES `assignment_submissions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sg_grader` FOREIGN KEY (`graded_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Kết quả chấm bài của giảng viên';

-- 6.8 Bảng Điểm chi tiết theo từng tiêu chí Rubric (Submission Rubric Scores)
CREATE TABLE IF NOT EXISTS `submission_rubric_scores` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `submission_grade_id` BIGINT NOT NULL,
    `criterion_id` BIGINT NOT NULL,
    `score` DECIMAL(5, 2) NOT NULL,
    `note` VARCHAR(255) NULL,
    UNIQUE KEY `uk_grade_criterion` (`submission_grade_id`, `criterion_id`),
    CONSTRAINT `fk_srs_grade` FOREIGN KEY (`submission_grade_id`) REFERENCES `submission_grades` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_srs_criterion` FOREIGN KEY (`criterion_id`) REFERENCES `rubric_criteria` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Điểm chi tiết phân bổ theo Rubric';

-- 6.9 Bảng Học liệu / Tài liệu buổi học (Session Materials)
CREATE TABLE IF NOT EXISTS `session_materials` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(255) NULL,
    `external_link` VARCHAR(255) NULL,
    `file_size_bytes` BIGINT NULL,
    `is_published_early` BOOLEAN DEFAULT FALSE COMMENT 'Mở tài liệu sớm trước buổi học',
    `uploaded_by` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sm_session` FOREIGN KEY (`session_id`) REFERENCES `class_sessions` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sm_uploader` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Kho học liệu đính kèm buổi học';


-- =============================================================================
-- PHÂN HỆ 7: HỌC PHÍ, BIỂU PHÍ & KẾ TOÁN (Sprint 7)
-- =============================================================================

-- 7.1 Bảng Biểu phí chuẩn theo Chương trình (Tuition Fee Templates)
CREATE TABLE IF NOT EXISTS `fee_templates` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `program_id` INT NOT NULL,
    `template_name` VARCHAR(150) NOT NULL,
    `total_amount` DECIMAL(12, 2) NOT NULL,
    `number_of_installments` INT NOT NULL DEFAULT 1 COMMENT 'Số đợt đóng',
    `version` INT NOT NULL DEFAULT 1,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ft_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng biểu phí mẫu theo chương trình';

-- 7.2 Bảng Cấu hình chi tiết các đợt đóng của biểu phí mẫu (Fee Installment Rules)
CREATE TABLE IF NOT EXISTS `fee_installment_rules` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `fee_template_id` INT NOT NULL,
    `installment_order` INT NOT NULL COMMENT 'Đợt 1, Đợt 2,...',
    `percentage` DECIMAL(5, 2) NOT NULL COMMENT 'Tỷ lệ phần trăm đợt này (%)',
    `due_days_after_start` INT NOT NULL DEFAULT 0 COMMENT 'Số ngày đến hạn sau khai giảng',
    CONSTRAINT `fk_fir_template` FOREIGN KEY (`fee_template_id`) REFERENCES `fee_templates` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Chi tiết tỷ lệ và hạn các đợt nộp phí mẫu';

-- 7.3 Bảng Kế hoạch học phí cá nhân của học viên (Student Tuition Plans)
CREATE TABLE IF NOT EXISTS `student_tuition_plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enrollment_id` BIGINT NOT NULL UNIQUE,
    `student_id` BIGINT NOT NULL,
    `original_tuition` DECIMAL(12, 2) NOT NULL COMMENT 'Học phí gốc theo biểu phí',
    `discount_type` ENUM('NONE', 'AMOUNT', 'PERCENT') DEFAULT 'NONE',
    `discount_value` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `discount_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Số tiền thực tế được giảm',
    `discount_reason` VARCHAR(255) NULL,
    `discount_approved_by` BIGINT NULL COMMENT 'Quản lý đào tạo duyệt nếu giảm > 30%',
    `final_payable_amount` DECIMAL(12, 2) NOT NULL COMMENT 'Học phí phải đóng thực tế',
    `paid_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Tổng đã thanh toán',
    `debt_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Công nợ còn lại',
    `status` ENUM('UNPAID', 'PARTIAL', 'COMPLETED', 'OVERDUE') DEFAULT 'UNPAID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_stp_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_stp_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_stp_approver` FOREIGN KEY (`discount_approved_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_stp_status` (`status`)
) ENGINE=InnoDB COMMENT='Bảng hồ sơ kế hoạch học phí của từng học viên theo lớp';

-- 7.4 Bảng Các đợt thanh toán cụ thể của học viên (Tuition Installment Bills)
CREATE TABLE IF NOT EXISTS `tuition_installment_bills` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `tuition_plan_id` BIGINT NOT NULL,
    `installment_order` INT NOT NULL COMMENT 'Đợt 1, Đợt 2,...',
    `due_date` DATE NOT NULL,
    `expected_amount` DECIMAL(12, 2) NOT NULL,
    `paid_amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `status` ENUM('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE') DEFAULT 'UNPAID',
    `last_reminder_sent_at` DATETIME NULL COMMENT 'Thời điểm gửi nhắc nợ gần nhất',
    CONSTRAINT `fk_tib_plan` FOREIGN KEY (`tuition_plan_id`) REFERENCES `student_tuition_plans` (`id`) ON DELETE CASCADE,
    INDEX `idx_tib_due_date` (`due_date`, `status`)
) ENGINE=InnoDB COMMENT='Bảng chi tiết các đợt hạn thu học phí';

-- 7.5 Bảng Ghi nhận Phiếu thu / Biên lai thanh toán (Payment Receipts)
CREATE TABLE IF NOT EXISTS `payment_receipts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `receipt_code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã biên lai duy nhất: BL-2026-0001',
    `installment_bill_id` BIGINT NOT NULL,
    `amount_paid` DECIMAL(12, 2) NOT NULL,
    `payment_method` ENUM('CASH', 'BANK_TRANSFER', 'VNPAY', 'OTHER') NOT NULL DEFAULT 'BANK_TRANSFER',
    `payment_date` DATE NOT NULL,
    `notes` VARCHAR(255) NULL,
    `collected_by` BIGINT NOT NULL COMMENT 'Kế toán thực hiện thu',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_pr_bill` FOREIGN KEY (`installment_bill_id`) REFERENCES `tuition_installment_bills` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_pr_collector` FOREIGN KEY (`collected_by`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
    INDEX `idx_pr_code` (`receipt_code`),
    INDEX `idx_pr_date` (`payment_date`)
) ENGINE=InnoDB COMMENT='Bảng biên lai thu tiền không được xóa (chỉ lập bút toán điều chỉnh)';


-- =============================================================================
-- PHÂN HỆ 8: ĐIỂM TỔNG KẾT, TỐT NGHIỆP, KHẢO SÁT & THÔNG BÁO (Sprint 6 & 8)
-- =============================================================================

-- 8.1 Bảng Cấu hình Thành phần Điểm Môn học (Grade Components)
CREATE TABLE IF NOT EXISTS `grade_components` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `subject_id` INT NOT NULL,
    `component_name` VARCHAR(100) NOT NULL COMMENT 'VD: Điểm chuyên cần, Điểm thực hành, Điểm Final Project',
    `weight_percent` DECIMAL(5, 2) NOT NULL COMMENT 'Trọng số % (Tổng các thành phần = 100%)',
    `min_pass_score` DECIMAL(4, 2) NOT NULL DEFAULT 4.00 COMMENT 'Điểm liệt môn/thành phần',
    CONSTRAINT `fk_gc_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Cấu hình tỷ lệ điểm thành phần môn học';

-- 8.2 Bảng Điểm Tổng kết Môn của Học viên (Student Subject Final Grades)
CREATE TABLE IF NOT EXISTS `student_final_grades` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enrollment_id` BIGINT NOT NULL,
    `class_subject_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `attendance_score` DECIMAL(4, 2) NULL COMMENT 'Điểm chuyên cần quy đổi',
    `assignment_score` DECIMAL(4, 2) NULL COMMENT 'Điểm bài tập trung bình',
    `exam_score` DECIMAL(4, 2) NULL COMMENT 'Điểm thi/Final Project',
    `final_score` DECIMAL(4, 2) NULL COMMENT 'Điểm tổng kết môn',
    `pass_status` ENUM('PASS', 'FAIL', 'INCOMPLETE') DEFAULT 'INCOMPLETE',
    `is_locked` BOOLEAN DEFAULT FALSE COMMENT 'Giảng viên đã chốt điểm chưa',
    `locked_by` BIGINT NULL,
    `locked_at` DATETIME NULL,
    UNIQUE KEY `uk_enrollment_subject_grade` (`enrollment_id`, `class_subject_id`),
    CONSTRAINT `fk_sfg_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sfg_class_sub` FOREIGN KEY (`class_subject_id`) REFERENCES `class_subjects` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sfg_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sfg_locker` FOREIGN KEY (`locked_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Bảng điểm tổng kết từng môn học của học viên';

-- 8.3 Bảng Xét Tốt nghiệp & Hoàn thành Khóa học (Graduation Records)
CREATE TABLE IF NOT EXISTS `graduation_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `enrollment_id` BIGINT NOT NULL UNIQUE,
    `student_id` BIGINT NOT NULL,
    `is_eligible` BOOLEAN DEFAULT FALSE COMMENT 'Đủ điều kiện (Đạt hết môn, vắng < ngưỡng, hết nợ)',
    `ineligibility_reason` VARCHAR(255) NULL,
    `gpa` DECIMAL(4, 2) NULL COMMENT 'Điểm trung bình tích lũy toàn khóa',
    `graduation_status` ENUM('NOT_ELIGIBLE', 'ELIGIBLE', 'GRADUATED') DEFAULT 'NOT_ELIGIBLE',
    `certificate_code` VARCHAR(50) NULL UNIQUE,
    `approved_by` BIGINT NULL,
    `approved_at` DATETIME NULL,
    CONSTRAINT `fk_gr_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gr_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_gr_approver` FOREIGN KEY (`approved_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Bảng xét hoàn thành khóa học và cấp chứng chỉ tốt nghiệp';

-- 8.4 Bảng Mẫu Khảo sát Chất lượng (Survey Templates)
CREATE TABLE IF NOT EXISTS `survey_templates` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Bảng bộ mẫu khảo sát';

-- 8.5 Bảng Câu hỏi Khảo sát (Survey Questions)
CREATE TABLE IF NOT EXISTS `survey_questions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `template_id` INT NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('RATING_1_5', 'TEXT') NOT NULL DEFAULT 'RATING_1_5',
    `order_index` INT NOT NULL DEFAULT 1,
    CONSTRAINT `fk_sq_template` FOREIGN KEY (`template_id`) REFERENCES `survey_templates` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Các câu hỏi trong mẫu khảo sát';

-- 8.6 Bảng Đợt Khảo sát Mở cho Lớp (Class Surveys)
CREATE TABLE IF NOT EXISTS `class_surveys` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `template_id` INT NOT NULL,
    `class_id` BIGINT NOT NULL,
    `instructor_id` BIGINT NOT NULL COMMENT 'Khảo sát giảng viên của lớp',
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `is_active` BOOLEAN DEFAULT TRUE,
    CONSTRAINT `fk_csu_template` FOREIGN KEY (`template_id`) REFERENCES `survey_templates` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_csu_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_csu_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng đợt mở khảo sát cho lớp';

-- 8.7 Bảng Đánh giá Học viên đã tham gia khảo sát (Survey Responses - Quản lý xem ai đã làm để nhắc)
CREATE TABLE IF NOT EXISTS `survey_responses` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_survey_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_student_survey` (`class_survey_id`, `student_id`),
    CONSTRAINT `fk_sr_survey` FOREIGN KEY (`class_survey_id`) REFERENCES `class_surveys` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sr_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Ghi nhận học viên đã hoàn thành (ẩn danh khi xem kết quả)';

-- 8.8 Bảng Câu trả lời khảo sát Ẩn danh (Survey Answers - Không lưu student_id để đảm bảo tính khách quan)
CREATE TABLE IF NOT EXISTS `survey_answers` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_survey_id` BIGINT NOT NULL,
    `question_id` INT NOT NULL,
    `rating_score` INT NULL COMMENT 'Điểm thang 1-5',
    `text_comment` TEXT NULL COMMENT 'Nhận xét tự luận',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sa_survey` FOREIGN KEY (`class_survey_id`) REFERENCES `class_surveys` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_sa_question` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Câu trả lời khảo sát ẩn danh';

-- 8.9 Bảng Thông báo Hệ thống (System Notifications)
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recipient_id` BIGINT NOT NULL COMMENT 'Người nhận thông báo',
    `sender_id` BIGINT NULL COMMENT 'Người gửi hoặc NULL nếu hệ thống tự động sinh',
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `notification_type` ENUM('ASSIGNMENT', 'GRADE', 'SCHEDULE', 'ABSENCE', 'TUITION', 'SYSTEM') DEFAULT 'SYSTEM',
    `link_url` VARCHAR(255) NULL COMMENT 'Đường dẫn chuyển hướng nhanh',
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notif_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_notif_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_notif_user_read` (`recipient_id`, `is_read`, `created_at`)
) ENGINE=InnoDB COMMENT='Bảng thông báo chuông thông báo';


-- =============================================================================
-- DỮ LIỆU KHỞI TẠO BAN ĐẦU (SEED DATA MẪU KHỚP VỚI FRONTEND DEMO)
-- =============================================================================

-- Seed 8 Vai trò
INSERT INTO `roles` (`id`, `role_code`, `role_name`, `description`) VALUES
(1, 'ADMIN', 'Quản trị hệ thống', 'Quản lý toàn bộ hệ thống, phân quyền và giám sát bảo mật'),
(2, 'TRAINING_MANAGER', 'Quản lý đào tạo', 'Quản lý chương trình, môn học, mở lớp, xếp thời khóa biểu và xét tốt nghiệp'),
(3, 'ADMISSIONS', 'Tư vấn tuyển sinh', 'Quản lý phễu khách hàng tiềm năng và chuyển đổi nhập học'),
(4, 'INSTRUCTOR', 'Giảng viên', 'Quản lý lớp, điểm danh, giao bài và chấm điểm theo rubric'),
(5, 'TA', 'Trợ giảng', 'Hỗ trợ điểm danh, chấm bài và kèm học viên'),
(6, 'STUDENT', 'Học viên', 'Xem thời khóa biểu, làm bài tập, tra cứu điểm và đóng học phí'),
(7, 'ACCOUNTANT', 'Kế toán', 'Quản lý biểu phí, công nợ và biên lai học phí'),
(8, 'GUEST', 'Khách truy cập', 'Xem thông tin và đăng ký tư vấn')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

-- Seed Users mẫu (Mật khẩu mã hóa BCrypt của chuỗi '123456')
-- BCrypt hash của '123456': $2a$10$7EqJtq98hPqEX7fNZaFWoO... (hoặc tương đương)
INSERT INTO `users` (`id`, `user_code`, `email`, `password_hash`, `full_name`, `phone`, `status`) VALUES
(1, 'NV-ADMIN-01', 'admin@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Quản trị viên', '0901234567', 'ACTIVE'),
(2, 'NV-QLDT-01', 'quanlydaotao@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Phạm Thị Quản', '0912345678', 'ACTIVE'),
(3, 'NV-GV-01', 'giangvien@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Trần Thị Giảng', '0923456789', 'ACTIVE'),
(4, 'NV-TG-01', 'trogiang@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Lê Văn Trợ', '0934567890', 'ACTIVE'),
(5, 'HV-2026-001', 'hocvien@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Nguyễn Văn Học', '0945678901', 'ACTIVE'),
(6, 'NV-KT-01', 'ketoan@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Đỗ Thị Kế', '0956789012', 'ACTIVE'),
(7, 'NV-TV-01', 'tuvan@edumanager.vn', '$2a$10$wI/zZf7ZkJw9p6Fh6n1o0e5Gq6tX3p8/uQ2Y.zQ8K9k7.R5k.dK2m', 'Hoàng Văn Tư', '0967890123', 'ACTIVE')
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);

-- Gán quyền tương ứng
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES
(1, 1), -- Admin
(2, 2), -- Training Manager
(3, 4), -- Instructor
(4, 5), -- TA
(5, 6), -- Student
(6, 7), -- Accountant
(7, 3)  -- Admissions
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- Seed permission catalog
INSERT INTO `permissions` (`permission_code`, `permission_name`, `module`, `description`) VALUES
('MENU_VIEW', 'Xem menu được phép', 'AUTH', 'Đọc menu theo quyền hiện tại'),
('DASHBOARD_VIEW', 'Xem tổng quan', 'DASHBOARD', 'Mở trang tổng quan'),
('PROFILE_VIEW', 'Xem hồ sơ cá nhân', 'PROFILE', 'Đọc hồ sơ của chính mình'),
('USER_READ', 'Xem tài khoản', 'USER', 'Đọc danh sách tài khoản'),
('USER_CREATE', 'Tạo tài khoản', 'USER', 'Tạo tài khoản mới'),
('USER_ROLE_ASSIGN', 'Gán vai trò', 'USER', 'Gán và thu hồi vai trò'),
('ROLE_PERMISSION_READ', 'Xem phân quyền', 'AUTH', 'Đọc vai trò và quyền'),
('ROLE_PERMISSION_UPDATE', 'Cập nhật phân quyền', 'AUTH', 'Gán hoặc thu hồi quyền cho vai trò'),
('PROGRAM_MANAGE', 'Quản lý chương trình', 'PROGRAM', 'Quản lý chương trình đào tạo'),
('CLASS_MANAGE', 'Quản lý lớp học', 'CLASS', 'Mở lớp và phân công'),
('LEAD_MANAGE', 'Quản lý tuyển sinh', 'ADMISSIONS', 'Quản lý khách hàng tiềm năng'),
('ATTENDANCE_MANAGE', 'Quản lý điểm danh', 'ATTENDANCE', 'Điểm danh học viên'),
('ASSIGNMENT_MANAGE', 'Quản lý bài tập', 'ASSIGNMENT', 'Giao và quản lý bài tập'),
('ASSIGNMENT_READ', 'Xem bài tập', 'ASSIGNMENT', 'Xem và nộp bài tập'),
('GRADE_MANAGE', 'Quản lý điểm', 'GRADE', 'Chấm và quản lý điểm'),
('GRADE_READ', 'Xem điểm', 'GRADE', 'Tra cứu điểm cá nhân'),
('FINANCE_MANAGE', 'Quản lý học phí', 'FINANCE', 'Ghi nhận thanh toán và công nợ'),
('FINANCE_READ', 'Xem học phí', 'FINANCE', 'Tra cứu học phí cá nhân'),
('REPORT_VIEW', 'Xem báo cáo', 'REPORT', 'Đọc báo cáo vận hành'),
('SCHEDULE_READ', 'Xem thời khóa biểu', 'SCHEDULE', 'Tra cứu lịch học'),
('CONSULTATION_CREATE', 'Gửi yêu cầu tư vấn', 'PUBLIC', 'Đăng ký tư vấn công khai')
ON DUPLICATE KEY UPDATE `permission_name` = VALUES(`permission_name`);

-- ADMIN nhận toàn bộ quyền. Mỗi role nghiệp vụ chỉ nhận đúng tập quyền của mình.
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r CROSS JOIN `permissions` p WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','PROGRAM_MANAGE','CLASS_MANAGE','SCHEDULE_READ','REPORT_VIEW')
WHERE r.role_code = 'TRAINING_MANAGER'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','LEAD_MANAGE')
WHERE r.role_code = 'ADMISSIONS'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','SCHEDULE_READ','ATTENDANCE_MANAGE','ASSIGNMENT_MANAGE','GRADE_MANAGE')
WHERE r.role_code = 'INSTRUCTOR'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','SCHEDULE_READ','ATTENDANCE_MANAGE','ASSIGNMENT_MANAGE')
WHERE r.role_code = 'TA'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','SCHEDULE_READ','ASSIGNMENT_READ','GRADE_READ','FINANCE_READ')
WHERE r.role_code = 'STUDENT'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','FINANCE_MANAGE','REPORT_VIEW')
WHERE r.role_code = 'ACCOUNTANT'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code = 'CONSULTATION_CREATE'
WHERE r.role_code = 'GUEST'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

INSERT INTO `menus` (`menu_code`, `title`, `href`, `icon`, `sort_order`) VALUES
('DASHBOARD', 'Tổng quan', '/dashboard.html#tong-quan', 'layout-dashboard', 1),
('SCHEDULE', 'Lịch học', '/student.html#lich-hoc', 'calendar-days', 10),
('ASSIGNMENTS', 'Bài tập', '/student.html#bai-tap', 'notebook-pen', 20),
('GRADES', 'Bảng điểm', '/student.html#bang-diem', 'award', 30),
('ATTENDANCE', 'Điểm danh', '/instructor.html#diem-danh', 'clipboard-check', 40),
('CLASSES', 'Lớp học', '/training-manager.html#mo-lop', 'calendar-range', 50),
('LEADS', 'Tuyển sinh', '/admissions.html#lead', 'users', 60),
('FINANCE', 'Học phí', '/accountant.html#thanh-toan', 'wallet', 70),
('USERS', 'Tài khoản', '/admin.html#tai-khoan', 'user-cog', 80),
('ROLES', 'Vai trò và quyền', '/admin.html#vai-tro', 'key-round', 90),
('PROFILE', 'Hồ sơ cá nhân', '/dashboard.html#ho-so', 'user-round', 100)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`), `href` = VALUES(`href`);

INSERT INTO `menu_permissions` (`menu_id`, `permission_id`)
SELECT m.id, p.id FROM `menus` m JOIN `permissions` p ON p.permission_code = CASE m.menu_code
    WHEN 'DASHBOARD' THEN 'DASHBOARD_VIEW'
    WHEN 'SCHEDULE' THEN 'SCHEDULE_READ'
    WHEN 'ASSIGNMENTS' THEN 'ASSIGNMENT_READ'
    WHEN 'GRADES' THEN 'GRADE_READ'
    WHEN 'ATTENDANCE' THEN 'ATTENDANCE_MANAGE'
    WHEN 'CLASSES' THEN 'CLASS_MANAGE'
    WHEN 'LEADS' THEN 'LEAD_MANAGE'
    WHEN 'FINANCE' THEN 'FINANCE_READ'
    WHEN 'USERS' THEN 'USER_READ'
    WHEN 'ROLES' THEN 'ROLE_PERMISSION_READ'
    WHEN 'PROFILE' THEN 'PROFILE_VIEW'
END
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);

-- Seed hồ sơ Học viên
INSERT INTO `students` (`id`, `user_id`, `student_code`, `entry_level`, `enrollment_status`) VALUES
(1, 5, 'HV2026-001', 'Cơ bản (Beginner)', 'STUDYING')
ON DUPLICATE KEY UPDATE `student_code` = VALUES(`student_code`);
