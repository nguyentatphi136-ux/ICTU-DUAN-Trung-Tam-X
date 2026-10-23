package com.ems.controller;

import com.ems.dao.PermissionDAO;
import com.ems.dto.CreateRoomRequest;
import com.ems.dto.RoomResponse;
import com.ems.dto.UpdateRoomRequest;
import com.ems.model.User;
import com.ems.service.RoomService;
import com.ems.test.MockRoomDAO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp REST API Controller RoomApiServlet
 */
class RoomApiServletTest {

    private static final Gson GSON = new Gson();
    private RoomApiServlet servlet;
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        MockRoomDAO mockRoomDAO = new MockRoomDAO();
        this.roomService = new RoomService(mockRoomDAO);
        PermissionDAO mockPermissionDAO = new PermissionDAO() {
            @Override
            public boolean hasPermission(long userId, String permissionCode) {
                return userId == 1L || userId == 2L; // 1: Admin, 2: Training Manager
            }
        };
        this.servlet = new RoomApiServlet(roomService, mockPermissionDAO);
    }

    @Test
    @DisplayName("API POST /api/rooms: Chưa đăng nhập nhận HTTP 401 Unauthorized")
    void testPostUnauthenticatedReturns401() throws Exception {
        FakeRequest req = new FakeRequest("POST", "/");
        req.setBody("{\"roomCode\":\"PH-101\",\"roomName\":\"Phòng 101\",\"roomType\":\"PHYSICAL\",\"capacity\":20}");
        FakeResponse resp = new FakeResponse();

        servlet.doPost(req.proxy(), resp.proxy());

        assertEquals(401, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("AUTH_UNAUTHORIZED", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/rooms: Giảng viên hoặc Học viên (không có quyền) nhận HTTP 403 Forbidden")
    void testPostUnauthorizedReturns403() throws Exception {
        FakeRequest req = new FakeRequest("POST", "/");
        User student = new User(5L, "HV01", "student@ems.vn", "Nguyễn Học Viên", "Student");
        student.setRoles(List.of("Student"));
        req.setCurrentUser(student);
        req.setBody("{\"roomCode\":\"PH-101\",\"roomName\":\"Phòng 101\",\"roomType\":\"PHYSICAL\",\"capacity\":20}");
        FakeResponse resp = new FakeResponse();

        servlet.doPost(req.proxy(), resp.proxy());

        assertEquals(403, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("AUTH_FORBIDDEN", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("API POST /api/rooms: Training Manager tạo phòng vật lý thành công nhận HTTP 201 Created")
    void testPostCreatePhysicalRoomReturns201() throws Exception {
        FakeRequest req = new FakeRequest("POST", "/");
        User tm = new User(2L, "NV02", "tm@ems.vn", "Trần Quản Lý", "TrainingManager");
        tm.setRoles(List.of("TrainingManager"));
        req.setCurrentUser(tm);
        req.setBody("{\"roomCode\":\"PH-101\",\"roomName\":\"Phòng 101\",\"roomType\":\"PHYSICAL\",\"capacity\":25,\"location\":\"Tầng 1\",\"isActive\":true}");
        FakeResponse resp = new FakeResponse();

        servlet.doPost(req.proxy(), resp.proxy());

        assertEquals(201, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("ROOM_CREATED", json.get("code").getAsString());
        assertEquals("PH-101", json.getAsJsonObject("data").get("roomCode").getAsString());
    }

    @Test
    @DisplayName("API POST /api/rooms: Tạo phòng trùng mã nhận HTTP 409 Conflict")
    void testPostDuplicateCodeReturns409() throws Exception {
        CreateRoomRequest cr = new CreateRoomRequest();
        cr.setRoomCode("PH-101");
        cr.setRoomName("Phòng 101");
        cr.setRoomType("PHYSICAL");
        cr.setCapacity(20);
        roomService.createRoom(cr);

        FakeRequest req = new FakeRequest("POST", "/");
        User tm = new User(2L, "NV02", "tm@ems.vn", "Trần Quản Lý", "TrainingManager");
        tm.setRoles(List.of("TrainingManager"));
        req.setCurrentUser(tm);
        req.setBody("{\"roomCode\":\"PH-101\",\"roomName\":\"Phòng 101 mới\",\"roomType\":\"PHYSICAL\",\"capacity\":30}");
        FakeResponse resp = new FakeResponse();

        servlet.doPost(req.proxy(), resp.proxy());

        assertEquals(409, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("ROOM_CODE_ALREADY_EXISTS", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("API GET /api/rooms/{id}: Lấy chi tiết phòng học thành công nhận HTTP 200")
    void testGetRoomByIdReturns200() throws Exception {
        CreateRoomRequest cr = new CreateRoomRequest();
        cr.setRoomCode("PH-102");
        cr.setRoomName("Phòng 102");
        cr.setRoomType("PHYSICAL");
        cr.setCapacity(25);
        RoomResponse created = roomService.createRoom(cr);

        FakeRequest req = new FakeRequest("GET", "/" + created.getId());
        User tm = new User(2L, "NV02", "tm@ems.vn", "Trần Quản Lý", "TrainingManager");
        tm.setRoles(List.of("TrainingManager"));
        req.setCurrentUser(tm);
        FakeResponse resp = new FakeResponse();

        servlet.doGet(req.proxy(), resp.proxy());

        assertEquals(200, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("ROOM_DETAIL_SUCCESS", json.get("code").getAsString());
    }

    @Test
    @DisplayName("API GET /api/rooms/{id}: ID không tồn tại nhận HTTP 404 Not Found")
    void testGetRoomByIdNotFoundReturns404() throws Exception {
        FakeRequest req = new FakeRequest("GET", "/99999");
        User tm = new User(2L, "NV02", "tm@ems.vn", "Trần Quản Lý", "TrainingManager");
        tm.setRoles(List.of("TrainingManager"));
        req.setCurrentUser(tm);
        FakeResponse resp = new FakeResponse();

        servlet.doGet(req.proxy(), resp.proxy());

        assertEquals(404, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("ROOM_NOT_FOUND", json.getAsJsonObject("error").get("code").getAsString());
    }

    @Test
    @DisplayName("API GET /api/rooms/{id}/validate-capacity: Sĩ số 30 vào phòng 20 chỗ nhận HTTP 400 và code ROOM_CAPACITY_EXCEEDED")
    void testValidateCapacityExceededReturns400() throws Exception {
        CreateRoomRequest cr = new CreateRoomRequest();
        cr.setRoomCode("PH-103");
        cr.setRoomName("Phòng 20 chỗ");
        cr.setRoomType("PHYSICAL");
        cr.setCapacity(20);
        RoomResponse created = roomService.createRoom(cr);

        FakeRequest req = new FakeRequest("GET", "/" + created.getId() + "/validate-capacity");
        req.setParameter("studentCount", "30");
        User tm = new User(2L, "NV02", "tm@ems.vn", "Trần Quản Lý", "TrainingManager");
        tm.setRoles(List.of("TrainingManager"));
        req.setCurrentUser(tm);
        FakeResponse resp = new FakeResponse();

        servlet.doGet(req.proxy(), resp.proxy());

        assertEquals(400, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertFalse(json.get("success").getAsBoolean());
        JsonObject err = json.getAsJsonObject("error");
        assertEquals("ROOM_CAPACITY_EXCEEDED", err.get("code").getAsString());
        assertEquals(30, err.getAsJsonObject("details").get("studentCount").getAsInt());
        assertEquals(20, err.getAsJsonObject("details").get("roomCapacity").getAsInt());
    }

    @Test
    @DisplayName("API DELETE /api/rooms/{id}: Xóa thành công nhận HTTP 200")
    void testDeleteRoomReturns200() throws Exception {
        CreateRoomRequest cr = new CreateRoomRequest();
        cr.setRoomCode("PH-104");
        cr.setRoomName("Phòng cần xóa");
        cr.setRoomType("PHYSICAL");
        cr.setCapacity(20);
        RoomResponse created = roomService.createRoom(cr);

        FakeRequest req = new FakeRequest("DELETE", "/" + created.getId());
        User tm = new User(2L, "NV02", "tm@ems.vn", "Trần Quản Lý", "TrainingManager");
        tm.setRoles(List.of("TrainingManager"));
        req.setCurrentUser(tm);
        FakeResponse resp = new FakeResponse();

        servlet.doDelete(req.proxy(), resp.proxy());

        assertEquals(200, resp.status);
        JsonObject json = GSON.fromJson(resp.getContent(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals("ROOM_DELETED", json.get("code").getAsString());
    }

    // =========================================================================
    // LIGHTWEIGHT TEST HARNESS CHO HTTP SERVLET REQUEST / RESPONSE
    // =========================================================================
    static class FakeRequest {
        private final String method;
        private final String pathInfo;
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private String body = "";
        private User currentUser;

        public FakeRequest(String method, String pathInfo) {
            this.method = method;
            this.pathInfo = pathInfo;
        }

        public void setParameter(String key, String value) {
            parameters.put(key, value);
        }

        public void setBody(String body) {
            this.body = body != null ? body : "";
        }

        public void setCurrentUser(User user) {
            this.currentUser = user;
            if (user != null) {
                attributes.put("authenticatedUserId", user.getId());
            }
        }

        public HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        switch (name) {
                            case "getMethod": return method;
                            case "getPathInfo": return pathInfo;
                            case "getParameter": return parameters.get(args[0]);
                            case "getAttribute": return attributes.get(args[0]);
                            case "setAttribute":
                                attributes.put((String) args[0], args[1]);
                                return null;
                            case "getReader":
                                return new BufferedReader(new StringReader(body));
                            case "getSession":
                                if (currentUser != null) {
                                    return createSessionProxy(currentUser);
                                }
                                return null;
                            default:
                                if (m.getReturnType().equals(boolean.class)) return false;
                                if (m.getReturnType().equals(int.class)) return 0;
                                return null;
                        }
                    }
            );
        }

        private HttpSession createSessionProxy(User user) {
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, m, args) -> {
                        if ("getAttribute".equals(m.getName()) && "currentUser".equals(args[0])) {
                            return user;
                        }
                        if ("getId".equals(m.getName())) return "fake-session-id";
                        return null;
                    }
            );
        }
    }

    static class FakeResponse {
        int status = 200;
        String contentType;
        private final StringWriter out = new StringWriter();

        public String getContent() {
            return out.toString();
        }

        public HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> {
                        String name = m.getName();
                        switch (name) {
                            case "setStatus":
                                status = (int) args[0];
                                return null;
                            case "getStatus":
                                return status;
                            case "setContentType":
                                contentType = (String) args[0];
                                return null;
                            case "getWriter":
                                return new PrintWriter(out);
                            default:
                                if (m.getReturnType().equals(boolean.class)) return false;
                                if (m.getReturnType().equals(int.class)) return 0;
                                return null;
                        }
                    }
            );
        }
    }
}
