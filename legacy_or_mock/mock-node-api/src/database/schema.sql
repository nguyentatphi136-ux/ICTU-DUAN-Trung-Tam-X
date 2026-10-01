-- =============================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EDUCATION MANAGEMENT SYSTEM - EMS)
-- CƠ SỞ DỮ LIỆU PHÂN QUYỀN VAI TRÒ (ROLE-BASED ACCESS CONTROL - RBAC)
-- Tác vụ Jira: IDTTX-39 [BE] Thiết kế Database
-- Người thực hiện: Nguyễn Minh Ngọc (MN) - nguyenminhngoc482006@gmail.com
-- Chuẩn hóa: Tuân thủ chuẩn 3NF (Third Normal Form)
-- Hỗ trợ: MySQL 8.0+ / PostgreSQL 14+
-- =============================================================================

-- 1. BẢNG ROLES (8 VAI TRÒ NGHIỆP VỤ HỆ THỐNG)
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã định danh vai trò (Admin, Instructor, Accountant, ...)',
    name VARCHAR(100) NOT NULL COMMENT 'Tên hiển thị tiếng Việt của vai trò',
    description VARCHAR(255) NULL COMMENT 'Mô tả phạm vi quyền hạn và trách nhiệm',
    is_assignable BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Có thể gán cho tài khoản đăng nhập hay không (False với Guest)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG USERS (TÀI KHOẢN NGƯỜI DÙNG)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE COMMENT 'Email đăng nhập duy nhất',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Mật khẩu đã được băm an toàn',
    name VARCHAR(100) NOT NULL COMMENT 'Họ và tên người dùng',
    phone VARCHAR(20) NULL COMMENT 'Số điện thoại liên hệ',
    avatar_url VARCHAR(255) NULL COMMENT 'Đường dẫn ảnh đại diện',
    status ENUM('active', 'inactive', 'locked') NOT NULL DEFAULT 'active' COMMENT 'Trạng thái tài khoản',
    last_login_at TIMESTAMP NULL COMMENT 'Thời điểm đăng nhập gần nhất',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG USER_ROLES (QUAN HỆ NHIỀU - NHIỀU: MỘT NGƯỜI DÙNG CÓ THỂ CÓ NHIỀU VAI TRÒ)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    assigned_by INT NULL COMMENT 'ID của Admin thực hiện phân quyền',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BẢNG PERMISSIONS (DANH MỤC QUYỀN HẠN CHI TIẾT THEO PHÂN HỆ)
CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE COMMENT 'Mã quyền (vd: GRADE_EDIT, TUITION_EDIT)',
    name VARCHAR(150) NOT NULL COMMENT 'Tên quyền tiếng Việt',
    module VARCHAR(50) NOT NULL COMMENT 'Phân hệ (GRADE, TUITION, USER, ROLE, CLASS, LEAD)',
    description VARCHAR(255) NULL COMMENT 'Mô tả chi tiết quyền hạn',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_permissions_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. BẢNG ROLE_PERMISSIONS (MA TRẬN PHÂN QUYỀN: VAI TRÒ - QUYỀN HẠN)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    INDEX idx_role_permissions_role (role_id),
    INDEX idx_role_permissions_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. BẢNG GRADES (ĐIỂM SỐ HỌC VIÊN - ĐẶC TẢ STORY IDTTX-20: GIẢNG VIÊN ĐƯỢC SỬA, KẾ TOÁN KHÔNG ĐƯỢC SỬA)
CREATE TABLE IF NOT EXISTS grades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL COMMENT 'ID học viên được chấm điểm',
    class_id INT NOT NULL COMMENT 'ID lớp học',
    component_name VARCHAR(100) NOT NULL COMMENT 'Tên đầu điểm (Chuyên cần, Giữa kỳ, Cuối kỳ, Project)',
    score DECIMAL(4, 2) NOT NULL COMMENT 'Thang điểm 10',
    notes TEXT NULL COMMENT 'Nhận xét của giảng viên',
    updated_by INT NOT NULL COMMENT 'Người cập nhật gần nhất (bắt buộc là Giảng viên hoặc Quản lý đào tạo/Admin)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_grades_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_grades_student (student_id),
    INDEX idx_grades_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. BẢNG TUITION_FEES (HỌC PHÍ - ĐẶC TẢ STORY IDTTX-20: KẾ TOÁN ĐƯỢC SỬA, GIẢNG VIÊN KHÔNG ĐƯỢC SỬA)
CREATE TABLE IF NOT EXISTS tuition_fees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL COMMENT 'ID học viên',
    course_name VARCHAR(150) NOT NULL COMMENT 'Tên khóa học hoặc học phần',
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Tổng học phí phải nộp',
    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT 'Số tiền đã thanh toán',
    status ENUM('unpaid', 'partially_paid', 'paid') NOT NULL DEFAULT 'unpaid' COMMENT 'Trạng thái nợ học phí',
    receipt_no VARCHAR(50) NULL COMMENT 'Mã số biên lai / phiếu thu',
    due_date DATE NULL COMMENT 'Hạn chót thanh toán',
    updated_by INT NOT NULL COMMENT 'Người cập nhật (bắt buộc là Kế toán hoặc Admin)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tuition_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tuition_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_tuition_student (student_id),
    INDEX idx_tuition_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. BẢNG AUDIT_LOGS (NHẬT KÝ THAO TÁC VÀ BẢO MẬT HỆ THỐNG)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL COMMENT 'Người thực hiện thao tác',
    action VARCHAR(100) NOT NULL COMMENT 'Tên hành động (vd: ASSIGN_ROLE, UPDATE_GRADE, UPDATE_TUITION, ACCESS_DENIED)',
    module VARCHAR(50) NOT NULL COMMENT 'Phân hệ bị tác động',
    record_id VARCHAR(50) NULL COMMENT 'ID bản ghi bị tác động',
    status ENUM('SUCCESS', 'FAILED') NOT NULL,
    ip_address VARCHAR(45) NULL,
    details JSON NULL COMMENT 'Chi tiết thay đổi dữ liệu hoặc lý do từ chối',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEED DATA (DỮ LIỆU KHỞI TẠO MẶC ĐỊNH)
