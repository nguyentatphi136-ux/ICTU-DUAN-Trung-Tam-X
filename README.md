# HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EDUCATION MANAGEMENT SYSTEM - EMS)

> **Dự án Thực tập Cơ sở CNTT**  
> **Đơn vị hợp tác:** ICTU & CodeGym  
> **Mô hình triển khai:** Agile / Scrum (8 Sprints - 75 User Stories - 350 Story Points)  
> **Nhánh phát triển chính:** `develop` | **Nhánh phát hành:** `main`

---

## 1. Giới thiệu tổng quan dự án

Hệ thống Quản lý Đào tạo Trung tâm là nền tảng quản trị số hóa toàn diện quy trình vận hành của một trung tâm đào tạo chuyên nghiệp. Dự án giải quyết trọn vẹn luồng nghiệp vụ thực tế từ đầu phễu tuyển sinh, quản lý chương trình học, điều phối lớp học, kiểm soát chuyên cần, quản lý bài tập - chấm điểm, theo dõi công nợ học phí cho đến đánh giá kết quả tốt nghiệp và xuất báo cáo vận hành.

### Các vai trò người dùng trong hệ thống (8 vai trò):
1. **Quản trị hệ thống (System Admin):** Quản lý tài khoản người dùng, phân quyền truy cập, giám sát bảo mật hệ thống.
2. **Quản lý đào tạo (Academic Manager):** Khai báo danh mục chương trình học, môn học, mở lớp, xếp thời khóa biểu và cấu hình quy chế tính điểm.
3. **Tư vấn tuyển sinh (Admission / Sales):** Quản lý phễu khách hàng tiềm năng (Leads), gọi điện chăm sóc và chuyển đổi nhập học.
4. **Giảng viên (Instructor):** Quản lý lớp phụ trách, điểm danh nhanh, upload học liệu, giao bài tập và chấm bài theo tiêu chí (Rubric).
5. **Học viên (Student):** Xem thời khóa biểu, nhận thông báo, nộp bài tập, tra cứu điểm số và đánh giá khảo sát giảng dạy.
6. **Kế toán (Accountant):** Thiết lập biểu phí theo khóa, ghi nhận phiếu thu, theo dõi công nợ và biên lai học phí.
7. **Khách truy cập (Public Guest):** Xem thông tin chương trình đào tạo, gửi form đăng ký tư vấn trực tuyến.
8. **Người dùng chung hệ thống:** Các tính năng dùng chung như xác thực, đổi mật khẩu, cập nhật hồ sơ cá nhân và nhận thông báo.

---

## 2. Lộ trình phát triển sản phẩm (Product Roadmap - 8 Sprints)

* **Sprint 1 (42 pts):** Tài khoản, phân quyền (Role-based Authorization) và quản trị người dùng.
* **Sprint 2 (43 pts):** Hồ sơ cá nhân, danh mục chương trình đào tạo và đầu phễu tuyển sinh.
* **Sprint 3 (42 pts):** Phễu tư vấn khách hàng, chuyển đổi nhập học và mở lớp.
* **Sprint 4 (45 pts):** Tự động sinh thời khóa biểu không trùng lịch và module điểm danh nhanh.
* **Sprint 5 (45 pts):** Cảnh báo chuyên cần tự động và chu trình giao - nộp bài tập.
* **Sprint 6 (45 pts):** Chấm điểm theo Rubric, quản lý kho học liệu buổi học và hệ thống thông báo.
* **Sprint 7 (44 pts):** Quản lý biểu phí, lập phiếu thu và theo dõi sổ sách công nợ.
* **Sprint 8 (44 pts):** Tính điểm tổng kết môn, xét điều kiện tốt nghiệp, khảo sát chất lượng và Dashboard phân tích vận hành.

---

## 3. Kiến trúc công nghệ sử dụng

* **Frontend:** HTML5, CSS3, JavaScript (ES6+), Bootstrap / CSS Framework.
* **Backend:** Java Servlet / JSP, JSTL, mô hình MVC (Model - View - Controller).
* **Cơ sở dữ liệu:** MySQL / PostgreSQL (Tuân thủ chuẩn hóa 3NF).
* **Giao tiếp dữ liệu:** JDBC, RESTful API / JSON.
* **Quản trị dự án & Mã nguồn:** Git/GitHub, Git Flow, Jira Software (Scrum Board).
* **Kiểm thử & Tài liệu:** Postman, JUnit, Test Cases Sheet.

