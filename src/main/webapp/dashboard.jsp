<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bảng điều khiển | EMS Đào tạo (JSP & Servlet)</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f8fafc; min-height: 100vh; }
        .hero-banner { background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); color: white; border-radius: 16px; padding: 32px; margin-bottom: 30px; box-shadow: 0 10px 25px rgba(30, 58, 138, 0.15); }
        .module-card { border: none; border-radius: 14px; transition: transform 0.2s, box-shadow 0.2s; background: #ffffff; box-shadow: 0 4px 15px rgba(0,0,0,0.05); height: 100%; }
        .module-card:hover { transform: translateY(-4px); box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
        .module-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 26px; margin-bottom: 18px; }
        .bg-icon-primary { background: #eff6ff; color: #2563eb; }
        .bg-icon-success { background: #f0fdf4; color: #16a34a; }
        .bg-icon-warning { background: #fffbeb; color: #d97706; }
        .bg-icon-danger { background: #fef2f2; color: #dc2626; }
        .bg-icon-purple { background: #faf5ff; color: #9333ea; }
        .bg-icon-info { background: #ecfeff; color: #0891b2; }
    </style>
</head>
<body>

    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container">
            <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/dashboard.jsp">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <div class="navbar-nav me-auto">
                <a class="nav-link active" href="${pageContext.request.contextPath}/dashboard.jsp">Trang tổng quan</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/grade/list">Điểm số</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/tuition/list">Học phí</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/training-programs">Chương trình học</a>
                <c:if test="${sessionScope.currentUser.hasRole('Admin')}">
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/users/roles">Phân quyền vai trò</a>
                </c:if>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3">Xin chào, <strong>${sessionScope.currentUser.name}</strong> 
                    <c:forEach var="role" items="${sessionScope.currentUser.roles}">
                        <span class="badge bg-primary ms-1">${role}</span>
                    </c:forEach>
                </span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container pb-5">
        <!-- Hero Banner -->
        <div class="hero-banner">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h2 class="fw-bold mb-2">Xin chào, ${sessionScope.currentUser.name}!</h2>
                    <p class="mb-0 text-white-50">Chào mừng bạn đến với Cổng thông tin Quản lý Đào tạo Trung tâm (EMS). Hệ thống được xây dựng trên nền tảng Java Servlet, JSP và kiến trúc MVC chuẩn mực.</p>
                </div>
                <div class="col-md-4 text-md-end mt-3 mt-md-0">
                    <span class="badge bg-light text-dark px-3 py-2 fs-6">
                        <i class="bi bi-person-badge me-1"></i> ${sessionScope.currentUser.email}
                    </span>
                </div>
            </div>
        </div>

        <!-- Danh sách chức năng nghiệp vụ -->
        <h4 class="fw-bold mb-3 text-secondary"><i class="bi bi-grid-fill me-2"></i>Các phân hệ nghiệp vụ</h4>
        
        <div class="row g-4 mb-4">
            <!-- 1. Quản lý Phân quyền vai trò (Jira IDTTX-24 - Admin) -->
            <c:if test="${sessionScope.currentUser.hasRole('Admin')}">
                <div class="col-md-4 col-sm-6">
                    <div class="card module-card p-4">
                        <div class="module-icon bg-icon-danger">
                            <i class="bi bi-shield-lock-fill"></i>
                        </div>
                        <h5 class="fw-bold mb-2">Phân bổ & Thu hồi Vai trò</h5>
                        <p class="text-muted small flex-grow-1">Tác vụ IDTTX-24: Gán nhiều vai trò cho người dùng, thu hồi vai trò có hiệu lực ngay, chống tự thu hồi quyền Admin.</p>
                        <a href="${pageContext.request.contextPath}/admin/users/roles" class="btn btn-danger btn-sm mt-2">
                            Truy cập phân quyền <i class="bi bi-arrow-right ms-1"></i>
                        </a>
                    </div>
                </div>
            </c:if>

            <!-- 2. Quản lý Điểm số (Giảng viên / Quản lý / Admin / Học viên) -->
            <div class="col-md-4 col-sm-6">
                <div class="card module-card p-4">
                    <div class="module-icon bg-icon-success">
                        <i class="bi bi-award-fill"></i>
                    </div>
                    <h5 class="fw-bold mb-2">Bảng điểm Học viên</h5>
                    <p class="text-muted small flex-grow-1">Xem danh sách điểm số. Giảng viên được phép sửa điểm trực tiếp; Kế toán bị hạn chế quyền sửa theo ma trận RBAC.</p>
                    <a href="${pageContext.request.contextPath}/grade/list" class="btn btn-success btn-sm mt-2">
                        Xem bảng điểm <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>

            <!-- 3. Quản lý Học phí (Kế toán / Admin / Học viên) -->
            <div class="col-md-4 col-sm-6">
                <div class="card module-card p-4">
                    <div class="module-icon bg-icon-purple">
                        <i class="bi bi-cash-stack"></i>
                    </div>
                    <h5 class="fw-bold mb-2">Quản lý Học phí</h5>
                    <p class="text-muted small flex-grow-1">Theo dõi công nợ học phí và biên lai thu tiền. Kế toán được phép sửa thông tin học phí; Giảng viên bị chặn sửa.</p>
                    <a href="${pageContext.request.contextPath}/tuition/list" class="btn btn-primary btn-sm mt-2" style="background-color: #6f42c1; border-color: #6f42c1;">
                        Xem sổ học phí <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>

            <!-- 4. Quản lý Chương trình đào tạo (Quản lý đào tạo / Admin) -->
            <div class="col-md-4 col-sm-6">
                <div class="card module-card p-4">
                    <div class="module-icon bg-icon-primary">
                        <i class="bi bi-book-half"></i>
                    </div>
                    <h5 class="fw-bold mb-2">Chương trình Đào tạo</h5>
                    <p class="text-muted small flex-grow-1">Danh mục các chương trình học, số lượng môn học, thời lượng buổi học và trạng thái vận hành của trung tâm.</p>
                    <a href="${pageContext.request.contextPath}/training-programs" class="btn btn-primary btn-sm mt-2">
                        Xem danh mục <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>

            <!-- 5. Đổi mật khẩu cá nhân -->
            <div class="col-md-4 col-sm-6">
                <div class="card module-card p-4">
                    <div class="module-icon bg-icon-warning">
                        <i class="bi bi-key-fill"></i>
                    </div>
                    <h5 class="fw-bold mb-2">Đổi Mật khẩu</h5>
                    <p class="text-muted small flex-grow-1">Cập nhật mật khẩu cá nhân an toàn theo chuẩn bảo mật BCrypt và chính sách độ phức tạp.</p>
                    <a href="${pageContext.request.contextPath}/change-password.jsp" class="btn btn-warning btn-sm mt-2 text-dark">
                        Đổi mật khẩu <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>

            <!-- 6. Quản trị Tài khoản (Admin) -->
            <c:if test="${sessionScope.currentUser.hasRole('Admin')}">
                <div class="col-md-4 col-sm-6">
                    <div class="card module-card p-4">
                        <div class="module-icon bg-icon-info">
                            <i class="bi bi-people-fill"></i>
                        </div>
                        <h5 class="fw-bold mb-2">Quản lý Tài khoản</h5>
                        <p class="text-muted small flex-grow-1">Xem thông tin chi tiết tài khoản, trạng thái phiên làm việc và nhật ký bảo mật người dùng.</p>
                        <a href="${pageContext.request.contextPath}/admin.jsp" class="btn btn-info btn-sm mt-2 text-white">
                            Quản lý tài khoản <i class="bi bi-arrow-right ms-1"></i>
                        </a>
                    </div>
                </div>
            </c:if>
        </div>

        <!-- Hộp thông tin phiên đăng nhập -->
        <div class="card shadow-sm border-0 bg-white p-4">
            <h6 class="fw-bold text-dark mb-2"><i class="bi bi-info-circle-fill text-primary me-2"></i>Thông tin phiên làm việc hiện tại:</h6>
            <div class="row text-secondary small">
                <div class="col-md-4"><strong>Mã Session ID:</strong> <code><%= session.getId() %></code></div>
                <div class="col-md-4"><strong>Thời gian sống phiên:</strong> 8 giờ (Sliding Window Session)</div>
                <div class="col-md-4"><strong>Trạng thái bảo mật:</strong> <span class="badge bg-success">An toàn (Protected by AuthorizationFilter)</span></div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
