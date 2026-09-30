# Backend

Backend Express cho dự án, yêu cầu Node.js 20 trở lên.

## Chạy ứng dụng

```sh
npm install
npm run dev
```

Mặc định API chạy tại `http://localhost:3000`. Có thể cấu hình `PORT` và `AUTH_DB_FILE`. Dữ liệu runtime được lưu trong `BE/data/auth-db.json` và không được đưa lên Git.

## Đặt lại mật khẩu

`POST /auth/reset-password` nhận JSON:

```json
{
  "token": "token-duoc-gui-qua-email",
  "email": "user@example.com",
  "newPassword": "mat-khau-moi"
}
```

Mật khẩu mới cần ít nhất 8 ký tự. Token chỉ hợp lệ nếu khớp với email, chưa hết hạn và chưa sử dụng. Khi thành công, mật khẩu được lưu dưới dạng hash scrypt và token được đánh dấu `is_used: true` trong cùng lần cập nhật dữ liệu.

Luồng gửi email hiện có có thể gọi `createPasswordResetToken(email)` từ `src/storage/authDatabase.js`; hàm lưu bản ghi gồm `token`, `email`, `expires_at` (sau 30 phút) và `is_used: false`. Tài khoản cần được đồng bộ vào kho `users` bằng `provisionUser(email, passwordHash)` trước khi reset được chấp nhận. Kho lưu trữ JSON phù hợp cho bản khởi tạo đơn tiến trình; khi triển khai nhiều tiến trình, thay thế bằng cơ sở dữ liệu dùng chung.
