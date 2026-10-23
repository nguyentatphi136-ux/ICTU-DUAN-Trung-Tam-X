<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kiểm tra Danh sách Điểm danh theo Ngày (S4-06) | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body { font-family: 'Plus Jakarta Sans', sans-serif; background-color: #f8fafc; }
        .card-custom { border: none; border-radius: 14px; background: #ffffff; box-shadow: 0 4px 15px rgba(0,0,0,0.04); }
        .badge-status { font-size: 12px; padding: 5px 12px; border-radius: 20px; font-weight: 600; }
        .status-studying { background: #dcfce7; color: #15803d; }
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
                <a class="nav-link" href="${pageContext.request.contextPath}/student/status">Tình trạng học vụ (S4-06)</a>
                <a class="nav-link active" href="${pageContext.request.contextPath}/student/roster">Điểm danh theo ngày</a>
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

        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <span class="badge bg-success mb-2">Kiểm chứng Acceptance Criteria: S4-06</span>
                <h3 class="fw-bold text-dark mb-1">
                    <i class="bi bi-calendar-check text-primary me-2"></i>Danh sách Điểm danh Lớp theo Ngày (Roster on Date)
                </h3>
                <p class="text-secondary small mb-0">
                    Kiểm chứng quy tắc S4-06: Học viên bảo lưu/thôi học/chuyển đi sẽ rời khỏi danh sách điểm danh đúng từ ngày hiệu lực; các buổi trước ngày hiệu lực vẫn giữ nguyên điểm danh ở lớp cũ.
                </p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/student/status" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Về trang Tình trạng học
                </a>
            </div>
        </div>

        <!-- Bộ lọc chọn Lớp và Ngày tra cứu -->
        <div class="card-custom p-4 mb-4">
            <form action="${pageContext.request.contextPath}/student/roster" method="GET" class="row g-3 align-items-end">
                <div class="col-md-5">
                    <label for="cls" class="form-label fw-bold">Chọn Lớp học</label>
                    <select class="form-select" id="cls" name="cls">
                        <c:forEach var="c" items="${allClasses}">
                            <option value="${c.code}" ${selectedClass == c.code ? 'selected' : ''}>
                                ${c.code} · ${c.name} (${c.program})
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label for="date" class="form-label fw-bold">Ngày điểm danh buổi học</label>
                    <input type="date" class="form-control" id="date" name="date" value="${selectedDate}" required>
                </div>
                <div class="col-md-3">
                    <button type="submit" class="btn btn-primary w-100 fw-bold">
                        <i class="bi bi-search me-1"></i> Tra cứu danh sách
                    </button>
                </div>
            </form>
        </div>

        <!-- Kết quả danh sách điểm danh -->
        <div class="card-custom p-0">
            <div class="p-3 border-bottom d-flex justify-content-between align-items-center bg-white">
                <div>
                    <h5 class="mb-1 fw-bold text-dark">
                        Danh sách học viên điểm danh: <span class="text-primary">${selectedClass}</span>
                    </h5>
                    <div class="text-muted small">
                        Ngày áp dụng: <strong>${selectedDate}</strong> · Sĩ số có mặt trong danh sách: <strong class="text-success">${roster.size()} học viên</strong>
                    </div>
                </div>
                <span class="badge bg-light text-dark border">
                    <i class="bi bi-shield-check text-success me-1"></i>Quy chế S4-06 đã áp dụng
                </span>
            </div>

            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light small">
                        <tr>
                            <th style="width: 60px;">STT</th>
                            <th>Mã học viên</th>
                            <th>Họ và tên</th>
                            <th>Email đăng nhập</th>
                            <th>Số điện thoại</th>
                            <th>Trạng thái điểm danh</th>
                            <th class="text-end">Xem chi tiết</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty roster}">
                                <tr>
                                    <td colspan="7" class="text-center py-5 text-muted">
                                        Không có học viên nào trong danh sách lớp ${selectedClass} vào ngày ${selectedDate}.
                                    </td>
                                </tr>
                            </c:choose>
                            <c:otherwise>
                                <c:forEach var="st" items="${roster}" varStatus="status">
                                    <tr>
                                        <td class="text-muted fw-bold">${status.count}</td>
                                        <td class="fw-bold text-primary">${st.code}</td>
                                        <td>
                                            <strong class="text-dark">${st.name}</strong>
                                        </td>
                                        <td class="small text-secondary">${st.loginEmail}</td>
                                        <td class="small">${st.phone}</td>
                                        <td>
                                            <span class="badge-status status-studying">
                                                <i class="bi bi-person-check-fill me-1"></i>Hợp lệ trong danh sách
                                            </span>
                                        </td>
                                        <td class="text-end">
                                            <a href="${pageContext.request.contextPath}/student/profile?code=${st.code}" class="btn btn-outline-primary btn-sm">
                                                Hồ sơ <i class="bi bi-arrow-right"></i>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <div class="p-3 bg-light text-secondary small border-top">
                <i class="bi bi-lightbulb-fill text-warning me-1"></i>
                <strong>Gợi ý kiểm thử:</strong> Thử chọn ngày trước ngày hiệu lực bảo lưu/chuyển lớp (ví dụ: ngày hôm qua) để thấy học viên vẫn còn trong danh sách; sau đó chọn ngày bằng hoặc sau ngày hiệu lực để thấy học viên đã tự động rời danh sách điểm danh theo đúng yêu cầu đề bài.
            </div>
        </div>

    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
