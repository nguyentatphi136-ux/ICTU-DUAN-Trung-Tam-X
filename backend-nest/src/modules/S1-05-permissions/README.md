# S1-05 Ma trận phân quyền

API: `GET /api/admin/roles, /api/admin/permissions; PUT /api/admin/roles/{code}/permissions`. Bản Java đang chạy: `backend/src/main/java/com/ems/controller/AdminApiServlet.java` và `security/PermissionPolicy.java` (xem thêm docs/S2-api.md).
Chuyển sang NestJS thì đặt controller, service, module của story vào thư mục này rồi thêm module vào `src/app.module.ts`.
