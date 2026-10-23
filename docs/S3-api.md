# API Sprint 3: phễu lead, học viên, lớp học, phòng học

Dùng cho các trang S3-01 đến S3-09. Backend Java (Servlet + MySQL), cùng khung với API S2 ([S2-api.md](S2-api.md)).

Mọi phản hồi có dạng `{ success, code, message, data }`. Khi lỗi, phản hồi là `{ success: false, error: { code, message } }`. Trường có giá trị `null` không có mặt trong JSON. Ngày có dạng `yyyy-MM-dd`.

Bảng và cột mới được `DatabaseMigration` tự tạo khi backend khởi động, không cần chạy SQL bằng tay. `database/schema.sql` đã cập nhật theo.

## Phễu lead (S3-01), quyền `LEAD_MANAGE`

Tư vấn viên chỉ thấy lead được giao cho mình; người có `LEAD_ASSIGN` thấy mọi lead (giống S2-09).

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/leads/funnel?q=` | `data: { today, overdue, columns: [{ status, label, count, items }] }`. Sáu cột theo thứ tự `NEW, CONTACTED, CONSULTING, TRIAL_SCHEDULED, WON, REJECTED` (Mới, Đã liên hệ, Đang tư vấn, Hẹn học thử, Chốt, Từ chối). Trong mỗi cột, lead quá hạn gọi lại lâu nhất đứng đầu |
| PUT | `/api/leads/{id}/stage` | `{ status, reason }`. Sang `REJECTED` bắt buộc có `reason`. Mỗi lần đổi ghi một dòng nhật ký `STAGE` |

Lead có thêm các trường `rejectReason`, `callbackDate`, `overdueDays` (chỉ có khi quá hạn), `studentId`, `studentCode` (khi đã chuyển thành học viên).

Mã lỗi: `REJECT_REASON_REQUIRED` (400), `LEAD_CONVERTED` (409, lead đã thành học viên nên không sửa, không đổi giai đoạn được; áp dụng cả cho `PUT /api/leads/{id}` của S2-09).

## Nhật ký chăm sóc (S3-02), quyền `LEAD_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/leads/{id}/logs` | `[{ id, type, typeLabel, content, at, by, callbackDate }]`, mới nhất trước |
| POST | `/api/leads/{id}/logs` | `{ type, content, at, callbackDate }`. `type` là `CALL`, `MESSAGE` hoặc `MEETING`. `at` dạng `yyyy-MM-ddTHH:mm`, bỏ trống là bây giờ. Có `callbackDate` thì đặt làm ngày nhắc gọi lại của lead. Trả về lead |

Hệ thống tự ghi thêm hai loại: `STAGE` (đổi giai đoạn) và `CONVERT` (chuyển thành học viên). Lịch sử phân công vẫn nằm ở `assignments` của `GET /api/leads/{id}`.

Lead quá hạn: có `callbackDate` trước hôm nay và chưa ở Chốt hoặc Từ chối.

## Chuyển lead thành học viên (S3-03), quyền `LEAD_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| POST | `/api/leads/{id}/convert` | `{ sendTo }`, email nhận thông tin đăng nhập; bỏ trống thì dùng email của lead. `data: { studentId, code, login, name, email, emailSent }` |

Trong một giao dịch: tạo tài khoản vai trò `STUDENT` với email đăng nhập `<mã học viên>@tms.vn` và mật khẩu mặc định `Tms@2026` (đăng nhập lần đầu phải đổi), tạo hồ sơ học viên với mã `HV` + 2 số cuối năm + số thứ tự 4 chữ số (ví dụ `HV260143`), gắn lead với học viên và ghi nhật ký. Email gửi sau khi đã lưu; gửi lỗi thì `emailSent: false` và học viên vẫn được tạo.

Mã lỗi: `LEAD_NOT_WON` (409, lead chưa ở cột Chốt), `LEAD_ALREADY_CONVERTED` (409), `PHONE_DUPLICATE` (409, số điện thoại đã thuộc tài khoản khác), `INVALID_EMAIL_FORMAT` (400).

## Hồ sơ học viên (S3-04), quyền `CLASS_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/students?q=&status=&program=&page=&size=` | `q` tìm theo mã, họ tên hoặc số điện thoại (từ 3 chữ số). `status`: `STUDYING`, `SUSPENDED`, `GRADUATED`, `DROPPED_OUT` |
| GET | `/api/students/{code}` | Hồ sơ kèm `currentClasses` (lớp chưa kết thúc đang ghi danh) và `history` (các lớp còn lại) |
| PUT | `/api/students/{code}` | `{ name, phone, email, birth, address, level, notes }`. `email` là email liên hệ, không đổi email đăng nhập |

Một học viên: `{ id, code, name, phone, email, login, programCode, programName, status, statusLabel, enrolledAt, currentClass }`. Trang chi tiết có thêm `birth, address, level, notes, leadId, source, counselor`.

