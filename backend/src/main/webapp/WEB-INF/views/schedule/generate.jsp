<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sinh Thời Khoá Biểu Tự Động | EMS Đào tạo</title>
    <!-- Bootstrap 5 & Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        :root {
            --primary-color: #0d6efd;
            --primary-hover: #0b5ed7;
            --step-active: #0d6efd;
            --step-done: #198754;
            --bg-light-soft: #f8fafc;
        }
        body {
            background-color: var(--bg-light-soft);
            font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
            color: #212529;
        }
        /* Step progress indicator */
        .step-indicator {
            display: flex;
            justify-content: space-between;
            position: relative;
            margin-bottom: 2rem;
            list-style: none;
            padding: 0;
        }
        .step-indicator::before {
            content: "";
            position: absolute;
            top: 50%;
            left: 10%;
            right: 10%;
            height: 3px;
            background-color: #dee2e6;
            z-index: 1;
            transform: translateY(-50%);
        }
        .step-item {
            position: relative;
            z-index: 2;
            background: var(--bg-light-soft);
            padding: 0 1rem;
            text-align: center;
        }
        .step-circle {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            background-color: #e9ecef;
            color: #6c757d;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            margin-bottom: 0.35rem;
            transition: all 0.25s ease-in-out;
        }
        .step-item.active .step-circle {
            background-color: var(--step-active);
            color: #fff;
            box-shadow: 0 0 0 4px rgba(13, 110, 253, 0.2);
        }
        .step-item.done .step-circle {
            background-color: var(--step-done);
            color: #fff;
        }
        .step-title {
            font-size: 0.88rem;
            font-weight: 600;
            color: #495057;
            display: block;
        }
        .step-item.active .step-title {
            color: var(--step-active);
        }
        .step-item.done .step-title {
            color: var(--step-done);
        }
        /* Day of week selector */
        .dow-btn-group .btn-check:checked + .btn {
            background-color: var(--primary-color);
            color: #fff;
            border-color: var(--primary-color);
            font-weight: bold;
        }
        .dow-btn-group .btn {
            min-width: 50px;
            border-radius: 8px !important;
            margin-right: 6px;
            margin-bottom: 6px;
        }
        /* Holiday item list */
        .holiday-badge {
            background-color: #fff3cd;
            color: #664d03;
            border: 1px solid #ffe69c;
            font-size: 0.8rem;
            padding: 0.35em 0.6em;
            border-radius: 6px;
        }
        .skipped-row {
            background-color: #fff8e6 !important;
            color: #856404;
            font-style: italic;
        }
        .card-custom {
            border-radius: 12px;
            border: none;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }
        .table-sessions th {
            background-color: #f1f5f9;
            font-size: 0.85rem;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #475569;
        }
        .lesson-map-box {
            background: #eff6ff;
            border-left: 4px solid #3b82f6;
            border-radius: 8px;
            padding: 1rem;
        }
    </style>
