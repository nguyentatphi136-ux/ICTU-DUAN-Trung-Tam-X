<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu | EduManager</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f7f9fc; margin: 0; padding: 0; }
        .box-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
        .card { background: #ffffff; width: 100%; max-width: 440px; padding: 36px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.06); }
        .title { font-size: 24px; font-weight: 800; color: #1c3e70; margin-bottom: 8px; }
        .subtitle { font-size: 13px; color: #64748b; margin-bottom: 24px; line-height: 1.5; }
        .form-group { margin-bottom: 18px; }
        .form-label { display: block; font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 6px; }
        .form-control { width: 100%; box-sizing: border-box; padding: 12px 14px; border: 1.5px solid #e2e8f0; border-radius: 10px; font-size: 14px; }
        .form-control:focus { outline: none; border-color: #0284c7; }
        .btn-submit { width: 100%; padding: 13px; background: #1c3e70; color: #fff; border: none; border-radius: 10px; font-size: 15px; font-weight: 700; cursor: pointer; }
        .btn-submit:hover { background: #152f55; }
        .alert-success { background-color: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; padding: 12px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; }
        .alert-danger { background-color: #fef2f2; color: #991b1b; border: 1px solid #fecaca; padding: 12px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; }
        .back-link { display: block; text-align: center; margin-top: 20px; font-size: 13px; color: #64748b; text-decoration: none; }
        .back-link:hover { color: #1c3e70; text-decoration: underline; }
    </style>
</head>
<body>

<div class="box-container">
    <div class="card">
        <h1 class="title">Quên mật khẩu?</h1>
        <p class="subtitle">Nhập địa chỉ email của bạn để nhận liên kết đặt lại mật khẩu an toàn (hiệu lực trong 30 phút).</p>

        <c:if test="${not empty infoMessage}">
            <div class="alert-success">
                📩 <c:out value="${infoMessage}" />
            </div>
        </c:if>

        <c:if test="${not empty errorMessage}">
            <div class="alert-danger">
                ❌ <c:out value="${errorMessage}" />
            </div>
        </c:if>

        <!-- Form gửi POST tới ForgotPasswordServlet (IDTTX-41) -->
        <form action="${pageContext.request.contextPath}/auth/forgot-password" method="POST">
            <div class="form-group">
                <label class="form-label" for="email">Địa chỉ Email</label>
                <input type="email" id="email" name="email" class="form-control" placeholder="example@edumanager.vn" required autofocus>
            </div>

            <button type="submit" class="btn-submit">Gửi liên kết đặt lại mật khẩu</button>
        </form>

        <a href="${pageContext.request.contextPath}/login.jsp" class="back-link">
            ← Quay lại trang đăng nhập
        </a>
    </div>
</div>

</body>
</html>