## Lớp học (S3-05, S3-07), quyền `CLASS_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/classes?q=&status=&program=&instructorId=&from=&to=&page=&size=` | `from`, `to` lọc theo ngày khai giảng. `data` có thêm `summary: { PLANNING: 2, IN_PROGRESS: 3 }` đếm theo trạng thái với cùng bộ lọc trừ `status` |
| POST | `/api/classes` | `{ code, name, programCode, startDate, maxCapacity, studyMode, roomId }`. `studyMode`: `OFFLINE`, `ONLINE`, `HYBRID`. Lớp mới ở trạng thái `PLANNING`, môn được sao chép từ lộ trình chương trình |
| GET | `/api/classes/{code}` | Chi tiết kèm `subjects` |
| PUT | `/api/classes/{code}/status` | `{ status }`. Chuẩn bị → Đang học → Kết thúc; Chuẩn bị hoặc Đang học → Huỷ |

Một lớp: `{ id, code, name, programCode, programName, startDate, endDate, maxCapacity, enrolled, studyMode, studyModeLabel, status, statusLabel, room, roomWarning, instructor, missingInstructors, sessionsDone, sessionsTotal }`. `enrolled / maxCapacity` là sĩ số; `sessionsDone / sessionsTotal` là tiến độ (buổi `COMPLETED` trong `class_sessions` trên tổng số buổi các môn). `instructor` là giảng viên chính của môn đầu tiên đã phân công.

Mã lỗi: `CLASS_CODE_EXISTS` (409), `PROGRAM_NOT_FOUND` (400), `PROGRAM_HAS_NO_SUBJECTS` (400), `CLASS_STATUS_INVALID` (409).

## Ghi danh (S3-06), quyền `CLASS_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/classes/{code}/enrollments` | `[{ studentCode, name, phone, enrolledAt, status, by }]` |
| POST | `/api/classes/{code}/enrollments` | `{ studentCodes: ["HV260001", "HV260002"] }`, tối đa 200. `data: { enrolled, class }` |

Ghi danh cả nhóm hoặc không ai: bị từ chối nếu một học viên đã có trong lớp, không ở trạng thái Đang học, hoặc lớp không đủ chỗ. Ghi lại ngày ghi danh và người thực hiện.

Mã lỗi: `ALREADY_ENROLLED` (409), `CLASS_FULL` (409), `CLASS_NOT_OPEN` (409, lớp đã kết thúc hoặc huỷ), `STUDENT_NOT_STUDYING` (409), `RESOURCE_NOT_FOUND` (404, mã học viên không có).

## Phân công giảng viên (S3-08)

| Phương thức | Đường dẫn | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/classes/staff` | `CLASS_MANAGE` | `{ instructors, assistants }`, chỉ tài khoản đang hoạt động có vai trò Giảng viên hoặc Trợ giảng, kèm `openClasses` |
| PUT | `/api/classes/{code}/subjects/{subjectCode}` | `CLASS_MANAGE` | `{ instructorId, assistantIds }`. Một giảng viên chính (`null` là bỏ phân công), tối đa 10 trợ giảng. Trả về chi tiết lớp |
| GET | `/api/classes/mine` | Đăng nhập | Lớp người đang đăng nhập dạy chính hoặc trợ giảng, mỗi lớp có `mySubjects: [{ code, name, role: INSTRUCTOR \| TA }]` |

Mỗi môn trong `subjects`: `{ id, code, name, sessions, status, instructor: { id, name }, assistants: [{ id, name }] }`.

Mã lỗi: `INSTRUCTOR_INVALID` (400), `ASSISTANT_INVALID` (400), `CLASS_NOT_OPEN` (409).

## Phòng học (S3-09), quyền `CLASS_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/rooms?type=` | `type`: `PHYSICAL` hoặc `ONLINE` |
| GET | `/api/rooms/{id}` | |
| GET | `/api/rooms/{id}/validate-capacity?students=30` | `{ roomId, capacity, students, ok, message }` |
| POST | `/api/rooms` | `{ code, name, type, capacity, location, meetingUrl }`. Phòng vật lý bắt buộc `capacity`; phòng trực tuyến bắt buộc `meetingUrl`, `capacity` bỏ trống là không giới hạn |
| PUT | `/api/rooms/{id}` | Như POST |
| DELETE | `/api/rooms/{id}` | Chặn khi phòng đang xếp cho lớp chưa kết thúc |
| PUT | `/api/classes/{code}/room` | `{ roomId }`, `null` là gỡ phòng. Trả về chi tiết lớp |

Một phòng: `{ id, code, name, type, capacity, location, meetingUrl, status, openClasses }`.

Khi sĩ số tối đa của lớp vượt sức chứa phòng, lớp có `roomWarning` (vẫn lưu, chỉ cảnh báo).

Mã lỗi: `ROOM_CODE_EXISTS` (409), `ROOM_IN_USE` (409).
