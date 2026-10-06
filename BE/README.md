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

## API Quản lý Lead & Tuyển sinh (S2-10 & S2-11)

### 1. Phân công lead cho tư vấn viên (S2-10 - IDTTX-165 / IDTTX-195)
Yêu cầu vai trò: `TrainingManager` hoặc `Admin`.

- `POST /api/leads/assign`: Phân công một hoặc nhiều lead cùng lúc cho tư vấn viên.
  - Body mẫu:
    ```json
    {
      "leadIds": ["3", "4"],
      "counselorId": "5",
      "note": "Giao chăm sóc khách hàng mới từ chiến dịch tuần này"
    }
    ```
  - Hệ thống tự động lưu lịch sử chuyển giao vào bảng `lead_assignments` và timeline của từng lead.
- `GET /api/leads/counselors`: Lấy danh sách các tư vấn viên (`Admissions`) có sẵn để phục vụ phân công.
- `GET /api/leads/:id/assignments`: Xem toàn bộ lịch sử phân công và chuyển giao của một lead.

### 2. Tìm kiếm và lọc lead đa điều kiện (S2-11 - IDTTX-166 / IDTTX-198)
Yêu cầu vai trò: `TrainingManager`, `Admin`, hoặc `Admissions`.
*Lưu ý phân quyền nghiệp vụ:* Nếu người dùng có vai trò `Admissions` (Tư vấn viên), hệ thống tự động lọc chỉ hiển thị các lead được phân công cho chính họ.

- `GET /api/leads`: Tìm kiếm và lọc danh sách lead.
  - Tham số query hỗ trợ:
    - `search` hoặc `q`: Tìm kiếm nhanh theo tên, số điện thoại, email hoặc khoá học.
    - `status`: Lọc theo trạng thái (`NEW`, `CONTACTED`, `CONSULTING`, `TRIAL_SCHEDULED`, `WON`, `REJECTED`, hỗ trợ cả alias tiếng Việt như `mới`, `đang tư vấn`, `đã đăng ký`).
    - `source`: Lọc theo nguồn (`WEBSITE`, `FACEBOOK`, `REFERRAL`, `HOTLINE`, `TIKTOK`, `EVENT`).
    - `counselorId`: Lọc theo ID tư vấn viên (`unassigned` để lọc lead chưa phân công).
    - `createdFrom`, `createdTo`: Lọc theo khoảng ngày tạo.
    - `datePreset`: Lọc theo mốc (`today`, `yesterday`, `this-week`, `this-month`, `last-month` để tìm lại cuộc trao đổi từ tháng trước).
    - `page`, `limit`: Phân trang dữ liệu.
    - `sortBy`, `sortOrder`: Sắp xếp theo trường (`createdAt`, `fullName`, `status`, `lastInteractionAt`).
- `GET /api/leads/stats`: Thống kê tổng quan số lượng lead theo trạng thái phục vụ hiển thị dashboard / cards.
- `GET /api/leads/:id`: Xem chi tiết thông tin và timeline của một lead.
- `POST /api/leads/:id/interactions`: Ghi nhận cuộc gọi, ghi chú trao đổi vào timeline chăm sóc khách hàng.
- `PATCH /api/leads/:id/status`: Cập nhật trạng thái xử lý lead.

