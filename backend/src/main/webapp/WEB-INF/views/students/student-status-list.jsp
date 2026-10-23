<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Tình trạng Học vụ (S4-06) | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f8fafc; }
        .stat-card { border: none; border-radius: 14px; background: #ffffff; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.04); transition: transform 0.2s; }
        .stat-card:hover { transform: translateY(-3px); }
        .stat-num { font-size: 28px; font-weight: 800; }
        .table-card { border: none; border-radius: 14px; background: #ffffff; box-shadow: 0 4px 15px rgba(0,0,0,0.04); }
        .badge-status { font-size: 12px; padding: 5px 12px; border-radius: 20px; font-weight: 600; }
        .status-studying { background: #dcfce7; color: #15803d; }
        .status-suspended { background: #fef3c7; color: #b45309; }
        .status-dropped { background: #fee2e2; color: #b91c1c; }
        .status-graduated { background: #ede9fe; color: #6d28d9; }
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
                <a class="nav-link active" href="${pageContext.request.contextPath}/student/status">Tình trạng học vụ (S4-06)</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/student/roster">Điểm danh theo ngày</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/training-programs">Chương trình học</a>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3">Xin chào, <strong>${currentUser.name}</strong></span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container pb-5">

        <!-- Tiêu đề & Giới thiệu nhiệm vụ -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <span class="badge bg-primary mb-2">User Story: S4-06 (IDTTX-252)</span>
                <h3 class="fw-bold text-dark mb-1">
                    <i class="bi bi-person-gear text-primary me-2"></i>Quản lý Tình trạng Học vụ Học viên
                </h3>
                <p class="text-secondary small mb-0">
                    Xử lý nghiệp vụ Bảo lưu, Chuyển lớp, Thôi học; bảo đảm danh sách điểm danh luôn phản ánh chính xác từ ngày hiệu lực.
                </p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/student/roster" class="btn btn-outline-primary">
                    <i class="bi bi-calendar-check me-1"></i> Kiểm tra Điểm danh theo ngày
                </a>
            </div>
        </div>

        <!-- Thống kê nhanh 4 chỉ số -->
        <div class="row g-3 mb-4">
            <div class="col-md-3 col-sm-6">
                <div class="stat-card">
                    <div class="text-secondary small fw-bold text-uppercase">Tổng học viên</div>
                    <div class="stat-num text-dark mt-1">${totalCount}</div>
                    <div class="text-muted small mt-1"><i class="bi bi-people me-1"></i>Hồ sơ được theo dõi</div>
                </div>
            </div>
            <div class="col-md-3 col-sm-6">
                <div class="stat-card">
                    <div class="text-secondary small fw-bold text-uppercase text-success">Đang học</div>
                    <div class="stat-num text-success mt-1">${studyingCount}</div>
                    <div class="text-muted small mt-1"><i class="bi bi-check-circle me-1"></i>Tham gia lớp đầy đủ</div>
                </div>
            </div>
            <div class="col-md-3 col-sm-6">
                <div class="stat-card">
                    <div class="text-secondary small fw-bold text-uppercase text-warning">Bảo lưu</div>
                    <div class="stat-num text-warning mt-1">${suspendedCount}</div>
                    <div class="text-muted small mt-1"><i class="bi bi-pause-circle me-1"></i>Rời danh sách điểm danh</div>
                </div>
            </div>
            <div class="col-md-3 col-sm-6">
                <div class="stat-card">
                    <div class="text-secondary small fw-bold text-uppercase text-danger">Thôi học</div>
                    <div class="stat-num text-danger mt-1">${droppedCount}</div>
                    <div class="text-muted small mt-1"><i class="bi bi-x-circle me-1"></i>Lưu trữ lịch sử học tập</div>
                </div>
            </div>
        </div>

        <!-- Bộ lọc & Tìm kiếm -->
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/student/status" method="GET" class="row g-3">
                    <div class="col-md-5">
                        <div class="input-group">
                            <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                            <input type="text" name="search" class="form-control" placeholder="Tìm theo mã học viên, tên hoặc số điện thoại..." value="<c:out value='${search}'/>">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <select name="status" class="form-select">
                            <option value="ALL">-- Tất cả trạng thái --</option>
                            <option value="Đang học" ${status == 'Đang học' ? 'selected' : ''}>Đang học</option>
                            <option value="Bảo lưu" ${status == 'Bảo lưu' ? 'selected' : ''}>Bảo lưu</option>
                            <option value="Thôi học" ${status == 'Thôi học' ? 'selected' : ''}>Thôi học</option>
                            <option value="Mới nhập học" ${status == 'Mới nhập học' ? 'selected' : ''}>Mới nhập học</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <select name="classCode" class="form-select">
                            <option value="ALL">-- Tất cả lớp --</option>
                            <c:forEach var="c" items="${allClasses}">
                                <option value="${c.code}" ${classCode == c.code ? 'selected' : ''}>${c.code}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-primary w-100"><i class="bi bi-funnel me-1"></i>Lọc</button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Danh sách học viên -->
        <div class="table-card p-0 mb-4">
            <div class="p-3 border-bottom d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold">Danh sách học viên (${students.size()})</h5>
                <span class="text-muted small">Bấm vào học viên để thực hiện Bảo lưu / Chuyển lớp / Thôi học</span>
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light small">
                        <tr>
                            <th>Mã HV</th>
                            <th>Họ và tên</th>
                            <th>Lớp đang học</th>
                            <th>Chương trình đào tạo</th>
                            <th>Số điện thoại</th>
                            <th>Trạng thái</th>
                            <th class="text-end">Hành động</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty students}">
                                <tr>
                                    <td colspan="7" class="text-center py-4 text-muted">
                                        Không tìm thấy học viên nào phù hợp với bộ lọc.
                                    </td>
                                </tr>
                            </c:choose>
                            <c:otherwise>
                                <c:forEach var="s" items="${students}">
                                    <tr>
                                        <td class="fw-bold text-primary">${s.code}</td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/student/profile?code=${s.code}" class="fw-bold text-dark text-decoration-none">
                                                ${s.name}
                                            </a>
                                            <div class="text-muted small">${s.email}</div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty s.currentClass}">
                                                    <span class="badge bg-light text-dark border fw-bold">${s.currentClass}</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted small">-- Chưa xếp lớp --</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="small">${s.program}</td>
                                        <td class="small">${s.phone}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${s.status == 'Đang học'}">
                                                    <span class="badge-status status-studying">Đang học</span>
                                                </c:when>
                                                <c:when test="${s.status == 'Bảo lưu'}">
                                                    <span class="badge-status status-suspended">Bảo lưu</span>
                                                </c:when>
                                                <c:when test="${s.status == 'Thôi học'}">
                                                    <span class="badge-status status-dropped">Thôi học</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge-status bg-secondary text-white">${s.status}</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-end">
                                            <a href="${pageContext.request.contextPath}/student/profile?code=${s.code}" class="btn btn-outline-primary btn-sm fw-semibold">
                                                <i class="bi bi-pencil-square me-1"></i>Hồ sơ & Đổi tình trạng
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Bảng nhật ký biến động học vụ gần đây (Toàn trung tâm) -->
        <div class="table-card p-0">
            <div class="p-3 border-bottom d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold"><i class="bi bi-clock-history text-primary me-2"></i>Nhật ký thay đổi tình trạng học gần đây</h5>
                <span class="badge bg-light text-secondary border">Audit Trail S4-06</span>
            </div>
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0 small">
                    <thead class="table-light">
                        <tr>
                            <th>Thời điểm</th>
                            <th>Mã HV</th>
                            <th>Họ và tên</th>
                            <th>Loại thao tác</th>
                            <th>Ngày hiệu lực</th>
                            <th>Biến động lớp</th>
                            <th>Lý do</th>
                            <th>Người thực hiện</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty recentLogs}">
                                <tr>
                                    <td colspan="8" class="text-center py-4 text-muted">Chưa có bản ghi thay đổi tình trạng nào.</td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="log" items="${recentLogs}">
                                    <tr>
                                        <td class="text-muted">${log.recordedDate}</td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/student/profile?code=${log.studentCode}" class="fw-bold text-decoration-none">
                                                ${log.studentCode}
                                            </a>
                                        </td>
                                        <td class="fw-semibold">${log.studentName}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${log.actionType == 'Bảo lưu'}">
                                                    <span class="badge bg-warning text-dark">Bảo lưu</span>
                                                </c:when>
                                                <c:when test="${log.actionType == 'Chuyển lớp'}">
                                                    <span class="badge bg-primary">Chuyển lớp</span>
                                                </c:when>
                                                <c:when test="${log.actionType == 'Thôi học'}">
                                                    <span class="badge bg-danger">Thôi học</span>
                                                </c:when>
                                            </c:choose>
                                        </td>
                                        <td class="fw-bold">${log.effectiveDate}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${log.actionType == 'Chuyển lớp'}">
                                                    ${log.fromClass} &rarr; <strong class="text-success">${log.toClass}</strong>
                                                </c:when>
                                                <c:otherwise>
                                                    Rời lớp: ${log.fromClass}
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="max-width: 250px;">
                                            <div class="text-truncate" title="${log.reason}">
                                                ${log.reason}
                                            </div>
                                        </td>
                                        <td class="text-secondary">
                                            <i class="bi bi-person me-1"></i>${log.actorName}
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>

    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
