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
| 2 | Nguyễn Minh Ngọc | [Mã SV] | Scrum Master | Điều phối Daily Scrum, Planning, gỡ blocker, hỗ trợ kiểm thử |
| 3 | Nông Hùng Nguyên | [Mã SV] | Backend Dev | Thiết kế CSDL, API quản lý tài khoản, mã hóa mật khẩu |
| 4 | Nguyễn Trung Kiên | DTC245200736 | Backend Dev | Xây dựng bộ lọc Authorization Filter, gán và thu hồi Role (Jira: IDTTX-24) |
| 5 | Nguyễn Văn Kỳ | [Mã SV] | Backend Dev | Xử lý Session, Forgot Password qua Email, API hồ sơ người dùng |
| 6 | Vũ Trọng Nghĩa  | [Mã SV] | Frontend Dev | Thiết kế Layout Master, Dynamic Sidebar Menu theo Role |
| 7 | Nguyễn Thanh Ngọc | [Mã SV] | Frontend Dev | Xây dựng giao diện Login, Forgot Password, Reset Password |
| 8 | Nguyễn Hữu Lợi | [Mã SV] | Frontend Dev | Xây dựng trang Quản trị danh sách người dùng, Modal Thêm/Sửa |
| 9 | Nguyễn Phạm Phương Lan | [Mã SV] | QA / Tester | Viết Test Cases cho Sprint 1 (Đăng nhập, phân quyền, CRUD user) |
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

## 6. Cấu trúc thư mục dự án sau khi sắp xếp chuẩn mực

```text
ICTU-DUAN-Trung-Tam-X/
├── pom.xml                                     # Cấu hình Maven (Java 17, Servlet 4.0, JSP, JSTL, MySQL)
├── package.json                                # Quản lý script npm (Proxy 'npm run dev' vào frontend)
├── README.md                                   # Tài liệu hướng dẫn toàn diện dự án
├── src/                                        # BỘ NGUỒN CHÍNH THỨC: JAVA SERVLET & JSP (MVC)
│   ├── main/
│   │   ├── java/com/ems/
│   │   │   ├── config/                         # DBConnection, SessionBlacklist, Migration
│   │   │   ├── constant/                       # RoleConstant (8 vai trò), PermissionConstant (Ma trận quyền)
│   │   │   ├── controller/                     # Servlets điều hướng: Login, Logout, UserRole, Grade, Tuition, Program
│   │   │   ├── dao/                            # Data Access Objects (UserDAO, AdminDAO, PermissionDAO, TrainingProgramDAO)
│   │   │   ├── dto/                            # Data Transfer Objects
│   │   │   ├── filter/                         # AuthorizationFilter (RBAC), SessionAuthFilter (Sliding session)
│   │   │   ├── model/                          # Entities: User, Grade, TuitionFee, TrainingProgram
│   │   │   ├── security/                       # ApiResponse, Password policies
│   │   │   └── service/                        # EmailService, TrainingProgramService, UserStore (In-memory store & fallback)
│   │   └── webapp/                             # Giao diện JSP, JSTL và cấu hình Web Descriptor
│   │       ├── index.jsp                       # Điều hướng trang chủ
│   │       ├── login.jsp                       # Giao diện đăng nhập (có nút chọn nhanh tài khoản test)
│   │       ├── dashboard.jsp                   # Bảng điều khiển phân hệ nghiệp vụ theo vai trò
│   │       ├── admin.jsp                       # Quản lý tài khoản người dùng
│   │       ├── change-password.jsp             # Đổi mật khẩu
│   │       ├── forgot-password.jsp             # Quên mật khẩu qua email
│   │       ├── reset-password.jsp              # Đặt lại mật khẩu từ liên kết bảo mật
│   │       └── WEB-INF/
│   │           ├── web.xml                     # Cấu hình Deployment Descriptor, session-timeout 8h, error pages
│   │           └── views/
│   │               ├── common/                 # 403.jsp, 404.jsp, 500.jsp (Thông báo tiếng Việt thân thiện)
│   │               ├── admin/                  # user-roles.jsp (Gán & thu hồi vai trò Jira IDTTX-24)
│   │               ├── grades/                 # list-grade.jsp, edit-grade.jsp (Giảng viên sửa điểm, Kế toán chỉ xem)
│   │               ├── tuition/                # list-tuition.jsp, edit-tuition.jsp (Kế toán sửa học phí, Giảng viên chỉ xem)
│   │               └── programs/               # list-programs.jsp (Danh mục chương trình đào tạo S2-04)
│   └── test/java/com/ems/                      # Bộ kiểm thử tự động JUnit 5 cho RBAC, IDTTX-24, Session, Auth
├── database/                                   # CSDL MySQL (schema.sql, DESIGN_DATABASE.md)
├── frontend/                                   # Giao diện web client (Vite, HTML5, CSS3, JS)
├── legacy_or_mock/                             # Lưu trữ prototype cũ Node.js & mock data để đối chiếu
└── UI_UX/                                      # Bản vẽ thiết kế, wireframes và screenshots nghiệm thu
```