</head>
<body>

    <!-- Header Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4 sticky-top shadow-sm">
        <div class="container">
            <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/dashboard.jsp">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navContent">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navContent">
                <div class="navbar-nav me-auto">
                    <a class="nav-link" href="${pageContext.request.contextPath}/dashboard.jsp">Trang tổng quan</a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/training-programs">Chương trình học</a>
                    <a class="nav-link active" href="${pageContext.request.contextPath}/schedule/generate">
                        <i class="bi bi-calendar-week me-1"></i>Sinh thời khoá biểu
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/grade/list">Điểm số</a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/tuition/list">Học phí</a>
                    <a class="nav-link" href="${pageContext.request.contextPath}/admin/users/roles">Phân quyền</a>
                </div>
                <div class="d-flex align-items-center text-white">
                    <span class="me-3 small">
                        <i class="bi bi-person-circle me-1"></i>
                        <strong>${sessionScope.currentUser != null ? sessionScope.currentUser.name : 'Phạm Thị Quản'}</strong> 
                        <span class="badge bg-primary ms-1">Quản lý đào tạo</span>
                    </span>
                    <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                        <i class="bi bi-box-arrow-right"></i> Đăng xuất
                    </a>
                </div>
            </div>
        </div>
    </nav>

    <div class="container pb-5">

        <!-- Banner Tác vụ Jira IDTTX-247 / IDTTX-263 [BE] -->
        <div class="card mb-4 border-0 shadow-sm border-start border-4 border-primary">
            <div class="card-body">
                <div class="d-flex flex-wrap align-items-center justify-content-between mb-2">
                    <div class="d-flex align-items-center">
                        <span class="badge bg-primary me-2">Jira: IDTTX-247</span>
                        <span class="badge bg-success me-2">Subtask: IDTTX-263 [BE]</span>
                        <span class="badge bg-secondary">Assignee: Nguyễn Trung Kiên (NK)</span>
                    </div>
                    <span class="badge bg-info text-dark">Sprint 4: S4-01</span>
                </div>
                <h5 class="card-title fw-bold text-primary mb-2">
                    S4-01: Sinh thời khoá biểu tự động theo mẫu lặp cho một lớp (60 buổi)
                </h5>
                <p class="card-text text-secondary mb-2 small">
                    <strong>Mô tả bài toán:</strong> Là Quản lý đào tạo, tôi muốn sinh thời khoá biểu tự động theo mẫu lặp, để xếp lịch cho một lớp 60 buổi trong vài phút thay vì gõ tay từng buổi.
                </p>
                <div class="row g-2 small text-muted">
                    <div class="col-md-3">
                        <i class="bi bi-check2-circle text-success me-1"></i> <strong>Khai báo mẫu:</strong> Thứ trong tuần, ca học, ngày bắt đầu, số buổi
                    </div>
                    <div class="col-md-3">
                        <i class="bi bi-check2-circle text-success me-1"></i> <strong>Tự động bỏ qua:</strong> Tự bỏ qua các ngày nghỉ lễ đã khai báo
                    </div>
                    <div class="col-md-3">
                        <i class="bi bi-check2-circle text-success me-1"></i> <strong>Gắn bài học:</strong> Buổi 1..N gắn bài tuần tự theo thứ tự môn
                    </div>
                    <div class="col-md-3">
                        <i class="bi bi-check2-circle text-success me-1"></i> <strong>Xem trước:</strong> Kiểm tra danh sách trước khi lưu chính thức
                    </div>
                </div>
            </div>
        </div>

        <!-- Thông báo Flash Messages -->
        <c:if test="${not empty successMessage}">
            <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-check-circle-fill me-2"></i><strong>Thành công!</strong> ${successMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i><strong>Lỗi:</strong> ${errorMessage}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Thanh chỉ báo các bước (Step Indicator) -->
        <ul class="step-indicator">
            <li class="step-item ${step == 1 ? 'active' : (step > 1 ? 'done' : '')}">
                <div class="step-circle">
                    <c:choose>
                        <c:when test="${step > 1}"><i class="bi bi-check-lg"></i></c:when>
                        <c:otherwise>1</c:otherwise>
                    </c:choose>
                </div>
                <span class="step-title">Khai báo mẫu lặp</span>
            </li>
            <li class="step-item ${step == 2 ? 'active' : (step > 2 ? 'done' : '')}">
                <div class="step-circle">
                    <c:choose>
                        <c:when test="${step > 2}"><i class="bi bi-check-lg"></i></c:when>
                        <c:otherwise>2</c:otherwise>
                    </c:choose>
                </div>
                <span class="step-title">Xem trước & kiểm tra</span>
            </li>
            <li class="step-item ${step == 3 ? 'active done' : ''}">
                <div class="step-circle">
                    <c:choose>
                        <c:when test="${step == 3}"><i class="bi bi-check-lg"></i></c:when>
                        <c:otherwise>3</c:otherwise>
                    </c:choose>
                </div>
                <span class="step-title">Đã lưu thời khoá biểu</span>
            </li>
        </ul>

        <!-- =================================================================== -->
        <!-- BƯỚC 1: KHAI BÁO MẪU LẶP -->
        <!-- =================================================================== -->
        <c:if test="${step == 1}">
            <div class="row g-4">
                <!-- Cột chính: Form khai báo mẫu lặp -->
                <div class="col-lg-8">
                    <div class="card card-custom shadow-sm p-4">
                        <h4 class="fw-bold mb-3 text-dark">
                            <i class="bi bi-calendar-plus text-primary me-2"></i>Mẫu Lặp Thời Khoá Biểu
                        </h4>

                        <form id="scheduleForm" action="${pageContext.request.contextPath}/schedule/generate" method="POST">
                            <input type="hidden" name="action" value="preview" />

                            <!-- Chọn Lớp học -->
                            <div class="mb-4">
                                <label for="classCodeSelect" class="form-label fw-bold">
                                    <i class="bi bi-people-fill text-primary me-1"></i>Chọn lớp học cần sinh lịch
                                </label>
                                <select id="classCodeSelect" name="classCode" class="form-select form-select-lg" onchange="window.location.href='${pageContext.request.contextPath}/schedule/generate?classCode=' + this.value;">
                                    <c:forEach var="c" items="${classes}">
                                        <option value="${c.code}" ${c.code == selectedClass.code ? 'selected' : ''}>
                                            ${c.code} · ${c.program} (${c.totalSessions} buổi) · ${c.status}
                                        </option>
                                    </c:forEach>
                                </select>
                                <div class="form-text">
                                    <c:if test="${not empty selectedClass}">
                                        Lớp <strong>${selectedClass.code}</strong>: ${selectedClass.program} | 
                                        Gồm <strong>${fn:length(selectedClass.subjects)} môn học</strong>, 
                                        tổng cộng <strong>${selectedClass.totalSessions} buổi</strong>.
                                    </c:if>
                                </div>
                            </div>

                            <!-- Khai báo các thứ trong tuần -->
                            <div class="mb-4">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <label class="form-label fw-bold mb-0">
                                        <i class="bi bi-calendar3 text-primary me-1"></i>Các thứ trong tuần
                                    </label>
                                    <div class="btn-group btn-group-sm">
                                        <button type="button" class="btn btn-outline-secondary" onclick="setQuickDays([1,3,5])">2-4-6</button>
                                        <button type="button" class="btn btn-outline-secondary" onclick="setQuickDays([2,4,6])">3-5-7</button>
                                        <button type="button" class="btn btn-outline-secondary" onclick="setQuickDays([2,5])">3-6</button>
                                        <button type="button" class="btn btn-outline-secondary" onclick="setQuickDays([4,6])">5-7</button>
                                        <button type="button" class="btn btn-outline-secondary" onclick="setQuickDays([6,0])">Thứ 7 & CN</button>
                                    </div>
                                </div>
                                <div class="dow-btn-group d-flex flex-wrap">
                                    <!-- 1: Thứ Hai -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow1" name="days" value="1" ${pattern.hasDay(1) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow1">Thứ 2</label>

                                    <!-- 2: Thứ Ba -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow2" name="days" value="2" ${pattern.hasDay(2) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow2">Thứ 3</label>

                                    <!-- 3: Thứ Tư -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow3" name="days" value="3" ${pattern.hasDay(3) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow3">Thứ 4</label>

                                    <!-- 4: Thứ Năm -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow4" name="days" value="4" ${pattern.hasDay(4) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow4">Thứ 5</label>

                                    <!-- 5: Thứ Sáu -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow5" name="days" value="5" ${pattern.hasDay(5) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow5">Thứ 6</label>

                                    <!-- 6: Thứ Bảy -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow6" name="days" value="6" ${pattern.hasDay(6) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow6">Thứ 7</label>

                                    <!-- 0: Chủ Nhật -->
                                    <input type="checkbox" class="btn-check dow-check" id="dow0" name="days" value="0" ${pattern.hasDay(0) ? 'checked' : ''}>
                                    <label class="btn btn-outline-primary" for="dow0">Chủ Nhật</label>
                                </div>
                            </div>

                            <!-- Ca học -->
                            <div class="mb-4">
                                <label class="form-label fw-bold">
                                    <i class="bi bi-clock-history text-primary me-1"></i>Ca học trong ngày
                                </label>
                                <div class="d-flex flex-wrap gap-2 mb-2">
                                    <button type="button" class="btn btn-outline-primary shift-btn" onclick="setShift('07:00', '10:00', this)">
                                        <i class="bi bi-sunrise me-1"></i>Sáng (07:00 – 10:00)
                                    </button>
                                    <button type="button" class="btn btn-outline-primary shift-btn" onclick="setShift('13:00', '16:00', this)">
                                        <i class="bi bi-sun me-1"></i>Chiều (13:00 – 16:00)
                                    </button>
                                    <button type="button" class="btn btn-outline-primary shift-btn active" onclick="setShift('18:00', '20:30', this)">
                                        <i class="bi bi-moon-stars me-1"></i>Tối (18:00 – 20:30)
                                    </button>
                                    <button type="button" class="btn btn-outline-secondary" onclick="toggleCustomTime()">
                                        <i class="bi bi-pencil me-1"></i>Tự nhập giờ
                                    </button>
                                </div>
                                <div id="customTimeBlock" class="row g-2 align-items-center mt-1" style="display: none;">
                                    <div class="col-auto">
                                        <label for="startTimeInput" class="col-form-label small">Giờ bắt đầu:</label>
                                    </div>
                                    <div class="col-auto">
                                        <input type="time" id="startTimeInput" class="form-control" value="${pattern.start}">
                                    </div>
                                    <div class="col-auto">
                                        <label for="endTimeInput" class="col-form-label small">Giờ kết thúc:</label>
                                    </div>
                                    <div class="col-auto">
                                        <input type="time" id="endTimeInput" class="form-control" value="${pattern.end}">
                                    </div>
                                </div>
                                <input type="hidden" id="hiddenStart" name="start" value="${pattern.start}" />
                                <input type="hidden" id="hiddenEnd" name="end" value="${pattern.end}" />
                            </div>

                            <!-- 3 trường hàng ngang: Ngày bắt đầu, Số buổi, Phòng học -->
                            <div class="row g-3 mb-4">
                                <div class="col-md-4">
                                    <label for="startDateInput" class="form-label fw-bold">
                                        <i class="bi bi-calendar-event text-primary me-1"></i>Ngày bắt đầu
                                    </label>
                                    <input type="date" id="startDateInput" name="from" class="form-control" value="${pattern.from}" required>
                                    <div class="form-text">Ngày khai giảng dự kiến</div>
                                </div>
                                <div class="col-md-4">
                                    <label for="sessionCountInput" class="form-label fw-bold">
                                        <i class="bi bi-123 text-primary me-1"></i>Số buổi cần sinh
                                    </label>
                                    <input type="number" id="sessionCountInput" name="count" class="form-control" min="1" max="300" value="${pattern.count}" required>
                                    <div class="form-text">Ví dụ: 60 buổi cho cả khoá</div>
                                </div>
                                <div class="col-md-4">
                                    <label for="roomSelect" class="form-label fw-bold">
                                        <i class="bi bi-building text-primary me-1"></i>Phòng học mặc định
                                    </label>
                                    <select id="roomSelect" name="room" class="form-select">
                                        <option value="">-- Chưa chọn phòng --</option>
                                        <c:forEach var="r" items="${rooms}">
                                            <option value="${r}" ${r == pattern.room ? 'selected' : ''}>${r}</option>
                                        </c:forEach>
                                    </select>
                                    <div class="form-text">Phòng vật lý / Online</div>
                                </div>
                            </div>

                            <!-- Gắn buổi học với bài học trong môn -->
                            <div class="lesson-map-box mb-4">
                                <div class="d-flex">
                                    <div class="me-3 fs-3 text-primary">
                                        <i class="bi bi-book-half"></i>
                                    </div>
                                    <div>
                                        <h6 class="fw-bold mb-1 text-primary">Quy tắc gắn bài học tự động</h6>
                                        <p class="mb-1 small text-dark">
                                            Hệ thống tự động xếp bài học theo đúng lộ trình môn của lớp <strong>${selectedClass.code}</strong>:
                                        </p>
                                        <ul class="mb-0 small text-secondary ps-3">
                                            <c:forEach var="sub" items="${selectedClass.subjects}" varStatus="status">
                                                <li>
                                                    <strong>${sub.code}</strong> (${sub.name}): ${sub.sessions} buổi 
                                                    (GV: <span class="${empty sub.teacher ? 'text-danger' : 'text-success fw-bold'}">${empty sub.teacher ? 'Chưa phân công' : sub.teacher}</span>)
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                </div>
                            </div>

                            <!-- Nút hành động -->
                            <div class="d-flex justify-content-end gap-2">
                                <a href="${pageContext.request.contextPath}/dashboard.jsp" class="btn btn-outline-secondary px-4">
                                    Huỷ
                                </a>
                                <button type="submit" class="btn btn-primary px-4 fw-bold">
                                    <i class="bi bi-eye me-1"></i>Xem trước thời khoá biểu
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Cột phụ: Ngày nghỉ lễ & Khung dự kiến -->
                <div class="col-lg-4">
                    <!-- Danh mục ngày nghỉ lễ -->
                    <div class="card card-custom shadow-sm mb-4">
                        <div class="card-body">
                            <h5 class="fw-bold text-dark mb-2">
                                <i class="bi bi-calendar-x text-danger me-2"></i>Ngày nghỉ lễ đã khai báo
                            </h5>
                            <p class="text-muted small mb-3">
                                Hệ thống tự động phát hiện và bỏ qua các ngày này khi sinh lịch học.
                            </p>

                            <ul class="list-group list-group-flush mb-3">
                                <c:forEach var="h" items="${holidays}">
                                    <li class="list-group-item px-0 py-2 d-flex justify-content-between align-items-center">
                                        <div>
                                            <span class="d-block fw-bold small text-dark">${h.name}</span>
                                            <span class="text-muted small"><i class="bi bi-calendar-date me-1"></i>${h.formattedRange}</span>
                                        </div>
                                        <span class="badge holiday-badge">Bỏ qua</span>
                                    </li>
                                </c:forEach>
                            </ul>

                            <!-- Form thêm ngày nghỉ nhanh -->
                            <div class="border-top pt-3">
                                <h6 class="fw-bold text-secondary small mb-2">Thêm ngày nghỉ mới:</h6>
                                <form action="${pageContext.request.contextPath}/schedule/generate" method="POST">
                                    <input type="hidden" name="action" value="add_holiday" />
                                    <input type="hidden" name="classCode" value="${selectedClass.code}" />
                                    <div class="mb-2">
                                        <input type="text" name="holidayName" class="form-control form-control-sm" placeholder="Tên ngày nghỉ (VD: Khai giảng...)" required />
                                    </div>
                                    <div class="row g-2 mb-2">
                                        <div class="col-6">
                                            <input type="date" name="holidayFrom" class="form-control form-control-sm" required title="Từ ngày" />
                                        </div>
                                        <div class="col-6">
                                            <input type="date" name="holidayTo" class="form-control form-control-sm" placeholder="Đến ngày (tuỳ chọn)" title="Đến ngày" />
                                        </div>
                                    </div>
                                    <button type="submit" class="btn btn-outline-danger btn-sm w-100">
                                        <i class="bi bi-plus-circle me-1"></i>Thêm ngày nghỉ
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>

                    <!-- Hướng dẫn & Lưu ý -->
                    <div class="card card-custom shadow-sm bg-primary text-white">
                        <div class="card-body">
                            <h6 class="fw-bold mb-2">
                                <i class="bi bi-lightbulb me-2"></i>Lợi ích tính năng (S4-01)
                            </h6>
                            <ul class="small mb-0 ps-3">
                                <li>Tiết kiệm hơn 95% thời gian: Sinh trọn vẹn 60 buổi học chỉ với một cú nhấp chuột.</li>
                                <li>Không lo trùng ngày lễ: Tự động cộng dồn số buổi và dời lịch đảm bảo đủ số tiết quy chuẩn.</li>
                                <li>Tự động gắn Syllabus bài học chuẩn và Giảng viên phân công theo từng môn.</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- =================================================================== -->
        <!-- BƯỚC 2: XEM TRƯỚC VÀ KIỂM TRA TRÙNG (PREVIEW TABLE) -->
        <!-- =================================================================== -->
        <c:if test="${step == 2}">
            <div class="card card-custom shadow-sm mb-4">
                <div class="card-body p-4">
                    <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
                        <div>
                            <h4 class="fw-bold text-dark mb-1">
                                <i class="bi bi-calendar-check text-primary me-2"></i>Xem trước ${fn:length(result.sessions)} buổi của lớp ${result.klass.code}
                            </h4>
                            <p class="text-secondary small mb-0">
                                Khung giờ: <strong>${result.pattern.start} – ${result.pattern.end}</strong> | 
                                Bắt đầu từ <strong>${result.formattedFirstDate}</strong> đến <strong>${result.formattedLastDate}</strong>
                                <c:if test="${fn:length(result.skipped) > 0}">
                                    | <span class="badge bg-warning text-dark"><i class="bi bi-exclamation-triangle me-1"></i>Bỏ qua ${fn:length(result.skipped)} ngày nghỉ lễ</span>
                                </c:if>
                            </p>
                        </div>
                        <div class="d-flex gap-2 mt-2 mt-md-0">
                            <form action="${pageContext.request.contextPath}/schedule/generate" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="back" />
                                <button type="submit" class="btn btn-outline-secondary px-3">
                                    <i class="bi bi-arrow-left me-1"></i>Sửa mẫu lặp
                                </button>
                            </form>
                            <form action="${pageContext.request.contextPath}/schedule/generate" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="save" />
                                <button type="submit" class="btn btn-success px-4 fw-bold">
                                    <i class="bi bi-check-circle me-1"></i>Lưu thời khoá biểu
                                </button>
                            </form>
                        </div>
                    </div>

                    <!-- Thống kê tóm tắt dự kiến -->
                    <div class="row g-3 mb-4">
                        <div class="col-md-3">
                            <div class="p-3 bg-light rounded text-center border">
                                <span class="text-muted small d-block">Buổi đầu</span>
                                <strong class="fs-6 text-primary">${result.formattedFirstDate}</strong>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="p-3 bg-light rounded text-center border">
                                <span class="text-muted small d-block">Buổi cuối</span>
                                <strong class="fs-6 text-primary">${result.formattedLastDate}</strong>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="p-3 bg-light rounded text-center border">
                                <span class="text-muted small d-block">Số buổi mỗi tuần</span>
                                <strong class="fs-6 text-dark">${result.sessionsPerWeek} buổi / tuần</strong>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="p-3 bg-light rounded text-center border">
                                <span class="text-muted small d-block">Ngày lễ bị bỏ qua</span>
                                <strong class="fs-6 text-danger">${fn:length(result.skipped)} ngày nghỉ</strong>
                            </div>
                        </div>
                    </div>

                    <!-- Tabs chọn lọc: Tất cả buổi vs Ngày bị bỏ qua -->
                    <ul class="nav nav-tabs mb-3" id="previewTabs" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active fw-bold" id="all-sessions-tab" data-bs-toggle="tab" data-bs-target="#all-sessions" type="button" role="tab">
                                <i class="bi bi-list-check me-1"></i>Tất cả buổi học (${fn:length(result.sessions)})
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link fw-bold text-danger" id="skipped-tab" data-bs-toggle="tab" data-bs-target="#skipped-days" type="button" role="tab">
                                <i class="bi bi-calendar-x me-1"></i>Ngày bị bỏ qua do nghỉ lễ (${fn:length(result.skipped)})
                            </button>
                        </li>
                    </ul>

                    <div class="tab-content" id="previewTabsContent">
                        <!-- Tab 1: Danh sách tất cả buổi học -->
                        <div class="tab-pane fade show active" id="all-sessions" role="tabpanel">
                            <div class="table-responsive">
                                <table class="table table-hover align-middle table-sessions border">
                                    <thead>
                                        <tr>
                                            <th class="text-center" style="width: 70px;">Buổi</th>
                                            <th>Ngày học</th>
                                            <th>Giờ học</th>
                                            <th>Môn · Bài số</th>
                                            <th>Chủ đề bài học</th>
                                            <th>Giảng viên</th>
                                            <th>Phòng học</th>
                                            <th class="text-center">Kiểm tra</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="s" items="${result.sessions}">
                                            <tr>
                                                <td class="text-center fw-bold text-primary">${s.no}</td>
                                                <td class="fw-bold">${s.shortDisplayDate}</td>
                                                <td><span class="badge bg-light text-dark border">${s.start} – ${s.end}</span></td>
                                                <td>
                                                    <span class="badge bg-secondary me-1">${s.subject}</span>
                                                    <strong>Bài ${s.lesson}</strong>
                                                </td>
                                                <td>${s.topic}</td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${not empty s.teacher}">
                                                            <i class="bi bi-person-check text-success me-1"></i>${s.teacher}
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-danger small">Chưa phân công</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td><span class="badge bg-light text-dark border">${empty s.room ? '–' : s.room}</span></td>
                                                <td class="text-center">
                                                    <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                        <i class="bi bi-check2"></i> Hợp lệ
                                                    </span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <!-- Tab 2: Danh sách ngày bị bỏ qua -->
                        <div class="tab-pane fade" id="skipped-days" role="tabpanel">
                            <c:choose>
                                <c:when test="${empty result.skipped}">
                                    <div class="alert alert-info my-3">
                                        <i class="bi bi-info-circle me-1"></i> Không có buổi học nào rơi vào ngày nghỉ lễ trong suốt khoá học này.
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="table-responsive">
                                        <table class="table table-hover align-middle border">
                                            <thead class="table-light">
                                                <tr>
                                                    <th>Ngày bị bỏ qua</th>
                                                    <th>Thứ</th>
                                                    <th>Nguyên nhân nghỉ lễ</th>
                                                    <th>Xử lý tự động của hệ thống</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <c:forEach var="sk" items="${result.skipped}">
                                                    <tr class="skipped-row">
                                                        <td class="fw-bold">${sk.formattedDate}</td>
                                                        <td><span class="badge bg-warning text-dark">${sk.dayOfWeek}</span></td>
                                                        <td><strong>${sk.reason}</strong></td>
                                                        <td>
                                                            <i class="bi bi-arrow-right-circle text-primary me-1"></i>
                                                            Tự động lùi sang buổi học kế tiếp, đảm bảo không hụt số tiết.
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

                    <!-- Footer actions -->
                    <div class="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
                        <form action="${pageContext.request.contextPath}/schedule/generate" method="POST">
                            <input type="hidden" name="action" value="back" />
                            <button type="submit" class="btn btn-outline-secondary px-4">
                                <i class="bi bi-pencil me-1"></i>Sửa lại mẫu lặp
                            </button>
                        </form>
                        <form action="${pageContext.request.contextPath}/schedule/generate" method="POST">
                            <input type="hidden" name="action" value="save" />
                            <button type="submit" class="btn btn-success px-4 py-2 fw-bold shadow-sm">
                                <i class="bi bi-check-circle-fill me-1"></i>Xác nhận lưu thời khoá biểu
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- =================================================================== -->
        <!-- BƯỚC 3: ĐÃ LƯU THỜI KHOÁ BIỂU THÀNH CÔNG -->
        <!-- =================================================================== -->
        <c:if test="${step == 3}">
            <div class="card card-custom shadow-sm text-center p-5 mb-4">
                <div class="mb-3">
                    <i class="bi bi-check-circle-fill text-success" style="font-size: 4rem;"></i>
                </div>
                <h3 class="fw-bold text-dark mb-2">
                    Đã lưu thành công ${fn:length(savedSessions)} buổi cho lớp ${selectedClass.code}!
                </h3>
                <p class="text-muted fs-6 mb-4">
                    Thời khoá biểu bắt đầu từ <strong>${result.formattedFirstDate}</strong> đến <strong>${result.formattedLastDate}</strong>.
                    Toàn bộ giảng viên và bài học theo syllabus đã được liên kết chính xác.
                </p>
                <div class="d-flex justify-content-center gap-3">
                    <a href="${pageContext.request.contextPath}/schedule/generate" class="btn btn-primary px-4 py-2 fw-bold">
                        <i class="bi bi-calendar-plus me-1"></i>Sinh lịch cho lớp khác
                    </a>
                    <a href="${pageContext.request.contextPath}/training-programs" class="btn btn-outline-secondary px-4 py-2">
                        <i class="bi bi-arrow-return-left me-1"></i>Về danh sách lớp
                    </a>
                </div>
            </div>
        </c:if>

    </div>

    <!-- Bootstrap 5 JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <!-- JavaScript hỗ trợ tương tác nhanh trên form -->
    <script>
        function setQuickDays(dayArray) {
            document.querySelectorAll('.dow-check').forEach(function(chk) {
                var val = parseInt(chk.value, 10);
                chk.checked = dayArray.indexOf(val) !== -1;
            });
        }

        function setShift(start, end, btn) {
            document.getElementById('hiddenStart').value = start;
            document.getElementById('hiddenEnd').value = end;
            document.getElementById('startTimeInput').value = start;
            document.getElementById('endTimeInput').value = end;
            document.getElementById('customTimeBlock').style.display = 'none';

            document.querySelectorAll('.shift-btn').forEach(function(b) {
                b.classList.remove('active');
            });
            if (btn) btn.classList.add('active');
        }

        function toggleCustomTime() {
            var block = document.getElementById('customTimeBlock');
            if (block.style.display === 'none' || block.style.display === '') {
                block.style.display = 'flex';
                document.querySelectorAll('.shift-btn').forEach(function(b) {
                    b.classList.remove('active');
                });
            } else {
                block.style.display = 'none';
            }
        var startInput = document.getElementById('startTimeInput');
        if (startInput) {
            startInput.addEventListener('change', function() {
                var hidden = document.getElementById('hiddenStart');
                if (hidden) hidden.value = this.value;
            });
        }
        var endInput = document.getElementById('endTimeInput');
        if (endInput) {
            endInput.addEventListener('change', function() {
                var hidden = document.getElementById('hiddenEnd');
                if (hidden) hidden.value = this.value;
            });
        }
    </script>
</body>
</html>