---

## 4. Danh sách thành viên nhóm & Phân công vai trò

| STT | Họ và tên | Mã SV | Vai trò Scrum | Phân công nhiệm vụ kỹ thuật |
|:---:|:---|:---:|:---|:---|
| 1 | Nguyễn Tất Phi | [Mã SV] | Team Leader / Dev | Quản trị dự án, thiết kế kiến trúc hệ thống, BE Core Auth (Sprint 1) |
| 2 | Nguyễn Minh Ngọc | [Mã SV] | Scrum Master / Backend Dev | Điều phối Scrum, thiết kế CSDL & xây dựng API phân công lead (S2-10), tìm kiếm & lọc lead đa điều kiện (S2-11) |
| 3 | Nông Hùng Nguyên | [Mã SV] | Backend Dev | Thiết kế CSDL, API quản lý tài khoản, mã hóa mật khẩu |
| 4 | Nguyễn Trung Kiên | [Mã SV] | Backend Dev | Xây dựng bộ lọc Authorization Filter, gán và thu hồi Role |
| 5 | Nguyễn Văn Kỳ | [Mã SV] | Backend Dev | Xử lý Session, Forgot Password qua Email, API hồ sơ người dùng |
| 6 | Vũ Trọng Nghĩa  | [Mã SV] | Frontend Dev | Thiết kế Layout Master, Dynamic Sidebar Menu theo Role |
| 7 | Nguyễn Thanh Ngọc | [Mã SV] | Frontend Dev | Xây dựng giao diện Login, Forgot Password, Reset Password |
| 8 | Nguyễn Hữu Lợi | [Mã SV] | Frontend Dev | Xây dựng trang Quản trị danh sách người dùng, UI Lead CRM (S2-11) |
| 9 | Nguyễn Phạm Phương Lan | [Mã SV] | QA / Tester | Viết Test Cases cho Sprint 1 & Sprint 2 |
| 10 | Nguyễn Duy Kiên | [Mã SV] | QA / Tester | Thực hiện Manual Test, kiểm thử biên (Edge Cases), theo dõi Bug trên Jira |

---

## 5. Quy ước cộng tác dự án (Git Flow & Commit Convention)

### 5.1. Nguyên tắc phân nhánh
* `main`: Nhánh chạy chính thức, chỉ merge code sau khi nghiệm thu kết thúc Sprint với Mentor.
* `develop`: Nhánh tích hợp chung của cả nhóm.
* `feature/<tên-tính-năng>`: Nhánh làm việc độc lập của từng thành viên (rẽ nhánh từ `develop`).
* `fix/<tên-lỗi>`: Nhánh xử lý bug sau khi test.

### 5.2. Chuẩn viết thông điệp Commit (Conventional Commits)
* `feat: <nội dung>` — Thêm chức năng/giao diện mới.
* `fix: <nội dung>` — Sửa lỗi phát hiện trong quá trình dev/test.
* `docs: <nội dung>` — Cập nhật tài liệu, file README.
* `refactor: <nội dung>` — Tối ưu hóa, dọn dẹp cấu trúc code.

---

## 6. Hướng dẫn cài đặt môi trường cho nhà phát triển

### 6.1. Yêu cầu hệ thống
* JDK: Phiên bản 17 hoặc 21 LTS.
* Apache Tomcat: Phiên bản 9.0 hoặc 10.1.
* Hệ quản trị CSDL: MySQL 8.0+.
* IDE khuyên dùng: IntelliJ IDEA Ultimate hoặc Eclipse EE.

### 6.2. Các bước khởi chạy dự án
```bash
# 1. Clone mã nguồn dự án
git clone [https://github.com/nguyentatphi136-ux/ICTU-DUAN-Trung-Tam-X.git](https://github.com/nguyentatphi136-ux/ICTU-DUAN-Trung-Tam-X.git)

# 2. Di chuyển vào thư mục dự án
cd ICTU-DUAN-Trung-Tam-X

# 3. Chuyển sang nhánh làm việc develop
git checkout develop

# 4. Cấu hình kết nối cơ sở dữ liệu:
# Mở file src/main/resources/database.properties và cập nhật DB_URL, DB_USER, DB_PASSWORD
# 5. Build và chạy ứng dụng trên Tomcat server tại địa chỉ:
# http://localhost:8080/
```

