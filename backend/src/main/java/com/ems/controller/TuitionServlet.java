package com.ems.controller;

import com.ems.model.TuitionFee;
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

@WebServlet(name = "TuitionServlet", urlPatterns = {"/tuition/list", "/tuition/edit", "/tuition/update"})
public class TuitionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final List<TuitionFee> tuitionList = new ArrayList<>();

    static {
        tuitionList.add(new TuitionFee(1, 3, "Nguyễn Văn Học", "Khóa học Fullstack Web Java",
                new BigDecimal("15000000"), new BigDecimal("10000000"), "partially_paid", "REC-2026-001", "Kế toán trưởng"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();

        if (uri.endsWith("/tuition/edit")) {
            int tuitionId = Integer.parseInt(req.getParameter("id") != null ? req.getParameter("id") : "1");
            TuitionFee fee = tuitionList.stream().filter(t -> t.getId() == tuitionId).findFirst().orElse(tuitionList.get(0));
            req.setAttribute("tuition", fee);
            req.getRequestDispatcher("/WEB-INF/views/tuition/edit-tuition.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("tuitionList", tuitionList);
        req.getRequestDispatcher("/WEB-INF/views/tuition/list-tuition.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String updaterName = (currentUser != null) ? currentUser.getName() : "Bộ phận Kế toán";

        int tuitionId = Integer.parseInt(req.getParameter("id"));
        BigDecimal paidAmount = new BigDecimal(req.getParameter("paidAmount"));
        String status = req.getParameter("status");

        for (TuitionFee fee : tuitionList) {
            if (fee.getId() == tuitionId) {
                fee.setPaidAmount(paidAmount);
                fee.setStatus(status);
                fee.setUpdatedBy(updaterName);
                break;
            }
        }

        req.setAttribute("successMessage", "Cập nhật thông tin học phí thành công!");
        req.setAttribute("tuitionList", tuitionList);
        req.getRequestDispatcher("/WEB-INF/views/tuition/list-tuition.jsp").forward(req, resp);
    }
}
