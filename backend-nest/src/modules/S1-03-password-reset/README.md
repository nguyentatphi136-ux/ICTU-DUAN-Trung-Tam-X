# S1-03 Quên mật khẩu, đặt lại mật khẩu

API: `POST /api/auth/forgot-password, /api/auth/reset-password`. Bản Java đang chạy: `backend/src/main/java/com/ems/controller/ForgotPasswordServlet.java` và `ResetPasswordServlet.java` (xem thêm docs/S2-api.md).
Chuyển sang NestJS thì đặt controller, service, module của story vào thư mục này rồi thêm module vào `src/app.module.ts`.
