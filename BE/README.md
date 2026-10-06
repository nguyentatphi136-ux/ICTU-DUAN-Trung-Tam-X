# Backend API

API demo bằng Node.js và Express: đăng nhập, bộ lọc ủy quyền theo vai trò, quản trị tài khoản và phân bổ / thu hồi vai trò.

## Chạy ứng dụng

Yêu cầu Node.js 18 trở lên.

```sh
npm install
```

Tùy chọn: sao chép `.env.example` thành `.env` để cấu hình cổng, JWT secret và thông tin tài khoản demo. Nếu không tạo `.env`, ứng dụng dùng cấu hình mặc định trong `.env.example`.

```sh
npm start
npm test
```

## Vai trò

Một người dùng có thể giữ nhiều vai trò cùng lúc. Danh mục thống nhất với frontend:

| Mã vai trò | Tên |
| --- | --- |
| `Admin` | Quản trị hệ thống |
| `TrainingManager` | Quản lý đào tạo |
| `Instructor` | Giảng viên |
| `TeachingAssistant` | Trợ giảng |
| `Admissions` | Tư vấn tuyển sinh |
| `Accountant` | Kế toán |
| `Student` | Học viên |

## Đăng nhập

`POST /api/auth/login` với `Content-Type: application/json`:

```json
{
  "email": "instructor@example.com",
  "password": "instructor123"
}
```

Phản hồi thành công gồm `user.id`, `user.email`, `user.name`, `user.roles` (mảng) và `token`.

`GET /api/auth/me` (cần token) trả về thông tin và vai trò hiện tại của người dùng. Client nên gọi lại API này để cập nhật vai trò sau khi được phân bổ hoặc thu hồi.

## Bộ lọc ủy quyền

`src/middleware/auth.js` cung cấp hai middleware:

- `authenticate`: kiểm tra header `Authorization: Bearer <token>`, nạp người dùng từ kho dữ liệu. Trả `401` nếu thiếu hoặc sai token, `403` với `code: ACCOUNT_INACTIVE` nếu tài khoản không ở trạng thái `active`.
- `authorize(...roles)`: cho qua nếu người dùng có ít nhất một vai trò trong danh sách, ngược lại trả `403` với `code: FORBIDDEN`.

```js
const { ROLES } = require('../constants/roles');
const { authenticate, authorize } = require('../middleware/auth');

router.get('/reports', authenticate, authorize(ROLES.ACCOUNTANT, ROLES.ADMIN), handler);
```

Vai trò và trạng thái luôn được đọc từ kho người dùng ở mỗi request, không lấy từ token, nên thu hồi vai trò hoặc khoá tài khoản có hiệu lực ngay, không cần chờ token hết hạn.

## API quản trị

Mọi API dưới `/admin` yêu cầu token của người dùng có vai trò `Admin`.

### Cập nhật tài khoản

`PUT /admin/users/:id`: gửi một hoặc nhiều trường `email`, `name`, `phone`, `status`; các trường không gửi sẽ được giữ nguyên.

```json
{
  "name": "Nguyen Van An",
  "phone": "0912345678",
  "status": "active"
}
```

Trạng thái hợp lệ: `active`, `inactive`, `locked`. Vai trò không sửa qua API này: gửi `role` hoặc `roles` sẽ nhận `400`. Admin không thể tự khoá tài khoản của mình (`409`, `CANNOT_DEACTIVATE_SELF`). Nếu ID không tồn tại, API trả HTTP `404` với `User not found`.

### Phân bổ và thu hồi vai trò

| Phương thức | Đường dẫn | Mô tả |
| --- | --- | --- |
| `GET` | `/admin/roles` | Danh mục vai trò |
| `GET` | `/admin/users/:id/roles` | Vai trò hiện tại của người dùng |
| `POST` | `/admin/users/:id/roles` | Phân bổ vai trò, body `{ "role": "TeachingAssistant" }` |
| `DELETE` | `/admin/users/:id/roles/:roleId` | Thu hồi vai trò, `roleId` là mã vai trò (vd. `Accountant`) |

Mã phản hồi:

