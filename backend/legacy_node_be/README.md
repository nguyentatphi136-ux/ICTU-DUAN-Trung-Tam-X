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

---

## API Chuyển Lead đã chốt thành Hồ sơ học viên (Story S3-03 - IDTTX-211 / Subtask IDTTX-240)

Áp dụng cho nhánh: **`feature/S3-03-chuyen-hoc-vien`**

### Yêu cầu nghiệp vụ Story S3-03:
> **User Story:** Là Tư vấn tuyển sinh, tôi muốn chuyển một lead đã chốt thành hồ sơ học viên, để không phải nhập lại thông tin khách đã cung cấp ba lần trước đó.

1. **Dữ liệu lead được chuyển sang hồ sơ học viên, không phải nhập lại:**
   - Họ tên, số điện thoại, chương trình đào tạo quan tâm, tư vấn viên, nguồn tiếp cận, ID lead gốc được tự động kế thừa vào hồ sơ học viên mới tạo.
2. **Hệ thống sinh mã học viên tự động:**
   - Định dạng mã: `HV26xxxx` cho năm 2026 (ví dụ: `HV260143`), tự động tăng tuần tự từ mã cao nhất hiện có.
   - API `GET /api/students/next-code` cung cấp trước mã dự kiến cho hộp thoại ConvertDialog trên frontend.
3. **Tài khoản đăng nhập cho học viên được tạo và gửi email:**
   - Email đăng nhập do hệ thống cấp theo chuẩn TMS: `S25200212{pad(n, 3)}@tms.vn` (ví dụ `S25200212143@tms.vn`).
   - Tài khoản người dùng được tạo trong bảng `users` với vai trò `Student` (`Học viên`) và mật khẩu tạm thời.
   - Email kích hoạt kèm thông tin tài khoản được gửi tới địa chỉ `sendTo` do tư vấn viên chỉ định hoặc email của lead.
4. **Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa:**
   - Cập nhật `lead.student = studentCode`, `lead.isConverted = true`, `lead.isLocked = true`, `lead.subStatus = 'Đã nhập học'`.
   - Tự động ghi nhật ký chuyển đổi vào dòng thời gian (`timeline`) và nhật ký chăm sóc (`care_logs`).
   - Chặn toàn bộ thao tác sửa lead, kéo thẻ đổi stage hoặc thêm nhật ký mới (trả về lỗi HTTP `400 Bad Request` với mã `LEAD_LOCKED`).

### Danh sách API Story S3-03:

| Phương thức | Đường dẫn | Quyền truy cập | Mô tả |
| --- | --- | --- | --- |
| `POST` | `/api/leads/:id/convert` hoặc `/admissions/leads/:id/convert` | `Admissions`, `TrainingManager`, `Admin` | Chuyển lead đã chốt thành học viên, tạo user và gửi email |
| `GET` | `/api/students/next-code` | `Admissions`, `TrainingManager`, `Admin` | Lấy trước mã học viên và email đăng nhập tự sinh tiếp theo |
| `GET` | `/api/students` | `Admissions`, `TrainingManager`, `Instructor`, `Admin` | Tra cứu danh sách học viên kèm bộ lọc và tìm kiếm |
| `GET` | `/api/students/:code` | `Admissions`, `TrainingManager`, `Instructor`, `Admin` | Xem chi tiết hồ sơ học viên kèm lịch sử học tập |
| `POST` | `/api/students/:code/notes` | `Admissions`, `TrainingManager`, `Admin` | Thêm ghi chú vào hồ sơ học viên |

