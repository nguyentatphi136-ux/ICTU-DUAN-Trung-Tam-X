<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Khách hàng tiềm năng (Leads) | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        .badge-source {
            font-size: 0.75rem;
            letter-spacing: 0.5px;
        }
        .table-responsive {
            border-radius: 8px;
            overflow: hidden;
        }
    </style>
</head>
<body class="bg-light">
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4 shadow-sm">
        <div class="container">
            <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/lead/list">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#emsNavbar">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="emsNavbar">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <a class="nav-link active fw-semibold" href="${pageContext.request.contextPath}/lead/list">
                            <i class="bi bi-funnel-fill text-warning me-1"></i> Tư vấn Tuyển sinh (Leads)
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/tuition/list">
                            <i class="bi bi-wallet2 me-1"></i> Học phí
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/grade/list">
                            <i class="bi bi-journal-check me-1"></i> Điểm số
                        </a>
                    </li>
                </ul>
                <div class="d-flex align-items-center text-white">
                    <span class="me-3">
                        Xin chào, <strong>${sessionScope.currentUser.fullName != null ? sessionScope.currentUser.fullName : sessionScope.currentUser.name}</strong>
                        <c:if test="${not empty sessionScope.currentUser.roles}">
                            <span class="badge bg-primary ms-1">${sessionScope.currentUser.roles[0]}</span>
                        </c:if>
                    </span>
                    <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                        <i class="bi bi-box-arrow-right"></i> Đăng xuất
                    </a>
                </div>
            </div>
        </div>
    </nav>

    <div class="container pb-5">
        <!-- Banner quy định nghiệp vụ S2-09 -->
        <div class="card mb-4 border-0 shadow-sm border-start border-4 border-primary">
            <div class="card-body">
                <div class="d-flex align-items-start">
                    <i class="bi bi-info-circle-fill text-primary fs-3 me-3"></i>
                    <div>
                        <h5 class="card-title fw-bold text-primary mb-1">
                            Quy chế Quản lý Khách hàng tiềm năng (User Story S2-09 · EP-03)
                        </h5>
                        <div class="text-secondary small">
                            • <strong>Tạo & Sửa lead:</strong> Điền đầy đủ Họ tên, Số điện thoại (kiểm tra chuẩn ĐTDĐ VN), Email, Nguồn và Chương trình quan tâm.<br>
                            • <strong>Cảnh báo trùng số điện thoại:</strong> Khi nhập số điện thoại đã có trong hệ thống, giao diện sẽ cảnh báo rõ ràng để tránh bỏ sót hoặc gọi trùng khách hàng.<br>
                            • <strong>Phân quyền xóa lead:</strong> 
                            <span class="text-danger fw-bold"><i class="bi bi-shield-lock-fill"></i> Chỉ Quản lý đào tạo (Training Manager) và Quản trị hệ thống (Admin) mới có quyền xóa lead.</span> 
                            Tư vấn tuyển sinh (Admissions) không được phép xóa nhằm bảo toàn dữ liệu phễu tuyển sinh.
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Thông báo Thành công / Lỗi -->
        <c:if test="${not empty successMessage}">
            <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-check-circle-fill me-2 fs-5"></i>${successMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>${errorMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Thẻ Danh sách & Bộ lọc -->
        <div class="card shadow-sm border-0">
            <div class="card-header bg-white py-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
                <div>
                    <h5 class="mb-0 fw-bold text-dark">
                        <i class="bi bi-people-fill text-primary me-2"></i>Danh sách Khách hàng tiềm năng
                        <span class="badge bg-secondary ms-2">${totalItems != null ? totalItems : 0} lead</span>
                    </h5>
                    <small class="text-muted">Theo dõi và chăm sóc khách hàng quan tâm đến các khóa đào tạo</small>
                </div>
                <div>
                    <c:if test="${canCreate}">
                        <a href="${pageContext.request.contextPath}/lead/add" class="btn btn-primary">
                            <i class="bi bi-person-plus-fill me-1"></i> Thêm mới Lead
                        </a>
                    </c:if>
                </div>
            </div>

            <!-- Bộ lọc & Tìm kiếm -->
            <div class="card-body bg-light border-bottom">
                <form action="${pageContext.request.contextPath}/lead/list" method="GET" class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label small fw-semibold">Tìm kiếm</label>
                        <div class="input-group">
                            <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                            <input type="text" name="keyword" class="form-control" 
                                   placeholder="Họ tên, SĐT, Email..." value="${keyword}">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Trạng thái</label>
                        <select name="status" class="form-select">
                            <option value="ALL" ${status == 'ALL' || empty status ? 'selected' : ''}>-- Tất cả trạng thái --</option>
                            <option value="NEW" ${status == 'NEW' ? 'selected' : ''}>Mới (NEW)</option>
                            <option value="CONTACTED" ${status == 'CONTACTED' ? 'selected' : ''}>Đã liên hệ (CONTACTED)</option>
                            <option value="CONSULTING" ${status == 'CONSULTING' ? 'selected' : ''}>Đang tư vấn (CONSULTING)</option>
                            <option value="TRIAL" ${status == 'TRIAL' ? 'selected' : ''}>Hẹn học thử (TRIAL)</option>
                            <option value="WON" ${status == 'WON' ? 'selected' : ''}>Chốt thành công (WON)</option>
                            <option value="LOST" ${status == 'LOST' ? 'selected' : ''}>Từ chối / Mất lead (LOST)</option>
                            <option value="ENROLLED" ${status == 'ENROLLED' ? 'selected' : ''}>Đã nhập học (ENROLLED)</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label small fw-semibold">Nguồn khách hàng</label>
                        <select name="source" class="form-select">
                            <option value="ALL" ${source == 'ALL' || empty source ? 'selected' : ''}>-- Tất cả nguồn --</option>
                            <option value="WEBSITE" ${source == 'WEBSITE' ? 'selected' : ''}>Website</option>
                            <option value="FACEBOOK" ${source == 'FACEBOOK' ? 'selected' : ''}>Facebook Fanpage</option>
                            <option value="REFERRAL" ${source == 'REFERRAL' ? 'selected' : ''}>Người quen giới thiệu</option>
                            <option value="DIRECT" ${source == 'DIRECT' ? 'selected' : ''}>Trực tiếp tại văn phòng</option>
                            <option value="ADS" ${source == 'ADS' ? 'selected' : ''}>Quảng cáo trực tuyến</option>
                            <option value="EVENT" ${source == 'EVENT' ? 'selected' : ''}>Hội thảo / Sự kiện</option>
                            <option value="OTHER" ${source == 'OTHER' ? 'selected' : ''}>Khác</option>
                        </select>
                    </div>
                    <div class="col-md-2 d-flex align-items-end gap-2">
                        <button type="submit" class="btn btn-outline-primary w-100">
                            <i class="bi bi-filter"></i> Lọc
                        </button>
                        <a href="${pageContext.request.contextPath}/lead/list" class="btn btn-outline-secondary" title="Đặt lại bộ lọc">
                            <i class="bi bi-arrow-counterclockwise"></i>
                        </a>
                    </div>
                </form>
            </div>

            <!-- Bảng dữ liệu Lead -->
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="text-center" style="width: 60px;">#ID</th>
                                <th>Họ và tên</th>
                                <th>Số điện thoại</th>
                                <th>Email</th>
                                <th>Nguồn</th>
                                <th>Chương trình quan tâm</th>
                                <th>Trạng thái</th>
                                <th>Tư vấn viên</th>
                                <th class="text-center" style="width: 160px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty leadList}">
                                    <c:forEach items="${leadList}" var="lead">
                                        <tr>
                                            <td class="text-center fw-bold text-muted">${lead.id}</td>
                                            <td>
                                                <div class="fw-bold text-dark">${lead.fullName}</div>
                                                <c:if test="${not empty lead.notes}">
                                                    <small class="text-muted text-truncate d-inline-block" style="max-width: 200px;" title="${lead.notes}">
                                                        <i class="bi bi-chat-left-dots me-1"></i>${lead.notes}
                                                    </small>
                                                </c:if>
                                            </td>
                                            <td>
                                                <a href="tel:${lead.phone}" class="fw-semibold text-decoration-none">
                                                    <i class="bi bi-telephone-outbound text-primary me-1"></i>${lead.phone}
                                                </a>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty lead.email}">
                                                        <a href="mailto:${lead.email}" class="text-secondary text-decoration-none small">
                                                            <i class="bi bi-envelope me-1"></i>${lead.email}
                                                        </a>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-muted small">--</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <span class="badge bg-light text-dark border badge-source">
                                                    ${lead.source}
                                                </span>
                                            </td>
                                            <td>
                                                <span class="text-dark small fw-medium">
                                                    ${lead.programInterest != null ? lead.programInterest : 'Chưa xác định'}
                                                </span>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${lead.status == 'NEW'}">
                                                        <span class="badge bg-primary">Mới</span>
                                                    </c:when>
                                                    <c:when test="${lead.status == 'CONTACTED'}">
                                                        <span class="badge bg-info text-dark">Đã liên hệ</span>
                                                    </c:when>
                                                    <c:when test="${lead.status == 'CONSULTING'}">
                                                        <span class="badge bg-warning text-dark">Đang tư vấn</span>
                                                    </c:when>
                                                    <c:when test="${lead.status == 'TRIAL'}">
                                                        <span class="badge bg-purple text-white" style="background-color: #6f42c1;">Hẹn học thử</span>
                                                    </c:when>
                                                    <c:when test="${lead.status == 'WON'}">
                                                        <span class="badge bg-success">Chốt thành công</span>
                                                    </c:when>
                                                    <c:when test="${lead.status == 'ENROLLED'}">
                                                        <span class="badge bg-success">Đã nhập học</span>
                                                    </c:when>
                                                    <c:when test="${lead.status == 'LOST'}">
                                                        <span class="badge bg-secondary">Từ chối / Mất lead</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-light text-dark border">${lead.status}</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <small class="text-muted">
                                                    ${lead.assignedToName != null ? lead.assignedToName : 'Chưa phân công'}
                                                </small>
                                            </td>
                                            <td class="text-center">
                                                <div class="btn-group btn-group-sm">
                                                    <!-- Nút Sửa: Admissions, TrainingManager, Admin đều có quyền -->
                                                    <c:if test="${canUpdate}">
                                                        <a href="${pageContext.request.contextPath}/lead/edit?id=${lead.id}" 
                                                           class="btn btn-outline-primary" title="Chỉnh sửa thông tin">
                                                            <i class="bi bi-pencil-square"></i> Sửa
                                                        </a>
                                                    </c:if>

                                                    <!-- Nút Xoá: CHỈ Quản lý đào tạo & Admin được xóa -->
                                                    <c:choose>
                                                        <c:when test="${canDelete}">
                                                            <button type="button" class="btn btn-outline-danger" 
                                                                    onclick="confirmDeleteLead(${lead.id}, '${lead.fullName}')" 
                                                                    title="Xóa khách hàng tiềm năng">
                                                                <i class="bi bi-trash"></i>
                                                            </button>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <!-- Hiển thị bị khoá quyền cho Tư vấn tuyển sinh -->
                                                            <button type="button" class="btn btn-outline-secondary disabled" 
                                                                    data-bs-toggle="tooltip" data-bs-placement="top"
                                                                    title="Chỉ Quản lý đào tạo mới được phép xoá lead">
                                                                <i class="bi bi-lock-fill"></i>
                                                            </button>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="9" class="text-center py-5 text-muted">
                                            <i class="bi bi-inbox fs-1 d-block mb-2 text-secondary"></i>
                                            Chưa có khách hàng tiềm năng nào phù hợp với điều kiện tìm kiếm.
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Phân trang -->
            <c:if test="${totalPages > 1}">
                <div class="card-footer bg-white py-3 d-flex justify-content-between align-items-center">
                    <span class="text-muted small">
                        Hiển thị trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng <strong>${totalItems}</strong> khách hàng)
                    </span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                <a class="page-link" href="${pageContext.request.contextPath}/lead/list?page=${currentPage - 1}&keyword=${keyword}&status=${status}&source=${source}">Trước</a>
                            </li>
                            <c:forEach begin="1" end="${totalPages}" var="p">
                                <li class="page-item ${currentPage == p ? 'active' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/lead/list?page=${p}&keyword=${keyword}&status=${status}&source=${source}">${p}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                <a class="page-link" href="${pageContext.request.contextPath}/lead/list?page=${currentPage + 1}&keyword=${keyword}&status=${status}&source=${source}">Sau</a>
                            </li>
                        </ul>
                    </nav>
                </div>
            </c:if>
        </div>
    </div>

    <!-- Modal Xác nhận Xóa Lead -->
    <div class="modal fade" id="deleteModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title"><i class="bi bi-exclamation-triangle-fill me-2"></i>Xác nhận xóa Lead</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <p class="mb-2">Bạn có chắc chắn muốn xóa khách hàng tiềm năng này không?</p>
                    <div class="p-3 bg-light rounded border mb-3">
                        <strong id="deleteLeadName" class="text-danger d-block"></strong>
                        <small class="text-muted">Mã lead: #<span id="deleteLeadId"></span></small>
                    </div>
                    <small class="text-muted">
                        <i class="bi bi-info-circle me-1"></i>Hành động này được ghi nhận vào nhật ký kiểm toán hệ thống.
                    </small>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy bỏ</button>
                    <form id="deleteForm" method="POST" action="">
                        <button type="submit" class="btn btn-danger">
                            <i class="bi bi-trash-fill me-1"></i> Đồng ý Xóa
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Kích hoạt Bootstrap tooltip
        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));

        function confirmDeleteLead(id, name) {
            document.getElementById('deleteLeadId').textContent = id;
            document.getElementById('deleteLeadName').textContent = name;
            document.getElementById('deleteForm').action = '${pageContext.request.contextPath}/lead/delete?id=' + id;
            new bootstrap.Modal(document.getElementById('deleteModal')).show();
        }
    </script>
</body>
</html>
