# API Sprint 2: môn học, lộ trình, buổi học, lead

Dùng cho các trang S2-05 đến S2-11. S2-01, S2-02 và S2-04 đã có API từ trước, xem [S2-sua-theo-nhan-xet-mentor.md](S2-sua-theo-nhan-xet-mentor.md).

Mọi phản hồi có dạng `{ success, code, message, data }`. Khi lỗi, phản hồi là `{ success: false, error: { code, message } }`. Trường có giá trị `null` không có mặt trong JSON, ví dụ lead chưa phân công thì không có `ownerId`.

## Môn học (S2-05), quyền `PROGRAM_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/subjects?q=&program=WEB-FS&page=&size=` | Không gửi `size` thì trả hết. `data: { items, total, page, pageSize }` |
| GET | `/api/subjects/{code}` | |
| POST | `/api/subjects` | `{ code, name, sessions, weight, outcome, programCodes }`. Môn được thêm vào cuối lộ trình của các chương trình trong `programCodes` |
| PUT | `/api/subjects/{code}` | Cùng thân như POST, đổi được mã. Bỏ `programCodes` thì giữ nguyên các chương trình |
| DELETE | `/api/subjects/{code}` | Chuyển vào thùng rác |
| GET | `/api/subjects/trash` | |
| POST | `/api/subjects/{id}/restore` | |
| DELETE | `/api/subjects/{id}/purge` | Chỉ ADMIN |

Một môn có dạng `{ id, code, name, sessions, weight, outcome, status, openedClasses, programs: [{ code, name }] }`.

Mã lỗi: `SUBJECT_CODE_CONFLICT` (409, mã đã dùng, kể cả môn trong thùng rác), `SUBJECT_HAS_CLASSES` (409, môn đã mở lớp nên không xoá được), `SESSIONS_BELOW_LESSONS` (409, giảm số buổi xuống dưới số buổi đã khai báo), `PROGRAM_NOT_FOUND` (400).

## Lộ trình (S2-06), quyền `PROGRAM_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/training-programs/{code}/subjects` | `[{ code, name, sessions, order, prereq: ["WEB-01"] }]` |
| PUT | `/api/training-programs/{code}/subjects` | `{ subjectCodes }`, đổi thứ tự (đã có từ trước). Môn tiên quyết không còn đứng trước sẽ bị bỏ |
| POST | `/api/training-programs/{code}/subjects` | `{ subjectCode }`, thêm vào cuối. 409 `SUBJECT_ALREADY_IN_PROGRAM` |
| DELETE | `/api/training-programs/{code}/subjects/{subjectCode}` | Gỡ môn và các quan hệ tiên quyết liên quan |
| PUT | `/api/training-programs/{code}/subjects/{subjectCode}/prerequisites` | `{ prereq: ["WEB-01", "DB-01"] }`. 400 `PREREQ_NOT_BEFORE` nếu môn tiên quyết không đứng trước |

Các API trên trả về lộ trình mới sau khi sửa.