#### Mẫu yêu cầu chuyển đổi lead (`POST /api/leads/:id/convert`):
```http
POST /api/leads/5/convert HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "sendTo": "duc.tm@gmail.com"
}
```
- **Phản hồi thành công (`201 Created`):**
```json
{
  "success": true,
  "message": "Chuyển đổi thành công lead Trần Minh Đức thành học viên HV260143",
  "data": {
    "code": "HV260143",
    "name": "Trần Minh Đức",
    "phone": "0903456789",
    "email": "duc.tm@gmail.com",
    "login": "S25200212143@tms.vn",
    "program": "Lập trình Frontend React",
    "status": "Mới nhập học",
    "enrolledAt": "2026-10-16",
    "counselor": "Hoàng Văn Tư (Tư vấn viên)",
    "source": "HOTLINE",
    "leadId": 5
  },
  "user": {
    "id": "7",
    "email": "s25200212143@tms.vn",
    "name": "Trần Minh Đức",
    "roles": ["Student"]
  },
  "emailSentTo": "duc.tm@gmail.com"
}
```

---

## API Quản lý Hồ sơ học viên (Story S3-04 - IDTTX-212 / Subtask IDTTX-241)

Áp dụng cho nhánh: **`feature/S3-04-ho-so-hoc-vien`**

### Yêu cầu nghiệp vụ Story S3-04:
> **User Story:** Là Quản lý đào tạo, tôi muốn quản lý hồ sơ học viên, để tra cứu được toàn bộ quá trình học của một người khi họ xin cấp lại bảng điểm.

1. **Xem và sửa thông tin cá nhân, trình độ đầu vào, ghi chú:**
   - Thông tin cá nhân: số điện thoại (chuẩn 10 số, đầu 03, 05, 07, 08, 09), ngày sinh, email liên hệ, địa chỉ, ngày nhập học, tư vấn viên, nguồn tiếp cận.
   - Trình độ đầu vào: điểm kiểm tra đầu vào, xếp loại trình độ, nền tảng học vấn / kinh nghiệm.
   - Ghi chú: Thêm ghi chú mới tự động kèm ngày hôm nay và tên người thực hiện (từ token đăng nhập). Hỗ trợ sửa hồ sơ và thêm ghi chú đồng thời.
2. **Trang hồ sơ hiển thị lớp đang học và lịch sử các lớp đã học:**
   - Lớp đang học: Mã lớp, chương trình đào tạo, ca học (`slot`), phòng học (`room`), tiến độ buổi học (`done`/`totalSessions`), sĩ số lớp (`classSize`/`max`), trạng thái.
   - Lịch sử các lớp đã học: Danh sách các lớp đã kết thúc hoặc bảo lưu kèm kết quả đánh giá (ví dụ: `Hoàn thành · 8,2`, `Bảo lưu`).
3. **Tìm kiếm và lọc học viên đa tiêu chí:**
   - Tìm kiếm nhanh (`search` hoặc `q`): Không phân biệt hoa thường theo mã học viên, họ tên, số điện thoại (hỗ trợ nhập số liền không khoảng cách như `0912345678`), email.
   - Lọc theo trạng thái học tập (`status`): `Mới nhập học`, `Đang học`, `Bảo lưu`, `Đã tốt nghiệp`, `Thôi học`.
   - Lọc theo chương trình đào tạo (`program`): `Lập trình web Full-stack`, `Phân tích dữ liệu`, `Thiết kế UI/UX`, v.v.
   - Phân trang chuẩn (`page`, `limit`) kèm thống kê: tổng số học viên thỏa mãn lọc (`total`), số học viên `Đang học` (`studyingCount`), tổng số học viên toàn trường (`totalAll`).
4. **Phân quyền người dùng (Role-Based Access Control):**
   - `TrainingManager` (Quản lý đào tạo), `Admin`, `Admissions`: Toàn quyền xem danh sách, xem chi tiết, sửa hồ sơ học viên và thêm ghi chú.
   - `Instructor` (Giảng viên): Được xem danh sách và xem chi tiết hồ sơ phục vụ công tác giảng dạy, không được sửa hồ sơ.
   - `Student` (Học viên): **Chỉ được xem hồ sơ của chính mình** (`GET /api/students/:code` kiểm tra `s.login === user.email`). Bị chặn khỏi API xem danh sách toàn trường (`403 FORBIDDEN_STUDENT_LIST`), bị chặn xem hồ sơ của học viên khác (`403 FORBIDDEN_STUDENT_PROFILE`), và không có quyền sửa hồ sơ (`403`).

