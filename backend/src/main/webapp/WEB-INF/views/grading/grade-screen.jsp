<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chấm điểm - ${currentSub.studentName} | EMS Đào tạo</title>
    <!-- Bootstrap 5 & Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css">
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    
    <style>
        :root {
            --blue: #2563eb;
            --blue-hover: #1d4ed8;
            --ink: #0f172a;
            --muted: #64748b;
            --line: #e2e8f0;
            --surface: #ffffff;
            --hover: #f8fafc;
            --faint: #94a3b8;
            --r: 8px;
        }

        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background-color: #f1f5f9;
            color: var(--ink);
            min-height: 100vh;
            margin: 0;
        }

        /* Top Bar & Breadcrumb */
        .grade-top-bar {
            background: var(--surface);
            border-bottom: 1px solid var(--line);
            padding: 12px 24px;
        }

        .back-link {
            color: var(--muted);
            text-decoration: none;
            font-size: 0.88rem;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-weight: 500;
        }

        .back-link:hover {
            color: var(--blue);
        }

        /* Header block with Navigation */
        .gr-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 16px 24px;
            background: var(--surface);
            border-bottom: 1px solid var(--line);
        }

        .gr-header h2 {
            margin: 0;
            font-size: 1.35rem;
            font-weight: 700;
            color: var(--ink);
        }

        .gr-header p {
            margin: 4px 0 0;
            font-size: 0.88rem;
            color: var(--muted);
        }

        .gr-nav {
            display: flex;
            align-items: center;
            gap: 10px;
            border: 1px solid var(--line);
            border-radius: var(--r);
            background: var(--surface);
            padding: 6px 12px;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
        }

        .icon-btn {
            width: 32px;
            height: 32px;
            border-radius: 6px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border: 1px solid var(--line);
            background: var(--surface);
            color: var(--ink);
            cursor: pointer;
            transition: all 0.15s ease;
            text-decoration: none;
        }

        .icon-btn:hover:not(:disabled) {
            background: var(--hover);
            color: var(--blue);
            border-color: var(--blue);
        }

        .icon-btn:disabled, .icon-btn.disabled {
            opacity: 0.35;
            cursor: not-allowed;
            pointer-events: none;
        }

        /* Main 2-column Grid */
        .gr-grid-container {
            padding: 20px 24px;
        }

        .gr-grid {
            display: grid;
            grid-template-columns: minmax(0, 1fr) 420px;
            gap: 18px;
            align-items: start;
        }

        .panel {
            background: var(--surface);
            border: 1px solid var(--line);
            border-radius: var(--r);
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
            overflow: hidden;
        }

        /* Left Side: Viewer Panel */
        .gr-view {
            overflow: hidden;
            display: flex;
            flex-direction: column;
            min-height: 580px;
        }

        .gr-tabs {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
            padding: 8px 12px;
            border-bottom: 1px solid var(--line);
            background: var(--hover);
            align-items: center;
        }

        .gr-tab-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            border: 1px solid transparent;
            background: none;
            border-radius: 6px;
            padding: 6px 12px;
            font-size: 0.85rem;
            color: var(--ink);
            text-decoration: none;
            font-weight: 500;
            transition: all 0.15s;
        }

        .gr-tab-btn:hover {
            color: var(--blue);
            background: rgba(37, 99, 235, 0.05);
        }

        .gr-tab-btn.on {
            background: var(--surface);
            color: var(--blue);
            font-weight: 600;
            border-color: var(--line);
            box-shadow: 0 1px 2px rgba(5, 36, 77, 0.08);
        }

        .gr-repo {
            margin-left: auto;
            color: var(--blue);
            font-weight: 600;
            font-size: 0.85rem;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            border-radius: 6px;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            transition: all 0.15s;
        }

        .gr-repo:hover {
            background: #dbeafe;
            color: var(--blue-hover);
        }

        .gr-doc {
            flex-grow: 1;
            min-height: 440px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 12px;
            color: var(--muted);
            text-align: center;
            padding: 32px 24px;
        }

        .gr-doc b {
            color: var(--ink);
            font-size: 1.05rem;
        }

        .doc-preview-box {
            width: 100%;
            background: #fafafa;
            border: 1px solid var(--line);
            border-radius: 6px;
            padding: 20px;
            text-align: left;
            font-family: inherit;
            line-height: 1.65;
            max-height: 420px;
            overflow-y: auto;
            font-size: 0.9rem;
            color: #334155;
            white-space: pre-wrap;
        }

        .code-preview-box {
            width: 100%;
            background: #0f172a;
            color: #e2e8f0;
            border-radius: 6px;
            padding: 18px;
            text-align: left;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.84rem;
            line-height: 1.6;
            max-height: 420px;
            overflow-y: auto;
        }

        .gr-link {
            display: block;
            padding: 12px 18px;
            border-top: 1px solid var(--line);
            word-break: break-all;
            font-size: 0.88rem;
            color: var(--blue);
            text-decoration: none;
            background: #f8fafc;
        }

        .gr-link:hover {
            text-decoration: underline;
        }

        .gr-note {
            padding: 12px 18px;
            background: #fffbeb;
            border-top: 1px solid #fef3c7;
            color: #92400e;
            font-size: 0.86rem;
            margin: 0;
        }

        /* Right Side: Grading Form */
        .gr-form {
            padding: 20px;
        }

        .gr-form .sb-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 18px;
            padding-bottom: 12px;
            border-bottom: 1px solid var(--line);
        }

        .gr-form .sb-head h3 {
            margin: 0;
            font-size: 1.15rem;
            font-weight: 700;
        }

        .field {
            margin-bottom: 16px;
        }

        .field label {
            display: block;
            font-size: 0.86rem;
            font-weight: 600;
            margin-bottom: 6px;
            color: var(--ink);
        }

        .gr-score {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .gr-score .inp input {
            width: 110px;
            font-size: 1.35rem;
            font-weight: 700;
            text-align: center;
            border: 2px solid var(--line);
            border-radius: 8px;
            padding: 6px 10px;
            color: var(--blue);
            background: #ffffff;
        }

        .gr-score .inp input:focus {
            outline: none;
            border-color: var(--blue);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }

        .gr-score span {
            color: var(--muted);
            font-size: 1.05rem;
            font-weight: 600;
        }

        .field textarea, .field input[type="text"] {
            width: 100%;
            border: 1px solid var(--line);
            border-radius: 8px;
            padding: 10px 12px;
            font-size: 0.88rem;
            font-family: inherit;
            color: var(--ink);
            box-sizing: border-box;
        }

        .field textarea:focus, .field input[type="text"]:focus {
            outline: none;
            border-color: var(--blue);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
        }

        .hint {
            font-size: 0.8rem;
            color: var(--muted);
            margin: 4px 0 0;
        }

        /* Audit Logs List */
        .gr-log {
            margin: 16px 0;
            padding: 14px;
            background: #f8fafc;
            border: 1px solid var(--line);
            border-radius: 8px;
        }

        .gr-log .lbl {
            font-size: 0.82rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: var(--muted);
            display: block;
            margin-bottom: 8px;
        }

        .gr-log ul {
            list-style: none;
            padding: 0;
            margin: 0;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .gr-log li {
            font-size: 0.83rem;
            line-height: 1.45;
            padding-bottom: 8px;
            border-bottom: 1px dashed var(--line);
        }

        .gr-log li:last-child {
            border-bottom: none;
            padding-bottom: 0;
        }

        .gr-log b {
            display: block;
            color: var(--ink);
            font-weight: 600;
        }

        .gr-log span {
            color: var(--muted);
        }

        /* Action Buttons */
        .gr-acts {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-top: 18px;
        }

        .btn-act {
            width: 100%;
            padding: 10px 16px;
            border-radius: 8px;
            font-size: 0.9rem;
            font-weight: 600;
            cursor: pointer;
            border: 1px solid transparent;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            transition: all 0.15s ease;
        }

        .btn-act.primary {
            background: var(--blue);
            color: #ffffff;
        }

        .btn-act.primary:hover {
            background: var(--blue-hover);
        }

        .btn-act.outline {
            background: #ffffff;
            border-color: var(--line);
            color: var(--ink);
        }

        .btn-act.outline:hover {
            background: var(--hover);
        }

        .btn-act.publish {
            background: #10b981;
            color: #ffffff;
        }

        .btn-act.publish:hover {
            background: #059669;
        }

        @media (max-width: 1100px) {
            .gr-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>

    <!-- Hidden configuration element containing JSP values for clean JS consumption -->
    <div id="gradingConfig"
         class="d-none"
         data-is-published="<c:out value='${currentSub.published}'/>"
         data-original-score="<c:out value='${currentSub.grade.formattedScore}'/>"
         data-max-score="<c:out value='${assignment.maxScore}'/>"
         data-student-name="<c:out value='${currentSub.studentName}'/>"></div>

    <!-- Top Navigation Bar -->
    <div class="grade-top-bar d-flex justify-content-between align-items-center">
        <div>
            <a href="${pageContext.request.contextPath}/assignment/submissions?assignmentId=${assignment.id}" class="back-link">
                <i class="bi bi-chevron-left"></i> Danh sách bài nộp
            </a>
            <span class="mx-2 text-muted">/</span>
            <span class="text-muted small">${assignment.shortTitle}</span>
            <span class="mx-2 text-muted">/</span>
            <span class="fw-bold small text-dark">${currentSub.studentName}</span>
        </div>
        <div class="d-flex align-items-center gap-3">
            <span class="small text-muted">
                Giảng viên: <strong>${currentUser.name}</strong>
            </span>
            <a href="${pageContext.request.contextPath}/assignment/student-view?assignmentId=${assignment.id}&studentCode=${currentSub.studentCode}"
               target="_blank" class="btn btn-sm btn-outline-secondary py-1 px-2" style="font-size: 0.8rem;">
                <i class="bi bi-eye me-1"></i> Góc nhìn học viên
            </a>
        </div>
    </div>

    <!-- Header Block: Student Profile & Sequential Navigation -->
    <div class="gr-header">
        <div>
            <h2>${currentSub.studentName}</h2>
            <p>
                ${currentSub.studentCode} &bull; Bản ${currentSub.versionNo} &bull; nộp ${currentSub.submittedAt} &bull; 
                <c:choose>
                    <c:when test="${currentSub.late}">
                        <span class="text-danger fw-semibold">trễ ${currentSub.lateHours} giờ</span>
                    </c:when>
                    <c:otherwise>
                        <span class="text-success fw-semibold">đúng hạn</span>
                    </c:otherwise>
                </c:choose>
            </p>
        </div>

        <!-- Sequential Quick Navigation (S6-01: Chấm hết 25 bài trong 1 buổi tối) -->
        <div class="gr-nav">
            <c:choose>
                <c:when test="${not empty prevSub}">
                    <a id="btnPrevSub" href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=${prevSub.studentCode}"
                       class="icon-btn" title="Bài trước: ${prevSub.studentName} (Alt+←)">
                        <i class="bi bi-chevron-left"></i>
                    </a>
                </c:when>
                <c:otherwise>
                    <button class="icon-btn" disabled title="Bài trước">
                        <i class="bi bi-chevron-left"></i>
                    </button>
                </c:otherwise>
            </c:choose>

            <span class="small">
                Bài <b>${currentIndex + 1}</b> / <b>${totalCount}</b>
            </span>

            <c:choose>
                <c:when test="${not empty nextSub}">
                    <a id="btnNextSub" href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=${nextSub.studentCode}"
                       class="icon-btn" title="Bài kế tiếp: ${nextSub.studentName} (Alt+→)">
                        <i class="bi bi-chevron-right"></i>
                    </a>
                </c:when>
                <c:otherwise>
                    <button class="icon-btn" disabled title="Bài kế tiếp">
                        <i class="bi bi-chevron-right"></i>
                    </button>
                </c:otherwise>
            </c:choose>

            <!-- Menu nhảy nhanh 25 học viên -->
            <div class="dropdown ms-1">
                <button class="btn btn-sm btn-light border dropdown-toggle py-1 px-2" type="button" data-bs-toggle="dropdown" style="font-size: 0.8rem;">
                    Danh sách
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm" style="max-height: 360px; overflow-y: auto; min-width: 280px;">
                    <li class="dropdown-header small fw-bold">25 học viên lớp ${assignment.classCode}</li>
                    <c:forEach var="s" items="${submissions}" varStatus="st">
                        <li>
                            <a class="dropdown-item d-flex justify-content-between align-items-center py-1 small ${s.studentCode == currentSub.studentCode ? 'active fw-bold' : ''}"
                               href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=${s.studentCode}">
                                <span>#${st.index + 1} ${s.studentName}</span>
                                <c:choose>
                                    <c:when test="${s.published}">
                                        <span class="badge bg-success">${s.grade.formattedScore}</span>
                                    </c:when>
                                    <c:when test="${not empty s.grade}">
                                        <span class="badge bg-warning text-dark">${s.grade.formattedScore}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary">Chưa</span>
                                    </c:otherwise>
                                </c:choose>
                            </a>
                        </li>
                    </c:forEach>
                </ul>
            </div>
        </div>
    </div>

    <!-- Alert thông báo Flash Message -->
    <c:if test="${not empty param.success}">
        <div class="container-fluid px-4 mt-3">
            <div class="alert alert-success alert-dismissible fade show shadow-sm py-2" role="alert">
                <i class="bi bi-check-circle-fill me-2"></i>${param.success}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </div>
    </c:if>
    <c:if test="${not empty param.error}">
        <div class="container-fluid px-4 mt-3">
            <div class="alert alert-danger alert-dismissible fade show shadow-sm py-2" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>${param.error}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </div>
    </c:if>

    <!-- Main Grid: Cột trái (Viewer) - Cột phải (Chấm điểm) -->
    <div class="gr-grid-container">
        <div class="gr-grid">

            <!-- ======================================================= -->
            <!-- CỘT TRÁI: XEM TRƯỚC BÀI NỘP VÀ MỞ LIÊN KẾT REPO MỚI -->
            <!-- ======================================================= -->
            <section class="panel gr-view">
                <!-- Thanh Tabs danh sách tệp của phiên bản mới nhất -->
                <div class="gr-tabs" role="tablist">
                    <c:forEach var="f" items="${currentSub.files}" varStatus="fIdx">
                        <a href="${pageContext.request.contextPath}/assignment/grading?assignmentId=${assignment.id}&studentCode=${currentSub.studentCode}&file=${fIdx.index}"
                           class="gr-tab-btn ${selectedFileIndex == fIdx.index ? 'on' : ''}">
                            <i class="bi ${f.pdf ? 'bi-file-earmark-pdf text-danger' : (f.code ? 'bi-file-earmark-code text-primary' : 'bi-file-earmark-zip text-warning')}"></i>
                            ${f.name}
                        </a>
                    </c:forEach>

                    <!-- Nút mở liên kết Repo GitHub trong tab mới (S6-01 AC1) -->
                    <c:if test="${not empty currentSub.repoUrl}">
                        <a href="${currentSub.repoUrl}" target="_blank" rel="noreferrer" class="gr-repo" title="Mở kho mã nguồn trong tab mới">
                            <i class="bi bi-box-arrow-up-right"></i> Repo
                        </a>
                    </c:if>
                </div>

                <!-- Khu vực hiển thị xem trước tệp -->
                <c:set var="curFile" value="${currentSub.files[selectedFileIndex]}" />
                <div class="gr-doc">
                    <c:choose>
                        <c:when test="${not empty curFile}">
                            <i class="bi ${curFile.pdf ? 'bi-file-earmark-pdf text-danger' : (curFile.code ? 'bi-file-earmark-code text-primary' : 'bi-file-earmark-zip text-warning')}"
                               style="font-size: 2.8rem;"></i>
                            <b>${curFile.name}</b>
                            <span>
                                ${curFile.formattedSize} &bull; 
                                <c:choose>
                                    <c:when test="${curFile.pdf}">xem trước ngay trong trình duyệt</c:when>
                                    <c:when test="${curFile.code}">mã nguồn định dạng trực tiếp</c:when>
                                    <c:otherwise>tệp nén mã nguồn, tải về để chạy thử</c:otherwise>
                                </c:choose>
                            </span>

                            <c:choose>
                                <c:when test="${curFile.pdf}">
                                    <div class="doc-preview-box">${curFile.previewContent}</div>
                                </c:when>
                                <c:when test="${curFile.code}">
                                    <div class="code-preview-box"><code>${curFile.previewContent}</code></div>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="btn btn-outline-secondary btn-sm mt-2" onclick="alert('Đang tải tệp: ' + '${curFile.name}');">
                                        <i class="bi bi-download me-1"></i> Tải về tệp
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <span>Học viên chỉ nộp liên kết repo.</span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <!-- Đường dẫn liên kết repo hiển thị rõ -->
                <c:if test="${not empty currentSub.repoUrl}">
                    <a class="gr-link" href="${currentSub.repoUrl}" target="_blank" rel="noreferrer">
                        <i class="bi bi-github me-1"></i> ${currentSub.repoUrl} &bull; mở trong tab mới
                    </a>
                </c:if>

                <!-- Ghi chú của học viên -->
                <c:if test="${not empty currentSub.studentNote}">
                    <p class="hint gr-note">
                        <i class="bi bi-chat-quote-fill me-1"></i> <strong>Ghi chú của học viên:</strong> ${currentSub.studentNote}
                    </p>
                </c:if>
            </section>

            <!-- ======================================================= -->
            <!-- CỘT PHẢI: KHUNG CHẤM ĐIỂM, NHẬN XÉT, CÔNG BỐ & LOG -->
            <!-- ======================================================= -->
            <section class="panel gr-form">
                <div class="sb-head">
                    <h3>Chấm điểm</h3>
                    <c:choose>
                        <c:when test="${currentSub.published}">
                            <span class="badge bg-success px-2 py-1">Đã công bố</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-secondary px-2 py-1">Chưa công bố</span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <form id="gradeForm" action="${pageContext.request.contextPath}/assignment/grading/save" method="POST">
                    <input type="hidden" name="assignmentId" value="${assignment.id}">
                    <input type="hidden" name="studentCode" value="${currentSub.studentCode}">
                    <input type="hidden" id="actionType" name="actionType" value="SAVE_DRAFT">
                    <input type="hidden" name="nextStudentCode" value="${not empty nextSub ? nextSub.studentCode : ''}">

                    <!-- Ô nhập điểm -->
                    <div class="field">
                        <label for="gr-score">Điểm</label>
                        <div class="gr-score">
                            <div class="inp">
                                <input id="gr-score"
                                       name="score"
                                       inputmode="decimal"
                                       value="${not empty currentSub.grade ? currentSub.grade.formattedScore : ''}"
                                       placeholder="0"
                                       required />
                            </div>
                            <span>/ ${assignment.maxScore}</span>
                        </div>
                        <p class="hint">Nhập số từ 0 đến ${assignment.maxScore}, cho phép một chữ số thập phân.</p>
                    </div>

                    <!-- Ô nhận xét -->
                    <div class="field">
                        <label for="gr-comment">Nhận xét</label>
                        <textarea id="gr-comment"
                                  name="comment"
                                  rows="4"
                                  placeholder="Viết nhận xét để học viên biết cần cải thiện gì..."
                                  required>${not empty currentSub.grade ? currentSub.grade.comment : ''}</textarea>
                    </div>

                    <!-- Ô lý do sửa điểm (Bắt buộc nếu điểm đã công bố bị thay đổi) -->
                    <div class="field ${currentSub.published ? '' : 'd-none'}" id="reasonField">
                        <label for="gr-reason">
                            Lý do sửa điểm <c:if test="${currentSub.published}"><span class="text-danger">*</span></c:if><c:if test="${not currentSub.published}">(không bắt buộc)</c:if>
                        </label>
                        <input id="gr-reason"
                               name="reason"
                               type="text"
                               placeholder="Ví dụ: chấm lại phần tìm kiếm" />
                        <c:if test="${currentSub.published}">
                            <p class="hint text-danger">Điểm đã công bố, ghi lý do sửa điểm.</p>
                        </c:if>
                    </div>

                    <!-- Nhật ký sửa điểm (Audit Log) -->
                    <c:if test="${not empty currentSub.grade && not empty currentSub.grade.auditLogs}">
                        <div class="gr-log">
                            <span class="lbl">Nhật ký sửa điểm</span>
                            <ul>
                                <c:forEach var="log" items="${currentSub.grade.auditLogs}">
                                    <li>
                                        <b>${log.timestamp} &bull; ${log.actorName}</b>
                                        <span>${log.logText}</span>
                                    </li>
                                </c:forEach>
                            </ul>
                        </div>
                    </c:if>

                    <p class="hint">Điểm chỉ hiện cho học viên sau khi bạn bấm Công bố.</p>

                    <!-- Khối nút hành động chuẩn Jira S6-01 -->
                    <div class="gr-acts">
                        <!-- Nút Công bố điểm bài này -->
                        <button type="button" class="btn-act publish" onclick="publishGrade();">
                            <i class="bi bi-send-check"></i> Công bố điểm bài này
                        </button>

                        <!-- Nút Lưu nháp và sang bài kế tiếp (hoặc Lưu nháp nếu bài cuối) -->
                        <c:choose>
                            <c:when test="${not empty nextSub}">
                                <button type="button" class="btn-act primary" onclick="submitGrade('SAVE_AND_NEXT');" title="Lưu và chuyển sang bài tiếp theo (Ctrl + Enter)">
                                    <i class="bi bi-arrow-right-circle"></i> Lưu nháp và sang bài kế tiếp
                                    <span class="badge bg-white text-primary ms-1 small fw-normal">Ctrl+Enter</span>
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button type="button" class="btn-act primary" onclick="submitGrade('SAVE_DRAFT');" title="Lưu điểm nháp (Ctrl + Enter)">
                                    <i class="bi bi-check-circle"></i> Lưu nháp
                                    <span class="badge bg-white text-primary ms-1 small fw-normal">Ctrl+Enter</span>
                                </button>
                            </c:otherwise>
                        </c:choose>

                        <c:if test="${not empty nextSub}">
                            <button type="button" class="btn-act outline" onclick="submitGrade('SAVE_DRAFT');">
                                Lưu nháp ở lại
                            </button>
                        </c:if>
                    </div>
                </form>

                <!-- Form ẩn công bố -->
                <form id="pubForm" class="d-none" action="${pageContext.request.contextPath}/assignment/grading/publish" method="POST">
                    <input type="hidden" name="assignmentId" value="${assignment.id}">
                    <input type="hidden" name="studentCode" value="${currentSub.studentCode}">
                </form>
            </section>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        var cfg = document.getElementById('gradingConfig');
        var isPub = cfg ? (cfg.getAttribute('data-is-published') === 'true') : false;
        var originalScore = cfg ? (cfg.getAttribute('data-original-score') || '').trim() : '';
        var maxScore = cfg ? (cfg.getAttribute('data-max-score') || '10') : '10';
        var studentName = cfg ? (cfg.getAttribute('data-student-name') || '') : '';

        var scoreInp = document.getElementById('gr-score');
        var reasonBox = document.getElementById('reasonField');

        if (scoreInp) {
            scoreInp.addEventListener('input', function() {
                var cur = scoreInp.value.trim().replace('.', ',');
                if (isPub && reasonBox) {
                    if (cur !== originalScore && cur !== '') {
                        reasonBox.classList.remove('d-none');
                    } else {
                        reasonBox.classList.add('d-none');
                    }
                }
            });
        }

        // Phím tắt bàn phím tiện ích cho giảng viên chấm bài liên tục (S6-01)
        document.addEventListener('keydown', function(e) {
            // Ctrl + Enter hoặc Cmd + Enter: Lưu nháp và sang bài tiếp theo
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                var nextBtn = document.getElementById('btnNextSub');
                if (nextBtn) {
                    submitGrade('SAVE_AND_NEXT');
                } else {
                    submitGrade('SAVE_DRAFT');
                }
            }

            // Alt + ArrowLeft: Bài trước
            if (e.altKey && e.key === 'ArrowLeft') {
                var prevBtn = document.getElementById('btnPrevSub');
                if (prevBtn) {
                    e.preventDefault();
                    prevBtn.click();
                }
            }

            // Alt + ArrowRight: Bài kế tiếp
            if (e.altKey && e.key === 'ArrowRight') {
                var nextBtn = document.getElementById('btnNextSub');
                if (nextBtn) {
                    e.preventDefault();
                    nextBtn.click();
                }
            }
        });

        function submitGrade(action) {
            if (!scoreInp) return;
            var val = scoreInp.value.trim().replace(',', '.');
            var commentEl = document.getElementById('gr-comment');
            var comment = commentEl ? commentEl.value.trim() : '';
            var reasonEl = document.getElementById('gr-reason');
            var reason = reasonEl ? reasonEl.value.trim() : '';

            var maxNum = parseFloat(maxScore) || 10;

            if (val === '') {
                alert('Nhập số từ 0 đến ' + maxScore + ', cho phép một chữ số thập phân.');
                scoreInp.focus();
                return;
            }

            var num = parseFloat(val);
            if (isNaN(num) || num < 0 || num > maxNum) {
                alert('Nhập số từ 0 đến ' + maxScore + ', cho phép một chữ số thập phân.');
                scoreInp.focus();
                return;
            }

            if (!comment) {
                alert('Viết nhận xét để học viên biết cần cải thiện gì.');
                if (commentEl) commentEl.focus();
                return;
            }

            var origNum = parseFloat(originalScore.replace(',', '.'));
            if (isPub && num !== origNum && !reason) {
                alert('Điểm đã công bố, ghi lý do sửa điểm.');
                if (reasonEl) reasonEl.focus();
                return;
            }

            var actionInput = document.getElementById('actionType');
            if (actionInput) actionInput.value = action;
            var form = document.getElementById('gradeForm');
            if (form) form.submit();
        }

        function publishGrade() {
            if (!scoreInp) return;
            var val = scoreInp.value.trim();
            if (!val && !originalScore) {
                alert('Nhập điểm và lưu nháp trước khi bấm Công bố.');
                scoreInp.focus();
                return;
            }

            var msg = 'Công bố điểm cho học viên ' + studentName + '?\nHọc viên sẽ nhận được thông báo và xem được điểm số.';
            if (confirm(msg)) {
                if (val !== originalScore) {
                    submitGrade('SAVE_DRAFT');
                    return;
                }
                var pubForm = document.getElementById('pubForm');
                if (pubForm) pubForm.submit();
            }
        }
    </script>
</body>
</html>
