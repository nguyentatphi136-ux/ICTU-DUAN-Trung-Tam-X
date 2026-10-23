package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.controller.ScheduleApiServlet;
import com.ems.dto.ScheduleRequest;
import com.ems.model.ClassSession;
import com.ems.model.Classroom;
import com.ems.model.SchoolClass;
import com.ems.model.User;
import com.ems.service.ScheduleConflictService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ScheduleApiServletTest {

    private static final Gson GSON = new Gson();
    private ScheduleApiServlet servlet;
    private ScheduleConflictTest.MockScheduleDAO mockScheduleDAO;
    private ScheduleConflictTest.MockClassroomDAO mockClassroomDAO;
    private ScheduleConflictTest.MockClassDAO mockClassDAO;
    private ScheduleConflictTest.MockUserDAO mockUserDAO;
    private ScheduleConflictService service;

    @BeforeEach
    void setUp() {
        mockScheduleDAO = new ScheduleConflictTest.MockScheduleDAO();
        mockClassroomDAO = new ScheduleConflictTest.MockClassroomDAO();
        mockClassDAO = new ScheduleConflictTest.MockClassDAO();
        mockUserDAO = new ScheduleConflictTest.MockUserDAO();

        service = new ScheduleConflictService(
                mockScheduleDAO,
                mockClassroomDAO,
                mockClassDAO,
                mockUserDAO,
                () -> ScheduleConflictTest.createMockConnection(mockScheduleDAO)
        );

        servlet = new ScheduleApiServlet(service);

        // Seeding
        mockClassroomDAO.save(new Classroom(1, "PH-101", "Phòng 101", 40, "PHYSICAL", "AVAILABLE"));
        mockClassroomDAO.save(new Classroom(2, "LAB-202", "Phòng 202", 35, "PHYSICAL", "AVAILABLE"));
        mockClassDAO.save(new SchoolClass(1L, "CNTT-K23-01", "Lớp Java 01", 1, "IN_PROGRESS"));
        mockClassDAO.save(new SchoolClass(2L, "CNTT-K23-02", "Lớp Web 02", 1, "IN_PROGRESS"));
        mockUserDAO.save(new User(3L, "NV-GV-01", "giangvien@edumanager.vn", "Trần Thị Giảng", RoleConstant.INSTRUCTOR));
    }

    @Test
    @DisplayName("API POST /api/schedules/check-conflicts: Trả về HTTP 200 và hasConflict = false khi lịch trống")
    void testCheckConflictsNoConflict() throws Exception {
        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/check-conflicts", "POST", GSON.toJson(reqDto));

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(200, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertFalse(json.get("hasConflict").getAsBoolean());
        assertEquals(0, json.get("conflicts").getAsJsonArray().size());
    }

    @Test
    @DisplayName("API POST /api/schedules/check-conflicts: Trả về HTTP 200 và hasConflict = true kèm chi tiết khi trùng giảng viên")
    void testCheckConflictsHasConflict() throws Exception {
        // Có sẵn buổi học 09:00 - 11:00
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/check-conflicts", "POST", GSON.toJson(reqDto));

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(200, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("hasConflict").getAsBoolean());
        assertEquals(1, json.get("conflicts").getAsJsonArray().size());
        JsonObject conflict = json.get("conflicts").getAsJsonArray().get(0).getAsJsonObject();
        assertEquals("INSTRUCTOR_CONFLICT", conflict.get("type").getAsString());
        assertTrue(conflict.get("message").getAsString().contains("Trần Thị Giảng"));
    }

    @Test
    @DisplayName("API POST /api/schedules: Tạo lịch hợp lệ thành công trả về HTTP 201 Created")
    void testCreateScheduleSuccess() throws Exception {
        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/", "POST", GSON.toJson(reqDto), trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(201, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("SCHEDULE_CREATED", json.get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/schedules: Xung đột lịch không có ghi đè bị từ chối với HTTP 409 Conflict")
    void testCreateScheduleConflictRejected() throws Exception {
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/", "POST", GSON.toJson(reqDto), trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(409, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("SCHEDULE_CONFLICT", json.get("code").getAsString());
        assertTrue(json.get("hasConflict").getAsBoolean());
    }

    @Test
    @DisplayName("API POST /api/schedules: Quản lý đào tạo xác nhận ghi đè có lý do được lưu với HTTP 201 Created")
    void testCreateScheduleOverrideSuccess() throws Exception {
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");
        reqDto.setOverrideConflict(true);
        reqDto.setOverrideReason("Lãnh đạo trung tâm phê duyệt dạy bù buổi sáng");

        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/", "POST", GSON.toJson(reqDto), trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(201, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(1, mockScheduleDAO.auditLogs.size());
        assertEquals("OVERRIDE_SCHEDULE_CONFLICT", mockScheduleDAO.auditLogs.get(0).getAction());
    }

    @Test
    @DisplayName("API POST /api/schedules: Giảng viên cố tình gửi overrideConflict = true bị chặn với HTTP 403 Forbidden")
    void testCreateScheduleOverrideForbiddenForOtherRoles() throws Exception {
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");
        reqDto.setOverrideConflict(true);
        reqDto.setOverrideReason("Tự ý ghi đè");

        User instructor = new User(3L, "giangvien@edumanager.vn", "Trần Thị Giảng", "ACTIVE", List.of(RoleConstant.INSTRUCTOR));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/", "POST", GSON.toJson(reqDto), instructor);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(403, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("FORBIDDEN_OVERRIDE", json.get("error").getAsJsonObject().get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/schedules: Thời gian kết thúc <= thời gian bắt đầu trả về HTTP 400 Bad Request")
    void testCreateScheduleValidationTimeError() throws Exception {
        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T10:00:00", "2026-11-01T08:00:00");

        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/", "POST", GSON.toJson(reqDto));

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(400, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("VALIDATION_ERROR", json.get("error").getAsJsonObject().get("code").getAsString());
    }

    @Test
    @DisplayName("API PUT /api/schedules/{id}: Cập nhật lịch học thành công")
    void testUpdateScheduleSuccess() throws Exception {
        addExistingSession(15L, 1L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest reqDto = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T11:30:00");

        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/15", "PUT", GSON.toJson(reqDto), trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(200, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("SCHEDULE_UPDATED", json.get("code").getAsString());
    }

    @Test
    @DisplayName("API DELETE /api/schedules/{id}: Hủy buổi học thành công")
    void testCancelScheduleSuccess() throws Exception {
        addExistingSession(15L, 1L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/15", "DELETE", null, trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(200, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("SCHEDULE_CANCELLED", json.get("code").getAsString());
    }

    @Test
    @DisplayName("API PATCH /api/schedules/{id}: Cập nhật một phần (chỉ đổi phòng học) thành công")
    void testPatchSchedulePartialUpdateSuccess() throws Exception {
        addExistingSession(16L, 1L, 3L, 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED");

        String patchJson = "{\"roomId\": 2}";
        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/16", "PATCH", patchJson, trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(200, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("SCHEDULE_UPDATED", json.get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/schedules/{id}/cancel: Hủy buổi học với lý do hợp lệ thành công")
    void testPostCancelScheduleWithReasonSuccess() throws Exception {
        addExistingSession(17L, 1L, 3L, 1, LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");

        String cancelBody = "{\"reason\": \"Giảng viên bận việc gia đình đột xuất\"}";
        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/17/cancel", "POST", cancelBody, trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(200, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("SCHEDULE_CANCELLED", json.get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/schedules/{id}/cancel: Hủy không có lý do bị từ chối HTTP 400")
    void testPostCancelScheduleWithoutReasonRejected() throws Exception {
        addExistingSession(18L, 1L, 3L, 1, LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");

        String emptyCancelBody = "{}";
        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/18/cancel", "POST", emptyCancelBody, trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(400, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("VALIDATION_ERROR", json.get("error").getAsJsonObject().get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/schedules/{id}/cancel: Hủy với lý do toàn khoảng trắng bị từ chối HTTP 400")
    void testPostCancelScheduleWhitespaceReasonRejected() throws Exception {
        addExistingSession(19L, 1L, 3L, 1, LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");

        String wsCancelBody = "{\"reason\": \"    \"}";
        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/19/cancel", "POST", wsCancelBody, trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(400, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("VALIDATION_ERROR", json.get("error").getAsJsonObject().get("code").getAsString());
    }

    @Test
    @DisplayName("API PATCH /api/schedules/{id}: Sửa buổi học đã điểm danh bị từ chối HTTP 400")
    void testPatchAttendedSessionRejected() throws Exception {
        addExistingSession(20L, 1L, 3L, 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED");
        mockScheduleDAO.markAttended(20L);

        String patchJson = "{\"roomId\": 2}";
        User trainingMgr = new User(2L, "quanly@edumanager.vn", "Phạm Quản Lý", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        TestResponse resp = new TestResponse();
        TestRequest req = new TestRequest("/20", "PATCH", patchJson, trainingMgr);

        servlet.service(req.getProxy(), resp.getProxy());

        assertEquals(400, resp.getStatus());
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertTrue(json.get("error").getAsJsonObject().get("message").getAsString().contains("đã được điểm danh"));
    }

    private void addExistingSession(Long id, Long classId, Long instructorId, Integer roomId,
                                    LocalTime start, LocalTime end, String status) {
        ClassSession s = new ClassSession();
        s.setId(id);
        s.setClassId(classId);
        s.setClassCode("CNTT-K23-0" + classId);
        s.setClassName("Lớp học #" + classId);
        s.setInstructorId(instructorId);
        s.setInstructorName("Trần Thị Giảng");
        s.setClassroomId(roomId);
        s.setRoomCode("PH-10" + roomId);
        s.setSessionDate(LocalDate.of(2026, 11, 1));
        s.setStartTime(start);
        s.setEndTime(end);
        s.setStatus(status);
        mockScheduleDAO.sessions.add(s);
    }

    // =========================================================================
    // Lightweight Test Request & Response Mocks
    // =========================================================================
    static class TestRequest {
        private final String pathInfo;
        private final String method;
        private final String body;
        private final User user;
        private final Map<String, Object> attributes = new HashMap<>();

        public TestRequest(String pathInfo, String method, String body) {
            this(pathInfo, method, body, null);
        }

        public TestRequest(String pathInfo, String method, String body, User user) {
            this.pathInfo = pathInfo;
            this.method = method;
            this.body = body != null ? body : "";
            this.user = user;
            if (user != null) attributes.put("authenticatedUserId", user.getId());
        }

        public HttpServletRequest getProxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        if ("getPathInfo".equals(name)) return pathInfo;
                        if ("getMethod".equals(name)) return method;
                        if ("getAttribute".equals(name)) return attributes.get((String) args[0]);
                        if ("setAttribute".equals(name)) { attributes.put((String) args[0], args[1]); return null; }
                        if ("getReader".equals(name)) {
                            return new BufferedReader(new StringReader(body));
                        }
                        if ("getParameter".equals(name)) return null;
                        if ("getSession".equals(name)) {
                            return createSessionProxy(user);
                        }
                        if ("getCharacterEncoding".equals(name)) return "UTF-8";
                        if ("getContentType".equals(name)) return "application/json";
                        return null;
                    }
            );
        }

        private HttpSession createSessionProxy(User u) {
            if (u == null) return null;
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, m, args) -> {
                        if ("getAttribute".equals(m.getName()) && "currentUser".equals(args[0])) return u;
                        return null;
                    }
            );
        }
    }

    static class TestResponse {
        private int status = 200;
        private final StringWriter stringWriter = new StringWriter();
        private final PrintWriter printWriter = new PrintWriter(stringWriter);

        public int getStatus() { return status; }
        public String getContent() { return stringWriter.toString(); }

        public HttpServletResponse getProxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        if ("setStatus".equals(name)) { status = (Integer) args[0]; return null; }
                        if ("getStatus".equals(name)) return status;
                        if ("getWriter".equals(name)) return printWriter;
                        if ("setContentType".equals(name) || "setCharacterEncoding".equals(name)) return null;
                        return null;
                    }
            );
        }
    }
}
