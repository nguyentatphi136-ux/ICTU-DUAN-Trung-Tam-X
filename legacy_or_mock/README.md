# Thư mục Lưu trữ Mã nguồn Cũ & Mock Prototype (Legacy & Mock)

Thư mục này được dùng để lưu trữ các thành phần prototype / mock server trước đây (viết bằng Node.js / Express) nhằm phục vụ mục đích lưu trữ lịch sử hoặc đối chiếu:

1. `legacy_node_be/`: Backend prototype trước đây bằng Node.js & Express (đã được chuyển đổi hoàn toàn sang **Java Servlet / JSP** trong `src/`).
2. `mock-node-api/`: Mock API server chạy độc lập để test nhanh frontend khi chưa khởi động Tomcat.

> **QUAN TRỌNG:**
> - Toàn bộ mã nguồn Backend chính thức của dự án EMS (ICTU & CodeGym) được triển khai trên **Java Servlet / JSP / JSTL / JDBC** theo cấu trúc chuẩn Maven tại thư mục `src/` và quản lý bằng file `pom.xml`.
> - Không chạy `npm run dev` trong thư mục này. Nếu muốn chạy giao diện web frontend, hãy vào thư mục `frontend/` và gõ `npm run dev`.
