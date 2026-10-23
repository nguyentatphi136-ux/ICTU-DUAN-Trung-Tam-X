<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kết quả bài nộp của tôi | EMS Đào tạo</title>
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
        .student-portal-card {
            background: #ffffff;
            border-radius: 16px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.04);
            overflow: hidden;
        }
        .score-hero-box {
            background: linear-gradient(135deg, #059669 0%, #10b981 100%);
            color: white;
            border-radius: 14px;
            padding: 24px;
            text-align: center;
        }
        .score-pending-box {
            background: linear-gradient(135deg, #475569 0%, #64748b 100%);
            color: white;
            border-radius: 14px;
            padding: 24px;
            text-align: center;
        }
    </style>
</head>
<body>

    <!-- Mock Student Portal Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4">
        <div class="container">
            <a class="navbar-brand fw-bold" href="#">
                <i class="bi bi-mortarboard-fill text-warning me-2"></i>EMS HỌC VIÊN
            </a>
            <div class="navbar-nav me-auto">
                <a class="nav-link" href="#">Trang cá nhân</a>
                <a class="nav-link active fw-bold" href="#">Bài tập của tôi</a>
                <a class="nav-link" href="#">Thời khoá biểu</a>
            </div>
            <div class="d-flex align-items-center text-white">
                <span class="me-3 small">
                    <i class="bi bi-person-fill me-1"></i>${sub.studentName} (${sub.studentCode})
                    <span class="badge bg-secondary ms-1">Học viên</span>
                </span>
                <a href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=${sub.studentCode}"
                   class="btn btn-outline-warning btn-sm">
                    <i class="bi bi-arrow-return-left me-1"></i> Về màn hình giảng viên
                </a>
            </div>
        </div>
    </nav>

    <div class="container py-4" style="max-width: 860px;">
        <div class="student-portal-card">
            <!-- Header bài tập -->
            <div class="p-4 border-bottom bg-light">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="badge bg-primary px-3 py-1">Lớp ${assignment.classCode}</span>
                    <span class="text-muted small">Môn học: <strong>${assignment.subjectName}</strong></span>
                </div>
                <h4 class="fw-bold text-dark mb-1">${assignment.title}</h4>
                <div class="text-muted small">
                    Giảng viên: <strong>${assignment.instructorName}</strong> &bull; Hạn nộp: <strong>${assignment.dueDate}</strong>
                </div>
            </div>

            <!-- Thân kết quả bài nộp -->
            <div class="p-4">
                <!-- Thông tin bài nộp của học viên -->
                <div class="card mb-4 border bg-light">
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="text-muted small">Mã học viên:</div>
                                <div class="fw-bold">${sub.studentName} (${sub.studentCode})</div>
                            </div>
                            <div class="col-md-6">
                                <div class="text-muted small">Thời điểm nộp bài:</div>
                                <div class="fw-bold">
                                    ${sub.submittedAt} &bull; Bản ${sub.versionNo}
                                    <c:choose>
                                        <c:when test="${sub.late}">
                                            <span class="badge bg-danger ms-1">Nộp trễ ${sub.lateHours}h</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-success ms-1">Đúng hạn</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                        <c:if test="${not empty sub.repoUrl}">
                            <div class="mt-3">
                                <span class="text-muted small">Liên kết kho lưu trữ:</span>
                                <a href="${sub.repoUrl}" target="_blank" class="fw-bold text-primary ms-1">
                                    <i class="bi bi-github me-1"></i>${sub.repoUrl}
                                </a>
                            </div>
                        </c:if>
                    </div>
                </div>

                <!-- ======================================================= -->
                <!-- KIỂM ĐỊNH YÊU CẦU: ĐIỂM CHỈ HIỂN THỊ KHI ĐÃ CÔNG BỐ -->
                <!-- ======================================================= -->
                <c:choose>
                    <c:when test="${sub.published}">
                        <!-- ĐIỂM ĐÃ CÔNG BỐ: Học viên được xem đầy đủ điểm và nhận xét -->
                        <div class="score-hero-box mb-4 shadow-sm">
                            <div class="text-uppercase small tracking-wider fw-bold text-white-50 mb-1">
                                <i class="bi bi-check-circle-fill me-1"></i>KẾT QUẢ ĐÃ CÔNG BỐ
                            </div>
                            <div class="display-3 fw-bold mb-1">${sub.grade.formattedScore}</div>
                            <div class="fs-6 text-white-50">Thang điểm tối đa: ${assignment.maxScore}</div>
                            <div class="small mt-2 pt-2 border-top border-white-50">
                                Công bố lúc: <strong>${sub.grade.publishedAt}</strong> bởi <strong>${sub.grade.publishedBy}</strong>
                            </div>
                        </div>

                        <!-- Nhận xét chi tiết của giảng viên -->
                        <div class="card border-0 shadow-sm border-start border-4 border-success p-3 mb-4">
                            <h6 class="fw-bold text-success mb-2">
                                <i class="bi bi-chat-dots-fill me-1"></i>Nhận xét từ giảng viên:
                            </h6>
                            <p class="mb-0 text-dark lh-base">${sub.grade.comment}</p>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <!-- ĐIỂM CHƯA CÔNG BỐ HOẶC ĐANG LƯU NHÁP: Tuyệt đối giấu điểm và nhận xét -->
                        <div class="score-pending-box mb-4 shadow-sm">
                            <div class="display-6 fw-bold mb-2">
                                <i class="bi bi-hourglass-split me-2 text-warning"></i>Đang chấm bài
                            </div>
                            <p class="mb-2 text-white-50">
                                Bài nộp của bạn đã được hệ thống ghi nhận thành công và đang trong quá trình chấm điểm.
                            </p>
                            <div class="badge bg-warning text-dark px-3 py-2 fs-6">
                                <i class="bi bi-shield-lock-fill me-1"></i>Điểm chưa công bố
                            </div>
                        </div>

                        <div class="alert alert-info border-0 shadow-sm py-3">
                            <div class="d-flex align-items-center">
                                <i class="bi bi-info-circle-fill fs-3 text-info me-3"></i>
                                <div>
                                    <h6 class="fw-bold mb-1">Quy định hiển thị điểm số (Acceptance Criteria S6-01):</h6>
                                    <div class="small text-secondary">
                                        "Điểm chỉ hiển thị cho học viên sau khi giảng viên bấm <strong>Công bố</strong>". Dù giảng viên đã nhập điểm nháp trên hệ thống, học viên vẫn chỉ thấy bài ở trạng thái <em>Đã nộp bài</em>.
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>

                <!-- Danh sách các tệp nộp -->
                <h6 class="fw-bold text-dark mt-4 mb-2"><i class="bi bi-paperclip me-1"></i>Các tệp bạn đã nộp:</h6>
                <ul class="list-group list-group-flush border rounded">
                    <c:forEach var="f" items="${sub.files}">
                        <li class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <i class="bi bi-file-earmark-text text-primary me-2"></i>
                                <span class="fw-semibold">${f.name}</span>
                            </div>
                            <span class="badge bg-light text-muted border">${f.formattedSize}</span>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