---

## 7. Danh sách tài khoản thử nghiệm (Demo Accounts)

Hệ thống hỗ trợ cơ chế xác thực kép: tự động truy vấn MySQL `UserDAO` nếu CSDL đang chạy, hoặc chuyển sang `UserStore` in-memory fallback nếu chưa cấu hình CSDL, đảm bảo luôn đăng nhập được 100%:

| STT | Vai trò nghiệp vụ | Email đăng nhập | Mật khẩu chuẩn | Mật khẩu phụ (chấp nhận) | Phân hệ được truy cập |
|:---:|:---|:---|:---:|:---:|:---|
| 1 | **Quản trị hệ thống (Admin)** | `admin@edumanager.vn` | `Admin@123` | `123456` | Toàn quyền, Phân quyền vai trò (`/admin/users/roles`), Quản lý tài khoản |
| 2 | **Giảng viên (Instructor)** | `giangvien@edumanager.vn` | `Giangvien@123` | `123456` | Bảng điểm học viên, Sửa điểm (`/grade/list`) — *Bị chặn sửa học phí (403)* |
| 3 | **Kế toán (Accountant)** | `ketoan@edumanager.vn` | `Ketoan@123` | `123456` | Bảng học phí, Sửa học phí (`/tuition/list`) — *Bị chặn sửa điểm (403)* |
| 4 | **Quản lý đào tạo (Training Manager)** | `daotao@edumanager.vn` | `Daotao@123` | `123456` | Danh mục chương trình đào tạo (`/training-programs`), Bảng điểm |
| 5 | **Học viên (Student)** | `hocvien@edumanager.vn` | `Hocvien@123` | `123456` | Xem bảng điểm cá nhân, xem học phí cá nhân |
| 6 | **Tư vấn tuyển sinh (Admissions)** | `tuvan@edumanager.vn` | `Tuyensinh@123` | `123456` | Xem thông tin chương trình học, quản lý tuyển sinh |
| 7 | **Trợ giảng (Teaching Assistant)** | `trogiang@edumanager.vn` | `Trogiang@123` | `123456` | Hỗ trợ lớp học, xem điểm |

---

## 8. Hướng dẫn chạy dự án

### Cách 1: Chạy toàn bộ ứng dụng Java Servlet / JSP trên Apache Tomcat (Khuyên dùng)
1. Mở dự án trên **IntelliJ IDEA** hoặc **Eclipse**: Chọn thư mục gốc `ICTU-DUAN-Trung-Tam-X`.
2. IntelliJ sẽ tự động nhận diện file `pom.xml` làm dự án Maven.
3. Thêm cấu hình chạy **Tomcat Server (Local)**:
   - Deployment: Thêm Artifact `education-management-system:war exploded` (hoặc thư mục `src/main/webapp`).
   - Application context: `/` hoặc `/ems`.
4. Nhấn **Run** (Shift + F10).
5. Mở trình duyệt tại: `http://localhost:8080/` (hệ thống sẽ tự động chuyển hướng đến `login.jsp`).
6. Bấm nút chọn nhanh tài khoản thử nghiệm trên giao diện để đăng nhập ngay!

### Cách 2: Chạy giao diện Web Frontend độc lập (Vite)
Nếu bạn muốn phát triển hoặc kiểm thử nhanh giao diện HTML/CSS/JS:
```bash
# Ở bất kỳ thư mục nào (thư mục gốc hoặc thư mục frontend/):
npm run dev

# Trình duyệt mở:
http://localhost:5173/login.html
```
