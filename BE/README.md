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

## Đăng ký tư vấn công khai (S2-08 – EP-03)

API công khai dành cho khách truy cập website gửi thông tin đăng ký tư vấn mà không cần đăng nhập. Khi gửi thành công, hệ thống tự động tạo một bản ghi Lead ở trạng thái `NEW` (Mới) với nguồn `WEBSITE`.

### Endpoint

`POST /api/public/consultation-requests`  
(Alias hỗ trợ: `POST /api/consultation-requests`)

### Request Body

```json
{
  "fullName": "Nguyễn Văn A",
  "phone": "0912345678",
  "email": "nguyenvana@gmail.com",
  "course": "Lập trình Web Front-end",
  "message": "Tôi muốn được tư vấn lộ trình học cho người mới bắt đầu",
  "preferredTime": "Buổi tối các ngày trong tuần"
}
```

*Hỗ trợ cả định dạng camelCase (`fullName`) và snake_case (`full_name`).*

- **Bắt buộc:** `fullName` (2 - 100 ký tự), `phone` (định dạng số điện thoại Việt Nam hợp lệ, 10 số).
- **Tùy chọn:** `email` (tối đa 150 ký tự, đúng định dạng email), `course` (tối đa 100 ký tự), `message` (tối đa 1000 ký tự), `preferredTime` (tối đa 100 ký tự), `interestedProgramId` (số nguyên dương).

### Cơ chế bảo mật và chống spam

1. **Rate Limiting:** Giới hạn tối đa 5 yêu cầu trong 1 phút trên mỗi IP (cấu hình qua `CONSULTATION_RATE_LIMIT_MAX` và `CONSULTATION_RATE_LIMIT_WINDOW_MS`). Vượt quá giới hạn trả HTTP `429 Too Many Requests`.
2. **Sanitization:** Tự động loại bỏ các thẻ HTML, `<script>`, `<iframe>` và mã độc để chống XSS.
3. **Mass Assignment Protection:** Khách gửi request không thể tự chỉ định các trường quản trị nội bộ (`status`, `assignedCounselorId`, `convertedStudentId`, `rejectReason`, `id`). Hệ thống luôn gán `status: "NEW"`, `source: "WEBSITE"`.
4. **Anti-duplicate Debounce:** Nếu cùng một số điện thoại bấm gửi liên tục trong vòng 10 giây (do double click hoặc lag mạng), hệ thống trả về HTTP `409` (`code: DUPLICATE_SUBMISSION`) và không tạo bản ghi trùng lặp trong cơ sở dữ liệu.

### Phản hồi mẫu

**Thành công (`201 Created`):**
```json
{
  "success": true,
  "message": "Cảm ơn bạn đã đăng ký tư vấn. Trung tâm sẽ liên hệ lại trong thời gian sớm nhất.",
  "data": {
    "id": "1",
    "fullName": "Nguyễn Văn A",
    "phone": "0912345678",
    "email": "nguyenvana@gmail.com",
    "course": "Lập trình Web Front-end",
    "status": "NEW",
    "createdAt": "2026-10-04T09:50:00.000Z"
  }
}
```

**Dữ liệu không hợp lệ (`400 Bad Request`):**
```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu đăng ký tư vấn không hợp lệ",
  "errors": [
    "Vui lòng nhập họ và tên.",
    "Số điện thoại chưa đúng định dạng (ví dụ: 0912345678 hoặc +84912345678)."
  ]
}
```

**Gửi trùng thao tác nhanh (`409 Conflict`):**
```json
{
  "success": false,
  "code": "DUPLICATE_SUBMISSION",
  "message": "Hệ thống đang xử lý yêu cầu trước đó của bạn. Vui lòng không bấm gửi liên tục!"
}
```

**Vượt quá tần suất gửi (`429 Too Many Requests`):**
```json
{
  "success": false,
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Bạn đã gửi quá nhiều yêu cầu tư vấn. Vui lòng thử lại sau ít phút!",
  "retryAfter": 60
}
```

## Tài khoản demo mặc định

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Admin | `admin@example.com` | `admin123` |
| Instructor | `instructor@example.com` | `instructor123` |
| Student | `student@example.com` | `student123` |

Danh sách người dùng và lead hiện được lưu trong bộ nhớ và dùng để demo; dữ liệu cập nhật sẽ mất khi tiến trình khởi động lại. Trước khi triển khai thực tế, cần thay bằng kho người dùng thật và đặt `JWT_SECRET` mạnh trong môi trường chạy. File migration CSDL tương ứng cho bảng leads: `database/migration_s2_08_consultation_requests.sql`.
