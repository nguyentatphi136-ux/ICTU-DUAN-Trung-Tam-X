<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Không tìm thấy trang | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        body { background-color: #f8fafc; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
        .error-card { background: #fff; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.06); max-width: 520px; width: 100%; padding: 40px; text-align: center; }
        .icon-box { font-size: 56px; color: #f59e0b; margin-bottom: 16px; }
    </style>
</head>
<body>
    <div class="error-card">
        <div class="icon-box"><i class="bi bi-compass"></i></div>
        <h2 class="fw-bold mb-2">404 - Không tìm thấy trang</h2>
        <p class="text-secondary mb-4">Đường dẫn bạn yêu cầu không tồn tại hoặc đã được chuyển sang địa chỉ khác trong hệ thống EMS.</p>
        <div class="d-flex justify-content-center gap-2">
            <a href="${pageContext.request.contextPath}/dashboard.jsp" class="btn btn-primary px-4">
                <i class="bi bi-house-door-fill me-1"></i> Trang chủ
            </a>
            <a href="javascript:history.back()" class="btn btn-outline-secondary px-4">
                <i class="bi bi-arrow-left me-1"></i> Quay lại
            </a>
        </div>
    </div>
</body>
</html>
