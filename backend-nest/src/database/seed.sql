-- Chép từ database/schema.sql (file gốc dùng chung với backend Java). Sửa ở file gốc rồi chép lại vào đây.
-- Chạy sau schema.sql.

USE `ems_database`;

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
-- Tất cả dùng cùng một hash BCrypt hợp lệ của '123456'.
INSERT INTO `users` (`id`, `user_code`, `email`, `password_hash`, `full_name`, `phone`, `status`) VALUES
(1, 'NV-ADMIN-01', 'A25200212001@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Quản trị viên', '0901234567', 'ACTIVE'),
(2, 'NV-QLDT-01', 'TM25200212002@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Phạm Thị Quản', '0912345678', 'ACTIVE'),
(3, 'NV-GV-01', 'IST25200212045@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Trần Thị Giảng', '0923456789', 'ACTIVE'),
(4, 'NV-TG-01', 'TA25200212007@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Lê Văn Trợ', '0934567890', 'ACTIVE'),
(5, 'HV-2026-001', 'S25200212101@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Nguyễn Văn Học', '0945678901', 'ACTIVE'),
(6, 'NV-KT-01', 'ACT25200212003@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Đỗ Thị Kế', '0956789012', 'ACTIVE'),
(7, 'NV-TV-01', 'AMS25200212012@tms.vn', '$2a$10$cyIvmCRxKLhympqO.PoHu.KNRNvKM7FIMEwUDn3MZJflOqYO9.EfS', 'Hoàng Văn Tư', '0967890123', 'ACTIVE')
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
('PROGRAM_READ', 'Xem chương trình', 'PROGRAM', 'Xem chương trình, môn học, lộ trình'),
('STUDENT_READ', 'Xem hồ sơ học viên', 'CLASS', 'Xem danh sách và hồ sơ học viên'),
('CLASS_READ', 'Xem lớp học', 'CLASS', 'Xem lớp, ghi danh, người dạy và phòng học'),
('STUDENT_WRITE', 'Sửa hồ sơ học viên', 'CLASS', 'Sửa thông tin cá nhân, trình độ đầu vào, ghi chú của học viên'),
('TRASH_PURGE', 'Xoá vĩnh viễn', 'SYSTEM', 'Xem thùng rác các module và xoá vĩnh viễn'),
('LEAD_MANAGE', 'Quản lý tuyển sinh', 'ADMISSIONS', 'Quản lý khách hàng tiềm năng'),
('LEAD_ASSIGN', 'Phân công lead', 'ADMISSIONS', 'Xem mọi lead, phân công tư vấn viên và xoá lead'),
('LEAD_READ', 'Xem lead', 'ADMISSIONS', 'Xem danh sách lead, phễu và nhật ký chăm sóc'),
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

-- Quyền từng vai trò theo Product Backlog: sheet 2 "User Roles" (ma trận module x vai trò) và sheet 4 (vai trò của story).
-- ADMIN chỉ quản lý tài khoản (S1-05, S1-08, S1-09, S1-10, S2-01) và xoá vĩnh viễn (nhận xét mentor S2).
-- TRAINING_MANAGER chỉ xem lead (R) nhưng phân công được (S2-10). Phải khớp DatabaseMigration.ROLE_PERMISSIONS.
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','USER_READ','USER_CREATE','USER_ROLE_ASSIGN','ROLE_PERMISSION_READ','ROLE_PERMISSION_UPDATE','TRASH_PURGE')
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','PROGRAM_MANAGE','PROGRAM_READ','STUDENT_READ','STUDENT_WRITE','CLASS_MANAGE','CLASS_READ','SCHEDULE_READ','LEAD_READ','LEAD_ASSIGN','ATTENDANCE_MANAGE','ASSIGNMENT_READ','GRADE_MANAGE','GRADE_READ','FINANCE_READ','REPORT_VIEW','USER_READ')
WHERE r.role_code = 'TRAINING_MANAGER'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','LEAD_MANAGE','PROGRAM_READ','STUDENT_READ','STUDENT_WRITE','CLASS_READ','SCHEDULE_READ','FINANCE_READ','REPORT_VIEW')
WHERE r.role_code = 'ADMISSIONS'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','PROGRAM_READ','STUDENT_READ','CLASS_READ','SCHEDULE_READ','ATTENDANCE_MANAGE','ASSIGNMENT_MANAGE','GRADE_MANAGE','GRADE_READ','REPORT_VIEW')
WHERE r.role_code = 'INSTRUCTOR'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','PROGRAM_READ','STUDENT_READ','CLASS_READ','SCHEDULE_READ','ATTENDANCE_MANAGE','ASSIGNMENT_MANAGE','GRADE_READ')
WHERE r.role_code = 'TA'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','PROGRAM_READ','SCHEDULE_READ','ASSIGNMENT_READ','GRADE_READ','FINANCE_READ')
WHERE r.role_code = 'STUDENT'
ON DUPLICATE KEY UPDATE `permission_id` = VALUES(`permission_id`);
INSERT INTO `role_permissions` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM `roles` r JOIN `permissions` p ON p.permission_code IN
('MENU_VIEW','DASHBOARD_VIEW','PROFILE_VIEW','LEAD_READ','STUDENT_READ','FINANCE_MANAGE','FINANCE_READ','REPORT_VIEW')
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
