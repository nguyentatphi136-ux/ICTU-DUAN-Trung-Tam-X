<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Điểm số | Hệ thống Quản lý Đào tạo (EMS)</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body class="bg-light">
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container">
            <a class="navbar-brand fw-bold" href="#">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <div class="d-flex align-items-center text-white">
                <span class="me-3">Xin chào, <strong>${sessionScope.currentUser.name}</strong> 
                    <span class="badge bg-primary ms-1">${sessionScope.currentUser.roles[0]}</span>
                </span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container">
        <!-- Thông báo nghiệp vụ -->
        <div class="card mb-4 border-0 shadow-sm border-start border-4 border-info">
            <div class="card-body">
                <h5 class="card-title fw-bold text-info"><i class="bi bi-shield-lock me-2"></i>Quy định phân quyền nghiệp vụ (User Story IDTTX-20)</h5>
                <p class="card-text mb-0 text-secondary">
                    • <strong>Giảng viên (Instructor):</strong> Được phép xem và sửa điểm số học viên.<br>
                    • <strong>Kế toán (Accountant):</strong> Chỉ có quyền xem điểm, <strong>tuyệt đối không được sửa điểm</strong> (Hệ thống sẽ từ chối và báo lỗi tiếng Việt).
                </p>
            </div>
        </div>

        <c:if test="${not empty successMessage}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <i class="bi bi-check-circle-fill me-2"></i>${successMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <div class="card shadow-sm border-0">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold"><i class="bi bi-journal-text text-primary me-2"></i>Bảng điểm học viên</h5>
                <!-- Nút thử truy cập trang học phí để kiểm tra quyền -->
                <a href="${pageContext.request.contextPath}/tuition/list" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-wallet2 me-1"></i> Sang trang Học phí (Kiểm tra quyền)
                </a>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>#</th>
                                <th>Học viên</th>
                                <th>Môn học</th>
                                <th>Đầu điểm</th>
                                <th>Điểm số</th>
                                <th>Ghi chú</th>
                                <th>Người cập nhật</th>
                                <th class="text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${gradeList}" var="g">
                                <tr>
                                    <td>${g.id}</td>
                                    <td class="fw-semibold">${g.studentName}</td>
                                    <td>${g.subjectName}</td>
                                    <td><span class="badge bg-secondary">${g.componentName}</span></td>
                                    <td><span class="badge bg-success fs-6">${g.score}</span></td>
                                    <td>${g.notes}</td>
                                    <td><small class="text-muted">${g.updatedBy}</small></td>
                                    <td class="text-center">
                                        <!-- Đường dẫn này đi qua AuthorizationFilter: Giảng viên được qua, Kế toán bị chặn 403 -->
                                        <a href="${pageContext.request.contextPath}/grade/edit?id=${g.id}" class="btn btn-warning btn-sm">
                                            <i class="bi bi-pencil-square me-1"></i> Chỉnh sửa điểm
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
