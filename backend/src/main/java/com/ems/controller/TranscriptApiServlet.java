package com.ems.controller;

import com.ems.config.GsonProvider;
import com.ems.constant.RoleConstant;
import com.ems.dao.TranscriptDAO;
import com.ems.dto.TranscriptResponse;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * REST API Servlet phục vụ chức năng S8-03 (Jira EP-07):
 * "Là Học viên, tôi muốn xem và xuất bảng điểm toàn khoá, để xin việc có bảng điểm chính thức mà không phải chờ trung tâm làm tay."
 *
 * AC1: Liệt kê toàn bộ môn đã học, điểm tổng kết, kết quả Đạt hoặc Không đạt
 * AC2: Hiển thị điểm trung bình toàn khoá (GPA theo trọng số môn)
 * AC3: Xuất PDF có mã học viên, tên trung tâm và ngày cấp
 *
 * Endpoints:
 *  - GET /api/transcript/mine
 *  - GET /api/transcripts/student/{studentCode}
 *  - GET /api/transcripts/student/{studentCode}/pdf (hoặc /export-pdf)
 */
@WebServlet(name = "TranscriptApiServlet", urlPatterns = {
        "/api/transcript/*",
        "/api/transcripts/*"
})
public class TranscriptApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = GsonProvider.getGson();

    private TranscriptDAO transcriptDAO;

    @Override
    public void init() throws ServletException {
        this.transcriptDAO = new TranscriptDAO();
    }

    public void setTranscriptDAO(TranscriptDAO transcriptDAO) {
        this.transcriptDAO = transcriptDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        User currentUser = getCurrentUser(req);
        String uri = req.getRequestURI();

        // 1. Endpoint xem bảng điểm của chính mình: /api/transcript/mine hoặc /api/transcripts/mine
        if (uri.endsWith("/mine")) {
            try {
                TranscriptResponse transcript = transcriptDAO.getTranscriptForUser(currentUser);
                if (transcript == null) {
                    ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "TRANSCRIPT_NOT_FOUND", "Chưa có bảng điểm: bạn chưa được xếp lớp hoặc chưa có môn nào chốt điểm.");
                    return;
                }
                ApiResponse.success(resp, "SUCCESS", "Lấy bảng điểm toàn khoá thành công", transcript);
            } catch (SQLException e) {
                ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Lỗi CSDL khi lấy bảng điểm: " + e.getMessage());
            }
            return;
        }

        // 2. Trích xuất mã học viên từ URL
        String studentCode = extractStudentCode(req);
        if (studentCode == null || studentCode.trim().isEmpty()) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_STUDENT_CODE", "Mã học viên không hợp lệ.");
            return;
        }

        // 3. Kiểm soát quyền bảo mật:
        // Học viên chỉ được xem bảng điểm của chính mình.
        // Quản lý đào tạo, Quản trị viên, Giảng viên có quyền xem học viên bất kỳ.
        boolean isStaff = currentUser != null && (
                currentUser.hasRole(RoleConstant.ADMIN) ||
                currentUser.hasRole(RoleConstant.TRAINING_MANAGER) ||
                currentUser.hasRole(RoleConstant.INSTRUCTOR) ||
                currentUser.hasRole(RoleConstant.ACCOUNTANT) ||
                currentUser.hasRole(RoleConstant.TA)
        );

        boolean isSelf = currentUser != null && (
                studentCode.equalsIgnoreCase(currentUser.getUserCode()) ||
                studentCode.equalsIgnoreCase(currentUser.getName())
        );

        if (!isStaff && !isSelf) {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                    "Học viên chỉ có quyền xem bảng điểm toàn khoá của chính mình.");
            return;
        }

        try {
            TranscriptResponse transcript = transcriptDAO.getTranscriptByStudentCode(studentCode);
            if (transcript == null) {
                ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "STUDENT_NOT_FOUND",
                        "Không tìm thấy bảng điểm cho học viên có mã: " + studentCode);
                return;
            }

            // AC3: Xuất tài liệu / PDF chính thức
            if (uri.endsWith("/pdf") || uri.endsWith("/export-pdf") || uri.endsWith("/print")) {
                resp.setContentType("text/html; charset=UTF-8");
                String printableHtml = transcriptDAO.generatePrintableHtml(transcript);
                try (PrintWriter out = resp.getWriter()) {
                    out.write(printableHtml);
                }
                return;
            }

            // Trả về JSON dữ liệu bảng điểm
            ApiResponse.success(resp, "SUCCESS", "Lấy chi tiết bảng điểm toàn khoá thành công", transcript);

        } catch (SQLException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Lỗi truy vấn CSDL: " + e.getMessage());
        }
    }

    private String extractStudentCode(HttpServletRequest req) {
        String uri = req.getRequestURI();
        // Dạng /api/transcripts/student/{code}[/pdf|export-pdf]
        if (uri.contains("/student/")) {
            String sub = uri.substring(uri.indexOf("/student/") + "/student/".length());
            return sub.split("/")[0].trim();
        }

        // Query param ?code=... hoặc ?studentCode=...
        String code = req.getParameter("code");
        if (code != null && !code.trim().isEmpty()) {
            return code.trim();
        }
        String sc = req.getParameter("studentCode");
        if (sc != null && !sc.trim().isEmpty()) {
            return sc.trim();
        }

        return null;
    }

    private User getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            User u = (User) session.getAttribute("currentUser");
            if (u != null) return u;
            User authUser = (User) session.getAttribute("authUser");
            if (authUser != null) return authUser;
        }

        // Người dùng mặc định nếu không có phiên đăng nhập (Học viên demo)
        User defaultStudent = new User(5L, "Nguyễn Văn Học", "hocvien@tms.vn", "ACTIVE", List.of(RoleConstant.STUDENT));
        defaultStudent.setUserCode("HV-2026-001");
        return defaultStudent;
    }
}
