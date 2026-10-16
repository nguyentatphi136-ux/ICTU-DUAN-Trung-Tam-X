# Backend NestJS (khung)

Khung backend viết bằng NestJS (TypeScript), đặt riêng ở `backend-nest/`. Backend đang dùng thật vẫn là bản Java trong `backend/`, có đủ API S1 và S2. Thư mục này mới có khung chạy được, chưa có API nghiệp vụ.

## Chạy

Cần Node.js 22 trở lên.

```sh
cd backend-nest
npm install
cp .env.example .env     # sửa PORT, DATABASE_URL theo máy
npm run start:dev        # chạy và tự tải lại khi sửa code
npm test                 # chạy test
```

Server chạy ở `http://localhost:3000/api` (đổi bằng `PORT`). Hiện có:

| Đường dẫn | Ghi chú |
|---|---|
| `GET /api/health` | Kiểm tra server |
| `GET /api/health/db` | Kiểm tra kết nối MySQL |
| `POST /api/auth/login` | Đăng nhập bằng tài khoản thử trong `src/auth/demo-accounts.ts`, chưa đọc bảng `users` |

Chạy bằng Docker:

```sh
docker build -t ems-backend-nest .
docker run -p 3000:3000 -e DATABASE_URL=mysql://root:matkhau@host.docker.internal:3306/ems_database ems-backend-nest
```

## Thư mục

```
src/
  main.ts               khởi động server, tiền tố /api
  app.module.ts         gom controller, service và các module story
  health.spec.ts        test của khung
  api/                  API thử nghiệm (health)
  auth/                 đăng nhập: contract (dạng dữ liệu), controller, service, tài khoản thử
  database/             kết nối MySQL (database.service.ts), schema.sql, seed.sql
  modules/<mã story>/   mỗi story một thư mục; README trong đó ghi API và file Java tương ứng
```

`database/schema.sql` và `database/seed.sql` được chép từ `database/schema.sql` ở gốc dự án. Muốn sửa CSDL thì sửa file gốc rồi chép lại.

## Chuyển một story từ Java sang NestJS

1. Đọc `src/modules/<mã story>/README.md` để biết API và file Java tương ứng. Đặc tả API S2 nằm ở `docs/S2-api.md`.
2. Tạo `<tên>.module.ts`, `<tên>.controller.ts`, `<tên>.service.ts` trong thư mục đó. Service đọc ghi CSDL qua `DatabaseService`.
3. Thêm module vào `imports` của `src/app.module.ts`.
4. Giữ cùng đường dẫn và dạng phản hồi `{ success, code, message, data }` như bản Java, để frontend không phải sửa.
