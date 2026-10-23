<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Trang Quản trị | EduManager</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background: #f8fafc; margin: 0; padding: 40px; }
        .card { background: white; max-width: 600px; margin: 0 auto; padding: 32px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }
        .badge { display: inline-block; padding: 4px 12px; background: #e0f2fe; color: #0369a1; border-radius: 99px; font-size: 12px; font-weight: 700; margin-bottom: 12px; }
        h1 { margin: 0 0 16px; color: #0f172a; font-size: 22px; }
        p { color: #475569; font-size: 14px; line-height: 1.6; }
        .btn-logout { display: inline-block; padding: 10px 20px; background: #ef4444; color: white; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; margin-top: 20px; }
        .btn-logout:hover { background: #dc2626; }
        .info-table { width: 100%; border-collapse: collapse; margin-top: 16px; }
        .info-table td { padding: 8px 0; border-bottom: 1px solid #f1f5f9; font-size: 13px; }
        .info-table td.label { font-weight: 600; color: #64748b; width: 140px; }
    </style>
</head>
<body>

<div class="card">
    <span class="badge">HỆ THỐNG QUẢN LÝ ĐÀO TẠO EMS</span>
    <h1>Xin chào, <c:out value="${sessionScope.currentUser.fullName}" />!</h1>
    <p>Bạn đã đăng nhập thành công vào hệ thống. Phiên làm việc của bạn đang hoạt động và được bảo vệ bởi <code>SessionAuthFilter</code>.</p>

    <table class="info-table">
        <tr>
            <td class="label">Mã người dùng:</td>
            <td><strong><c:out value="${sessionScope.currentUser.userCode}" /></strong></td>
        </tr>
        <tr>
            <td class="label">Email:</td>
            <td><c:out value="${sessionScope.currentUser.email}" /></td>
        </tr>
        <tr>
            <td class="label">Vai trò:</td>
            <td><c:out value="${sessionScope.currentUser.primaryRole}" /></td>
        </tr>
        <tr>
            <td class="label">Mã Session ID:</td>
            <td><code><%= session.getId() %></code></td>
        </tr>
    </table>

    <!-- Nút Đăng xuất gọi LogoutServlet (IDTTX-35, S1-02) -->
    <a href="${pageContext.request.contextPath}/logout" class="btn-logout">
        🚪 Đăng xuất an toàn
    </a>
</div>

</body>
</html>
