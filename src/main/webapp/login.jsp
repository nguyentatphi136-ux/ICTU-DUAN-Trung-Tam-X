<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập | EduManager (JSP & Servlet)</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/frontend/style.css">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f7f9fc; margin: 0; padding: 0; }
        .login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
        .login-card { background: #ffffff; width: 100%; max-width: 440px; padding: 36px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.06); }
        .login-title { font-size: 24px; font-weight: 800; color: #1c3e70; margin-bottom: 8px; }
        .login-subtitle { font-size: 14px; color: #64748b; margin-bottom: 24px; }
        .form-group { margin-bottom: 18px; }
        .form-label { display: block; font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; }
        .form-control { width: 100%; box-sizing: border-box; padding: 12px 14px; border: 1.5px solid #e2e8f0; border-radius: 10px; font-size: 14px; transition: border-color 0.2s; }
        .form-control:focus { outline: none; border-color: #0284c7; }
        .btn-submit { width: 100%; padding: 13px; background: #1c3e70; color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 700; cursor: pointer; transition: background 0.2s; }
        .btn-submit:hover { background: #152f55; }
        .alert { padding: 12px 16px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; line-height: 1.4; }
        .alert-danger { background-color: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
        .alert-success { background-color: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
        .alert-warning { background-color: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
        .demo-box { margin-top: 24px; padding: 14px; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 10px; font-size: 12px; color: #475569; }
    </style>
</head>
<body>

<div class="login-container">
    <div class="login-card">
        <h1 class="login-title">EduManager</h1>
        <p class="login-subtitle">Hệ thống Quản lý Đào tạo (JSP / Servlet / JDBC)</p>

        <%-- Mục 10: JSP và JSTL - Hiển thị thông báo động theo trạng thái phiên --%>
        
        <%-- 1. Thông báo lỗi khi phiên hết hạn (IDTTX-54) --%>
        <c:if test="${param.error == 'session_expired'}">
            <div class="alert alert-warning">
                ⚠️ <strong>Phiên làm việc đã hết hạn.</strong> Vui lòng đăng nhập lại để tiếp tục công việc.
            </div>
        </c:if>

        <%-- 2. Thông báo khi phiên bị thu hồi do đăng xuất (IDTTX-55) --%>
        <c:if test="${param.error == 'session_revoked'}">
            <div class="alert alert-danger">
                🚫 <strong>Phiên đã bị thu hồi.</strong> Bạn đã đăng xuất từ phiên này.
            </div>
        </c:if>

        <%-- 3. Thông báo đăng xuất thành công --%>
        <c:if test="${param.message == 'logged_out'}">
            <div class="alert alert-success">
                ✅ Đăng xuất thành công. Hẹn gặp lại bạn!
            </div>
        </c:if>

        <%-- 4. Thông báo sai tài khoản / mật khẩu từ LoginServlet --%>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger">
                ❌ <c:out value="${errorMessage}" />
            </div>
        </c:if>

        <!-- Form gửi POST tới LoginServlet (Mục 11: MVC Model) -->
        <form action="${pageContext.request.contextPath}/login" method="POST">
            <div class="form-group">
                <label class="form-label" for="email">Email</label>
                <input type="email" id="email" name="email" class="form-control" placeholder="admin@edumanager.vn" required autofocus>
            </div>

            <div class="form-group">
                <label class="form-label" for="password">Mật khẩu</label>
                <input type="password" id="password" name="password" class="form-control" placeholder="Nhập mật khẩu" required>
            </div>

            <button type="submit" class="btn-submit">Đăng nhập</button>
        </form>

        <div class="demo-box">
            <strong style="display:block; margin-bottom:8px;">Chọn nhanh tài khoản thử nghiệm:</strong>
            <div style="display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px;">
                <button type="button" class="btn-demo" onclick="fillLogin('admin@edumanager.vn', '123456')">Admin</button>
                <button type="button" class="btn-demo" onclick="fillLogin('giangvien@edumanager.vn', '123456')">Giảng viên</button>
                <button type="button" class="btn-demo" onclick="fillLogin('ketoan@edumanager.vn', '123456')">Kế toán</button>
                <button type="button" class="btn-demo" onclick="fillLogin('daotao@edumanager.vn', '123456')">Quản lý ĐT</button>
                <button type="button" class="btn-demo" onclick="fillLogin('hocvien@edumanager.vn', '123456')">Học viên</button>
            </div>
            <span style="font-size: 11px; color: #64748b;">Mật khẩu chung: <code>123456</code> hoặc <code>Admin@123</code></span>
        </div>
    </div>
</div>

<style>
    .btn-demo { background: #e2e8f0; border: none; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; color: #1e293b; cursor: pointer; transition: background 0.15s; }
    .btn-demo:hover { background: #cbd5e1; }
</style>

<script>
    function fillLogin(email, pass) {
        document.getElementById('email').value = email;
        document.getElementById('password').value = pass;
    }
</script>

</body>
</html>
