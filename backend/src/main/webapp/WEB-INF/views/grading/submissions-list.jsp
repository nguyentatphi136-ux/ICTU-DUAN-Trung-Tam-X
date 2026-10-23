<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh sách bài nộp của lớp - ${assignment.classCode} | EMS Đào tạo</title>
    <!-- Bootstrap 5 & Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-color: #f8fafc;
            color: #0f172a;
        }
        .hero-banner {
            background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
            color: white;
            border-radius: 16px;
            padding: 28px;
            box-shadow: 0 10px 25px rgba(30, 58, 138, 0.12);
        }
        .stat-card {
            background: #ffffff;
            border-radius: 12px;
            padding: 18px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 2px 6px rgba(0,0,0,0.02);
            height: 100%;
        }
        .table-card {
            background: #ffffff;
            border-radius: 14px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 4px 15px rgba(0,0,0,0.03);
            overflow: hidden;
        }
    </style>
</head>
<body>

    <!-- Navbar Hệ thống -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container">
            <a class="navbar-brand fw-bold" href="${pageContext.request.contextPath}/dashboard.jsp">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS ĐÀO TẠO
            </a>
            <div class="navbar-nav me-auto">
                <a class="nav-link" href="${pageContext.request.contextPath}/dashboard.jsp">Trang tổng quan</a>
                <a class="nav-link active fw-bold text-info" href="${pageContext.request.contextPath}/assignment/submissions?assignmentId=${assignment.id}">
                    <i class="bi bi-card-checklist me-1"></i>Chấm bài nộp (S6-01)
                </a>
                <a class="nav-link" href="${pageContext.request.contextPath}/grade/list">Điểm số</a>
                <a class="nav-link" href="${pageContext.request.contextPath}/schedule/generate">Thời khoá biểu</a>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3 small">
                    <i class="bi bi-person-check-fill text-success me-1"></i>${currentUser.name} 
                    <span class="badge bg-primary ms-1">Giảng viên</span>
                </span>
                <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-danger btn-sm">
                    <i class="bi bi-box-arrow-right"></i> Đăng xuất
                </a>
            </div>
        </div>
    </nav>

    <div class="container pb-5">
        <!-- Thông báo Flash message nếu có -->
        <c:if test="${not empty param.success}">
            <div class="alert alert-success alert-dismissible fade show shadow-sm mb-4" role="alert">
                <i class="bi bi-check-circle-fill me-2"></i>${param.success}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Banner thông tin bài tập -->
        <div class="hero-banner mb-4">
            <div class="row align-items-center">
                <div class="col-lg-8">
                    <div class="badge bg-light text-dark mb-2 px-3 py-1 fw-bold">
                        Lớp: ${assignment.classCode} &bull; Môn: ${assignment.subjectName}
                    </div>
                    <h3 class="fw-bold mb-2">${assignment.title}</h3>
                    <p class="mb-0 text-white-50">
                        Giảng viên phụ trách: <strong>${assignment.instructorName}</strong> &bull; Hạn nộp: <strong>${assignment.dueDate}</strong> &bull; Điểm tối đa: <strong>${assignment.maxScore}</strong>
                    </p>
                </div>
                <div class="col-lg-4 text-lg-end mt-3 mt-lg-0">
                    <a href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=HV260101"
                       class="btn btn-warning text-dark fw-bold px-4 py-2 shadow-sm">
                        <i class="bi bi-play-circle-fill me-1"></i> Bắt đầu chấm bài (Bài 1/25)
                    </a>
                </div>
            </div>
        </div>

        <!-- Thống kê tiến độ chấm cả lớp (25 học viên) -->
        <div class="row g-3 mb-4">
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">Tổng số học viên / bài nộp</div>
                    <div class="fs-3 fw-bold text-dark mt-1">${stats.total} bài</div>
                    <div class="progress mt-2" style="height: 6px;">
                        <div class="progress-bar bg-primary" style="width: 100%;"></div>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">Tiến độ chấm bài</div>
                    <div class="fs-3 fw-bold text-primary mt-1">${stats.graded} / ${stats.total} bài</div>
                    <div class="progress mt-2" style="height: 6px;">
                        <div class="progress-bar bg-success" role="progressbar" data-progress="${stats.progressPercent}" aria-valuenow="${stats.progressPercent}" aria-valuemin="0" aria-valuemax="100"></div>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">Đã công bố cho học viên</div>
                    <div class="fs-3 fw-bold text-success mt-1">${stats.published} bài</div>
                    <div class="text-muted small mt-1">Học viên đã tra cứu được điểm</div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card">
                    <div class="text-muted small">Bài nộp muộn</div>
                    <div class="fs-3 fw-bold text-danger mt-1">${stats.lateCount} bài</div>
                    <div class="text-muted small mt-1">Gắn nhãn nộp trễ hạn</div>
                </div>
            </div>
        </div>

        <!-- Bảng danh sách 25 bài nộp -->
        <div class="table-card">
            <div class="p-3 bg-white border-bottom d-flex justify-content-between align-items-center">
                <h5 class="mb-0 fw-bold text-dark">
                    <i class="bi bi-people-fill text-primary me-2"></i>Danh sách bài nộp cả lớp (${submissions.size()} bài)
                </h5>
                <form action="${pageContext.request.contextPath}/assignment/grading/publish-all" method="POST" onsubmit="return confirm('Bạn có chắc muốn CÔNG BỐ toàn bộ điểm đã chấm cho cả lớp?');">
                    <input type="hidden" name="assignmentId" value="${assignment.id}">
                    <button type="submit" class="btn btn-outline-success btn-sm">
                        <i class="bi bi-send-check me-1"></i> Công bố tất cả bài đã chấm
                    </button>
                </form>
            </div>

            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3">#</th>
                            <th>Học viên</th>
                            <th>Mã HV</th>
                            <th>Phiên bản</th>
                            <th>Thời điểm nộp</th>
                            <th>Repo & Tệp</th>
                            <th>Điểm số</th>
                            <th>Trạng thái</th>
                            <th class="text-end pe-3">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="sub" items="${submissions}" varStatus="loop">
                            <tr>
                                <td class="ps-3 text-muted">${loop.index + 1}</td>
                                <td>
                                    <div class="fw-bold text-dark">${sub.studentName}</div>
                                    <small class="text-muted">${sub.studentEmail}</small>
                                </td>
                                <td><span class="badge bg-light text-dark border">${sub.studentCode}</span></td>
                                <td><span class="badge bg-light text-secondary border">Bản ${sub.versionNo}</span></td>
                                <td>
                                    <div>${sub.submittedAt}</div>
                                    <c:choose>
                                        <c:when test="${sub.late}">
                                            <span class="badge bg-danger-subtle text-danger small">Trễ ${sub.lateHours}h</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-success-subtle text-success small">Đúng hạn</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${not empty sub.repoUrl}">
                                        <a href="${sub.repoUrl}" target="_blank" class="badge bg-dark text-white text-decoration-none me-1" title="Mở repo Github">
                                            <i class="bi bi-github"></i> Repo
                                        </a>
                                    </c:if>
                                    <span class="badge bg-light text-dark border">${sub.files.size()} tệp</span>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty sub.grade}">
                                            <span class="fw-bold fs-6 text-primary">${sub.grade.formattedScore}</span>
                                            <small class="text-muted">/ ${assignment.maxScore}</small>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">–</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${sub.published}">
                                            <span class="badge bg-success"><i class="bi bi-check-circle-fill me-1"></i>Đã công bố</span>
                                        </c:when>
                                        <c:when test="${not empty sub.grade}">
                                            <span class="badge bg-warning text-dark"><i class="bi bi-pencil-fill me-1"></i>Lưu nháp</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary"><i class="bi bi-hourglass me-1"></i>Chưa chấm</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end pe-3">
                                    <a href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=${sub.studentCode}"
                                       class="btn btn-sm btn-primary">
                                        <i class="bi bi-pencil-square me-1"></i> Chấm bài
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.querySelectorAll('.progress-bar[data-progress]').forEach(function(el) {
            el.style.width = el.getAttribute('data-progress') + '%';
        });
    </script>
</body>
</html>
