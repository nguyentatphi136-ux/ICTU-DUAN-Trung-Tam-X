<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhập người dùng hàng loạt từ Excel | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        .dropzone-box {
            border: 2px dashed #0d6efd;
            border-radius: 12px;
            background-color: #f8faff;
            padding: 40px 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.2s ease-in-out;
        }
        .dropzone-box:hover {
            background-color: #eef4ff;
            border-color: #0b5ed7;
        }
        .table-preview th { background-color: #f1f5f9; font-size: 0.85rem; text-transform: uppercase; }
        .row-valid { background-color: #f0fdf4 !important; }
        .row-error { background-color: #fef2f2 !important; }
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
                <a class="nav-link" href="${pageContext.request.contextPath}/training-programs">Chương trình học</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/admin/users/roles">Phân quyền vai trò</a>
                <a class="nav-link active" href="${pageContext.request.contextPath}/admin/user-import">Nhập Excel</a>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3">Xin chào, <strong>${sessionScope.currentUser != null ? sessionScope.currentUser.name : 'Quản trị viên'}</strong> 
                    <span class="badge bg-danger ms-1">Admin</span>
                </span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container pb-5">
        <!-- Banner Tiêu chí Story S2-01 (5 SP) -->
        <div class="card mb-4 border-0 shadow-sm border-start border-4 border-primary">
            <div class="card-body">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <div class="d-flex align-items-center">
                        <span class="badge bg-primary me-2">Story: S2-01 (5 SP)</span>
                        <h5 class="card-title fw-bold mb-0 text-primary">Nhập danh sách người dùng hàng loạt từ tệp Excel</h5>
                    </div>
                    <a href="${pageContext.request.contextPath}/admin/user-import?action=template" class="btn btn-outline-success btn-sm fw-semibold">
                        <i class="bi bi-download me-1"></i> Tải tệp mẫu chuẩn (.CSV / Excel)
                    </a>
                </div>
                <p class="card-text text-secondary mb-2">
                    <strong>Đặc tả nghiệp vụ:</strong> Là Quản trị hệ thống, tôi muốn nhập danh sách người dùng hàng loạt từ tệp Excel, để tạo tài khoản cho cả một khoá học viên mới trong vài phút thay vì gõ tay từng người.
                </p>
                <div class="row g-2 text-muted small">
                    <div class="col-md-4"><i class="bi bi-check-circle-fill text-success me-1"></i> <strong>AC1:</strong> Tải được tệp mẫu có sẵn cấu trúc cột chuẩn.</div>
                    <div class="col-md-4"><i class="bi bi-check-circle-fill text-success me-1"></i> <strong>AC2:</strong> Xem trước và báo lỗi theo từng dòng trước khi nhập.</div>
                    <div class="col-md-4"><i class="bi bi-check-circle-fill text-success me-1"></i> <strong>AC3:</strong> Dòng lỗi bị bỏ qua, dòng hợp lệ vẫn được nhập, có báo cáo tổng kết.</div>
                </div>
            </div>
        </div>

        <!-- Thông báo Toast / Alert -->
        <c:if test="${not empty message}">
            <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-check-circle-fill me-2"></i> ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i> ${errorMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        </c:if>

        <!-- KẾT QUẢ BÁO CÁO TỔNG KẾT (AC3) -->
        <c:if test="${not empty summary}">
            <div class="card mb-4 border-0 shadow-sm border-start border-4 border-success">
                <div class="card-header bg-white py-3">
                    <div class="d-flex align-items-center justify-content-between">
                        <h6 class="mb-0 fw-bold text-success">
                            <i class="bi bi-file-earmark-check-fill me-2"></i> Báo cáo tổng kết đợt nhập: <code>${summary.batchCode}</code>
                        </h6>
                        <span class="badge bg-secondary">${summary.fileName}</span>
                    </div>
                </div>
                <div class="card-body">
                    <div class="row text-center mb-3 g-2">
                        <div class="col-md-4">
                            <div class="p-3 bg-light rounded-3 border">
                                <div class="text-muted small">Tổng số dòng tệp</div>
                                <div class="fs-4 fw-bold text-dark">${summary.totalRows}</div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="p-3 bg-success-subtle rounded-3 border border-success-subtle">
                                <div class="text-success small fw-semibold">Đã nhập thành công</div>
                                <div class="fs-4 fw-bold text-success">${summary.successCount}</div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="p-3 bg-danger-subtle rounded-3 border border-danger-subtle">
                                <div class="text-danger small fw-semibold">Dòng lỗi bị bỏ qua</div>
                                <div class="fs-4 fw-bold text-danger">${summary.errorCount}</div>
                            </div>
                        </div>
                    </div>

                    <c:if test="${summary.errorCount > 0}">
                        <h6 class="fw-bold text-danger mt-3 mb-2"><i class="bi bi-exclamation-octagon me-1"></i> Chi tiết các dòng bị bỏ qua do lỗi:</h6>
                        <div class="table-responsive">
                            <table class="table table-bordered table-sm small align-middle">
                                <thead class="table-light">
                                    <tr>
                                        <th style="width: 80px;">Dòng số</th>
                                        <th>Họ và tên</th>
                                        <th>Email</th>
                                        <th>Lý do lỗi</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="err" items="${summary.errorItems}">
                                        <tr class="table-danger">
                                            <td class="text-center fw-bold">${err.rowIndex}</td>
                                            <td>${err.fullName}</td>
                                            <td><code>${err.email}</code></td>
                                            <td class="text-danger fw-semibold">${err.reason}</td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:if>
                    <div class="mt-3">
                        <a href="${pageContext.request.contextPath}/admin/user-import" class="btn btn-outline-primary btn-sm">
                            <i class="bi bi-arrow-left"></i> Nhập tệp mới
                        </a>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- BƯỚC 1: FORM TẢI TỆP (AC1) & BƯỚC 2: XEM TRƯỚC (AC2) -->
        <c:if test="${empty summary}">
            <!-- Upload Box -->
            <div class="card mb-4 border-0 shadow-sm">
                <div class="card-header bg-white py-3">
                    <h6 class="mb-0 fw-bold text-dark">
                        <i class="bi bi-cloud-arrow-up-fill text-primary me-2"></i> Tải lên tệp danh sách (.csv, .xlsx)
                    </h6>
                </div>
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/admin/user-import" method="post" enctype="multipart/form-data" id="importForm">
                        <input type="hidden" name="action" id="formAction" value="preview">
                        
                        <div class="dropzone-box" onclick="document.getElementById('fileInput').click();">
                            <i class="bi bi-file-earmark-spreadsheet display-4 text-primary"></i>
                            <h6 class="mt-3 fw-bold">Nhấn vào đây hoặc kéo thả tệp Excel/CSV vào vùng này</h6>
                            <p class="text-muted small mb-0">Hỗ trợ định dạng .csv, .xlsx, tối đa 500 dòng/lần</p>
                            <input type="file" name="file" id="fileInput" class="d-none" accept=".csv, .xlsx, .xls, text/csv" onchange="this.form.submit();">
                        </div>

                        <div class="mt-3 d-flex justify-content-between align-items-center">
                            <span class="text-muted small">
                                <i class="bi bi-info-circle me-1"></i> Định dạng mẫu: Họ và tên, Email, Số điện thoại, Vai trò, Ngày sinh, Giới tính, Địa chỉ.
                            </span>
                            <a href="${pageContext.request.contextPath}/admin/user-import?action=template" class="btn btn-link text-decoration-none btn-sm">
                                <i class="bi bi-download"></i> Tải tệp mẫu
                            </a>
                        </div>
                    </form>
                </div>
            </div>

            <!-- XEM TRƯỚC VÀ BÁO LỖI THEO TỪNG DÒNG (AC2) -->
            <c:if test="${not empty preview}">
                <div class="card border-0 shadow-sm">
                    <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="mb-1 fw-bold text-dark">
                                <i class="bi bi-table text-primary me-2"></i> Kết quả xem trước dữ liệu (Xem trước & Báo lỗi từng dòng)
                            </h6>
                            <span class="text-muted small">Tệp: <strong>${uploadedFileName}</strong></span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-primary">Tổng: ${preview.totalRows} dòng</span>
                            <span class="badge bg-success">Hợp lệ: ${preview.validRows}</span>
                            <span class="badge bg-danger">Lỗi: ${preview.errorRows}</span>
                        </div>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover table-preview mb-0 align-middle">
                                <thead>
                                    <tr>
                                        <th class="text-center" style="width: 60px;">Dòng</th>
                                        <th>Họ và tên</th>
                                        <th>Email</th>
                                        <th>Số điện thoại</th>
                                        <th>Vai trò</th>
                                        <th class="text-center" style="width: 110px;">Trạng thái</th>
                                        <th>Chi tiết / Lý do lỗi</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="row" items="${preview.rows}">
                                        <tr class="${row.isValid ? 'row-valid' : 'row-error'}">
                                            <td class="text-center fw-bold text-secondary">${row.rowIndex}</td>
                                            <td class="fw-semibold">${row.fullName}</td>
                                            <td><code>${row.email}</code></td>
                                            <td>${row.phone != null ? row.phone : '-'}</td>
                                            <td><span class="badge bg-secondary">${row.role}</span></td>
                                            <td class="text-center">
                                                <c:choose>
                                                    <c:when test="${row.isValid}">
                                                        <span class="badge bg-success"><i class="bi bi-check-lg"></i> Hợp lệ</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-danger"><i class="bi bi-x-lg"></i> Lỗi</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${row.isValid}">
                                                        <span class="text-success small fw-medium">Sẵn sàng nhập</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-danger small fw-semibold">${row.errorMessage}</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="card-footer bg-white py-3 d-flex justify-content-between align-items-center">
                        <a href="${pageContext.request.contextPath}/admin/user-import" class="btn btn-outline-secondary btn-sm">
                            <i class="bi bi-arrow-repeat"></i> Chọn tệp khác
                        </a>
                        <form action="${pageContext.request.contextPath}/admin/user-import" method="post">
                            <input type="hidden" name="action" value="confirmImport">
                            <input type="hidden" name="fileName" value="${uploadedFileName}">
                            <button type="submit" class="btn btn-success fw-bold px-4" ${preview.validRows == 0 ? 'disabled' : ''}>
                                <i class="bi bi-check-circle-fill me-1"></i> Xác nhận nhập ${preview.validRows} dòng hợp lệ (${preview.errorRows} lỗi bỏ qua)
                            </button>
                        </form>
                    </div>
                </div>
            </c:if>
        </c:if>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
