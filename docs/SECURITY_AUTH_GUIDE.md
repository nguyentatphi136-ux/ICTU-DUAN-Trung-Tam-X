# Hướng Dẫn Kiến Trúc Bảo Mật & Xác Thực Người Dùng (EMS Security Guide)

## 1. Tổng Quan Kiến Trúc Xác Thực (Authentication & Security)
Hệ thống Quản lý Đào tạo (EMS) áp dụng mô hình bảo mật đa tầng nhằm đảm bảo an toàn thông tin người dùng và ngăn chặn các hình thức tấn công mạng phổ biến (Brute-force, Account Denial of Service, Session Hijacking).

### Các Lớp Bảo Mật:
1. **Lớp Lọc Tần Suất Yêu Cầu (AuthRateLimitFilter)**:
   - Giới hạn 30 yêu cầu đăng nhập trong 15 phút theo địa chỉ IP.
   - Trả về mã HTTP `429 Too Many Requests` kèm tiêu đề `Retry-After`.
2. **Lớp Kiểm Soát Đăng Nhập Sai Theo IP (IpLoginAttemptService)**:
   - Theo dõi số lần nhập sai thông tin đăng nhập theo địa chỉ IP máy gửi yêu cầu (Client IP).
   - Tạm khóa 15 phút sau 5 lần nhập sai liên tiếp từ cùng một IP.
   - **Chống tấn công Account DoS**: Không khóa tài khoản người dùng trong CSDL, đảm bảo người dùng hợp lệ không bị gián đoạn công việc từ thiết bị khác.
3. **Lớp Mã Hóa Mật Khẩu (BCrypt Password Hashing)**:
   - Mật khẩu được băm bằng thuật toán BCrypt với độ phức tạp `cost = 10`.
   - Sử dụng salt ngẫu nhiên chống lại các cuộc tấn công Rainbow Table.
   - Thời gian kiểm tra mật khẩu hằng số (Constant-time check) chống Timing Attack.
4. **Lớp Kiểm Soát Phiên Làm Việc (Session Management & Blacklist)**:
   - Quản lý phiên làm việc tập trung qua `SessionBlacklist`.
   - Tự động hủy phiên cũ khi đăng nhập trên phiên mới (Single Active Session).
   - Giới hạn thời gian sống tối đa của phiên là 8 giờ (`MaxInactiveInterval = 28800s`).
