<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Chấm điểm học viên | EMS</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body class="bg-light py-5">
    <div class="container" style="max-width: 600px;">
        <div class="card shadow-sm border-0">
            <div class="card-header bg-primary text-white py-3">
                <h5 class="mb-0 fw-bold"><i class="bi bi-pencil-square me-2"></i>Cập nhật điểm số học viên</h5>
            </div>
            <div class="card-body p-4">
                <form action="${pageContext.request.contextPath}/grade/update" method="POST">
                    <input type="hidden" name="id" value="${grade.id}">

                    <div class="mb-3">
                        <label class="form-label text-muted">Học viên</label>
                        <input type="text" class="form-control bg-light" value="${grade.studentName}" readonly>
                    </div>

                    <div class="mb-3">
                        <label class="form-label text-muted">Môn học</label>
                        <input type="text" class="form-control bg-light" value="${grade.subjectName}" readonly>
                    </div>

                    <div class="mb-3">
                        <label class="form-label text-muted">Đầu điểm</label>
                        <input type="text" class="form-control bg-light" value="${grade.componentName}" readonly>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-bold">Điểm số (Thang điểm 10)</label>
                        <input type="number" step="0.1" min="0" max="10" name="score" class="form-control" value="${grade.score}" required>
                    </div>

                    <div class="mb-4">
                        <label class="form-label fw-bold">Nhận xét của giảng viên</label>
                        <textarea name="notes" rows="3" class="form-control">${grade.notes}</textarea>
                    </div>

                    <div class="d-flex justify-content-between">
                        <a href="${pageContext.request.contextPath}/grade/list" class="btn btn-outline-secondary">
                            <i class="bi bi-arrow-left me-1"></i> Quay lại
                        </a>
                        <button type="submit" class="btn btn-success">
                            <i class="bi bi-check-lg me-1"></i> Lưu điểm số
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
