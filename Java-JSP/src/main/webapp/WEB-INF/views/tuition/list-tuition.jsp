<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản lý Học phí | Hệ thống Quản lý Đào tạo (EMS)</title>
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
                    <span class="badge bg-success ms-1">${sessionScope.currentUser.roles[0]}</span>
                </span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container">
        <!-- Thông báo nghiệp vụ -->
        <div class="card mb-4 border-0 shadow-sm border-start border-4 border-warning">
            <div class="card-body">
                <h5 class="card-title fw-bold text-warning"><i class="bi bi-shield-lock me-2"></i>Quy định phân quyền học phí (User Story IDTTX-20)</h5>
                <p class="card-text mb-0 text-secondary">
                    • <strong>Kế toán (Accountant):</strong> Được phép xem và chỉnh sửa, ghi nhận học phí học viên.<br>
                    • <strong>Giảng viên (Instructor):</strong> <strong>Tuyệt đối không được sửa học phí</strong> (Hệ thống sẽ từ chối và báo lỗi tiếng Việt).
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
                <h5 class="mb-0 fw-bold"><i class="bi bi-wallet2 text-success me-2"></i>Danh sách theo dõi học phí</h5>
                <a href="${pageContext.request.contextPath}/grade/list" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-journal-text me-1"></i> Sang trang Điểm số (Kiểm tra quyền)
                </a>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>#</th>
                                <th>Học viên</th>
                                <th>Khóa học</th>
                                <th>Tổng học phí</th>
                                <th>Đã thanh toán</th>
                                <th>Trạng thái</th>
                                <th>Biên lai</th>
                                <th>Kế toán phụ trách</th>
                                <th class="text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach items="${tuitionList}" var="t">
                                <tr>
                                    <td>${t.id}</td>
                                    <td class="fw-semibold">${t.studentName}</td>
                                    <td>${t.courseName}</td>
                                    <td class="text-primary fw-bold">${t.totalAmount} VNĐ</td>
                                    <td class="text-success fw-bold">${t.paidAmount} VNĐ</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${t.status == 'paid'}">
                                                <span class="badge bg-success">Đã hoàn thành</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-warning text-dark">Còn nợ học phí</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><code>${t.receiptNo}</code></td>
                                    <td><small class="text-muted">${t.updatedBy}</small></td>
                                    <td class="text-center">
                                        <!-- Đường dẫn này đi qua AuthorizationFilter: Kế toán được qua, Giảng viên bị chặn 403 -->
                                        <a href="${pageContext.request.contextPath}/tuition/edit?id=${t.id}" class="btn btn-success btn-sm">
                                            <i class="bi bi-cash-stack me-1"></i> Thu / Sửa học phí
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
