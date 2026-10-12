<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu | EduManager</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        body { background-color: #f8fafc; font-family: system-ui, -apple-system, sans-serif; }
        .card-custom { max-width: 500px; margin: 60px auto; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }
    </style>
</head>
<body>
<div class="container">
    <div class="card card-custom p-4 bg-white border-0">
        <div class="text-center mb-4">
            <div class="d-inline-flex align-items-center justify-content-center bg-primary bg-opacity-10 text-primary rounded-circle mb-3" style="width: 56px; height: 56px;">
                <i class="bi bi-shield-lock fs-3"></i>
            </div>
            <h4 class="fw-bold">Đổi mật khẩu tài khoản</h4>
            <p class="text-muted small">Bảo vệ tài khoản bằng mật khẩu mạnh tối thiểu 8 ký tự, có chữ và số.</p>
        </div>

        <form id="changePasswordForm">
            <div class="mb-3">
                <label for="currentPassword" class="form-label fw-semibold">Mật khẩu hiện tại</label>
                <div class="input-group">
                    <input type="password" class="form-control" id="currentPassword" name="currentPassword" required placeholder="Nhập mật khẩu hiện tại">
                    <button class="btn btn-outline-secondary toggle-pass" type="button" data-target="currentPassword"><i class="bi bi-eye"></i></button>
                </div>
            </div>

            <div class="mb-3">
                <label for="newPassword" class="form-label fw-semibold">Mật khẩu mới</label>
                <div class="input-group">
                    <input type="password" class="form-control" id="newPassword" name="newPassword" required minlength="8" placeholder="Tối thiểu 8 ký tự (chữ + số)">
                    <button class="btn btn-outline-secondary toggle-pass" type="button" data-target="newPassword"><i class="bi bi-eye"></i></button>
                </div>
                <div class="form-text small">Tối thiểu 8 ký tự, phải bao gồm cả chữ cái và chữ số.</div>
            </div>

            <div class="mb-4">
                <label for="confirmPassword" class="form-label fw-semibold">Xác nhận mật khẩu mới</label>
                <div class="input-group">
                    <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" required placeholder="Nhập lại mật khẩu mới">
                    <button class="btn btn-outline-secondary toggle-pass" type="button" data-target="confirmPassword"><i class="bi bi-eye"></i></button>
                </div>
            </div>

            <div id="alertMessage" class="alert d-none py-2 mb-3 small" role="alert"></div>

            <div class="d-grid gap-2">
                <button type="submit" class="btn btn-primary fw-semibold" id="submitBtn">
                    <i class="bi bi-check2-circle me-1"></i> Lưu thay đổi
                </button>
                <a href="${pageContext.request.contextPath}/admin.jsp" class="btn btn-light text-muted">Hủy bỏ</a>
            </div>
        </form>
    </div>
</div>

<script>
    document.querySelectorAll('.toggle-pass').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.target);
            const isPass = input.type === 'password';
            input.type = isPass ? 'text' : 'password';
            btn.innerHTML = `<i class="bi bi-${isPass ? 'eye-slash' : 'eye'}"></i>`;
        });
    });

    document.getElementById('changePasswordForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const currentPassword = document.getElementById('currentPassword').value.trim();
        const newPassword = document.getElementById('newPassword').value.trim();
        const confirmPassword = document.getElementById('confirmPassword').value.trim();
        const alertBox = document.getElementById('alertMessage');

        if (newPassword.length < 8 || !/[a-zA-Z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
            alertBox.className = 'alert alert-danger py-2 mb-3 small';
            alertBox.textContent = 'Mật khẩu mới phải có tối thiểu 8 ký tự và bao gồm cả chữ cái và chữ số.';
            return;
        }

        if (newPassword !== confirmPassword) {
            alertBox.className = 'alert alert-danger py-2 mb-3 small';
            alertBox.textContent = 'Xác nhận mật khẩu mới không trùng khớp.';
            return;
        }

        if (currentPassword === newPassword) {
            alertBox.className = 'alert alert-danger py-2 mb-3 small';
            alertBox.textContent = 'Mật khẩu mới không được trùng với mật khẩu hiện tại.';
            return;
        }

        const submitBtn = document.getElementById('submitBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';

        try {
            const res = await fetch('${pageContext.request.contextPath}/api/auth/change-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ currentPassword, newPassword, confirmPassword })
            });
            const data = await res.json();
            if (res.ok && data.success) {
                alertBox.className = 'alert alert-success py-2 mb-3 small';
                alertBox.textContent = data.message || 'Đổi mật khẩu thành công. Các phiên khác đã được thu hồi.';
                document.getElementById('changePasswordForm').reset();
            } else {
                alertBox.className = 'alert alert-danger py-2 mb-3 small';
                alertBox.textContent = data.error?.message || 'Không thể đổi mật khẩu.';
            }
        } catch (err) {
            alertBox.className = 'alert alert-danger py-2 mb-3 small';
            alertBox.textContent = 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.';
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Lưu thay đổi';
        }
    });
</script>
</body>
</html>
