package com.ems.controller;

import com.ems.model.Grade;
import com.ems.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GradeServlet", urlPatterns = {"/grade/list", "/grade/edit", "/grade/update"})
public class GradeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final List<Grade> gradeList = new ArrayList<>();

    static {
        gradeList.add(new Grade(1, 3, "Nguyễn Văn Học", "Lập trình Web Java", "Chuyên cần", 9.0, "Đi học đầy đủ", "Giảng viên"));
        gradeList.add(new Grade(2, 3, "Nguyễn Văn Học", "Lập trình Web Java", "Giữa kỳ", 8.5, "Làm bài tốt", "Giảng viên"));
        gradeList.add(new Grade(3, 3, "Nguyễn Văn Học", "Cơ sở dữ liệu", "Cuối kỳ", 8.0, "Đạt yêu cầu", "Giảng viên"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/grade/edit")) {
            int gradeId = Integer.parseInt(req.getParameter("id") != null ? req.getParameter("id") : "1");
            Grade selectedGrade = gradeList.stream().filter(g -> g.getId() == gradeId).findFirst().orElse(gradeList.get(0));
            req.setAttribute("grade", selectedGrade);
            req.getRequestDispatcher("/WEB-INF/views/grades/edit-grade.jsp").forward(req, resp);
            return;
        }

        // Mặc định hiển thị danh sách điểm
        req.setAttribute("gradeList", gradeList);
        req.getRequestDispatcher("/WEB-INF/views/grades/list-grade.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String updaterName = (currentUser != null) ? currentUser.getName() : "Hệ thống";

        int gradeId = Integer.parseInt(req.getParameter("id"));
        double newScore = Double.parseDouble(req.getParameter("score"));
        String newNotes = req.getParameter("notes");

        for (Grade g : gradeList) {
            if (g.getId() == gradeId) {
                g.setScore(newScore);
                g.setNotes(newNotes);
                g.setUpdatedBy(updaterName);
                break;
            }
        }

        req.setAttribute("successMessage", "Cập nhật điểm số thành công!");
        req.setAttribute("gradeList", gradeList);
        req.getRequestDispatcher("/WEB-INF/views/grades/list-grade.jsp").forward(req, resp);
    }
}
