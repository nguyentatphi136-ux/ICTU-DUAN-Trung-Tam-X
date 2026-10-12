package com.ems.controller;

import com.ems.dao.TrainingProgramDAO;
import com.ems.model.TrainingProgram;
import com.ems.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servlet điều hướng và hiển thị danh mục Chương trình đào tạo (JSP View)
 * User Story S2-04: Quản lý danh mục chương trình đào tạo
 * URL: /training-programs
 */
@WebServlet(name = "TrainingProgramServlet", urlPatterns = {"/training-programs", "/training-programs/list"})
public class TrainingProgramServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TrainingProgramDAO trainingProgramDAO = new TrainingProgramDAO();

    // Dữ liệu mẫu dự phòng khi chưa cấu hình MySQL
    private static final List<TrainingProgram> DEMO_PROGRAMS = new ArrayList<>();

    static {
        DEMO_PROGRAMS.add(new TrainingProgram(1, "JAVA-FULLSTACK", "Lập trình Java Fullstack Web Chuyên nghiệp",
                "Đào tạo từ nền tảng Java Core, Servlet, JSP, Spring Boot đến React frontend.", 72, new BigDecimal("18500000"), "ACTIVE"));
        DEMO_PROGRAMS.add(new TrainingProgram(2, "PYTHON-AI", "Khoa học Dữ liệu & Trí tuệ Nhân tạo với Python",
                "Khóa học Machine Learning, Deep Learning và xử lý dữ liệu lớn.", 60, new BigDecimal("21000000"), "ACTIVE"));
        DEMO_PROGRAMS.add(new TrainingProgram(3, "FRONTEND-REACT", "Lập trình Web Frontend Hiện đại",
                "Chuyên sâu HTML5, CSS3, JavaScript ES6+ và ReactJS.", 48, new BigDecimal("14000000"), "ACTIVE"));
        DEMO_PROGRAMS.add(new TrainingProgram(4, "DATABASE-MYSQL", "Thiết kế & Tối ưu Cơ sở Dữ liệu SQL",
                "Khai thác và quản trị hệ quản trị CSDL quan hệ.", 36, new BigDecimal("9500000"), "INACTIVE"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp?error=session_expired");
            return;
        }

        String keyword = req.getParameter("keyword");
        String status = req.getParameter("status");

        List<TrainingProgram> programList = new ArrayList<>();
        try {
            Map<String, Object> result = trainingProgramDAO.search(keyword, status, 1, 50);
            @SuppressWarnings("unchecked")
            List<TrainingProgram> dbList = (List<TrainingProgram>) result.get("items");
            if (dbList != null && !dbList.isEmpty()) {
                programList = dbList;
            } else {
                programList = filterDemoPrograms(keyword, status);
            }
        } catch (Exception e) {
            // Fallback sang demo data
            programList = filterDemoPrograms(keyword, status);
        }

        req.setAttribute("programs", programList);
        req.setAttribute("currentUser", currentUser);
        req.setAttribute("keyword", keyword);
        req.setAttribute("status", status);

        req.getRequestDispatcher("/WEB-INF/views/programs/list-programs.jsp").forward(req, resp);
    }

    private List<TrainingProgram> filterDemoPrograms(String keyword, String status) {
        List<TrainingProgram> list = new ArrayList<>();
        for (TrainingProgram p : DEMO_PROGRAMS) {
            boolean matchKey = keyword == null || keyword.isBlank()
                    || p.getName().toLowerCase().contains(keyword.toLowerCase())
                    || p.getCode().toLowerCase().contains(keyword.toLowerCase());
            boolean matchStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                    || p.getStatus().equalsIgnoreCase(status);
            if (matchKey && matchStatus) {
                list.add(p);
            }
        }
        return list;
    }
}
