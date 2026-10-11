package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.controller.ClassApiServlet;
import com.ems.dao.ClassDAO;
import com.ems.dao.ProgramDAO;
import com.ems.model.User;
import com.ems.service.ClassService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử bảo mật & phân quyền API mở lớp (User Story S3-05 Acceptance Criteria 6)
 * Kiểm tra các vai trò:
 * - Training Manager: CÓ quyền mở lớp -> HTTP 201 Created
 * - Admin: CÓ quyền mở lớp -> HTTP 201 Created
 * - Admissions: KHÔNG ĐƯỢC PHÉP -> HTTP 403 Forbidden
 * - Student: KHÔNG ĐƯỢC PHÉP -> HTTP 403 Forbidden
 * - Instructor: KHÔNG ĐƯỢC PHÉP -> HTTP 403 Forbidden
 * - TA: KHÔNG ĐƯỢC PHÉP -> HTTP 403 Forbidden
 * - Accountant: KHÔNG ĐƯỢC PHÉP -> HTTP 403 Forbidden
 * - Guest: KHÔNG ĐƯỢC PHÉP -> HTTP 403 Forbidden
 * - Unauthenticated (Chưa đăng nhập): KHÔNG ĐƯỢC PHÉP -> HTTP 401 Unauthorized
 */
public class ClassApiServletSecurityTest {

    private ClassApiServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private StringWriter responseWriter;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws SQLException, IOException {
        TestDbHelper.initDatabase();
        ClassDAO classDAO = new ClassDAO(TestDbHelper::getConnection);
        ProgramDAO programDAO = new ProgramDAO(TestDbHelper::getConnection);
        ClassService classService = new ClassService(classDAO, programDAO, TestDbHelper::getConnection);
        servlet = new ClassApiServlet(classService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
    }

    private void setupRequestBody(String json) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(request.getReader()).thenReturn(reader);
    }

    @Test
    @DisplayName("TC 1 & AC 6: Training Manager mở lớp thành công -> HTTP 201 Created")
    void testTrainingManagerCanOpenClass() throws ServletException, IOException {
        User trainingManager = new User(2L, "quanlydaotao@edumanager.vn", "Phạm Thị Quản", "ACTIVE",
                Collections.singletonList(RoleConstant.TRAINING_MANAGER));
        when(session.getAttribute("currentUser")).thenReturn(trainingManager);

        String json = """
            {
                "classCode": "CNTT-TM-01",
                "programId": 1,
                "expectedStartDate": "2026-11-01",
                "maxStudents": 30,
                "studyMode": "Offline"
            }
        """;
        setupRequestBody(json);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertTrue(resJson.get("success").getAsBoolean());
        assertEquals("CLASS_OPENED_SUCCESS", resJson.get("code").getAsString());
        JsonObject data = resJson.getAsJsonObject("data");
        assertEquals("CNTT-TM-01", data.get("classCode").getAsString());
        assertEquals("PLANNING", data.get("status").getAsString());
        assertEquals(3, data.get("subjectCount").getAsInt());
    }

    @Test
    @DisplayName("Admin có toàn quyền mở lớp -> HTTP 201 Created")
    void testAdminCanOpenClass() throws ServletException, IOException {
        User admin = new User(1L, "admin@edumanager.vn", "Quản trị viên", "ACTIVE",
                Collections.singletonList(RoleConstant.ADMIN));
        when(session.getAttribute("currentUser")).thenReturn(admin);

        String json = """
            {
                "classCode": "CNTT-ADMIN-01",
                "programId": 1,
                "expectedStartDate": "2026-11-01",
                "maxStudents": 25,
                "studyMode": "Online"
            }
        """;
        setupRequestBody(json);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertTrue(resJson.get("success").getAsBoolean());
    }

