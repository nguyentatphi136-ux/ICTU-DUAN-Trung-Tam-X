# HỆ THỐNG QUẢN LÝ ĐÀO TẠO TRUNG TÂM (EMS) - MODULE JAVA SERVLET / JSP

> **Dự án Thực tập Cơ sở CNTT - ICTU & CodeGym**  
> **User Story:** `IDTTX-20` (Phân quyền theo vai trò cho toàn hệ thống)  
> **Các Subtask hoàn thành:**  
> - `IDTTX-39` [BE] Thiết kế Database (Chuẩn 3NF, schema DDL trong `database/schema.sql`)  
> - `IDTTX-81` [BE] Xây dựng middleware kiểm tra quyền ở tầng server, mặc định từ chối  
> **Người thực hiện:** Nguyễn Minh Ngọc (MN) - `nguyenminhngoc482006@gmail.com`  

---

## 1. Cấu trúc thư mục dự án Java Servlet / JSP

```text
Java-JSP/
├── pom.xml                                     # Cấu hình Maven (Java 17, Servlet 4.0, JSTL, MySQL, JUnit 5)
├── README.md                                   # Hướng dẫn chạy và triển khai
└── src/
    ├── main/
    │   ├── java/vn/edu/ictu/ems/
    │   │   ├── constant/
    │   │   │   ├── RoleConstant.java           # Định nghĩa đủ 8 vai trò nghiệp vụ
    │   │   │   └── PermissionConstant.java     # Ma trận phân quyền: Giảng viên sửa điểm, Kế toán sửa học phí
    │   │   ├── filter/
    │   │   │   └── AuthorizationFilter.java    # Middleware kiểm tra quyền tầng server, mặc định từ chối
    │   │   ├── controller/
    │   │   │   ├── GradeServlet.java           # Servlet xử lý điểm số
    │   │   │   ├── TuitionServlet.java         # Servlet xử lý học phí
    │   │   │   └── LoginServlet.java           # Servlet đăng nhập / đăng xuất
    │   │   └── model/
    │   │       ├── User.java                   # Thực thể người dùng & danh sách vai trò
    │   │       ├── Grade.java                  # Thực thể điểm số
    │   │       └── TuitionFee.java             # Thực thể học phí
    │   └── webapp/
    │       ├── login.jsp                       # Giao diện đăng nhập (có nút chọn nhanh tài khoản test)
    │       └── WEB-INF/
    │           ├── web.xml                     # Cấu hình Deployment Descriptor, Filter, trang lỗi 403
    │           └── views/
    │               ├── common/
    │               │   └── 403.jsp             # Trang thông báo từ chối truy cập tiếng Việt thân thiện
    │               ├── grades/
    │               │   ├── list-grade.jsp      # Bảng điểm học viên
    │               │   └── edit-grade.jsp      # Giao diện sửa điểm (Giảng viên)
    │               └── tuition/
    │                   ├── list-tuition.jsp    # Bảng học phí
    │                   └── edit-tuition.jsp    # Giao diện sửa học phí (Kế toán)
    └── test/
        └── java/vn/edu/ictu/ems/
            └── RoleAuthorizationTest.java      # Bộ kiểm thử tự động JUnit 5 cho ít nhất 3 vai trò
```

---

## 2. Quy tắc nghiệp vụ cốt lõi đã giải quyết trong `AuthorizationFilter.java`

1. **Khai báo 8 vai trò nghiệp vụ:** `Admin`, `TrainingManager`, `Admissions`, `Instructor`, `TeachingAssistant`, `Accountant`, `Student`, `Guest`.
2. **Nguyên tắc mặc định từ chối (Default-Deny):**
   - Mọi request đều đi qua `AuthorizationFilter`.
   - Nếu chưa đăng nhập -> Chuyển hướng về `login.jsp` kèm thông báo: *"Vui lòng đăng nhập để tiếp tục"*.
   - Nếu tài khoản bị khoá (`status = locked`) -> Chặn ngay lập tức.
   - Bất kỳ chức năng được bảo vệ nào không được cấp quyền rõ ràng -> Trả về mã lỗi HTTP `403` và hiển thị trang `403.jsp`.
3. **Phân tách quyền rõ ràng giữa Giảng viên và Kế toán:**
   - **Giảng viên (Instructor):** Được phép vào `/grade/edit` và `/grade/update`. Nhưng nếu cố tình truy cập `/tuition/edit` hoặc `/tuition/update` -> Bị chặn với thông báo: *"Giảng viên không có quyền chỉnh sửa thông tin học phí."*
   - **Kế toán (Accountant):** Được phép vào `/tuition/edit` và `/tuition/update`. Nhưng nếu cố tình truy cập `/grade/edit` hoặc `/grade/update` -> Bị chặn với thông báo: *"Kế toán không có quyền chỉnh sửa điểm số học viên."*
4. **Thông báo tiếng Việt thân thiện:**
   - Thay vì hiển thị lỗi kỹ thuật (500 Internal Server Error hoặc Exception stack trace), hệ thống hiển thị trang `403.jsp` bằng tiếng Việt rõ ràng, kèm nút điều hướng quay về màn hình làm việc đúng chuyên môn.

---

## 3. Hướng dẫn chạy dự án trên IntelliJ IDEA / Eclipse

### Yêu cầu môi trường:
- JDK 17 hoặc 21 LTS.
- Apache Tomcat 9.0 hoặc 10.1.

### Các bước mở trên IntelliJ IDEA:
1. Mở IntelliJ IDEA -> Chọn **Open** -> Chọn thư mục `Java-JSP/`.
2. Chọn **Add Framework Support...** -> Tích chọn **Web Application**.
3. Cấu hình Run/Debug Configuration:
   - Thêm cấu hình **Tomcat Server** -> **Local**.
   - Tab **Deployment**: Thêm Artifact `ems-jsp:war exploded` (hoặc thư mục `src/main/webapp`).
   - Application context: `/` hoặc `/ems`.
4. Nhấn nút **Run** (Shift + F10).
5. Trình duyệt tự động mở: `http://localhost:8080/login.jsp`.

### Tài khoản kiểm thử nhanh (Demo Accounts):
- **Giảng viên:** `instructor@example.com` / `123456`
- **Kế toán:** `accountant@example.com` / `123456`
- **Quản trị viên:** `admin@example.com` / `123456`
- **Học viên:** `student@example.com` / `123456`
*(Trên trang `login.jsp` đã có sẵn các nút bấm chọn nhanh từng vai trò để kiểm thử phân quyền ngay lập tức)*.
