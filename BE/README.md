# IDTTX-44 Backend

API đăng nhập demo bằng Node.js và Express. API xác định vai trò `Admin`, `Teacher` hoặc `Student` rồi trả thông tin người dùng cùng JWT.

## Chạy ứng dụng

Yêu cầu Node.js 18 trở lên.

```sh
npm install
```

Tùy chọn: sao chép `.env.example` thành `.env` để cấu hình cổng, JWT secret và thông tin tài khoản demo. Nếu không tạo `.env`, ứng dụng dùng cấu hình mặc định trong `.env.example`.

```sh
npm start
```

## Đăng nhập

`POST /api/auth/login` với `Content-Type: application/json`:

```json
{
  "email": "teacher@example.com",
  "password": "teacher123"
}
```

Phản hồi thành công gồm `user.id`, `user.email`, `user.name`, `user.role` và `token`. Sai email hoặc mật khẩu luôn trả cùng thông báo `Email hoặc mật khẩu không chính xác`.

## Đổi mật khẩu

`POST /auth/change-password` (cũng có alias `POST /api/auth/change-password`) yêu cầu JWT trong header `Authorization: Bearer <token>` và nhận `newPassword`. Mật khẩu phải có ít nhất 8 ký tự, gồm chữ cái ASCII và chữ số. Nếu không hợp lệ, API trả HTTP `400`, mã `INVALID_NEW_PASSWORD` cùng thông báo cụ thể và không cập nhật hash.

Tài khoản demo mặc định:

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Admin | `admin@example.com` | `admin123` |
| Teacher | `teacher@example.com` | `teacher123` |
| Student | `student@example.com` | `student123` |

Danh sách người dùng hiện được lưu trong bộ nhớ và dùng để demo. Trước khi triển khai thực tế, cần thay bằng kho người dùng thật và đặt `JWT_SECRET` mạnh trong môi trường chạy.
