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

## API Tìm kiếm & Lọc Lead đa điều kiện (Story S2-11 - IDTTX-166 / Subtask IDTTX-198)

Áp dụng cho nhánh: **`s2-11-minhngoc(BE)`**

### Yêu cầu nghiệp vụ Story S2-11:
> **User Story:** Là Tư vấn tuyển sinh, tôi muốn tìm kiếm và lọc lead theo nhiều điều kiện, để tìm lại được cuộc trao đổi từ tháng trước khi khách gọi lại.

1. **Lọc theo nhiều điều kiện:**
   - Trạng thái lead (`status`): `NEW`, `CONTACTED`, `CONSULTING`, `TRIAL_SCHEDULED`, `WON`, `REJECTED` (hỗ trợ cả alias tiếng Việt như `mới`, `đang tư vấn`, `đã đăng ký`).
   - Nguồn lead (`source`): `WEBSITE`, `FACEBOOK`, `REFERRAL`, `HOTLINE`, `TIKTOK`, `EVENT`.
   - Người phụ trách (`counselorId`): Lọc theo ID tư vấn viên hoặc `unassigned` cho lead chưa phân công.
   - Khoảng thời gian: Lọc theo khoảng ngày (`createdFrom`, `createdTo`) hoặc theo mốc định sẵn (`datePreset`: `today`, `yesterday`, `this-week`, `this-month`, `last-month` để tìm lại cuộc trao đổi từ tháng trước).
2. **Tìm nhanh theo tên hoặc số điện thoại:**
   - Hỗ trợ tham số `search` hoặc `q`: tìm kiếm không phân biệt hoa thường theo tên khách hàng, số điện thoại (hỗ trợ partial match đầu số như `0988`), email hoặc khoá học quan tâm.
3. **Tra cứu lịch sử cuộc trao đổi từ tháng trước:**
   - Bảng `lead_interactions` và dòng thời gian `timeline` trong chi tiết lead lưu lại toàn bộ các cuộc gọi, ghi chú trao đổi, thời lượng cuộc gọi và người thực hiện.
4. **Phân trang và sắp xếp:**
   - Hỗ trợ `page`, `limit`, `sortBy`, `sortOrder`.

### Danh sách API Story S2-11:

| Phương thức | Đường dẫn | Vai trò được phép | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/leads` | `Admissions`, `TrainingManager`, `Admin` | Tìm kiếm và lọc danh sách lead theo nhiều tiêu chí (hỗ trợ datePreset `last-month`) |
| `GET` | `/api/leads/stats` | `Admissions`, `TrainingManager`, `Admin` | Thống kê số lượng lead theo phễu tuyển sinh phục vụ Dashboard / Thẻ KPI |
| `GET` | `/api/leads/:id` | `Admissions`, `TrainingManager`, `Admin` | Xem chi tiết thông tin và dòng thời gian cuộc trao đổi (timeline) của lead |
| `POST` | `/api/leads/:id/interactions` | `Admissions`, `TrainingManager`, `Admin` | Ghi nhận cuộc gọi tư vấn, ghi chú chăm sóc khách hàng vào lịch sử |
| `PATCH` | `/api/leads/:id/status` | `Admissions`, `TrainingManager`, `Admin` | Cập nhật trạng thái xử lý lead |

#### Chi tiết tham số `GET /api/leads`:
```
GET /api/leads?search=0988&status=CONSULTING&source=FACEBOOK&datePreset=last-month&page=1&limit=20&sortBy=createdAt&sortOrder=desc
```
- **Phản hồi mẫu (`200 OK`):**
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "1",
        "fullName": "Vũ Minh Anh",
        "phone": "0988123456",
        "email": "minhanh.vu@gmail.com",
        "source": "FACEBOOK",
        "course": "Lập trình Web Fullstack",
        "status": "CONSULTING",
        "statusLabel": "Đang tư vấn",
        "assignedCounselorId": "5",
        "assignedCounselorName": "Lê Thị Thu Hà (Tư vấn viên)",
        "createdAt": "2026-09-01T10:30:00.000Z",
        "timeline": [
          {
            "id": "1",
            "type": "CALL",
            "title": "Cuộc gọi tư vấn học phí tháng trước",
            "content": "Khách hàng hỏi chi tiết về chính sách chia nhỏ đợt đóng học phí 3 lần...",
            "durationSeconds": 240,
            "author": "Lê Thị Thu Hà (Tư vấn viên)"
          }
        ]
      }
    ],
    "pagination": {
      "total": 1,
      "page": 1,
      "limit": 20,
      "totalPages": 1
    }
  }
  ```