| HTTP | `code` | Trường hợp |
| --- | --- | --- |
| `201` | | Phân bổ thành công |
| `200` | | Thu hồi thành công |
| `400` | | Vai trò không hợp lệ |
| `404` | | Không tìm thấy người dùng |
| `404` | `ROLE_NOT_ASSIGNED` | Thu hồi vai trò mà người dùng không có |
| `409` | `ROLE_ALREADY_ASSIGNED` | Phân bổ vai trò người dùng đã có |
| `409` | `CANNOT_REVOKE_OWN_ADMIN` | Admin tự thu hồi vai trò `Admin` của mình |

## Tài khoản demo mặc định

| Vai trò | Email | Mật khẩu | Ghi chú |
| --- | --- | --- | --- |
| Admin | `admin@example.com` | `admin123` | Quản trị hệ thống |
| TrainingManager | `manager@example.com` | `manager123` | Quản lý đào tạo (phân công lead) |
| Instructor | `instructor@example.com` | `instructor123` | Giảng viên |
| Student | `student@example.com` | `student123` | Học viên |
| Admissions | `admissions@example.com` | `admissions123` | Tư vấn viên 1 (Lê Thị Thu Hà) |
| Admissions | `tuvan@example.com` | `tuvan123` | Tư vấn viên 2 (Hoàng Văn Tư) |

## API Phân công Lead cho Tư vấn viên (Story S2-10 - IDTTX-165 / Subtask IDTTX-195)

Áp dụng cho nhánh: **`s2-10-minhngoc(BE)`**

### Yêu cầu nghiệp vụ Story S2-10:
1. **Phân công một hoặc nhiều lead cùng lúc:** Quản lý đào tạo (`TrainingManager`) hoặc Quản trị hệ thống (`Admin`) có thể giao nhiều khách hàng tiềm năng cùng lúc cho một tư vấn viên tuyển sinh (`Admissions`).
2. **Phân quyền truy cập theo vai trò:** Tư vấn viên tuyển sinh (`Admissions`) chỉ nhìn thấy các lead được phân công cho chính mình. Không thể xem hoặc can thiệp lead của tư vấn viên khác.
3. **Ghi lịch sử chuyển giao:** Hệ thống tự động ghi nhật ký chuyển giao đầy đủ vào bảng `lead_assignments` và timeline chi tiết của lead (tư vấn viên cũ, tư vấn viên mới, người thực hiện phân công, ghi chú, thời điểm).

### Danh sách API Story S2-10:

| Phương thức | Đường dẫn | Vai trò được phép | Mô tả |
| --- | --- | --- | --- |
| `POST` | `/api/leads/assign` | `TrainingManager`, `Admin` | Phân công một hoặc nhiều lead cùng lúc cho tư vấn viên |
| `GET` | `/api/leads/counselors` | `TrainingManager`, `Admin` | Lấy danh sách tư vấn viên (`Admissions`) phục vụ dropdown phân công |
| `GET` | `/api/leads` | `TrainingManager`, `Admin`, `Admissions` | Lấy danh sách lead (Tự động lọc chỉ hiển thị lead của chính mình nếu là `Admissions`) |
| `GET` | `/api/leads/:id` | `TrainingManager`, `Admin`, `Admissions` | Xem chi tiết lead (Chặn 403 nếu tư vấn viên xem lead của người khác) |
| `GET` | `/api/leads/:id/assignments` | `TrainingManager`, `Admin`, `Admissions` | Xem toàn bộ lịch sử chuyển giao của lead |

#### Chi tiết API `POST /api/leads/assign`:
- **Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Body:**
  ```json
  {
    "leadIds": ["3", "4"],
    "counselorId": "5",
    "note": "Giao chăm sóc khách hàng mới từ chiến dịch tuyển sinh"
  }
  ```
- **Phản hồi thành công (`200 OK`):**
  ```json
  {
    "success": true,
    "message": "Đã phân công thành công 2 lead cho Lê Thị Thu Hà (Tư vấn viên)",
    "data": {
      "updatedCount": 2,
      "leads": [...],
      "assignments": [...]
    }
  }
  ```


