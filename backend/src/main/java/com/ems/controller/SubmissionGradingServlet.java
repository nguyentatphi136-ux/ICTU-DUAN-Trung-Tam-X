package com.ems.controller;

import com.ems.constant.RoleConstant;
import com.ems.dao.AssignmentGradingDAO;
import com.ems.model.AssignmentInfo;
import com.ems.model.AssignmentSubmission;
import com.ems.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Controller Servlet cho nghiệp vụ S6-01 (Jira IDTTX-273 / Subtask IDTTX-280 [BE]):
 * Màn hình chấm điểm và viết nhận xét cho một bài nộp của giảng viên.
 * Hỗ trợ điều hướng đa luồng URL cả RESTful tiếng Anh (/assignment/*) và tiếng Việt (/bai-tap/*).
 */
@WebServlet(name = "SubmissionGradingServlet", urlPatterns = {
        "/assignment/submissions",
        "/assignment/grading",
        "/assignment/grading/save",
        "/assignment/grading/publish",
        "/assignment/grading/publish-all",
        "/assignment/student-view",
        "/bai-tap/danh-sach",
        "/bai-tap/cham",
        "/bai-tap/cham/luu",
        "/bai-tap/cham/cong-bo",
        "/bai-tap/ket-qua"
})
public class SubmissionGradingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final AssignmentGradingDAO gradingDAO = new AssignmentGradingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(true);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Giảng viên mẫu của lớp WEB-K15 theo Jira & assignments.ts: VU DUC LONG
        if (currentUser == null) {
            currentUser = new User(2L, "VU DUC LONG", "long.vd@tms.vn", "ACTIVE",
                    List.of(RoleConstant.INSTRUCTOR, RoleConstant.ADMIN));
            currentUser.setUserCode("GV-002");
            session.setAttribute("currentUser", currentUser);
        }
        req.setAttribute("currentUser", currentUser);

        String uri = req.getRequestURI();

        // 1. Màn hình Chấm điểm chi tiết (/assignment/grading hoặc /bai-tap/cham)
        if (uri.endsWith("/assignment/grading") || uri.endsWith("/bai-tap/cham")) {
            handleGradingScreen(req, resp);
            return;
        }

        // 2. Màn hình Góc nhìn học viên (/assignment/student-view hoặc /bai-tap/ket-qua)
        if (uri.endsWith("/assignment/student-view") || uri.endsWith("/bai-tap/ket-qua")) {
            handleStudentView(req, resp);
            return;
        }

        // 3. Mặc định: Danh sách bài nộp của lớp (/assignment/submissions hoặc /bai-tap/danh-sach)
        handleSubmissionsList(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(true);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "VU DUC LONG";

        String uri = req.getRequestURI();

        // 1. Lưu điểm nháp hoặc Lưu nháp & Chuyển sang bài kế tiếp
        if (uri.endsWith("/assignment/grading/save") || uri.endsWith("/bai-tap/cham/luu")) {
            handleSaveGrade(req, resp, actorName);
            return;
        }

        // 2. Công bố điểm một bài
        if (uri.endsWith("/assignment/grading/publish") || uri.endsWith("/bai-tap/cham/cong-bo")) {
            handlePublishGrade(req, resp, actorName);
            return;
        }

        // 3. Công bố tất cả bài đã chấm
        if (uri.endsWith("/assignment/grading/publish-all")) {
            handlePublishAll(req, resp, actorName);
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/assignment/submissions");
    }

    private void handleGradingScreen(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int assignmentId = parseAssignmentId(req);
        AssignmentInfo assignment = gradingDAO.getAssignment(assignmentId);
        if (assignment == null) {
            req.setAttribute("errorMessage", "Không tìm thấy bài tập yêu cầu!");
            req.getRequestDispatcher("/WEB-INF/views/common/404.jsp").forward(req, resp);
            return;
        }

        List<AssignmentSubmission> submissions = gradingDAO.getSubmissions(assignmentId);
        if (submissions.isEmpty()) {
            req.setAttribute("errorMessage", "Chưa có bài nộp nào cho bài tập này.");
            req.getRequestDispatcher("/WEB-INF/views/grading/submissions-list.jsp").forward(req, resp);
            return;
        }

        String studentCode = req.getParameter("studentCode");
        if (studentCode == null || studentCode.trim().isEmpty()) {
            studentCode = req.getParameter("student");
        }

        AssignmentSubmission currentSub = null;
        int currentIndex = 0;

        if (studentCode != null && !studentCode.trim().isEmpty()) {
            currentIndex = gradingDAO.getSubmissionIndex(assignmentId, studentCode.trim());
            if (currentIndex >= 0) {
                currentSub = submissions.get(currentIndex);
            }
        }

        if (currentSub == null) {
            currentSub = submissions.get(0);
            currentIndex = 0;
        }

        AssignmentSubmission prevSub = (currentIndex > 0) ? submissions.get(currentIndex - 1) : null;
        AssignmentSubmission nextSub = (currentIndex < submissions.size() - 1) ? submissions.get(currentIndex + 1) : null;

        // Chọn file xem trước
        int selectedFileIndex = 0;
        String fileIdxStr = req.getParameter("file");
        if (fileIdxStr != null) {
            try {
                selectedFileIndex = Integer.parseInt(fileIdxStr);
                if (selectedFileIndex < 0 || selectedFileIndex >= currentSub.getFiles().size()) {
                    selectedFileIndex = 0;
                }
            } catch (NumberFormatException ignored) {}
        }

        req.setAttribute("assignment", assignment);
        req.setAttribute("submissions", submissions);
        req.setAttribute("currentSub", currentSub);
        req.setAttribute("currentIndex", currentIndex);
        req.setAttribute("totalCount", submissions.size());
        req.setAttribute("prevSub", prevSub);
        req.setAttribute("nextSub", nextSub);
        req.setAttribute("selectedFileIndex", selectedFileIndex);

        req.getRequestDispatcher("/WEB-INF/views/grading/grade-screen.jsp").forward(req, resp);
    }

    private void handleSaveGrade(HttpServletRequest req, HttpServletResponse resp, String actorName) throws IOException {
        int assignmentId = parseAssignmentId(req);
        String studentCode = req.getParameter("studentCode");
        if (studentCode == null || studentCode.trim().isEmpty()) {
            studentCode = req.getParameter("student");
        }
        String actionType = req.getParameter("actionType");
        String nextStudentCode = req.getParameter("nextStudentCode");
        String scoreStr = req.getParameter("score");
        String comment = req.getParameter("comment");
        String reason = req.getParameter("reason");

        if (studentCode == null || studentCode.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/assignment/submissions?assignmentId=" + assignmentId);
            return;
        }

        double score;
        try {
            score = AssignmentGradingDAO.parseScoreVN(scoreStr);
        } catch (NumberFormatException e) {
            AssignmentInfo a = gradingDAO.getAssignment(assignmentId);
            double max = (a != null) ? a.getMaxScore() : 10.0;
            String errorMsg = URLEncoder.encode(String.format("Nhập số từ 0 đến %.0f, cho phép một chữ số thập phân.", max), StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/assignment/grading?assignmentId=" + assignmentId
                    + "&studentCode=" + studentCode + "&error=" + errorMsg);
            return;
        }

        try {
            gradingDAO.saveGrade(assignmentId, studentCode, score, comment, reason, actorName);

            if ("SAVE_AND_NEXT".equalsIgnoreCase(actionType) && nextStudentCode != null && !nextStudentCode.trim().isEmpty()) {
                String successMsg = URLEncoder.encode("Đã lưu điểm nháp", StandardCharsets.UTF_8);
                resp.sendRedirect(req.getContextPath() + "/assignment/grading?assignmentId=" + assignmentId
                        + "&studentCode=" + nextStudentCode.trim() + "&success=" + successMsg);
            } else {
                String successMsg = URLEncoder.encode("Đã lưu điểm nháp", StandardCharsets.UTF_8);
                resp.sendRedirect(req.getContextPath() + "/assignment/grading?assignmentId=" + assignmentId
                        + "&studentCode=" + studentCode + "&success=" + successMsg);
            }
        } catch (IllegalArgumentException ex) {
            String errorMsg = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/assignment/grading?assignmentId=" + assignmentId
                    + "&studentCode=" + studentCode + "&error=" + errorMsg);
        }
    }

    private void handlePublishGrade(HttpServletRequest req, HttpServletResponse resp, String actorName) throws IOException {
        int assignmentId = parseAssignmentId(req);
        String studentCode = req.getParameter("studentCode");
        if (studentCode == null || studentCode.trim().isEmpty()) {
            studentCode = req.getParameter("student");
        }

        try {
            gradingDAO.publishGrade(assignmentId, studentCode, actorName);
            AssignmentSubmission sub = gradingDAO.getSubmission(assignmentId, studentCode);
            String scoreStr = (sub != null && sub.getGrade() != null) ? sub.getGrade().getFormattedScore() : "";
            String successMsg = URLEncoder.encode("Đã công bố điểm", StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/assignment/grading?assignmentId=" + assignmentId
                    + "&studentCode=" + studentCode + "&success=" + successMsg);
        } catch (Exception ex) {
            String errorMsg = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            resp.sendRedirect(req.getContextPath() + "/assignment/grading?assignmentId=" + assignmentId
                    + "&studentCode=" + studentCode + "&error=" + errorMsg);
        }
    }

    private void handlePublishAll(HttpServletRequest req, HttpServletResponse resp, String actorName) throws IOException {
        int assignmentId = parseAssignmentId(req);
        int count = gradingDAO.publishAllGraded(assignmentId, actorName);
        String successMsg = URLEncoder.encode("Đã công bố điểm cho " + count + " bài nộp", StandardCharsets.UTF_8);
        resp.sendRedirect(req.getContextPath() + "/assignment/submissions?assignmentId=" + assignmentId + "&success=" + successMsg);
    }

    private void handleSubmissionsList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int assignmentId = parseAssignmentId(req);
        AssignmentInfo assignment = gradingDAO.getAssignment(assignmentId);
        List<AssignmentSubmission> submissions = gradingDAO.getSubmissions(assignmentId);
        Map<String, Object> stats = gradingDAO.getGradingStatistics(assignmentId);

        req.setAttribute("assignment", assignment);
        req.setAttribute("submissions", submissions);
        req.setAttribute("stats", stats);

        req.getRequestDispatcher("/WEB-INF/views/grading/submissions-list.jsp").forward(req, resp);
    }

    private void handleStudentView(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int assignmentId = parseAssignmentId(req);
        String studentCode = req.getParameter("studentCode");
        if (studentCode == null || studentCode.trim().isEmpty()) {
            studentCode = req.getParameter("student");
        }
        if (studentCode == null || studentCode.trim().isEmpty()) {
            studentCode = "HV260101";
        }

        AssignmentInfo assignment = gradingDAO.getAssignment(assignmentId);
        AssignmentSubmission sub = gradingDAO.getSubmission(assignmentId, studentCode);

        req.setAttribute("assignment", assignment);
        req.setAttribute("sub", sub);
        req.setAttribute("studentCode", studentCode);

        req.getRequestDispatcher("/WEB-INF/views/grading/student-grade-view.jsp").forward(req, resp);
    }

    private int parseAssignmentId(HttpServletRequest req) {
        String idStr = req.getParameter("assignmentId");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("id");
        }
        if (idStr != null && !idStr.trim().isEmpty()) {
            try {
                return Integer.parseInt(idStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        return 14; // Mặc định Bài 14 của Sprint 6
    }
}
