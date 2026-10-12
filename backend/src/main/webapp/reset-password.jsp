<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu | EduManager</title>
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
        .alert-danger { background-color: #fef2f2; color: #991b1b; border: 1px solid #fecaca; padding: 12px; border-radius: 8px; font-size: 13px; margin-bottom: 20px; line-height: 1.4; }
        .back-link { display: block; text-align: center; margin-top: 20px; font-size: 13px; color: #64748b; text-decoration: none; }
        .back-link:hover { color: #1c3e70; text-decoration: underline; }
    </style>
</head>
<body>

<div class="box-container">
    <div class="card">
        <h1 class="title">Tạo mật khẩu mới</h1>
        <p class="subtitle">Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và số.</p>

        <%-- Hiển thị thông báo lỗi nếu token không hợp lệ / hết hạn / đã dùng (IDTTX-42) --%>
        <c:if test="${not empty errorMessage}">
            <div class="alert-danger">
                ⚠️ <strong>Không thể đặt lại mật khẩu:</strong><br>
                <c:out value="${errorMessage}" />
            </div>
        </c:if>

        <c:choose>
            <c:when test="${not empty errorMessage && empty token}">
                <a href="${pageContext.request.contextPath}/auth/forgot-password" class="back-link">
                    → Yêu cầu lại liên kết đặt lại mật khẩu mới
                </a>
            </c:when>
            <c:otherwise>
                <!-- Form gửi POST tới ResetPasswordServlet (IDTTX-42) -->
                <form action="${pageContext.request.contextPath}/auth/reset-password" method="POST">
                    <input type="hidden" name="token" value="${token}">

                    <div class="form-group">
                        <label class="form-label" for="password">Mật khẩu mới</label>
                        <input type="password" id="password" name="password" class="form-control" placeholder="Tối thiểu 8 ký tự (chữ và số)" required minlength="8">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="confirmPassword">Xác nhận mật khẩu mới</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" placeholder="Nhập lại mật khẩu mới" required minlength="8">
                    </div>

                    <button type="submit" class="btn-submit">Xác nhận đổi mật khẩu</button>
                </form>
            </c:otherwise>
        </c:choose>

        <a href="${pageContext.request.contextPath}/login.jsp" class="back-link">
            ← Quay lại trang đăng nhập
        </a>
    </div>
</div>

</body>
</html>
