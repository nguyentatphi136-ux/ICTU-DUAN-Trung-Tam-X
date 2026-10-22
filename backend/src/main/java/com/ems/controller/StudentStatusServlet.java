package com.ems.controller;

import com.ems.constant.RoleConstant;
import com.ems.dao.StudentStatusDAO;
import com.ems.model.ClassItem;
import com.ems.model.Student;
import com.ems.model.StudentStatusLog;
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
import java.time.LocalDate;
import java.util.List;

/**
 * Controller / Servlet điều hướng nghiệp vụ S4-06 (Jira IDTTX-252, IDTTX-268 [BE]):
 * Xử lý Bảo lưu, Chuyển lớp, Thôi học của học viên;
 * Cập nhật hồ sơ học viên, lưu vết lịch sử và kiểm soát danh sách điểm danh theo ngày (Roster on date).
 */
@WebServlet(name = "StudentStatusServlet", urlPatterns = {
        "/student/status",
        "/student/profile",
        "/student/change-status",
        "/student/roster"
})
public class StudentStatusServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final StudentStatusDAO studentStatusDAO = new StudentStatusDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Nếu chưa đăng nhập, cấp tạm user giả lập vai trò Quản lý đào tạo để kiểm thử thuận tiện
        if (currentUser == null) {
            currentUser = new User(1L, "Nguyễn Minh Ngọc", "ngoc.nm@tms.vn", "ACTIVE",
                    List.of(RoleConstant.TRAINING_MANAGER, RoleConstant.ADMIN));
            currentUser.setUserCode("QLDT-001");
            if (session != null) {
                session.setAttribute("currentUser", currentUser);
            }
        }
        req.setAttribute("currentUser", currentUser);

        String uri = req.getRequestURI();

        // 1. Chi tiết Hồ sơ học viên & Nút thao tác S4-06 (/student/profile)
        if (uri.endsWith("/student/profile")) {
            handleStudentProfile(req, resp);
            return;
        }

        // 2. Kiểm tra Danh sách điểm danh theo ngày hiệu lực (/student/roster)
        if (uri.endsWith("/student/roster")) {
            handleClassRoster(req, resp);
            return;
        }

        // 3. Mặc định: Danh sách quản lý tình trạng học viên toàn trung tâm (/student/status)
        handleStudentStatusList(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorName = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : "Quản lý đào tạo";
        Long actorId = (currentUser != null) ? currentUser.getId() : 1L;

        String uri = req.getRequestURI();

        if (uri.endsWith("/student/change-status")) {
            String studentCode = req.getParameter("studentCode");
            String actionType = req.getParameter("actionType"); // "Bảo lưu", "Chuyển lớp", "Thôi học"
            String effectiveDate = req.getParameter("effectiveDate"); // yyyy-MM-dd
            String reason = req.getParameter("reason");
            String notes = req.getParameter("notes");
            String toClassCode = req.getParameter("toClassCode");

            // Kiểm tra tính hợp lệ dữ liệu (Validation)
            if (studentCode == null || studentCode.isBlank()) {
                redirectWithError(req, resp, studentCode, "Không tìm thấy mã học viên!");
                return;
            }

            if (effectiveDate == null || effectiveDate.isBlank()) {
                redirectWithError(req, resp, studentCode, "Vui lòng chọn ngày có hiệu lực!");
                return;
            }

            if (reason == null || reason.isBlank()) {
                redirectWithError(req, resp, studentCode, "Vui lòng chọn lý do thực hiện!");
                return;
            }

            if ("Lý do khác".equalsIgnoreCase(reason) && (notes == null || notes.isBlank())) {
                redirectWithError(req, resp, studentCode, "Khi chọn 'Lý do khác', bạn bắt buộc phải ghi rõ nội dung vào ô ghi chú!");
                return;
            }

            if ("Chuyển lớp".equalsIgnoreCase(actionType)) {
                if (toClassCode == null || toClassCode.isBlank()) {
                    redirectWithError(req, resp, studentCode, "Vui lòng chọn lớp học phù hợp để chuyển đến!");
                    return;
                }
            }

            // Thực thi nghiệp vụ lưu trữ & đổi trạng thái
            boolean success = studentStatusDAO.processStatusChange(
                    studentCode, actionType, effectiveDate, reason, notes, toClassCode, actorName, actorId);

            if (success) {
                String successMsg = "Chuyển lớp".equalsIgnoreCase(actionType)
                        ? "Đã chuyển học viên " + studentCode + " sang lớp " + toClassCode + " từ ngày " + effectiveDate
                        : "Đã ghi nhận " + actionType.toLowerCase() + " cho học viên " + studentCode + " từ ngày " + effectiveDate;

                resp.sendRedirect(req.getContextPath() + "/student/profile?code=" + studentCode +
                        "&success=" + URLEncoder.encode(successMsg, StandardCharsets.UTF_8));
            } else {
                redirectWithError(req, resp, studentCode, "Có lỗi xảy ra trong quá trình xử lý thay đổi tình trạng học!");
            }
        } else {
            resp.sendRedirect(req.getContextPath() + "/student/status");
        }
    }

    private void handleStudentProfile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String code = req.getParameter("code");
        if (code == null || code.isBlank()) {
            code = "HV260101"; // Mặc định mở học viên mẫu
        }

        Student student = studentStatusDAO.getStudentByCode(code);
        if (student == null) {
            req.setAttribute("errorMessage", "Không tìm thấy học viên có mã: " + code);
            handleStudentStatusList(req, resp);
            return;
        }

        // Lấy danh sách lớp chuyển đến hợp lệ: cùng chương trình, đang mở, còn chỗ
        List<ClassItem> availableClasses = studentStatusDAO.getAvailableClassesForTransfer(
                student.getCurrentClass(), student.getProgram());

        req.setAttribute("student", student);
        req.setAttribute("availableClasses", availableClasses);
        req.setAttribute("today", LocalDate.now().toString());

        // Lấy thông báo từ query parameter nếu có
        String successMsg = req.getParameter("success");
        String errorMsg = req.getParameter("error");
        if (successMsg != null) req.setAttribute("successMessage", successMsg);
        if (errorMsg != null) req.setAttribute("errorMessage", errorMsg);

        req.getRequestDispatcher("/WEB-INF/views/students/student-profile.jsp").forward(req, resp);
    }

    private void handleStudentStatusList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String search = req.getParameter("search");
        String status = req.getParameter("status");
        String classCode = req.getParameter("classCode");

        List<Student> students = studentStatusDAO.getAllStudents(search, status, classCode, null);
        List<StudentStatusLog> recentLogs = studentStatusDAO.getAllStatusLogs(15);
        List<ClassItem> allClasses = studentStatusDAO.getAllClasses();

        // Thống kê nhanh
        long totalCount = students.size();
        long studyingCount = students.stream().filter(Student::isStudying).count();
        long suspendedCount = students.stream().filter(Student::isSuspended).count();
        long droppedCount = students.stream().filter(Student::isDroppedOut).count();

        req.setAttribute("students", students);
        req.setAttribute("recentLogs", recentLogs);
        req.setAttribute("allClasses", allClasses);
        req.setAttribute("totalCount", totalCount);
        req.setAttribute("studyingCount", studyingCount);
        req.setAttribute("suspendedCount", suspendedCount);
        req.setAttribute("droppedCount", droppedCount);
        req.setAttribute("search", search);
        req.setAttribute("status", status);
        req.setAttribute("classCode", classCode);

        req.getRequestDispatcher("/WEB-INF/views/students/student-status-list.jsp").forward(req, resp);
    }

    private void handleClassRoster(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String cls = req.getParameter("cls");
        if (cls == null || cls.isBlank()) {
            cls = "WEB-K15";
        }
        String date = req.getParameter("date");
        if (date == null || date.isBlank()) {
            date = LocalDate.now().toString();
        }

        List<Student> roster = studentStatusDAO.getRosterOnDate(cls, date);
        List<ClassItem> allClasses = studentStatusDAO.getAllClasses();

        req.setAttribute("selectedClass", cls);
        req.setAttribute("selectedDate", date);
        req.setAttribute("roster", roster);
        req.setAttribute("allClasses", allClasses);

        req.getRequestDispatcher("/WEB-INF/views/students/class-roster.jsp").forward(req, resp);
    }

    private void redirectWithError(HttpServletRequest req, HttpServletResponse resp, String studentCode, String errorMsg)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + "/student/profile?code=" + studentCode +
                "&error=" + URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
    }
}
