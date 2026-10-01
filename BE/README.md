# Backend

API demo bằng Node.js và Express cho đăng nhập, tạo tài khoản và cập nhật tài khoản. Dữ liệu người dùng hiện nằm trong bộ nhớ.

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

## Tạo tài khoản

`POST /api/auth/register` nhận `email`, `name` và `password`; tài khoản mới mặc định có vai trò `Student`. Email được chuẩn hóa bằng cách bỏ khoảng trắng ở hai đầu và chuyển thành chữ thường trước khi dò trùng. Nếu email đã được dùng, API không tạo tài khoản và trả HTTP `409`:

```json
{
  "success": false,
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "Email này đã tồn tại trong hệ thống, vui lòng chọn email khác!"
}
```

## Cập nhật tài khoản

`PATCH /api/auth/users/:id` nhận một hoặc nhiều trường `email`, `name`, `role`, `password` và yêu cầu JWT của tài khoản `Admin` trong header `Authorization: Bearer <token>`. Khi đổi email, API loại trừ chính tài khoản đang sửa khỏi phép dò trùng; email thuộc tài khoản khác trả cùng HTTP `409` và thông báo cụ thể như khi tạo mới.

Tài khoản demo mặc định:

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Admin | `admin@example.com` | `admin123` |
| Teacher | `teacher@example.com` | `teacher123` |
| Student | `student@example.com` | `student123` |

Danh sách người dùng hiện được lưu trong bộ nhớ và dùng để demo, nên dữ liệu tạo/cập nhật mất khi tiến trình khởi động lại. Khi thay bằng cơ sở dữ liệu, cần giữ kiểm tra email không phân biệt hoa thường và đặt unique constraint trên email để chống trùng khi có nhiều yêu cầu đồng thời. Đặt `JWT_SECRET` mạnh trong môi trường chạy.