-- =============================================================================

INSERT INTO roles (id, code, name, description, is_assignable) VALUES
(1, 'Admin', 'Quản trị hệ thống', 'Quản lý tài khoản người dùng, phân quyền truy cập, giám sát bảo mật hệ thống', TRUE),
(2, 'TrainingManager', 'Quản lý đào tạo', 'Khai báo danh mục chương trình học, mở lớp, xếp thời khóa biểu và cấu hình quy chế tính điểm', TRUE),
(3, 'Admissions', 'Tư vấn tuyển sinh', 'Quản lý phễu khách hàng tiềm năng (Leads), gọi điện chăm sóc và chuyển đổi nhập học', TRUE),
(4, 'Instructor', 'Giảng viên', 'Quản lý lớp phụ trách, điểm danh nhanh, upload học liệu, giao bài tập và chấm điểm', TRUE),
(5, 'TeachingAssistant', 'Trợ giảng', 'Hỗ trợ điểm danh, chấm bài tập thực hành và kèm cặp học viên', TRUE),
(6, 'Accountant', 'Kế toán', 'Thiết lập biểu phí theo khóa, ghi nhận phiếu thu, theo dõi công nợ và biên lai học phí', TRUE),
(7, 'Student', 'Học viên', 'Xem thời khóa biểu, nhận thông báo, nộp bài tập, tra cứu điểm số và biên lai học phí', TRUE),
(8, 'Guest', 'Khách truy cập', 'Xem thông tin chương trình đào tạo công khai, gửi form đăng ký tư vấn trực tuyến', FALSE)
ON DUPLICATE KEY UPDATE name=VALUES(name), description=VALUES(description);

INSERT INTO permissions (code, name, module, description) VALUES
('USER_VIEW', 'Xem danh sách người dùng', 'USER', 'Xem thông tin hồ sơ và trạng thái tài khoản'),
('USER_EDIT', 'Cập nhật tài khoản người dùng', 'USER', 'Sửa thông tin cơ bản của người dùng'),
('USER_LOCK', 'Khóa/Mở khóa tài khoản', 'USER', 'Thay đổi trạng thái tài khoản active/locked'),
('ROLE_VIEW', 'Xem danh mục vai trò', 'ROLE', 'Xem danh sách vai trò và phân quyền'),
('ROLE_ASSIGN', 'Gán/Thu hồi vai trò', 'ROLE', 'Phân bổ hoặc hủy vai trò của tài khoản'),
('GRADE_VIEW', 'Xem bảng điểm', 'GRADE', 'Xem điểm số học viên'),
('GRADE_EDIT', 'Chỉnh sửa và chấm điểm', 'GRADE', 'Nhập điểm, sửa điểm số học viên (Dành riêng cho Giảng viên & Quản lý đào tạo/Admin)'),
('TUITION_VIEW', 'Xem thông tin học phí', 'TUITION', 'Tra cứu công nợ và lịch sử nộp học phí'),
('TUITION_EDIT', 'Chỉnh sửa và ghi nhận học phí', 'TUITION', 'Lập phiếu thu, điều chỉnh học phí, cập nhật trạng thái thanh toán (Dành riêng cho Kế toán & Admin)'),
('CLASS_MANAGE', 'Quản lý lớp học', 'CLASS', 'Mở lớp, phân công giảng viên, xếp phòng học'),
('LEAD_MANAGE', 'Quản lý tuyển sinh', 'LEAD', 'Tiếp nhận lead, gọi điện tư vấn và chuyển đổi nhập học'),
('PUBLIC_VIEW', 'Truy cập cổng công khai', 'PUBLIC', 'Xem thông tin khóa học không cần đăng nhập')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE
  (r.code = 'Admin')
  OR (r.code = 'TrainingManager' AND p.code IN ('CLASS_MANAGE', 'GRADE_VIEW', 'GRADE_EDIT', 'USER_VIEW', 'PUBLIC_VIEW'))
  OR (r.code = 'Instructor' AND p.code IN ('GRADE_VIEW', 'GRADE_EDIT', 'PUBLIC_VIEW'))
  OR (r.code = 'TeachingAssistant' AND p.code IN ('GRADE_VIEW', 'PUBLIC_VIEW'))
  OR (r.code = 'Admissions' AND p.code IN ('LEAD_MANAGE', 'PUBLIC_VIEW'))
  OR (r.code = 'Accountant' AND p.code IN ('TUITION_VIEW', 'TUITION_EDIT', 'GRADE_VIEW', 'PUBLIC_VIEW'))
  OR (r.code = 'Student' AND p.code IN ('GRADE_VIEW', 'TUITION_VIEW', 'PUBLIC_VIEW'))
  OR (r.code = 'Guest' AND p.code IN ('PUBLIC_VIEW'));
