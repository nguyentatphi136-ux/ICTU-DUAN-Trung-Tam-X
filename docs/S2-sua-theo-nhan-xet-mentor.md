# S2: sửa theo nhận xét mentor (10/10/2026)

Nhánh: `fix/s2-nhan-xet-mentor`, tách từ `feature/frontend-theo-figma_NPPL` và gộp thêm backend S2 từ `feature/frontend-theo-figma`.

## Đối chiếu 14 mục

| # | Nhận xét | Đã sửa | FE | BE |
|---|---|---|---|---|
| 1 | Bản ghi lỗi / trùng / hợp lệ | Kết quả nhập chia 3 nhóm, mỗi dòng ghi rõ lý do | ✓ | ✓ |
| 2 | Tìm theo email trong bảng nhập | Ô tìm email và lọc theo nhóm | ✓ | |
| 3 | Excel số lượng lớn, phân trang | Đọc xlsx/xls/csv bằng Apache POI, tối đa 5.000 dòng. 1.200 dòng nhập trong khoảng 8 giây (trước đây hơn 2 phút). Bảng có phân trang | ✓ | ✓ |
| 4 | Hồ sơ không lưu được | `/api/profile` biên dịch và chạy được, có quyền truy cập, chỉ sửa hồ sơ của chính người đang đăng nhập | ✓ | ✓ |
| 5 | Trùng số điện thoại | Kiểm tra định dạng và trùng số ở hồ sơ, form tài khoản và khi nhập Excel. Máy chủ trả 409 `PHONE_DUPLICATE` | ✓ | ✓ |
| 6 | Đổi tên module | "Dashboard người dùng và nhật ký" đổi thành "Danh sách người dùng" | ✓ | |
| 7 | Lỗi giao diện S2-05 | Sửa tiêu đề và bố cục trang Môn học | ✓ | |
| 8 | Giao diện xoá | Dùng hộp xác nhận riêng (`ConfirmDialog`) thay cho `window.confirm` | ✓ | |
| 9 | Phân trang | `Pager` có nút Trước/Sau và chọn số dòng mỗi trang | ✓ | |
| 10 | Môn thuộc chương trình nào | Hiện các chương trình chứa môn; gán chương trình ngay trong form môn | ✓ | |
| 11 | Không xoá được thì ẩn nút xoá | Ẩn nút xoá khi môn hoặc chương trình đang được dùng | ✓ | |
| 12 | Kéo thả đổi thứ tự môn | Có vạch báo vị trí thả, nút lên/xuống cho bàn phím và màn hình cảm ứng. Lưu qua `PUT /api/training-programs/{id}/subjects`. Nếu chương trình đã bị đổi ở nơi khác, máy chủ trả 409 `CURRICULUM_CHANGED` và giao diện trả về thứ tự cũ | ✓ | ✓ |
| 13 | Đồng bộ toàn bộ giao diện | Các trang S2 đã dùng chung `Pager`, `ConfirmDialog`, `Toast`. Phần còn lại cần nhóm thống nhất, chưa sửa trong code | ◐ | |
| 14 | Xoá mềm, thùng rác | Thêm cột `deleted_at`, `deleted_by`. Xoá thì chuyển vào thùng rác, sau đó khôi phục được hoặc xoá vĩnh viễn (chỉ ADMIN). Có trang Thùng rác ở Đào tạo, Quản trị, Tuyển sinh | ✓ | ✓ |

## Lỗi backend tìm thêm khi sửa

- `PermissionPolicy` chưa có luật cho `/api/profile` và API nhập Excel, nên các API này luôn trả 403.
- `UserProfileServlet` cho phép sửa hồ sơ người khác qua tham số `userId` hoặc `email` (lỗi IDOR).
- Trang JSP nhập Excel không kiểm tra quyền `USER_CREATE`.
- `schema.sql` dùng cột `duration_months` trong khi code dùng `duration`.
- `DatabaseMigration` không bao giờ được gọi. Đã thêm `AppStartupListener` để chạy migration khi khởi động.
- `AdminApiServlet` đổi mã lỗi 409 thành 400.

## API mới

| Phương thức | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/admin/users/import/template` | USER_CREATE |
| POST | `/api/admin/users/import/preview` | USER_CREATE |
| POST | `/api/admin/users/import` | USER_CREATE |
| GET | `/api/admin/users/trash` | USER_READ |
| DELETE | `/api/admin/users/{id}` (chuyển vào thùng rác) | USER_ROLE_ASSIGN |
| POST | `/api/admin/users/{id}/restore` | USER_ROLE_ASSIGN |
| DELETE | `/api/admin/users/{id}/purge` | USER_ROLE_ASSIGN và vai trò ADMIN |
| GET | `/api/training-programs/trash` | PROGRAM_MANAGE |
| GET, PUT | `/api/training-programs/{idOrCode}/subjects` | PROGRAM_MANAGE |
| POST | `/api/training-programs/{id}/restore` | PROGRAM_MANAGE |
| DELETE | `/api/training-programs/{id}/purge` | PROGRAM_MANAGE và vai trò ADMIN |

Nếu xoá vĩnh viễn một tài khoản đang có dữ liệu liên quan, máy chủ trả 409 `USER_HAS_RELATED_DATA`.

## Chạy và kiểm tra

- Backend: `mvn test` chạy 74 test, tất cả đều qua (trong đó `S2MentorFixesTest` có 9 test).
- Frontend: khi không có `VITE_API_URL`, frontend dùng dữ liệu mẫu lưu trong localStorage. Khi có `VITE_API_URL=http://localhost:8080`, Vite chuyển tiếp `/api` sang backend.
- Đã chạy thử đầu cuối với Tomcat và H2:
  - Nhập 1.200 dòng: 1.195 hợp lệ, 1 trùng, 4 lỗi.
  - Hồ sơ: lưu thành công; nhập số điện thoại trùng thì nhận 409.
  - Thứ tự môn: lưu được; khi xung đột thì trả về thứ tự cũ.
  - Sau khi đăng xuất, API trả 401.

## Còn lại

- Chưa chạy thử trên MySQL thật, mới chạy trên H2 ở chế độ MySQL.
- Danh sách môn học, buổi học và lead chưa có API backend; các trang này vẫn dùng dữ liệu mẫu, kể cả khi đã cấu hình API.
- Mục 13 cần nhóm thống nhất bộ giao diện chung trước khi sửa tiếp.