### Danh sách API Story S3-04:

| Phương thức | Đường dẫn | Quyền truy cập | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/students` hoặc `/students` | `TrainingManager`, `Admin`, `Admissions`, `Instructor` | Danh sách học viên có tìm kiếm, lọc theo trạng thái & chương trình, phân trang và thống kê |
| `GET` | `/api/students/:code` hoặc `/students/:code` | `TrainingManager`, `Admin`, `Admissions`, `Instructor`, `Student` (chỉ chính mình) | Xem chi tiết hồ sơ học viên kèm thông tin lớp đang học và lịch sử các lớp đã học |
| `GET` | `/api/students/me` | Mọi người dùng đã đăng nhập | Tra cứu hồ sơ học viên của chính tài khoản đang đăng nhập |
| `PUT` / `PATCH` | `/api/students/:code` | `TrainingManager`, `Admin`, `Admissions` | Cập nhật thông tin cá nhân, trình độ đầu vào và thêm ghi chú mới vào hồ sơ học viên |
| `POST` | `/api/students/:code/notes` | `TrainingManager`, `Admin`, `Admissions` | Thêm ghi chú mới vào hồ sơ học viên |
| `GET` | `/api/classes` | Mọi người dùng đã đăng nhập | Danh sách các lớp học kèm tiến độ buổi học và sĩ số |
| `GET` | `/api/classes/:code` | Mọi người dùng đã đăng nhập | Chi tiết lớp học theo mã lớp |

#### Mẫu sửa hồ sơ học viên (`PUT /api/students/HV260101`):
```http
PUT /api/students/HV260101 HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json

{
  "phone": "0988 777 666",
  "birth": "2004-05-20",
  "email": "minhanh.new@gmail.com",
  "address": "Số 99 Cầu Giấy, Hà Nội",
  "entryScore": "8,0 / 10",
  "level": "Khá",
  "background": "Đã hoàn thành khóa React cơ bản",
  "note": "Học viên xin bảo lưu 1 tuần vì thi học kỳ ở trường"
}
```
- **Phản hồi thành công (`200 OK`):**
```json
{
  "success": true,
  "message": "Đã cập nhật hồ sơ học viên HV260101 · NGUYEN MINH ANH",
  "data": {
    "code": "HV260101",
    "name": "NGUYEN MINH ANH",
    "phone": "0988 777 666",
    "email": "minhanh.new@gmail.com",
    "login": "S25200212101@tms.vn",
    "program": "Lập trình web Full-stack",
    "status": "Đang học",
    "enrolledAt": "2026-08-12",
    "cls": "WEB-K15",
    "birth": "2004-05-20",
    "address": "Số 99 Cầu Giấy, Hà Nội",
    "entryScore": "8,0 / 10",
    "level": "Khá",
    "background": "Đã hoàn thành khóa React cơ bản",
    "currentClass": {
      "code": "WEB-K15",
      "program": "Lập trình web Full-stack",
      "slot": "Tối 2-4-6",
      "room": "P.301",
      "done": 22,
      "totalSessions": 64,
      "classSize": 27
    },
    "notes": [
      {
        "at": "2026-10-16",
        "by": "Phạm Thị Quản (Quản lý đào tạo)",
        "text": "Học viên xin bảo lưu 1 tuần vì thi học kỳ ở trường"
      }
    ],
    "history": [
      { "cls": "GIT-K03", "program": "Git và làm việc nhóm", "period": "06/2026 – 07/2026", "result": "Hoàn thành · 8,2" }
    ]
  }
}
```

