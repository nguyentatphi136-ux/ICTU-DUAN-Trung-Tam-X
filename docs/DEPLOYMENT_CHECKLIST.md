# Hướng Dẫn Triển Khai & Cấu Hình Máy Chủ Thực Tế (Deployment Checklist)

## 1. Yêu Cầu Môi Trường (System Prerequisites)
- **Java**: JDK 17 hoặc 21 LTS (Eclipse Adoptium / Oracle OpenJDK).
- **Node.js**: Phiên bản 20.x hoặc 22.x LTS.
- **Cơ Sở Dữ Liệu**: MySQL 8.0+ (hỗ trợ `utf8mb4_unicode_ci` và `character_set_server=utf8mb4`).

## 2. Cấu Hình Reverse Proxy (Nginx / Nginx Proxy Manager)
Khi triển khai hệ thống sau Reverse Proxy, cần cấu hình chuyển tiếp IP thực của client thông qua header HTTP:

```nginx
location / {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

## 3. Cấu Hình Tomcat RemoteIpValve
Trong tệp `conf/server.xml` của Apache Tomcat:
```xml
<Valve className="org.apache.catalina.valves.RemoteIpValve"
       internalProxies="127\.0\.[0-1]\.1"
       remoteIpHeader="x-forwarded-for"
       proxiesHeader="x-forwarded-by"
       protocolHeader="x-forwarded-proto" />
```

## 4. Danh Mục Kiểm Tra Trước Khi Bàn Giao (Pre-Release Checklist)
- [x] Đã cấu hình mã hóa mật khẩu BCrypt cho toàn bộ tài khoản.
- [x] Đã kích hoạt cơ chế khóa tạm 15 phút theo IP máy (`IpLoginAttemptService`).
- [x] Đã thiết lập giới hạn tần suất yêu cầu (`AuthRateLimitFilter`).
- [x] Đã đồng bộ phân quyền động 7 Actor (Admin, Quản lý đào tạo, Giảng viên, Trợ giảng, Tuyển sinh, Kế toán, Học viên).
- [x] Kiểm thử tự động (Unit Test / Integration Test) đạt 100% tỷ lệ vượt qua.
