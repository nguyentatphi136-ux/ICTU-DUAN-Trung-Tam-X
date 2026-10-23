<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ học viên - ${student.name} | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f8fafc; }
        .profile-card { border: none; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.04); background: #ffffff; }
        .avatar-circle { width: 72px; height: 72px; border-radius: 50%; background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 700; }
        .badge-status { font-size: 13px; padding: 6px 14px; border-radius: 20px; font-weight: 600; }
        .status-studying { background: #dcfce7; color: #15803d; }
        .status-suspended { background: #fef3c7; color: #b45309; }
        .status-dropped { background: #fee2e2; color: #b91c1c; }
        .status-graduated { background: #ede9fe; color: #6d28d9; }
        .section-title { font-size: 16px; font-weight: 700; color: #1e293b; display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
        .effect-preview-box { background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 10px; padding: 14px; }
        .nav-tabs .nav-link { color: #64748b; font-weight: 600; border: none; border-bottom: 2px solid transparent; }
        .nav-tabs .nav-link.active { color: #2563eb; border-bottom: 2px solid #2563eb; background: none; }
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

        <!-- Thông báo thành công / lỗi -->
        <c:if test="${not empty successMessage}">
            <div class="alert alert-success alert-dismissible fade show border-0 shadow-sm" role="alert">
                <i class="bi bi-check-circle-fill me-2 fs-5 align-middle"></i>
                <strong>Thành công!</strong> <c:out value="${successMessage}"/>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show border-0 shadow-sm" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2 fs-5 align-middle"></i>
                <strong>Lưu ý:</strong> <c:out value="${errorMessage}"/>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Thanh điều hướng phụ -->
        <div class="d-flex justify-content-between align-items-center mb-3">
            <a href="${pageContext.request.contextPath}/student/status" class="text-decoration-none text-secondary">
                <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách học viên
            </a>
            <span class="badge bg-primary px-3 py-2">Story Jira: S4-06 (IDTTX-252)</span>
        </div>

        <!-- Header Card Hồ sơ học viên -->
        <div class="profile-card p-4 mb-4">
            <div class="row align-items-center">
                <div class="col-auto">
                    <div class="avatar-circle">
                        ${student.name.substring(0, 1)}
                    </div>
                </div>
                <div class="col">
                    <div class="d-flex align-items-center gap-3 mb-1">
                        <h3 class="fw-bold mb-0 text-dark">${student.name}</h3>
                        <c:choose>
                            <c:when test="${student.status == 'Đang học'}">
                                <span class="badge-status status-studying"><i class="bi bi-check-circle-fill me-1"></i>Đang học</span>
                            </c:when>
                            <c:when test="${student.status == 'Bảo lưu'}">
                                <span class="badge-status status-suspended"><i class="bi bi-pause-circle-fill me-1"></i>Bảo lưu</span>
                            </c:when>
                            <c:when test="${student.status == 'Thôi học'}">
                                <span class="badge-status status-dropped"><i class="bi bi-x-circle-fill me-1"></i>Thôi học</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge-status bg-secondary text-white">${student.status}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="text-secondary small d-flex flex-wrap gap-3">
                        <span><i class="bi bi-qr-code me-1 text-primary"></i>Mã HV: <strong>${student.code}</strong></span>
                        <span><i class="bi bi-envelope me-1 text-primary"></i>${student.email}</span>
                        <span><i class="bi bi-telephone me-1 text-primary"></i>${student.phone}</span>
                        <span><i class="bi bi-mortarboard me-1 text-primary"></i>${student.program}</span>
                        <span><i class="bi bi-door-open me-1 text-primary"></i>Lớp: 
                            <strong class="text-dark">${not empty student.currentClass ? student.currentClass : 'Chưa xếp lớp / Đã rời lớp'}</strong>
                        </span>
                    </div>
                </div>

                <!-- Nhóm 3 Nút nghiệp vụ S4-06: Bảo lưu, Chuyển lớp, Thôi học -->
                <div class="col-md-auto mt-3 mt-md-0 d-flex flex-wrap gap-2">
                    <button type="button" class="btn btn-warning text-dark fw-bold px-3 shadow-sm" onclick="openStatusModal('Bảo lưu')">
                        <i class="bi bi-pause-circle me-1"></i> Bảo lưu
                    </button>
                    <button type="button" class="btn btn-primary fw-bold px-3 shadow-sm" onclick="openStatusModal('Chuyển lớp')">
                        <i class="bi bi-arrow-left-right me-1"></i> Chuyển lớp
                    </button>
                    <button type="button" class="btn btn-outline-danger fw-bold px-3 shadow-sm" onclick="openStatusModal('Thôi học')">
                        <i class="bi bi-person-x me-1"></i> Thôi học
                    </button>
                    <c:if test="${not empty student.currentClass}">
                        <a href="${pageContext.request.contextPath}/student/roster?cls=${student.currentClass}" class="btn btn-outline-secondary px-3 shadow-sm">
                            <i class="bi bi-calendar-check me-1"></i> Xem danh sách điểm danh
                        </a>
                    </c:if>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <!-- Cột trái: Thông tin học vụ & Lịch sử lớp đã học -->
            <div class="col-lg-5">
                <div class="profile-card p-4 mb-4">
                    <div class="section-title">
                        <i class="bi bi-person-lines-fill text-primary"></i> Thông tin học vụ chi tiết
                    </div>
                    <ul class="list-group list-group-flush small">
                        <li class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Tài khoản đăng nhập:</span>
                            <span class="fw-semibold text-dark">${student.loginEmail}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Ngày nhập học:</span>
                            <span class="fw-semibold text-dark">${student.enrolledAt}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Trình độ đầu vào:</span>
                            <span class="fw-semibold text-dark">${student.level} (${student.entryScore})</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Tư vấn viên phụ trách:</span>
                            <span class="fw-semibold text-dark">${student.counselor}</span>
                        </li>
                        <li class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Địa chỉ liên hệ:</span>
                            <span class="fw-semibold text-dark">${student.address}</span>
                        </li>
                    </ul>
                    <div class="mt-3 p-3 bg-light rounded text-secondary small">
                        <strong>Lý lịch / Ghi chú:</strong> ${student.background}
                    </div>
                </div>

                <!-- Lịch sử các lớp đã học -->
                <div class="profile-card p-4">
                    <div class="section-title">
                        <i class="bi bi-clock-history text-primary"></i> Các lớp đã từng học (${student.history.size()})
                    </div>
                    <c:choose>
                        <c:when test="${empty student.history}">
                            <div class="text-muted small py-3 text-center">Chưa có lớp học nào trong lịch sử.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table table-sm table-hover align-middle mb-0 small">
                                    <thead class="table-light">
                                        <tr>
                                            <th>Lớp</th>
                                            <th>Chương trình</th>
                                            <th>Thời gian</th>
                                            <th>Kết quả</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="h" items="${student.history}">
                                            <tr>
                                                <td class="fw-bold text-primary">${h.classCode}</td>
                                                <td>${h.programName}</td>
                                                <td class="text-secondary">${h.period}</td>
                                                <td>
                                                    <span class="badge ${h.result.contains('Bảo lưu') ? 'bg-warning text-dark' : (h.result.contains('Chuyển') ? 'bg-info text-dark' : 'bg-success')}">
                                                        ${h.result}
                                                    </span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <!-- Cột phải: Lịch sử thay đổi tình trạng học vụ (Trọng tâm S4-06) -->
            <div class="col-lg-7">
                <div class="profile-card p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div class="section-title mb-0">
                            <i class="bi bi-journal-text text-primary"></i> Lịch sử thay đổi tình trạng học (${student.statusChanges.size()})
                        </div>
                        <span class="badge bg-light text-secondary border">Tự động cập nhật vào hồ sơ</span>
                    </div>

                    <p class="text-secondary small mb-3">
                        Toàn bộ thao tác bảo lưu, chuyển lớp và thôi học được ghi vết chi tiết, bao gồm lý do, ngày hiệu lực và người thực hiện.
                    </p>

                    <c:choose>
                        <c:when test="${empty student.statusChanges}">
                            <div class="text-center py-5 text-muted">
                                <i class="bi bi-file-earmark-check fs-1 text-secondary opacity-50 mb-2"></i>
                                <p class="mb-0">Học viên chưa có biến động học vụ nào.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0">
                                    <thead class="table-light small">
                                        <tr>
                                            <th>Thao tác</th>
                                            <th>Ngày hiệu lực</th>
                                            <th>Từ lớp &rarr; Lớp mới</th>
                                            <th>Lý do</th>
                                            <th>Người thực hiện</th>
                                        </tr>
                                    </thead>
                                    <tbody class="small">
                                        <c:forEach var="log" items="${student.statusChanges}">
                                            <tr>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${log.actionType == 'Bảo lưu'}">
                                                            <span class="badge bg-warning text-dark px-2 py-1"><i class="bi bi-pause-circle me-1"></i>Bảo lưu</span>
                                                        </c:when>
                                                        <c:when test="${log.actionType == 'Chuyển lớp'}">
                                                            <span class="badge bg-primary px-2 py-1"><i class="bi bi-arrow-left-right me-1"></i>Chuyển lớp</span>
                                                        </c:when>
                                                        <c:when test="${log.actionType == 'Thôi học'}">
                                                            <span class="badge bg-danger px-2 py-1"><i class="bi bi-x-circle me-1"></i>Thôi học</span>
                                                        </c:when>
                                                    </c:choose>
                                                </td>
                                                <td class="fw-bold text-dark">
                                                    ${log.effectiveDate}
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${log.actionType == 'Chuyển lớp'}">
                                                            <span class="text-muted">${log.fromClass}</span> &rarr; <strong class="text-success">${log.toClass}</strong>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-muted">Rời lớp: ${log.fromClass}</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td style="max-width: 200px;">
                                                    <div class="text-truncate" title="${log.reason}">
                                                        ${log.reason}
                                                    </div>
                                                </td>
                                                <td class="text-secondary">
                                                    <i class="bi bi-person-circle me-1"></i>${log.actorName}
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL THAY ĐỔI TÌNH TRẠNG HỌC (S4-06) -->
    <div class="modal fade" id="statusModal" tabindex="-1" aria-labelledby="statusModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg" style="border-radius: 16px;">
                <form action="${pageContext.request.contextPath}/student/change-status" method="POST">
                    <input type="hidden" name="studentCode" value="${student.code}">
                    <input type="hidden" name="actionType" id="actionTypeInput" value="Bảo lưu">

                    <div class="modal-header border-0 pb-0">
                        <div>
                            <h4 class="modal-title fw-bold text-dark" id="statusModalLabel">Thay đổi tình trạng học</h4>
                            <p class="text-muted small mb-0">${student.name} · ${student.code} · Lớp hiện tại: <strong>${not empty student.currentClass ? student.currentClass : 'Chưa xếp lớp'}</strong></p>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body pt-3">
                        <!-- Chọn loại thao tác (Tab buttons) -->
                        <div class="btn-group w-100 mb-3" role="group" aria-label="Action Type Selector">
                            <button type="button" class="btn btn-outline-warning text-dark fw-bold" id="btnTypeSuspend" onclick="switchActionType('Bảo lưu')">
                                <i class="bi bi-pause-circle me-1"></i> Bảo lưu
                            </button>
                            <button type="button" class="btn btn-outline-primary fw-bold" id="btnTypeTransfer" onclick="switchActionType('Chuyển lớp')">
                                <i class="bi bi-arrow-left-right me-1"></i> Chuyển lớp
                            </button>
                            <button type="button" class="btn btn-outline-danger fw-bold" id="btnTypeDrop" onclick="switchActionType('Thôi học')">
                                <i class="bi bi-x-circle me-1"></i> Thôi học
                            </button>
                        </div>

                        <!-- Dropdown chọn lớp chuyển đến (Chỉ hiện khi chọn Chuyển lớp) -->
                        <div class="mb-3" id="transferClassGroup" style="display: none;">
                            <label for="toClassCode" class="form-label fw-bold">Lớp chuyển đến <span class="text-danger">*</span></label>
                            <select class="form-select" id="toClassCode" name="toClassCode">
                                <c:choose>
                                    <c:when test="${empty availableClasses}">
                                        <option value="">-- Không có lớp cùng chương trình còn chỗ --</option>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach var="c" items="${availableClasses}">
                                            <option value="${c.code}">
                                                ${c.code} · ${c.name} (còn ${c.availableSeats} chỗ trống)
                                            </option>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                            </select>
                            <div class="form-text text-muted">
                                <i class="bi bi-info-circle me-1"></i> Chỉ hiển thị các lớp cùng chương trình đào tạo (${student.program}), đang mở và còn chỗ trống.
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <!-- Ngày hiệu lực -->
                            <div class="col-md-6">
                                <label for="effectiveDate" class="form-label fw-bold">Ngày hiệu lực <span class="text-danger">*</span></label>
                                <input type="date" class="form-control" id="effectiveDate" name="effectiveDate" value="${today}" required>
                                <div class="form-text text-muted">Từ ngày này, học viên rời danh sách điểm danh lớp cũ.</div>
                            </div>

                            <!-- Lý do -->
                            <div class="col-md-6">
                                <label for="reasonSelect" class="form-label fw-bold">Lý do <span class="text-danger">*</span></label>
                                <select class="form-select" id="reasonSelect" name="reason" onchange="checkReasonOther(this.value)" required>
                                    <!-- Options sẽ được cập nhật động bằng Javascript tùy theo loại thao tác -->
                                </select>
                            </div>
                        </div>

                        <!-- Ghi chú thêm -->
                        <div class="mb-3">
                            <label for="notesInput" class="form-label fw-bold">Ghi chú thêm <span id="noteRequiredMark" class="text-danger" style="display: none;">*</span></label>
                            <textarea class="form-control" id="notesInput" name="notes" rows="2" placeholder="Nhập ghi chú chi tiết (bắt buộc nếu chọn 'Lý do khác')..."></textarea>
                        </div>

                        <!-- Hộp thoại cảnh báo nghiệp vụ theo Jira S4-06 -->
                        <div class="effect-preview-box">
                            <h6 class="fw-bold text-success mb-2"><i class="bi bi-shield-check me-1"></i>Điều sẽ xảy ra từ ngày hiệu lực:</h6>
                            <ul class="mb-0 small ps-3 text-secondary" id="effectsList">
                                <!-- Dynamic effects description -->
                            </ul>
                        </div>
                    </div>

                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-light px-4" data-bs-dismiss="modal">Huỷ</button>
                        <button type="submit" class="btn btn-primary px-4 fw-bold" id="btnSubmitStatus">
                            Xác nhận Bảo lưu
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        const REASONS = {
            'Bảo lưu': ['Lý do sức khoẻ', 'Bận công việc', 'Lịch thi ở trường', 'Lý do gia đình', 'Lý do khác'],
            'Chuyển lớp': ['Đổi sang lịch học phù hợp hơn', 'Học lại từ đầu', 'Chuyển cơ sở học', 'Lý do khác'],
            'Thôi học': ['Không theo kịp chương trình', 'Chuyển sang trung tâm khác', 'Lý do tài chính', 'Lý do khác']
        };

        const EFFECTS = {
            'Bảo lưu': [
                'Rời khỏi danh sách điểm danh của lớp ${student.currentClass} từ ngày hiệu lực.',
                'Điểm số và kết quả điểm danh các buổi trước đó được giữ nguyên vẹn trong hồ sơ.',
                'Thao tác được ghi vào Lịch sử thay đổi tình trạng kèm tên người thực hiện (${currentUser.name}).',
                'Khi quay lại, Quản lý đào tạo có thể ghi danh học viên vào lớp phù hợp.'
            ],
            'Chuyển lớp': [
                'Rời danh sách điểm danh lớp ${student.currentClass}, chuyển sang danh sách điểm danh của lớp mới từ ngày hiệu lực.',
                'Điểm số và điểm danh các buổi học trước ngày hiệu lực ở lớp cũ được giữ nguyên trong lịch sử.',
                'Thao tác được ghi vết chi tiết vào hồ sơ học viên kèm người thực hiện (${currentUser.name}).'
            ],
            'Thôi học': [
                'Rời khỏi danh sách điểm danh của lớp ${student.currentClass} từ ngày hiệu lực.',
                'Hồ sơ chuyển sang trạng thái Thôi học, dữ liệu cũ vẫn được lưu trữ để tra cứu về sau.',
                'Thao tác được ghi nhận vào nhật ký biến động học vụ kèm người thực hiện (${currentUser.name}).'
            ]
        };

        let currentModal = null;

        function openStatusModal(type) {
            if (!currentModal) {
                currentModal = new bootstrap.Modal(document.getElementById('statusModal'));
            }
            switchActionType(type);
            currentModal.show();
        }

        function switchActionType(type) {
            document.getElementById('actionTypeInput').value = type;

            // Đổi trạng thái active nút bấm
            const btnSuspend = document.getElementById('btnTypeSuspend');
            const btnTransfer = document.getElementById('btnTypeTransfer');
            const btnDrop = document.getElementById('btnTypeDrop');
            const btnSubmit = document.getElementById('btnSubmitStatus');
            const transferGroup = document.getElementById('transferClassGroup');

            btnSuspend.className = type === 'Bảo lưu' ? 'btn btn-warning text-dark fw-bold' : 'btn btn-outline-warning text-dark fw-bold';
            btnTransfer.className = type === 'Chuyển lớp' ? 'btn btn-primary fw-bold' : 'btn btn-outline-primary fw-bold';
            btnDrop.className = type === 'Thôi học' ? 'btn btn-danger fw-bold' : 'btn btn-outline-danger fw-bold';

            if (type === 'Chuyển lớp') {
                transferGroup.style.display = 'block';
                btnSubmit.className = 'btn btn-primary px-4 fw-bold';
                btnSubmit.innerText = 'Xác nhận Chuyển lớp';
            } else if (type === 'Bảo lưu') {
                transferGroup.style.display = 'none';
                btnSubmit.className = 'btn btn-warning text-dark px-4 fw-bold';
                btnSubmit.innerText = 'Xác nhận Bảo lưu';
            } else {
                transferGroup.style.display = 'none';
                btnSubmit.className = 'btn btn-danger px-4 fw-bold';
                btnSubmit.innerText = 'Xác nhận Thôi học';
            }

            // Nạp danh sách lý do
            const reasonSelect = document.getElementById('reasonSelect');
            reasonSelect.innerHTML = '<option value="">-- Chọn lý do chính thức --</option>';
            REASONS[type].forEach(r => {
                const opt = document.createElement('option');
                opt.value = r;
                opt.textContent = r;
                reasonSelect.appendChild(opt);
            });

            // Nạp hiệu lực nghiệp vụ
            const effectsList = document.getElementById('effectsList');
            effectsList.innerHTML = '';
            EFFECTS[type].forEach(e => {
                const li = document.createElement('li');
                li.textContent = e;
                effectsList.appendChild(li);
            });

            checkReasonOther(reasonSelect.value);
        }

        function checkReasonOther(val) {
            const mark = document.getElementById('noteRequiredMark');
            const notes = document.getElementById('notesInput');
            if (val === 'Lý do khác') {
                mark.style.display = 'inline';
                notes.required = true;
                notes.focus();
            } else {
                mark.style.display = 'none';
                notes.required = false;
            }
        }
    </script>
</body>
</html>