---

## 7. Tiến độ Sprint 2: Quản lý Lead & Phân công Tuyển sinh (Nhánh `s2-10-minhngoc(BE)`)

Nhánh **`s2-10-minhngoc(BE)`** hoàn thành 2 hạng mục Backend cốt lõi thuộc Sprint 2 cho phân hệ Tuyển sinh & Quản lý Lead (Admissions / Leads CRM):

### 7.1. Story S2-10 (IDTTX-165 / Subtask IDTTX-195): Phân công lead cho tư vấn viên
- **Nghiệp vụ đáp ứng:**
  - Quản lý đào tạo (`TrainingManager`) hoặc Quản trị viên (`Admin`) có thể phân công một hoặc nhiều lead cùng lúc cho một tư vấn viên tuyển sinh (`Admissions`).
  - **Phân quyền truy cập:** Tư vấn viên (`Admissions`) chỉ nhìn thấy các lead được phân công cho chính mình; không thể truy cập hoặc thao tác trên lead của tư vấn viên khác.
  - **Lịch sử chuyển giao:** Hệ thống tự động ghi nhận nhật ký chuyển giao vào bảng `lead_assignments` và timeline của từng lead (tư vấn viên cũ, tư vấn viên mới, người phân công, lý do, thời gian).
- **APIs triển khai:**
  - `POST /api/leads/assign`: Phân công lead hàng loạt.
  - `GET /api/leads/counselors`: Lấy danh sách tư vấn viên phục vụ phân công.
  - `GET /api/leads/:id/assignments`: Xem lịch sử chuyển giao của một lead.

### 7.2. Story S2-11 (IDTTX-166 / Subtask IDTTX-198): Tìm kiếm và lọc lead đa điều kiện
- **Nghiệp vụ đáp ứng:**
  - Tìm kiếm nhanh theo tên, số điện thoại (hỗ trợ partial match), email hoặc khóa học quan tâm.
  - Bộ lọc đa điều kiện: Trạng thái lead (`NEW`, `CONTACTED`, `CONSULTING`, `TRIAL_SCHEDULED`, `WON`, `REJECTED`), nguồn lead (`WEBSITE`, `FACEBOOK`, `REFERRAL`, `HOTLINE`, `TIKTOK`, `EVENT`), người phụ trách (`counselorId` hoặc `unassigned`).
  - Lọc theo khoảng thời gian: Khoảng ngày cụ thể hoặc theo mốc định sẵn (`today`, `yesterday`, `this-week`, `this-month`, `last-month` để tìm lại cuộc trao đổi từ tháng trước khi khách gọi lại).
  - Phân trang (`page`, `limit`) và sắp xếp dữ liệu (`sortBy`, `sortOrder`).
- **APIs triển khai:**
  - `GET /api/leads`: Tìm kiếm và lọc danh sách lead (tự động phân quyền theo vai trò).
  - `GET /api/leads/stats`: Thống kê số lượng lead theo phễu phục vụ hiển thị thẻ KPI / Dashboard.
  - `GET /api/leads/:id`: Chi tiết thông tin và timeline tương tác của lead.
  - `POST /api/leads/:id/interactions`: Ghi nhật ký cuộc gọi / ghi chú trao đổi tư vấn.
  - `PATCH /api/leads/:id/status`: Cập nhật trạng thái xử lý lead.

### 7.3. Cơ sở dữ liệu & Kiểm thử
- **Migration SQL:** [`database/migration_s2_10_s2_11_lead_management.sql`](database/migration_s2_10_s2_11_lead_management.sql) (bảng `leads`, `lead_assignments`, `lead_interactions` đạt chuẩn 3NF và đánh index tối ưu truy vấn).
- **Chạy kiểm thử tự động:**
  ```bash
  cd BE
  npm test
  ```
  *Kết quả kiểm thử:* **31/31 tests PASS (100%)** bao quát toàn bộ ca kiểm thử chức năng và phân quyền bảo mật cho S2-10 và S2-11.
