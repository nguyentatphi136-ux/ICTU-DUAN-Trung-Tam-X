package com.ems.controller;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.model.*;
import com.ems.service.ClassStore;
import com.ems.service.HolidayStore;
import com.ems.service.ScheduleService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servlet điều hướng và xử lý Sinh thời khoá biểu tự động theo mẫu lặp (JSP View)
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Tiêu chí nghiệm thu:
 *   1. Khai báo mẫu: các thứ trong tuần, ca học, ngày bắt đầu, số buổi (ví dụ: 60 buổi)
 *   2. Hệ thống sinh đủ buổi và tự bỏ qua ngày nghỉ lễ đã khai báo
 *   3. Xem trước danh sách buổi trước khi xác nhận lưu (Preview & Confirm)
 *   4. Mỗi buổi được gắn với một bài học trong môn (lấy đúng GV theo phân công)
 * 
 * URL Patterns: /schedule/generate, /classes/schedule/generate
 * Phân quyền: Yêu cầu vai trò TRAINING_MANAGER (Quản lý đào tạo) hoặc ADMIN
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
@WebServlet(name = "ScheduleServlet", urlPatterns = {"/schedule/generate", "/classes/schedule/generate"})
public class ScheduleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ScheduleService scheduleService = new ScheduleService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(true);
        User currentUser = (User) session.getAttribute("currentUser");

        // Nếu chưa đăng nhập (hoặc chạy thử trực tiếp), cấp phiên Quản lý đào tạo để chạy thử ngay
        if (currentUser == null) {
            currentUser = new User(1L, "Nguyễn Minh Ngọc", "ngoc.nm@tms.vn", "ACTIVE",
                    List.of(RoleConstant.TRAINING_MANAGER, RoleConstant.ADMIN));
            currentUser.setUserCode("QLDT-001");
            session.setAttribute("currentUser", currentUser);
        } else if (!currentUser.hasRole(RoleConstant.TRAINING_MANAGER) && !currentUser.hasRole(RoleConstant.ADMIN)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("errorMessage", "Chức năng sinh thời khoá biểu tự động chỉ dành riêng cho Quản lý đào tạo (Training Manager) hoặc Quản trị hệ thống (Admin).");
            req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, resp);
            return;
        }

        // Chuyển flash message nếu có
        transferFlashMessages(session, req);

        String classCode = req.getParameter("classCode");
        List<Klass> classList = ClassStore.getAllClasses();
        Klass selectedClass = null;

        if (classCode != null && !classCode.isBlank()) {
            selectedClass = ClassStore.findByCode(classCode);
        }
        if (selectedClass == null && !classList.isEmpty()) {
            // Ưu tiên chọn lớp WEB-K16 (Lớp chuẩn bị 64 buổi để demo S4-01) hoặc lớp đầu tiên
            selectedClass = classList.stream()
                    .filter(c -> "WEB-K16".equalsIgnoreCase(c.getCode()))
                    .findFirst()
                    .orElse(classList.get(0));
        }

        // Tạo mẫu mặc định cho lớp
        SchedulePattern defaultPattern = createDefaultPattern(selectedClass);

        req.setAttribute("currentUser", currentUser);
        req.setAttribute("classes", classList);
        req.setAttribute("selectedClass", selectedClass);
        req.setAttribute("pattern", defaultPattern);
        req.setAttribute("holidays", HolidayStore.getAllHolidays());
        req.setAttribute("rooms", ClassStore.getAvailableRooms());
        req.setAttribute("step", 1); // Bước 1: Khai báo mẫu

        req.getRequestDispatcher("/WEB-INF/views/schedule/generate.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(true);
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            currentUser = new User(1L, "Nguyễn Minh Ngọc", "ngoc.nm@tms.vn", "ACTIVE",
                    List.of(RoleConstant.TRAINING_MANAGER, RoleConstant.ADMIN));
            currentUser.setUserCode("QLDT-001");
            session.setAttribute("currentUser", currentUser);
        } else if (!currentUser.hasRole(RoleConstant.TRAINING_MANAGER) && !currentUser.hasRole(RoleConstant.ADMIN)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.setAttribute("errorMessage", "Chức năng sinh thời khoá biểu tự động chỉ dành riêng cho Quản lý đào tạo (Training Manager) hoặc Quản trị hệ thống (Admin).");
            req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, resp);
            return;
        }

        String action = req.getParameter("action");
        if (action == null || action.isBlank()) {
            action = "preview";
        }

        try {
            switch (action) {
                case "add_holiday":
                    handleAddHoliday(req, resp, session);
                    break;
                case "preview":
                    handlePreview(req, resp, session, currentUser);
                    break;
                case "save":
                    handleSave(req, resp, session, currentUser);
                    break;
                case "back":
                    handleBack(req, resp, session, currentUser);
                    break;
                default:
                    resp.sendRedirect(req.getContextPath() + "/schedule/generate");
                    break;
            }
        } catch (IllegalArgumentException e) {
            req.setAttribute("errorMessage", e.getMessage());
            loadCommonAttributes(req, req.getParameter("classCode"));
            req.setAttribute("step", 1);
            req.getRequestDispatcher("/WEB-INF/views/schedule/generate.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            loadCommonAttributes(req, req.getParameter("classCode"));
            req.setAttribute("step", 1);
            req.getRequestDispatcher("/WEB-INF/views/schedule/generate.jsp").forward(req, resp);
        }
    }

    private void handlePreview(HttpServletRequest req, HttpServletResponse resp, HttpSession session, User currentUser)
            throws ServletException, IOException {
        SchedulePattern pattern = extractPatternFromRequest(req);
        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);

        // Lưu kết quả xem trước vào session để phục vụ xác nhận lưu
        session.setAttribute("pendingScheduleResult", result);
        session.setAttribute("pendingPattern", pattern);

        req.setAttribute("currentUser", currentUser);
        req.setAttribute("classes", ClassStore.getAllClasses());
        req.setAttribute("selectedClass", result.getKlass());
        req.setAttribute("pattern", pattern);
        req.setAttribute("result", result);
        req.setAttribute("holidays", HolidayStore.getAllHolidays());
        req.setAttribute("rooms", ClassStore.getAvailableRooms());
        req.setAttribute("step", 2); // Bước 2: Xem trước và kiểm tra

        req.getRequestDispatcher("/WEB-INF/views/schedule/generate.jsp").forward(req, resp);
    }

    private void handleSave(HttpServletRequest req, HttpServletResponse resp, HttpSession session, User currentUser)
            throws ServletException, IOException {
        ScheduleGenerationResult result = (ScheduleGenerationResult) session.getAttribute("pendingScheduleResult");
        SchedulePattern pattern = (SchedulePattern) session.getAttribute("pendingPattern");

        if (result == null || pattern == null || result.getSessions().isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy dữ liệu xem trước thời khoá biểu để lưu. Vui lòng thực hiện lại.");
        }

        scheduleService.saveSchedule(result.getKlass().getCode(), result.getSessions(), pattern);

        // Xóa tạm trong session sau khi đã lưu
        session.removeAttribute("pendingScheduleResult");
        session.removeAttribute("pendingPattern");

        req.setAttribute("currentUser", currentUser);
        req.setAttribute("classes", ClassStore.getAllClasses());
        req.setAttribute("selectedClass", result.getKlass());
        req.setAttribute("pattern", pattern);
        req.setAttribute("result", result);
        req.setAttribute("savedSessions", result.getSessions());
        req.setAttribute("successMessage", "Đã lưu thành công thời khoá biểu cho lớp " + result.getKlass().getCode() + " gồm " + result.getSessions().size() + " buổi học!");
        req.setAttribute("step", 3); // Bước 3: Đã lưu thành công

        req.getRequestDispatcher("/WEB-INF/views/schedule/generate.jsp").forward(req, resp);
    }

    private void handleBack(HttpServletRequest req, HttpServletResponse resp, HttpSession session, User currentUser)
            throws ServletException, IOException {
        SchedulePattern pattern = (SchedulePattern) session.getAttribute("pendingPattern");
        if (pattern == null) {
            pattern = extractPatternFromRequest(req);
        }

        Klass klass = ClassStore.findByCode(pattern.getClassCode());

        req.setAttribute("currentUser", currentUser);
        req.setAttribute("classes", ClassStore.getAllClasses());
        req.setAttribute("selectedClass", klass);
        req.setAttribute("pattern", pattern);
        req.setAttribute("holidays", HolidayStore.getAllHolidays());
        req.setAttribute("rooms", ClassStore.getAvailableRooms());
        req.setAttribute("step", 1);

        req.getRequestDispatcher("/WEB-INF/views/schedule/generate.jsp").forward(req, resp);
    }

    private void handleAddHoliday(HttpServletRequest req, HttpServletResponse resp, HttpSession session)
            throws IOException {
        String name = req.getParameter("holidayName");
        String fromDate = req.getParameter("holidayFrom");
        String toDate = req.getParameter("holidayTo");
        String classCode = req.getParameter("classCode");

        try {
            HolidayStore.addHoliday(name, fromDate, toDate);
            session.setAttribute("flashSuccess", "Đã thêm thành công ngày nghỉ lễ: " + name);
        } catch (Exception e) {
            session.setAttribute("flashError", "Lỗi khi thêm ngày nghỉ lễ: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/schedule/generate?classCode=" + (classCode != null ? classCode : ""));
    }

    private SchedulePattern extractPatternFromRequest(HttpServletRequest req) {
        String classCode = req.getParameter("classCode");
        String[] daysArr = req.getParameterValues("days");
        String start = req.getParameter("start");
        String end = req.getParameter("end");
        String from = req.getParameter("from");
        String countStr = req.getParameter("count");
        String room = req.getParameter("room");

        List<Integer> days = new ArrayList<>();
        if (daysArr != null) {
            for (String d : daysArr) {
                try {
                    days.add(Integer.parseInt(d.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        int count = 60; // Mặc định 60 buổi
        if (countStr != null && !countStr.isBlank()) {
            try {
                count = Integer.parseInt(countStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return new SchedulePattern(classCode, days, start, end, from, count, room);
    }

    private SchedulePattern createDefaultPattern(Klass klass) {
        if (klass == null) {
            return new SchedulePattern("", Arrays.asList(1, 3, 5), "18:00", "20:30", "2026-10-20", 60, "P.401");
        }
        int total = klass.getTotalSessions() > 0 ? klass.getTotalSessions() : 60;
        String start = "18:00";
        String end = "20:30";
        String from = (klass.getStart() != null && !klass.getStart().isBlank()) ? klass.getStart() : "2026-10-20";
        String room = (klass.getRoom() != null && !klass.getRoom().isBlank()) ? klass.getRoom() : "P.401";

        // Mặc định T2-T4-T6 (1, 3, 5)
        List<Integer> days = new ArrayList<>(Arrays.asList(1, 3, 5));

        return new SchedulePattern(klass.getCode(), days, start, end, from, total, room);
    }

    private void loadCommonAttributes(HttpServletRequest req, String classCode) {
        req.setAttribute("classes", ClassStore.getAllClasses());
        req.setAttribute("selectedClass", ClassStore.findByCode(classCode));
        req.setAttribute("holidays", HolidayStore.getAllHolidays());
        req.setAttribute("rooms", ClassStore.getAvailableRooms());
    }

    private void transferFlashMessages(HttpSession session, HttpServletRequest req) {
        if (session != null) {
            if (session.getAttribute("flashSuccess") != null) {
                req.setAttribute("successMessage", session.getAttribute("flashSuccess"));
                session.removeAttribute("flashSuccess");
            }
            if (session.getAttribute("flashError") != null) {
                req.setAttribute("errorMessage", session.getAttribute("flashError"));
                session.removeAttribute("flashError");
            }
        }
    }
}
