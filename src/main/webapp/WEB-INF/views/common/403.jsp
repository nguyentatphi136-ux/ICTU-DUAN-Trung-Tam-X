<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>403 - Từ chối truy cập | Hệ thống Quản lý Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        body {
            background-color: #f8fafc;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .error-card {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
            max-width: 580px;
            width: 100%;
            padding: 40px;
            text-align: center;
            border-top: 6px solid #ef4444;
        }
        .icon-circle {
            width: 80px;
            height: 80px;
            background: #fee2e2;
            color: #ef4444;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 40px;
            margin: 0 auto 24px;
        }
        .error-title {
            font-size: 24px;
            font-weight: 700;
            color: #1e293b;
            margin-bottom: 12px;
        }
        .error-message {
            font-size: 16px;
            color: #475569;
            line-height: 1.6;
            margin-bottom: 24px;
            padding: 16px;
            background-color: #f1f5f9;
            border-radius: 8px;
            border-left: 4px solid #f59e0b;
        }
        .user-info {
            font-size: 14px;
            color: #64748b;
            margin-bottom: 28px;
        }
        .btn-action {
            padding: 10px 24px;
            font-weight: 600;
            border-radius: 8px;
        }
    </style>
</head>
<body>
    <div class="error-card">
        <div class="icon-circle">
            <i class="bi bi-shield-x"></i>
        </div>
        <h1 class="error-title">Truy cập bị từ chối (403 Forbidden)</h1>
        
        <div class="error-message">
            <c:choose>
                <c:when test="${not empty errorMessage}">
                    <strong><i class="bi bi-exclamation-triangle-fill text-warning me-2"></i> ${errorMessage}</strong>
                </c:when>
                <c:otherwise>
                    <strong><i class="bi bi-exclamation-triangle-fill text-warning me-2"></i> Bạn không có quyền hạn truy cập hoặc chỉnh sửa chức năng này trên hệ thống.</strong>
                </c:otherwise>
            </c:choose>
        </div>

        <c:if test="${not empty sessionScope.currentUser}">
            <div class="user-info">
                Tài khoản đang đăng nhập: <strong>${sessionScope.currentUser.name}</strong> (${sessionScope.currentUser.email})<br>
                Vai trò của bạn: 
                <c:forEach items="${sessionScope.currentUser.roles}" var="role">
                    <span class="badge bg-secondary">${role}</span>
                </c:forEach>
            </div>
        </c:if>

        <div class="d-flex justify-content-center gap-3">
            <a href="javascript:history.back()" class="btn btn-outline-secondary btn-action">
                <i class="bi bi-arrow-left me-1"></i> Quay lại
            </a>
            <c:choose>
                <c:when test="${sessionScope.currentUser.hasRole('Instructor')}">
                    <a href="${pageContext.request.contextPath}/grade/list" class="btn btn-primary btn-action">
                        <i class="bi bi-journal-check me-1"></i> Về Quản lý điểm số
                    </a>
                </c:when>
                <c:when test="${sessionScope.currentUser.hasRole('Accountant')}">
                    <a href="${pageContext.request.contextPath}/tuition/list" class="btn btn-primary btn-action">
                        <i class="bi bi-wallet2 me-1"></i> Về Quản lý học phí
                    </a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/login" class="btn btn-primary btn-action">
                        <i class="bi bi-house-door me-1"></i> Trang chủ
                    </a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>
