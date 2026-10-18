# Sơ Đồ Quan Hệ Dữ Liệu & Thực Thể (Database Relationships)

## 1. Bảng Dữ Liệu Cốt Lõi (Core Entities)
Hệ thống quản lý dữ liệu tập trung qua MySQL với các mối quan hệ được ràng buộc chặt chẽ:

### Phân Hệ 1: Người Dùng & Phân Quyền (RBAC)
- `users`: Lưu trữ thông tin cá nhân (email, full_name, phone, password_hash, status).
- `roles`: Danh mục vai trò (ADMIN, TRAINING_MANAGER, INSTRUCTOR, TA, ADMISSIONS, ACCOUNTANT, STUDENT).
- `user_roles`: Quan hệ nhiều-nhiều (N-N) giữa `users` và `roles`.

### Phân Hệ 2: Tuyển Sinh & Chăm Sóc Khách Hàng (CRM)
- `leads`: Lưu trữ khách hàng tiềm năng từ form đăng ký và import.
- `lead_interaction_logs`: Nhật ký chăm sóc Lead theo từng mốc thời gian (cuộc gọi, ghi chú, đặt lịch).
- `lead_assignments`: Lịch sử phân công Lead cho chuyên viên tuyển sinh.

### Phân Hệ 3: Đào Tạo & Lớp Học (LMS)
- `training_programs`: Chương trình đào tạo khung.
- `subjects`: Danh mục các môn học.
- `program_subjects`: Môn học thuộc chương trình và thứ tự sắp xếp (order_index).
- `classes`: Lớp học thực tế mở theo kỳ.
- `classrooms`: Phòng học với sức chứa tối đa.
- `class_enrollments`: Danh sách học viên ghi danh vào lớp.
