# Backend API

API demo bằng Node.js và Express. API đăng nhập xác định vai trò `Admin`, `Teacher` hoặc `Student`; API quản trị cho phép sửa thông tin người dùng.

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

Phản hồi thành công gồm `user.id`, `user.email`, `user.name`, `user.role` và `token`.

## Cập nhật tài khoản người dùng

`PUT /admin/users/:id` yêu cầu JWT của tài khoản `Admin` trong header `Authorization: Bearer <token>`. Gửi một hoặc nhiều trường có thể chỉnh sửa; các trường không gửi sẽ được giữ nguyên:

```json
{
  "name": "Nguyen Van An",
  "phone": "0912345678",
  "role": "Teacher",
  "status": "active"
}
```

Vai trò hợp lệ: `Admin`, `Teacher`, `Student`. Trạng thái hợp lệ: `active`, `inactive`, `locked`. Nếu ID không tồn tại, API trả HTTP `404` với `User not found`; thành công trả HTTP `200` cùng thông tin người dùng đã cập nhật.

Tài khoản demo mặc định:

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Admin | `admin@example.com` | `admin123` |
| Teacher | `teacher@example.com` | `teacher123` |
| Student | `student@example.com` | `student123` |

Danh sách người dùng hiện được lưu trong bộ nhớ và dùng để demo; dữ liệu cập nhật sẽ mất khi tiến trình khởi động lại. Trước khi triển khai thực tế, cần thay bằng kho người dùng thật và đặt `JWT_SECRET` mạnh trong môi trường chạy.
