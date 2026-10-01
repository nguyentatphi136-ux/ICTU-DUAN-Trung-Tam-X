<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập | Hệ thống Quản lý Đào tạo (EMS)</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        body {
            background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .login-card {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3);
            max-width: 480px;
            width: 100%;
            padding: 36px;
        }
        .demo-pill {
            cursor: pointer;
            transition: all 0.2s;
            font-size: 13px;
        }
        .demo-pill:hover {
            transform: translateY(-2px);
            opacity: 0.9;
        }
    </style>
</head>
<body>
    <div class="login-card">
        <div class="text-center mb-4">
            <div class="display-6 text-primary mb-2"><i class="bi bi-mortarboard-fill"></i></div>
            <h3 class="fw-bold">HỆ THỐNG EMS</h3>
            <p class="text-muted small">Cơ chế phân quyền theo vai trò (User Story IDTTX-20)</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger py-2 small" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-1"></i> ${errorMessage}
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="POST" id="loginForm">
            <div class="mb-3">
                <label class="form-label fw-semibold">Email tài khoản</label>
                <input type="email" id="emailInput" name="email" class="form-control" placeholder="name@example.com" required value="instructor@example.com">
            </div>

            <div class="mb-3">
                <label class="form-label fw-semibold">Mật khẩu</label>
                <input type="password" id="passwordInput" name="password" class="form-control" placeholder="••••••••" required value="123456">
            </div>

            <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold">
                <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập hệ thống
            </button>
        </form>

        <hr class="my-4">

        <div>
            <p class="text-muted small fw-bold mb-2">Chọn nhanh tài khoản để kiểm thử phân quyền:</p>
            <div class="d-flex flex-wrap gap-2">
                <span class="badge bg-primary p-2 demo-pill" onclick="selectAccount('instructor@example.com', '123456')">
                    <i class="bi bi-person-workspace me-1"></i> Giảng viên
                </span>
                <span class="badge bg-success p-2 demo-pill" onclick="selectAccount('accountant@example.com', '123456')">
                    <i class="bi bi-cash-coin me-1"></i> Kế toán
                </span>
                <span class="badge bg-dark p-2 demo-pill" onclick="selectAccount('admin@example.com', '123456')">
                    <i class="bi bi-shield-check me-1"></i> Quản trị (Admin)
                </span>
                <span class="badge bg-secondary p-2 demo-pill" onclick="selectAccount('student@example.com', '123456')">
                    <i class="bi bi-person me-1"></i> Học viên
                </span>
            </div>
        </div>
    </div>

    <script>
        function selectAccount(email, password) {
            document.getElementById('emailInput').value = email;
            document.getElementById('passwordInput').value = password;
            document.getElementById('loginForm').submit();
        }
    </script>
</body>
</html>
