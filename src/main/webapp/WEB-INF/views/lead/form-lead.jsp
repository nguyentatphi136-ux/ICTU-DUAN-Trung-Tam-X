<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${action == 'add' ? 'Thêm mới' : 'Chỉnh sửa'} Khách hàng tiềm năng | EMS Đào tạo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <style>
        .form-card {
            border-radius: 12px;
            overflow: hidden;
        }
        .required-star {
            color: #dc3545;
            font-weight: bold;
        }
    </style>
</head>
<body class="bg-light py-4">
    <div class="container" style="max-width: 780px;">
        <!-- Header điều hướng -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <a href="${pageContext.request.contextPath}/lead/list" class="btn btn-outline-secondary btn-sm">
                <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách lead
            </a>
            <span class="text-muted small">
                User Story S2-09 · EP-03 · Tư vấn tuyển sinh
            </span>
        </div>

        <!-- CẢNH BÁO SỐ ĐIỆN THOẠI TRÙNG LẶP (Tiêu chí AC 2 của S2-09) -->
        <c:if test="${duplicateWarning}">
            <div class="card border-warning shadow-sm mb-4 bg-warning-subtle">
                <div class="card-body p-4">
                    <div class="d-flex align-items-start">
                        <i class="bi bi-exclamation-triangle-fill text-warning fs-1 me-3"></i>
                        <div class="flex-grow-1">
                            <h5 class="card-title fw-bold text-dark mb-2">
                                <i class="bi bi-telephone-x me-1 text-danger"></i> CẢNH BÁO: TRÙNG LẶP SỐ ĐIỆN THOẠI
                            </h5>
                            <p class="card-text text-dark mb-3">
                                Số điện thoại <strong>${lead.phone}</strong> vừa nhập đã tồn tại trong hệ thống với khách hàng:
                            </p>
                            <div class="bg-white p-3 rounded border border-warning mb-3">
                                <div class="row">
                                    <div class="col-sm-6">
                                        <small class="text-muted d-block">Khách hàng trùng:</small>
                                        <strong class="text-primary fs-6">${duplicateLead.fullName}</strong>
                                    </div>
                                    <div class="col-sm-3">
                                        <small class="text-muted d-block">Mã Lead:</small>
                                        <span class="badge bg-secondary">#${duplicateLead.id}</span>
                                    </div>
                                    <div class="col-sm-3">
                                        <small class="text-muted d-block">Trạng thái:</small>
                                        <span class="badge bg-info text-dark">${duplicateLead.status}</span>
                                    </div>
                                </div>
                                <c:if test="${not empty duplicateLead.notes}">
                                    <div class="mt-2 text-muted small border-top pt-2">
                                        <strong>Ghi chú trước đó:</strong> ${duplicateLead.notes}
                                    </div>
                                </c:if>
                            </div>

                            <div class="d-flex flex-wrap gap-2 align-items-center">
                                <!-- Nút xác nhận vẫn muốn lưu dù trùng -->
                                <button type="button" class="btn btn-warning fw-bold text-dark" onclick="submitForceDuplicate()">
                                    <i class="bi bi-check2-circle me-1"></i> Tôi hiểu, vẫn lưu khách hàng này
                                </button>
                                <!-- Nút quay lại sửa SĐT -->
                                <button type="button" class="btn btn-outline-secondary" onclick="focusPhoneInput()">
                                    <i class="bi bi-pencil me-1"></i> Đổi số điện thoại khác
                                </button>
                                <!-- Xem lead trùng -->
                                <a href="${pageContext.request.contextPath}/lead/edit?id=${duplicateLead.id}" target="_blank" class="btn btn-link text-decoration-none">
                                    <i class="bi bi-box-arrow-up-right me-1"></i> Xem lead đang trùng
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- Thông báo lỗi kiểm tra dữ liệu -->
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
                <i class="bi bi-exclamation-circle-fill me-2 fs-5"></i>
                <div>${errorMessage}</div>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- Thẻ Form tạo / sửa lead -->
        <div class="card shadow-sm border-0 form-card">
            <div class="card-header ${action == 'add' ? 'bg-primary' : 'bg-success'} text-white py-3">
                <h5 class="mb-0 fw-bold">
                    <c:choose>
                        <c:when test="${action == 'add'}">
                            <i class="bi bi-person-plus-fill me-2"></i>Thêm mới Khách hàng tiềm năng (Lead)
                        </c:when>
                        <c:otherwise>
                            <i class="bi bi-pencil-square me-2"></i>Cập nhật Khách hàng tiềm năng #${lead.id}
                        </c:otherwise>
                    </c:choose>
                </h5>
            </div>
            <div class="card-body p-4">
                <form id="leadForm" action="${pageContext.request.contextPath}/lead/save" method="POST">
                    <input type="hidden" name="id" value="${lead.id != null ? lead.id : 0}">
                    <input type="hidden" id="forceDuplicate" name="forceDuplicate" value="false">

                    <!-- HỌ VÀ TÊN -->
                    <div class="mb-3">
                        <label for="fullName" class="form-label fw-bold">
                            Họ và tên khách hàng <span class="required-star">*</span>
                        </label>
                        <input type="text" class="form-control" id="fullName" name="fullName" 
                               value="${lead.fullName}" placeholder="Ví dụ: Nguyễn Văn An" required maxlength="100">
                        <div class="form-text">Họ tên đầy đủ có độ dài từ 2 đến 100 ký tự.</div>
                    </div>

                    <!-- SỐ ĐIỆN THOẠI & EMAIL -->
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label for="phone" class="form-label fw-bold">
                                Số điện thoại di động <span class="required-star">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-telephone-fill"></i></span>
                                <input type="tel" class="form-control ${duplicateWarning ? 'is-invalid' : ''}" 
                                       id="phone" name="phone" value="${lead.phone}" 
                                       placeholder="Ví dụ: 0912345678" required maxlength="20">
                            </div>
                            <div class="form-text">Định dạng di động 10 số Việt Nam (đầu 03, 05, 07, 08, 09 hoặc +84).</div>
                        </div>

                        <div class="col-md-6 mb-3">
                            <label for="email" class="form-label fw-semibold">
                                Địa chỉ Email
                            </label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-envelope-fill"></i></span>
                                <input type="email" class="form-control" id="email" name="email" 
                                       value="${lead.email}" placeholder="Ví dụ: email@domain.com" maxlength="150">
                            </div>
                            <div class="form-text">Dùng để gửi thông tin học liệu và lộ trình khóa học.</div>
                        </div>
                    </div>

                    <!-- NGUỒN KHÁCH HÀNG & CHƯƠNG TRÌNH QUAN TÂM -->
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label for="source" class="form-label fw-bold">
                                Nguồn khách hàng <span class="required-star">*</span>
                            </label>
                            <select class="form-select" id="source" name="source" required>
                                <option value="WEBSITE" ${lead.source == 'WEBSITE' ? 'selected' : ''}>Website trung tâm</option>
                                <option value="FACEBOOK" ${lead.source == 'FACEBOOK' ? 'selected' : ''}>Facebook Fanpage / Chat</option>
                                <option value="REFERRAL" ${lead.source == 'REFERRAL' ? 'selected' : ''}>Người quen / Bạn bè giới thiệu</option>
                                <option value="DIRECT" ${lead.source == 'DIRECT' ? 'selected' : ''}>Khách đến trực tiếp văn phòng</option>
                                <option value="ADS" ${lead.source == 'ADS' ? 'selected' : ''}>Quảng cáo (Google, TikTok, FB Ads)</option>
                                <option value="EVENT" ${lead.source == 'EVENT' ? 'selected' : ''}>Hội thảo / Sự kiện hướng nghiệp</option>
                                <option value="OTHER" ${lead.source == 'OTHER' ? 'selected' : ''}>Khác</option>
                            </select>
                        </div>

                        <div class="col-md-6 mb-3">
                            <label for="programInterest" class="form-label fw-bold">
                                Chương trình đào tạo quan tâm
                            </label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-mortarboard"></i></span>
                                <input type="text" class="form-control" id="programInterest" name="programInterest" 
                                       value="${lead.programInterest}" list="programSuggestions"
                                       placeholder="Ví dụ: Lập trình Java Web Fullstack">
                                <datalist id="programSuggestions">
                                    <option value="Lập trình Java Web Fullstack">
                                    <option value="Lập trình Frontend ReactJS">
                                    <option value="Lập trình Python & Trí tuệ nhân tạo (AI)">
                                    <option value="Kiểm thử phần mềm (Tester / QA)">
                                    <option value="Lập trình ứng dụng di động Flutter">
                                    <option value="Tiếng Anh chuyên ngành CNTT">
                                </datalist>
                            </div>
                            <div class="form-text">Gõ để chọn gợi ý hoặc nhập tên khóa học khách mong muốn.</div>
                        </div>
                    </div>

                    <!-- TRẠNG THÁI TIẾN TRÌNH -->
                    <div class="mb-3">
                        <label for="status" class="form-label fw-bold">
                            Trạng thái tiến trình tư vấn
                        </label>
                        <select class="form-select" id="status" name="status">
                            <option value="NEW" ${lead.status == 'NEW' || empty lead.status ? 'selected' : ''}>Mới tiếp nhận (NEW)</option>
                            <option value="CONTACTED" ${lead.status == 'CONTACTED' ? 'selected' : ''}>Đã liên hệ gọi điện / nhắn tin (CONTACTED)</option>
                            <option value="CONSULTING" ${lead.status == 'CONSULTING' ? 'selected' : ''}>Đang tư vấn lộ trình học (CONSULTING)</option>
                            <option value="TRIAL" ${lead.status == 'TRIAL' ? 'selected' : ''}>Đã hẹn tham gia học thử (TRIAL)</option>
                            <option value="WON" ${lead.status == 'WON' ? 'selected' : ''}>Chốt thành công (WON)</option>
                            <option value="ENROLLED" ${lead.status == 'ENROLLED' ? 'selected' : ''}>Đã đóng học phí / Nhập học (ENROLLED)</option>
                            <option value="LOST" ${lead.status == 'LOST' ? 'selected' : ''}>Khách từ chối / Không tiềm năng (LOST)</option>
                        </select>
                    </div>

                    <!-- GHI CHÚ NHU CẦU -->
                    <div class="mb-4">
                        <label for="notes" class="form-label fw-semibold">
                            Ghi chú nhu cầu & Lịch sử trao đổi
                        </label>
                        <textarea class="form-control" id="notes" name="notes" rows="4" 
                                  placeholder="Ghi chú thời gian gọi lại thích hợp, mong muốn của khách, tài liệu đã gửi...">${lead.notes}</textarea>
                    </div>

                    <!-- NÚT THAO TÁC -->
                    <div class="d-flex justify-content-between align-items-center pt-3 border-top">
                        <a href="${pageContext.request.contextPath}/lead/list" class="btn btn-outline-secondary">
                            <i class="bi bi-x-circle me-1"></i> Hủy bỏ
                        </a>
                        <button type="submit" class="btn ${action == 'add' ? 'btn-primary' : 'btn-success'} px-4">
                            <i class="bi bi-check-lg me-1"></i> 
                            ${action == 'add' ? 'Thêm mới khách hàng' : 'Lưu thay đổi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function submitForceDuplicate() {
            document.getElementById('forceDuplicate').value = 'true';
            document.getElementById('leadForm').submit();
        }

        function focusPhoneInput() {
            const phoneInput = document.getElementById('phone');
            phoneInput.focus();
            phoneInput.select();
        }
    </script>
</body>
</html>