## Buổi học (S2-07), quyền `PROGRAM_MANAGE`

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/subjects/{code}/lessons` | `[{ order, topic, goal }]` |
| POST | `/api/subjects/{code}/lessons` | `{ topic, goal }`, thêm vào cuối. 409 `LESSON_LIMIT_REACHED` khi đã đủ số buổi |
| PUT | `/api/subjects/{code}/lessons/{order}` | `{ topic, goal }` |
| DELETE | `/api/subjects/{code}/lessons/{order}` | Xoá hẳn, các buổi sau được dồn số thứ tự |
| POST | `/api/subjects/{code}/lessons/clone` | `{ fromSubjectCode, overwrite }`. Môn đã có buổi mà `overwrite` khác `true` thì trả 409 `LESSONS_EXIST`. Môn nguồn nhiều buổi hơn thì chỉ lấy các buổi đầu |

Các API trên trả về danh sách buổi mới.

## Đăng ký tư vấn (S2-08), không cần đăng nhập

| Phương thức | Đường dẫn | Ghi chú |
|---|---|---|
| GET | `/api/public/programs` | `[{ code, name }]`, các chương trình đang áp dụng |
| POST | `/api/public/consultations` | `{ name, phone, email, programCode, message }`. Tạo lead trạng thái `NEW`, nguồn `WEBSITE`, chưa phân công |

Mỗi IP gửi tối đa 5 lần trong 15 phút, quá thì trả 429. Ô "Tôi không phải người máy" chưa phải CAPTCHA thật. Nếu trường ẩn `website` có giá trị, máy chủ vẫn trả thành công nhưng không lưu (dùng làm bẫy bot).

## Lead (S2-09, S2-10, S2-11)

Cần quyền `LEAD_MANAGE`. Người có thêm quyền `LEAD_ASSIGN` (Quản trị, Quản lý đào tạo) thấy mọi lead. Tư vấn viên chỉ thấy và sửa lead được giao cho mình, và lead họ tự thêm được giao luôn cho họ.

| Phương thức | Đường dẫn | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/api/leads?q=&status=&source=&owner=&from=&to=&page=1&size=20` | LEAD_MANAGE | `q` tìm theo tên, hoặc theo số điện thoại khi gõ từ 3 chữ số. `owner` là id tư vấn viên hoặc `none`. `from`, `to` dạng `yyyy-MM-dd` |
| GET | `/api/leads/{id}` | LEAD_MANAGE | Kèm `assignments: [{ at, from, to, by, note }]` |
| GET | `/api/leads/check-phone?phone=&excludeId=` | LEAD_MANAGE | Lead khác cùng số, để cảnh báo trùng |
| POST | `/api/leads` | LEAD_MANAGE | `{ name, phone, email, source, programCode, status, note }` |
| PUT | `/api/leads/{id}` | LEAD_MANAGE | Cùng thân như POST |
| GET | `/api/leads/counselors` | LEAD_ASSIGN | `[{ id, name, email, openLeads }]` |
| POST | `/api/leads/assign` | LEAD_ASSIGN | `{ leadIds, counselorId, note }`. Trả `{ assigned }` là số lead thực sự đổi người phụ trách |
| DELETE | `/api/leads/{id}` | LEAD_ASSIGN | Chuyển vào thùng rác |
| GET | `/api/leads/trash` | LEAD_ASSIGN | |
| POST | `/api/leads/{id}/restore` | LEAD_ASSIGN | |
| DELETE | `/api/leads/{id}/purge` | LEAD_ASSIGN và vai trò ADMIN | |

Một lead có dạng `{ id, name, phone, email, source, sourceLabel, programCode, programName, status, statusLabel, ownerId, ownerName, note, createdAt }`. Số điện thoại được lưu dạng 10 chữ số, `+84` được đổi thành `0`.

| `status` | Nhãn trên giao diện |
|---|---|
| `NEW` | Mới |
| `CONSULTING` | Đang chăm sóc |
| `CONTACTED` | Hẹn gọi lại |
| `TRIAL_SCHEDULED` | Hẹn học thử (giao diện S2 chưa dùng) |
| `WON` | Đã nhập học |
| `REJECTED` | Không quan tâm |

`source` nhận một trong các giá trị: `WEBSITE` (Biểu mẫu web), `FACEBOOK`, `ZALO`, `REFERRAL` (Giới thiệu), `HOTLINE`, `EVENT`, `TIKTOK`.

Mã lỗi: `INVALID_PHONE_FORMAT`, `INVALID_EMAIL_FORMAT`, `PROGRAM_NOT_FOUND` (400), `COUNSELOR_INVALID` (400, người được giao không phải tư vấn tuyển sinh đang hoạt động), `RESOURCE_NOT_FOUND` (404, kể cả khi tư vấn viên mở lead của người khác).

## Cơ sở dữ liệu

- Hai bảng mới: `program_subject_prereqs` (một môn có nhiều môn tiên quyết) và `lead_assignments` (lịch sử phân công). Cả hai đã có trong `database/schema.sql`.
- Quyền mới `LEAD_ASSIGN`, gán cho ADMIN và TRAINING_MANAGER. TRAINING_MANAGER được gán thêm `LEAD_MANAGE`.
- Với CSDL đang chạy, `DatabaseMigration` tạo các bảng còn thiếu khi khởi động. Quyền chỉ được gán một lần, lúc tạo `LEAD_ASSIGN`, nên nếu quản trị thu hồi quyền ở S1-05 thì khởi động lại cũng không gán lại.

## Kiểm thử

`S2ApiTest` (9 test, tầng service) và `S2ApiServletTest` (4 test, gọi servlet với request giả). Cả hai chạy trên H2 ở chế độ MySQL, chưa chạy trên MySQL thật. `mvn test` chạy 87 test, tất cả đều qua.

Frontend chưa nối vào các API này. Các trang vẫn dùng dữ liệu mẫu trong `frontend/src/data/training.ts` và `leads.ts`.
