package com.ems.controller;

import com.ems.dao.UserDAO;
import com.ems.model.User;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@WebServlet(urlPatterns = {"/login", "/auth/login", "/api/auth/login", "/api/auth/me"})
public class LoginApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng.";
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (request.getRequestURI().endsWith("/api/auth/me")) {
            sendMe(request, response, session);
            return;
        }
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp");
            return;
        }
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean apiRequest = isApiRequest(request);
        String email;
        String password;
        try {
            if (apiRequest && request.getContentType() != null
                    && request.getContentType().contains("application/json")) {
                com.google.gson.JsonObject body = GSON.fromJson(request.getReader(), com.google.gson.JsonObject.class);
                email = body == null || !body.has("email") ? null : body.get("email").getAsString();
                password = body == null || !body.has("password") ? null : body.get("password").getAsString();
            } else {
                email = request.getParameter("email");
                password = request.getParameter("password");
            }
        } catch (RuntimeException exception) {
            sendApiError(response, 400, "REQUEST_INVALID_JSON", "Dữ liệu đăng nhập không hợp lệ.");
            return;
        }

        User user = null;
        try {
            user = userDAO.authenticate(email, password);
        } catch (SQLException exception) {
            getServletContext().log("Không thể kết nối MySQL, chuyển sang xác thực dự phòng.", exception);
        }

        // Dự phòng xác thực in-memory UserStore (IDTTX-20, IDTTX-24) khi DB chưa sẵn sàng
        if (user == null) {
            user = com.ems.service.UserStore.authenticate(email, password);
        }

        if (user == null) {
            long lockedSeconds = 0;
            try {
                lockedSeconds = userDAO.lockedSeconds(email);
            } catch (SQLException ignored) {
            }
            if (apiRequest && lockedSeconds > 0) {
                // S1-01: sai 5 lần thì khoá tạm 15 phút, giao diện cần số giây còn lại để đếm ngược.
                response.setHeader("Retry-After", Long.toString(lockedSeconds));
                response.setStatus(423);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(GSON.toJson(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "AUTH_ACCOUNT_TEMP_LOCKED",
                                "message", "Bạn đã nhập sai 5 lần. Vui lòng thử lại sau.",
                                "retryAfterSeconds", lockedSeconds
                        )
                )));
                return;
            }
            if (apiRequest) {
                sendApiError(response, 401, "AUTH_CREDENTIALS_INVALID", INVALID_CREDENTIALS);
            } else {
                request.setAttribute("errorMessage", INVALID_CREDENTIALS);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
            return;
        }

        HttpSession previousSession = request.getSession(false);
        if (previousSession != null) previousSession.invalidate();
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute("currentUser", user);
        session.setMaxInactiveInterval(com.ems.filter.SessionAuthFilter.SESSION_TIMEOUT_SECONDS);
        com.ems.config.SessionBlacklist.registerSession(user.getId(), session.getId());

        String role = roleSlug(user.getPrimaryRole());
        String redirectUrl = roleHome(role);
        if (!apiRequest) {
            // Chuyển hướng nội bộ theo JSP & Servlet
            String jspRedirect = jspHome(role);
            response.sendRedirect(request.getContextPath() + jspRedirect);
            return;
        }

        Map<String, Object> userData = userData(user);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(Map.of(
                "success", true,
                "code", "AUTH_LOGIN_SUCCESS",
                "message", "Đăng nhập thành công.",
                "data", Map.of("user", userData, "redirectUrl", redirectUrl)
        )));
    }

    /** GET /api/auth/me: người đang đăng nhập, đọc lại từ CSDL để vai trò mới có hiệu lực ngay. */
    private void sendMe(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        User user = session == null ? null : (User) session.getAttribute("currentUser");
        try {
            User fresh = user == null ? null : userDAO.findById(user.getId());
            if (fresh != null) {
                if (!"ACTIVE".equalsIgnoreCase(fresh.getStatus()) || fresh.getRoles().isEmpty()) {
                    session.invalidate();
                    sendApiError(response, 401, "AUTH_UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
                    return;
                }
                session.setAttribute("currentUser", fresh);
                user = fresh;
            }
        } catch (SQLException exception) {
            getServletContext().log("Không đọc được người dùng từ CSDL, dùng dữ liệu trong phiên.", exception);
        }
        Map<String, Object> data = new LinkedHashMap<>(userData(user));
        data.put("sessionTimeoutSeconds", session.getMaxInactiveInterval());
        com.ems.security.ApiResponse.success(response, "AUTH_ME_SUCCESS", "Lấy thông tin phiên thành công.", data);
    }

    private static Map<String, Object> userData(User user) {
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getFullName());
        userData.put("role", roleSlug(user.getPrimaryRole()));
        userData.put("roles", user.getRoles().stream().map(LoginApiServlet::roleSlug).toList());
        return userData;
    }

    private static boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().endsWith("/api/auth/login")
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }

    private static String roleSlug(String role) {
        return role == null ? "student" : role.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String roleHome(String role) {
        return switch (role) {
            case "admin" -> "/admin.html";
            case "training-manager" -> "/training-manager.html";
            case "admissions" -> "/admissions.html";
            case "instructor" -> "/instructor.html";
            case "ta" -> "/ta.html";
            case "accountant" -> "/accountant.html";
            case "student" -> "/student.html";
            default -> "/index.html";
        };
    }

    private static String jspHome(String role) {
        return switch (role) {
            case "admin" -> "/admin/users/roles";
            case "instructor" -> "/grade/list";
            case "accountant" -> "/tuition/list";
            case "student" -> "/grade/list";
            case "training-manager" -> "/training-programs";
            default -> "/dashboard.jsp";
        };
    }

    private void sendApiError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(Map.of(
                "success", false,
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "action", Map.of("label", "Quay lại đăng nhập", "href", "/login.html")
                )
        )));
    }
}