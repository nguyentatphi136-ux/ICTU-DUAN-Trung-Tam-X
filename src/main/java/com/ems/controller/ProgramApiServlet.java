package com.ems.controller;

import com.ems.dao.ProgramDAO;
import com.ems.model.Program;
import com.ems.model.Subject;
import com.ems.model.User;
import com.ems.security.ApiResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller API phục vụ danh mục Chương trình đào tạo & Môn học (User Story S3-05)
 * Endpoint: GET /api/programs (Danh sách chương trình đang hoạt động)
 * Endpoint: GET /api/programs/{id}/subjects (Danh sách môn học thuộc chương trình)
 */
@WebServlet(name = "ProgramApiServlet", urlPatterns = {"/api/programs", "/api/programs/*"})
public class ProgramApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ProgramDAO programDAO;

    public ProgramApiServlet() {
        this.programDAO = new ProgramDAO();
    }

    public ProgramApiServlet(ProgramDAO programDAO) {
        this.programDAO = programDAO;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                    "Vui lòng đăng nhập để thực hiện chức năng.", "Đăng nhập", "/login.html");
            return;
        }

        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                List<Program> programs = programDAO.listAllActivePrograms();
                ApiResponse.success(resp, "PROGRAM_LIST_SUCCESS", "Lấy danh sách chương trình đào tạo thành công.", programs);
            } else if (pathInfo.matches("^/\\d+/subjects$")) {
                String[] parts = pathInfo.split("/");
                int programId = Integer.parseInt(parts[1]);
                List<Subject> subjects = programDAO.findSubjectsByProgramId(programId);
                ApiResponse.success(resp, "PROGRAM_SUBJECTS_SUCCESS", "Lấy danh sách môn học của chương trình thành công.", subjects);
            } else if (pathInfo.matches("^/\\d+$")) {
                int programId = Integer.parseInt(pathInfo.substring(1));
                Program program = programDAO.findById(programId);
                if (program == null) {
                    ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "PROGRAM_NOT_FOUND",
                            "Không tìm thấy chương trình đào tạo có ID: " + programId);
                    return;
                }
                ApiResponse.success(resp, "PROGRAM_DETAIL_SUCCESS", "Lấy chi tiết chương trình đào tạo thành công.", program);
            } else {
                ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "API_NOT_FOUND", "Đường dẫn không hợp lệ.");
            }
        } catch (SQLException e) {
            getServletContext().log("Lỗi CSDL khi truy vấn chương trình: " + e.getMessage(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                    "Không thể kết nối cơ sở dữ liệu để lấy thông tin chương trình.");
        }
    }
}
