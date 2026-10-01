<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Cập nhật học phí | EMS</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
</head>
<body class="bg-light py-5">
    <div class="container" style="max-width: 600px;">
        <div class="card shadow-sm border-0">
            <div class="card-header bg-success text-white py-3">
                <h5 class="mb-0 fw-bold"><i class="bi bi-wallet-fill me-2"></i>Ghi nhận & Cập nhật học phí</h5>
            </div>
            <div class="card-body p-4">
                <form action="${pageContext.request.contextPath}/tuition/update" method="POST">
                    <input type="hidden" name="id" value="${tuition.id}">

                    <div class="mb-3">
                        <label class="form-label text-muted">Học viên</label>
                        <input type="text" class="form-control bg-light" value="${tuition.studentName}" readonly>
                    </div>

                    <div class="mb-3">
                        <label class="form-label text-muted">Khóa học</label>
                        <input type="text" class="form-control bg-light" value="${tuition.courseName}" readonly>
                    </div>

                    <div class="mb-3">
                        <label class="form-label text-muted">Tổng học phí</label>
                        <input type="text" class="form-control bg-light" value="${tuition.totalAmount} VNĐ" readonly>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-bold">Số tiền đã thanh toán (VNĐ)</label>
                        <input type="number" step="100000" min="0" name="paidAmount" class="form-control" value="${tuition.paidAmount}" required>
                    </div>

                    <div class="mb-4">
                        <label class="form-label fw-bold">Trạng thái công nợ</label>
                        <select name="status" class="form-select">
                            <option value="partially_paid" ${tuition.status == 'partially_paid' ? 'selected' : ''}>Còn nợ học phí</option>
                            <option value="paid" ${tuition.status == 'paid' ? 'selected' : ''}>Đã thanh toán đủ (Hoàn thành)</option>
                            <option value="unpaid" ${tuition.status == 'unpaid' ? 'selected' : ''}>Chưa thanh toán</option>
                        </select>
                    </div>

                    <div class="d-flex justify-content-between">
                        <a href="${pageContext.request.contextPath}/tuition/list" class="btn btn-outline-secondary">
                            <i class="bi bi-arrow-left me-1"></i> Quay lại
                        </a>
                        <button type="submit" class="btn btn-success">
                            <i class="bi bi-check-lg me-1"></i> Lưu thông tin học phí
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
