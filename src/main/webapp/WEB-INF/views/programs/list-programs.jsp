<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục Chương trình đào tạo | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f8fafc; }
        .program-card { transition: all 0.2s; border: none; border-radius: 12px; }
        .program-card:hover { transform: translateY(-3px); box-shadow: 0 10px 20px rgba(0,0,0,0.08); }
    </style>
</head>
<body class="bg-light">

    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container">
            <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/dashboard.jsp">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <div class="navbar-nav me-auto">
                <a class="nav-link" href="${pageContext.request.contextPath}/dashboard.jsp">Trang tổng quan</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/grade/list">Điểm số</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/tuition/list">Học phí</a>
                <a class="nav-link active" href="${pageContext.request.contextPath}/training-programs">Chương trình học</a>
                <c:if test="${sessionScope.currentUser.hasRole('Admin')}">
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/users/roles">Phân quyền vai trò</a>
                </c:if>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3">Xin chào, <strong>${sessionScope.currentUser.name}</strong></span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container pb-5">
        <!-- Header -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <span class="badge bg-primary mb-2">User Story: S2-04</span>
                <h3 class="fw-bold text-dark mb-1"><i class="bi bi-book-fill text-primary me-2"></i>Danh mục Chương trình Đào tạo</h3>
                <p class="text-secondary small mb-0">Quản lý và theo dõi thông tin các khóa học, số buổi học và học phí tiêu chuẩn.</p>
            </div>
        </div>

        <!-- Bộ lọc & Tìm kiếm -->
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/training-programs" method="GET" class="row g-3">
                    <div class="col-md-6">
                        <div class="input-group">
                            <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                            <input type="text" name="keyword" class="form-control" placeholder="Tìm theo mã hoặc tên chương trình..." value="<c:out value='${keyword}'/>">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <select name="status" class="form-select">
                            <option value="ALL">-- Tất cả trạng thái --</option>
                            <option value="ACTIVE" ${status == 'ACTIVE' ? 'selected' : ''}>Đang áp dụng (Active)</option>
                            <option value="INACTIVE" ${status == 'INACTIVE' ? 'selected' : ''}>Ngừng áp dụng (Inactive)</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-primary w-100"><i class="bi bi-funnel-fill me-1"></i>Lọc</button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Bảng danh sách chương trình -->
        <div class="card border-0 shadow-sm">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold">Danh sách chương trình (${programs.size()})</h5>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 60px;">ID</th>
                                <th>Mã chương trình</th>
                                <th>Tên chương trình đào tạo</th>
                                <th>Thời lượng</th>
                                <th>Học phí chuẩn</th>
                                <th>Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="p" items="${programs}">
                                <tr>
                                    <td class="text-muted fw-bold">#${p.id}</td>
                                    <td><span class="badge bg-secondary font-monospace">${p.code}</span></td>
                                    <td>
                                        <div class="fw-bold text-dark">${p.name}</div>
                                        <small class="text-muted">${p.description}</small>
                                    </td>
                                    <td>
                                        <i class="bi bi-clock me-1 text-secondary"></i>${p.duration} buổi
                                    </td>
                                    <td class="fw-bold text-primary">
                                        <fmt:formatNumber value="${p.standardTuition}" type="number" groupingUsed="true"/> đ
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${p.status == 'ACTIVE'}">
                                                <span class="badge bg-success"><i class="bi bi-check-circle-fill me-1"></i>Đang áp dụng</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-warning text-dark"><i class="bi bi-dash-circle-fill me-1"></i>Ngừng áp dụng</span>
                                            </c:otherwise>
                                        </c:choose>
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
