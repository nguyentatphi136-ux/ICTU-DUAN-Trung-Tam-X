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

## Quản lý chương trình đào tạo & Môn học tiên quyết (S2-06 – EP-02)

API dành cho vai trò **Quản lý đào tạo (`TrainingManager`)** và **`Admin`** để gắn môn học vào chương trình, sắp xếp thứ tự học và khai báo môn học tiên quyết.

### Danh sách API

| Phương thức | Đường dẫn | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/training-programs` | Public / Token | Danh sách tất cả chương trình đào tạo |
| `GET` | `/api/training-programs/subjects-catalog` | Public / Token | Danh mục tất cả môn học trong hệ thống |
| `GET` | `/api/training-programs/:programId/courses` | Public / Token | Danh sách môn học trong chương trình theo thứ tự học |
| `POST` | `/api/training-programs/:programId/courses` | `TrainingManager`, `Admin` | Gắn môn học vào chương trình đào tạo |
| `DELETE` | `/api/training-programs/:programId/courses/:courseId` | `TrainingManager`, `Admin` | Gỡ môn học khỏi chương trình |
| `PUT` | `/api/training-programs/:programId/courses/order` | `TrainingManager`, `Admin` | Sắp xếp lại thứ tự học các môn |
| `PUT` | `/api/training-programs/:programId/courses/:courseId/prerequisite` | `TrainingManager`, `Admin` | Khai báo / cập nhật môn tiên quyết |
| `DELETE` | `/api/training-programs/:programId/courses/:courseId/prerequisite` | `TrainingManager`, `Admin` | Gỡ bỏ môn tiên quyết |

*Ghi chú: Các đường dẫn `:programId` và `:courseId` hỗ trợ cả ID số (1, 2, 3...) và slug/mã code (`frontend`, `html-css`, `WEB101`). Có thể dùng `/subjects` thay cho `/courses`.*

### Ví dụ Request & Response

#### 1. Gắn môn học vào chương trình (`POST /api/training-programs/:programId/courses`)
```json
{
  "courseId": 4,
  "prerequisiteId": 1
}
```
Phản hồi thành công: HTTP `201 Created`. Nếu môn đã có trong chương trình trả `409 Conflict`.

#### 2. Sắp xếp thứ tự môn học (`PUT /api/training-programs/:programId/courses/order`)
```json
{
  "courses": [
    { "courseId": 11, "order": 1 },
    { "courseId": 13, "order": 2 },
    { "courseId": 12, "order": 3 },
    { "courseId": 14, "order": 4 }
  ]
}
```
Kiểm tra tính nhất quán: Môn tiên quyết bắt buộc phải có thứ tự đứng trước môn cần tiên quyết.

#### 3. Khai báo môn học tiên quyết (`PUT /api/training-programs/:programId/courses/:courseId/prerequisite`)
```json
{
  "prerequisiteId": 1
}
```
Kiểm tra chống chu trình (Circular Dependency): Ngăn chặn A → B → A hoặc A → B → C → A.

## Tài khoản demo mặc định

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Admin | `admin@example.com` | `admin123` |
| Training Manager | `trainingmanager@example.com` | `trainingmanager123` |
| Instructor | `instructor@example.com` | `instructor123` |
| Student | `student@example.com` | `student123` |

Danh sách dữ liệu hiện được lưu trong bộ nhớ và dùng để demo; dữ liệu cập nhật sẽ mất khi tiến trình khởi động lại. Script migration CSDL tương ứng: `database/migration_s2_06_program_subjects.sql`.