    @Test
    @DisplayName("TC 10: Tư vấn tuyển sinh (Admissions) không được phép mở lớp -> HTTP 403 Forbidden")
    void testAdmissionsCannotOpenClass() throws ServletException, IOException {
        User admissions = new User(7L, "tuvan@edumanager.vn", "Hoàng Văn Tư", "ACTIVE",
                Collections.singletonList(RoleConstant.ADMISSIONS));
        when(session.getAttribute("currentUser")).thenReturn(admissions);

        setupRequestBody("{\"classCode\":\"CNTT-TEST\",\"programId\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
        assertEquals("AUTH_FORBIDDEN", resJson.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("TC 11: Học viên (Student) không được phép mở lớp -> HTTP 403 Forbidden")
    void testStudentCannotOpenClass() throws ServletException, IOException {
        User student = new User(5L, "hocvien@edumanager.vn", "Nguyễn Văn Học", "ACTIVE",
                Collections.singletonList(RoleConstant.STUDENT));
        when(session.getAttribute("currentUser")).thenReturn(student);

        setupRequestBody("{\"classCode\":\"CNTT-TEST\",\"programId\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
        assertEquals("AUTH_FORBIDDEN", resJson.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("Giảng viên (Instructor) không được phép mở lớp -> HTTP 403 Forbidden")
    void testInstructorCannotOpenClass() throws ServletException, IOException {
        User instructor = new User(3L, "giangvien@edumanager.vn", "Trần Thị Giảng", "ACTIVE",
                Collections.singletonList(RoleConstant.INSTRUCTOR));
        when(session.getAttribute("currentUser")).thenReturn(instructor);

        setupRequestBody("{\"classCode\":\"CNTT-TEST\",\"programId\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
    }

    @Test
    @DisplayName("Trợ giảng (TA) không được phép mở lớp -> HTTP 403 Forbidden")
    void testTeachingAssistantCannotOpenClass() throws ServletException, IOException {
        User ta = new User(4L, "trogiang@edumanager.vn", "Lê Văn Trợ", "ACTIVE",
                Collections.singletonList(RoleConstant.TEACHING_ASSISTANT));
        when(session.getAttribute("currentUser")).thenReturn(ta);

        setupRequestBody("{\"classCode\":\"CNTT-TEST\",\"programId\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
    }

    @Test
    @DisplayName("Kế toán (Accountant) không được phép mở lớp -> HTTP 403 Forbidden")
    void testAccountantCannotOpenClass() throws ServletException, IOException {
        User accountant = new User(6L, "ketoan@edumanager.vn", "Đỗ Thị Kế", "ACTIVE",
                Collections.singletonList(RoleConstant.ACCOUNTANT));
        when(session.getAttribute("currentUser")).thenReturn(accountant);

        setupRequestBody("{\"classCode\":\"CNTT-TEST\",\"programId\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
    }

    @Test
    @DisplayName("TC 12: Người chưa đăng nhập không thể mở lớp -> HTTP 401 Unauthorized")
    void testUnauthenticatedUserCannotOpenClass() throws ServletException, IOException {
        when(session.getAttribute("currentUser")).thenReturn(null);
        when(request.getSession(false)).thenReturn(null);

        setupRequestBody("{\"classCode\":\"CNTT-TEST\",\"programId\":1}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
        assertEquals("AUTH_UNAUTHORIZED", resJson.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("TC 15: Kiểm tra API trả đúng mã lỗi 400 khi JSON sai cú pháp")
    void testInvalidJsonReturnsBadRequest() throws ServletException, IOException {
        User trainingManager = new User(2L, "quanlydaotao@edumanager.vn", "Phạm Thị Quản", "ACTIVE",
                Collections.singletonList(RoleConstant.TRAINING_MANAGER));
        when(session.getAttribute("currentUser")).thenReturn(trainingManager);

        setupRequestBody("{ invalid json body");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
        assertEquals("REQUEST_INVALID_JSON", resJson.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("TC 15: Kiểm tra API trả đúng mã lỗi 409 khi mã lớp bị trùng")
    void testDuplicateClassCodeReturnsConflict() throws ServletException, IOException {
        User trainingManager = new User(2L, "quanlydaotao@edumanager.vn", "Phạm Thị Quản", "ACTIVE",
                Collections.singletonList(RoleConstant.TRAINING_MANAGER));
        when(session.getAttribute("currentUser")).thenReturn(trainingManager);

        String json = """
            {
                "classCode": "CNTT-DUPLICATE",
                "programId": 1,
                "expectedStartDate": "2026-11-01",
                "maxStudents": 30,
                "studyMode": "Offline"
            }
        """;

        // Lần 1: Thành công
        setupRequestBody(json);
        servlet.doPost(request, response);
        verify(response).setStatus(HttpServletResponse.SC_CREATED);

        // Lần 2: Trùng mã lớp
        responseWriter.getBuffer().setLength(0);
        reset(response);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        setupRequestBody(json);
        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertFalse(resJson.get("success").getAsBoolean());
        assertEquals("CLASS_CODE_ALREADY_EXISTS", resJson.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("Kiểm tra GET /api/classes trả danh sách lớp thành công -> HTTP 200 OK")
    void testGetClassList() throws ServletException, IOException {
        User trainingManager = new User(2L, "quanlydaotao@edumanager.vn", "Phạm Thị Quản", "ACTIVE",
                Collections.singletonList(RoleConstant.TRAINING_MANAGER));
        when(session.getAttribute("currentUser")).thenReturn(trainingManager);
        when(request.getPathInfo()).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        JsonObject resJson = gson.fromJson(responseWriter.toString(), JsonObject.class);
        assertTrue(resJson.get("success").getAsBoolean());
        assertEquals("CLASS_LIST_SUCCESS", resJson.get("code").getAsString());
    }
}
