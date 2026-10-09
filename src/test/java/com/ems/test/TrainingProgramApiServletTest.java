package com.ems.test;

import com.ems.controller.TrainingProgramApiServlet;
import com.ems.dao.TrainingProgramDAO;
import com.ems.dto.TrainingProgramRequest;
import com.ems.dto.TrainingProgramResponse;
import com.ems.service.TrainingProgramService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử lớp Controller REST API Servlet cho Chương trình đào tạo (S2-04)
 */
public class TrainingProgramApiServletTest {

    private static final Gson GSON = new Gson();
    private TrainingProgramApiServlet servlet;
    private TrainingProgramService service;
    private TrainingProgramDAO dao;

    @BeforeAll
    static void initDatabase() {
        TestDatabaseHelper.initTestDatabase();
    }

    @BeforeEach
    void setUp() {
        TestDatabaseHelper.clearData();
        dao = new TrainingProgramDAO();
        service = new TrainingProgramService(dao);
        servlet = new TrainingProgramApiServlet();
        servlet.setTrainingProgramService(service);
    }

    @Test
    @DisplayName("Servlet GET /api/training-programs -> 200 OK và danh sách")
    void testServletGetList() throws Exception {
        service.create(new TrainingProgramRequest("TEST-01", "Chương trình 1", "Mô tả", 100, new BigDecimal("5000000"), "ACTIVE"));

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("GET", "", null);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(200, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("PROGRAM_LIST_SUCCESS", json.get("code").getAsString());
    }

    @Test
    @DisplayName("Servlet GET /api/training-programs/{id} -> 200 OK")
    void testServletGetDetail() throws Exception {
        TrainingProgramResponse created = service.create(
                new TrainingProgramRequest("DETAIL-01", "Chi tiết", "Mô tả", 120, new BigDecimal("6000000"), "ACTIVE"));

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("GET", "/" + created.getId(), null);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(200, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertEquals("PROGRAM_DETAIL_SUCCESS", json.get("code").getAsString());
        assertEquals("DETAIL-01", json.getAsJsonObject("data").get("code").getAsString());
    }

    @Test
    @DisplayName("Servlet POST /api/training-programs -> 201 Created")
    void testServletPostCreate() throws Exception {
        String body = """
            {
              "code": "JAVA-SERVLET",
              "name": "Java Servlet & JSP",
              "description": "Backend Web Development",
              "duration": 200,
              "standardTuition": 15000000,
              "status": "ACTIVE"
            }
        """;

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("POST", "", body);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(201, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertEquals("PROGRAM_CREATED", json.get("code").getAsString());
        assertEquals("JAVA-SERVLET", json.getAsJsonObject("data").get("code").getAsString());
    }

    @Test
    @DisplayName("Servlet POST với code trùng -> 409 Conflict")
    void testServletPostDuplicateCodeConflict() throws Exception {
        service.create(new TrainingProgramRequest("DUP-CODE", "Gốc", "Mô tả", 100, new BigDecimal("5000000"), "ACTIVE"));

        String body = """
            {
              "code": "DUP-CODE",
              "name": "Trùng lặp",
              "duration": 100,
              "standardTuition": 5000000,
              "status": "ACTIVE"
            }
        """;

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("POST", "", body);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(409, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("PROGRAM_CODE_CONFLICT", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("Servlet POST với dữ liệu không hợp lệ (duration = -10) -> 400 Bad Request")
    void testServletPostValidationFailure() throws Exception {
        String body = """
            {
              "code": "INVALID-REQ",
              "name": "Tên hợp lệ",
              "duration": -10,
              "standardTuition": 5000000,
              "status": "ACTIVE"
            }
        """;

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("POST", "", body);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(400, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertEquals("VALIDATION_ERROR", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("Servlet DELETE khi có lớp đang chạy -> 409 Conflict")
    void testServletDeleteBlockedByRunningClass() throws Exception {
        TrainingProgramResponse prog = service.create(
                new TrainingProgramRequest("BLOCK-DEL", "Chương trình", "Mô tả", 100, new BigDecimal("5000000"), "ACTIVE"));

        // Thêm lớp IN_PROGRESS
        addClass(prog.getId(), "CL-01", "IN_PROGRESS");

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("DELETE", "/" + prog.getId(), null);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(409, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertEquals("PROGRAM_HAS_RUNNING_CLASSES", json.getAsJsonObject("error").get("code").getAsString());
        assertEquals("Không thể xoá chương trình đào tạo vì đang có lớp học đang chạy.",
                json.getAsJsonObject("error").get("message").getAsString());
    }

    @Test
    @DisplayName("Servlet PATCH /api/training-programs/{id}/deactivate -> 200 OK và status = INACTIVE")
    void testServletPatchDeactivate() throws Exception {
        TrainingProgramResponse prog = service.create(
                new TrainingProgramRequest("DEACT-ME", "Chương trình", "Mô tả", 100, new BigDecimal("5000000"), "ACTIVE"));

        TestResponse res = new TestResponse();
        TestRequest req = new TestRequest("PATCH", "/" + prog.getId() + "/deactivate", null);

        servlet.service(req.asServletRequest(), res.asServletResponse());

        assertEquals(200, res.status);
        JsonObject json = GSON.fromJson(res.getBody(), JsonObject.class);
        assertEquals("PROGRAM_DEACTIVATED", json.get("code").getAsString());
        assertEquals("INACTIVE", json.getAsJsonObject("data").get("status").getAsString());
    }

    private void addClass(int programId, String classCode, String status) throws SQLException {
        String sql = "INSERT INTO classes (class_code, class_name, program_id, start_date, max_capacity, status) "
                + "VALUES (?, ?, ?, CURRENT_DATE, 30, ?)";
        try (Connection conn = com.ems.config.DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, classCode);
            stmt.setString(2, "Lớp " + classCode);
            stmt.setInt(3, programId);
            stmt.setString(4, status);
            stmt.executeUpdate();
        }
    }

    // Helper classes giả lập HttpServletRequest và HttpServletResponse
    static class TestRequest {
        final String method;
        final String pathInfo;
        final String body;
        final Map<String, String> parameters = new HashMap<>();

        TestRequest(String method, String pathInfo, String body) {
            this.method = method;
            this.pathInfo = pathInfo;
            this.body = body != null ? body : "";
        }

        HttpServletRequest asServletRequest() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getMethod" -> method;
                        case "getPathInfo" -> pathInfo;
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getReader" -> new BufferedReader(new StringReader(body));
                        case "getInputStream" -> new ServletInputStream() {
                            private final ByteArrayInputStream bais = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                            public boolean isFinished() { return bais.available() == 0; }
                            public boolean isReady() { return true; }
                            public void setReadListener(ReadListener readListener) {}
                            public int read() { return bais.read(); }
                        };
                        case "getCharacterEncoding" -> "UTF-8";
                        case "getContentType" -> "application/json";
                        default -> null;
                    }
            );
        }
    }

    static class TestResponse {
        int status = 200;
        final StringWriter writer = new StringWriter();

        String getBody() {
            return writer.toString();
        }

        HttpServletResponse asServletResponse() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "setStatus" -> {
                            status = (int) args[0];
                            yield null;
                        }
                        case "getStatus" -> status;
                        case "getWriter" -> new PrintWriter(writer);
                        case "setContentType", "setCharacterEncoding" -> null;
                        default -> null;
                    }
            );
        }
    }
}
