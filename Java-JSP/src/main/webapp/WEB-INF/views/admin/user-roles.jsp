<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Phân quyền & Vai trò | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        .role-badge { font-size: 0.85rem; padding: 0.35em 0.65em; }
        .badge-Admin { background-color: #dc3545; }
        .badge-TrainingManager { background-color: #0d6efd; }
        .badge-Instructor { background-color: #198754; }
        .badge-TeachingAssistant { background-color: #20c997; }
        .badge-Admissions { background-color: #fd7e14; }
        .badge-Accountant { background-color: #6f42c1; }
        .badge-Student { background-color: #0dcaf0; color: #000; }
        .badge-Guest { background-color: #6c757d; }
    </style>
</head>
<body class="bg-light">
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container">
            <a class="navbar-brand fw-bold" href="#">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <div class="navbar-nav me-auto">
                <a class="nav-link" href="${pageContext.request.contextPath}/grade/list">Điểm số</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/tuition/list">Học phí</a>
                <a class="nav-link active" href="${pageContext.request.contextPath}/admin/users/roles">Phân quyền vai trò</a>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3">Xin chào, <strong>${sessionScope.currentUser.name}</strong> 
                    <span class="badge bg-danger ms-1">Admin</span>
                </span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container pb-5">
        <!-- Banner Tác vụ Jira IDTTX-24 -->
        <div class="card mb-4 border-0 shadow-sm border-start border-4 border-primary">
            <div class="card-body">
                <div class="d-flex align-items-center mb-2">
                    <span class="badge bg-primary me-2">Jira: IDTTX-24</span>
                    <h5 class="card-title fw-bold mb-0 text-primary">Quản lý Phân bổ & Thu hồi Vai trò (Role-based Authorization)</h5>
                </div>
                <p class="card-text text-secondary mb-1">
                    <strong>Đặc tả nghiệp vụ:</strong> Là Quản trị hệ thống, tôi muốn gán và thu hồi vai trò của một người dùng, để xử lý được trường hợp một người vừa là giảng viên vừa là quản lý đào tạo.
                </p>
                <ul class="mb-0 text-muted small">
                    <li>Một người dùng có thể nắm giữ đồng thời nhiều vai trò.</li>
                    <li>Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại.</li>
                    <li>Quản trị viên <strong>tuyệt đối không thể tự thu hồi vai trò Admin của chính mình</strong> (tránh mất quyền quản trị hệ thống).</li>
                </ul>
            </div>
        </div>

        <!-- Thông báo kết quả -->
        <c:if test="${not empty successMessage}">
            <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-check-circle-fill me-2"></i>${successMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>${errorMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Bảng danh sách người dùng và vai trò -->
        <div class="card shadow-sm border-0">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold"><i class="bi bi-people-fill text-primary me-2"></i>Danh sách Người dùng & Vai trò</h5>
                <span class="badge bg-secondary">Tổng số: ${users.size()} tài khoản</span>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 50px;">ID</th>
                                <th>Họ và tên</th>
                                <th>Email</th>
                                <th>Trạng thái</th>
                                <th style="min-width: 280px;">Vai trò hiện tại</th>
                                <th style="min-width: 250px;">Gán thêm vai trò</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="u" items="${users}">
                                <tr>
                                    <td class="text-muted fw-bold">#${u.id}</td>
                                    <td>
                                        <div class="fw-bold">${u.name}</div>
                                        <c:if test="${u.id == sessionScope.currentUser.id}">
                                            <span class="badge bg-light text-dark border"><i class="bi bi-person-check me-1"></i>Tài khoản của bạn</span>
                                        </c:if>
                                    </td>
                                    <td><code>${u.email}</code></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${u.status == 'active'}">
                                                <span class="badge bg-success">Hoạt động</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-danger">${u.status}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <div class="d-flex flex-wrap gap-1 align-items-center">
                                            <c:forEach var="r" items="${u.roles}">
                                                <span class="badge role-badge badge-${r} d-inline-flex align-items-center">
                                                    ${r}
                                                    <!-- Nút thu hồi vai trò -->
                                                    <c:choose>
                                                        <c:when test="${u.id == sessionScope.currentUser.id && r == 'Admin'}">
                                                            <button class="btn btn-link btn-sm text-white p-0 ms-2 text-decoration-none" 
                                                                    disabled title="Không thể tự thu hồi vai trò Admin của chính mình">
                                                                <i class="bi bi-lock-fill"></i>
                                                            </button>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <form action="${pageContext.request.contextPath}/admin/users/roles" method="POST" class="d-inline ms-1"
                                                                  onsubmit="return confirm('Bạn có chắc chắn muốn thu hồi vai trò ${r} của ${u.name}?');">
                                                                <input type="hidden" name="action" value="revoke">
                                                                <input type="hidden" name="userId" value="${u.id}">
                                                                <input type="hidden" name="role" value="${r}">
                                                                <button type="submit" class="btn btn-link btn-sm text-white p-0 ms-1 text-decoration-none" title="Thu hồi vai trò ${r}">
                                                                    <i class="bi bi-x-circle-fill"></i>
                                                                </button>
                                                            </form>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </c:forEach>
                                        </div>
                                    </td>
                                    <td>
                                        <!-- Form gán thêm vai trò -->
                                        <form action="${pageContext.request.contextPath}/admin/users/roles" method="POST" class="d-flex gap-2">
                                            <input type="hidden" name="action" value="assign">
                                            <input type="hidden" name="userId" value="${u.id}">
                                            <select name="role" class="form-select form-select-sm" required>
                                                <option value="" disabled selected>-- Chọn vai trò --</option>
                                                <c:forEach var="availRole" items="${assignableRoles}">
                                                    <c:if test="${!u.hasRole(availRole)}">
                                                        <option value="${availRole}">+ ${availRole}</option>
                                                    </c:if>
                                                </c:forEach>
                                            </select>
                                            <button type="submit" class="btn btn-primary btn-sm text-nowrap">
                                                <i class="bi bi-plus-lg me-1"></i>Gán
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
