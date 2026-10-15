package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.controller.LeadApiServlet;
import com.ems.controller.PublicApiServlet;
import com.ems.controller.SubjectApiServlet;
import com.ems.controller.TrainingProgramApiServlet;
import com.ems.test.TrainingProgramApiServletTest.TestRequest;
import com.ems.test.TrainingProgramApiServletTest.TestResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Gọi thẳng các servlet S2 với request giả: định tuyến, đọc JSON, mã HTTP và mã lỗi. */
class S2ApiServletTest {
    private static final Gson GSON = new Gson();

    @BeforeAll
    static void initDatabase() {
        TestDatabaseHelper.initTestDatabase();
    }

    @BeforeEach
    void reset() throws SQLException {
        TestDatabaseHelper.clearData();
        for (String t : new String[] {"subject_lessons", "program_subject_prereqs", "lead_assignments", "leads"}) exec("DELETE FROM " + t);
        exec("INSERT INTO programs (id, program_code, program_name, status) VALUES (1, 'WEB-FS', 'Lập trình web', 'ACTIVE')");
        exec("INSERT INTO roles (id, role_code, role_name) VALUES (2, 'TRAINING_MANAGER', 'Quản lý đào tạo'), (3, 'ADMISSIONS', 'Tư vấn')");
        exec("INSERT INTO permissions (id, permission_code, permission_name, module) VALUES (1, 'LEAD_ASSIGN', 'Phân công', 'ADMISSIONS')");
        exec("INSERT INTO role_permissions (role_id, permission_id) VALUES (2, 1)");
        exec("INSERT INTO users (id, user_code, email, password_hash, full_name) VALUES (10, 'U10', 'ha@x.vn', 'x', 'TRAN THU HA'), "
                + "(20, 'U20', 'ql@x.vn', 'x', 'QUAN LY')");
        exec("INSERT INTO user_roles (user_id, role_id) VALUES (10, 3), (20, 2)");
    }

    private static void exec(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private record Result(int status, JsonObject json) {
        String code() {
            return json.has("code") ? json.get("code").getAsString() : json.getAsJsonObject("error").get("code").getAsString();
        }
    }

    /** Gọi servlet như SessionAuthFilter đã cho qua với người dùng actorId (null là khách chưa đăng nhập). */
    private static Result call(HttpServlet servlet, String method, String path, String body, Long actorId, Map<String, String> params)
            throws Exception {
        TestRequest base = new TestRequest(method, path, body);
        if (params != null) base.parameters.putAll(params);
        HttpServletRequest inner = base.asServletRequest();
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (p, m, args) -> "getAttribute".equals(m.getName()) && "authenticatedUserId".equals(args[0]) ? actorId : m.invoke(inner, args));
        TestResponse res = new TestResponse();
        servlet.service(req, res.asServletResponse());
        return new Result(res.status, GSON.fromJson(res.getBody(), JsonObject.class));
    }

    private static Result call(HttpServlet servlet, String method, String path, String body, Long actorId) throws Exception {
        return call(servlet, method, path, body, actorId, null);
    }

    @Test
    @DisplayName("Môn học và buổi học qua /api/subjects")
    void subjects() throws Exception {
        SubjectApiServlet s = new SubjectApiServlet();
        Result created = call(s, "POST", "", "{\"code\":\"web-01\",\"name\":\"HTML\",\"sessions\":2,\"weight\":1.5,\"programCodes\":[\"WEB-FS\"]}", 20L);
        assertEquals(201, created.status());
        assertEquals("WEB-01", created.json().getAsJsonObject("data").get("code").getAsString());

        assertEquals(400, call(s, "POST", "", "{\"code\":\"X\",\"name\":\"Y\",\"sessions\":12.5}", 20L).status());
        assertEquals(400, call(s, "POST", "", "{không phải json", 20L).status());
        assertEquals(409, call(s, "POST", "", "{\"code\":\"WEB-01\",\"name\":\"Y\",\"sessions\":3}", 20L).status());

        assertEquals(201, call(s, "POST", "/WEB-01/lessons", "{\"topic\":\"Cấu trúc trang\",\"goal\":\"Dựng trang\"}", 20L).status());
        Result put = call(s, "PUT", "/WEB-01/lessons/1", "{\"topic\":\"Thẻ HTML\"}", 20L);
        assertEquals("Thẻ HTML", put.json().getAsJsonArray("data").get(0).getAsJsonObject().get("topic").getAsString());
        assertEquals(404, call(s, "PUT", "/WEB-01/lessons/9", "{\"topic\":\"A\"}", 20L).status());

        Result list = call(s, "GET", "", null, 20L, Map.of("q", "web", "program", "WEB-FS"));
        assertEquals(1, list.json().getAsJsonObject("data").get("total").getAsInt());

        assertEquals(403, call(s, "DELETE", "/1/purge", null, 20L).status(), "Không phải ADMIN thì không xoá vĩnh viễn");
        assertEquals("SUBJECT_TRASHED", call(s, "DELETE", "/WEB-01", null, 20L).code());
        assertEquals(404, call(s, "GET", "/WEB-01", null, 20L).status());
        assertEquals(404, call(s, "GET", "/a/b/c", null, 20L).status());
    }

    @Test
    @DisplayName("Lộ trình qua /api/training-programs/{code}/subjects")
    void curriculum() throws Exception {
        SubjectApiServlet s = new SubjectApiServlet();
        call(s, "POST", "", "{\"code\":\"A-01\",\"name\":\"A\",\"sessions\":2}", 20L);
        call(s, "POST", "", "{\"code\":\"B-01\",\"name\":\"B\",\"sessions\":2}", 20L);
        TrainingProgramApiServlet p = new TrainingProgramApiServlet();
        p.init();
        assertEquals(200, call(p, "POST", "/WEB-FS/subjects", "{\"subjectCode\":\"A-01\"}", 20L).status());
        assertEquals(200, call(p, "POST", "/WEB-FS/subjects", "{\"subjectCode\":\"B-01\"}", 20L).status());
        assertEquals(409, call(p, "POST", "/WEB-FS/subjects", "{\"subjectCode\":\"B-01\"}", 20L).status());

        Result prereq = call(p, "PUT", "/WEB-FS/subjects/B-01/prerequisites", "{\"prereq\":[\"A-01\"]}", 20L);
        assertEquals("A-01", prereq.json().getAsJsonArray("data").get(1).getAsJsonObject().getAsJsonArray("prereq").get(0).getAsString());
        assertEquals("PREREQ_NOT_BEFORE", call(p, "PUT", "/WEB-FS/subjects/A-01/prerequisites", "{\"prereq\":[\"B-01\"]}", 20L).code());

        Result removed = call(p, "DELETE", "/WEB-FS/subjects/A-01", null, 20L);
        assertEquals(1, removed.json().getAsJsonArray("data").size());
    }

    @Test
    @DisplayName("Lead: quản lý thêm và phân công; tư vấn viên chỉ thấy lead của mình")
    void leads() throws Exception {
        LeadApiServlet s = new LeadApiServlet();
        assertEquals(401, call(s, "GET", "", null, null).status());

        Result created = call(s, "POST", "", "{\"name\":\"LEAD A\",\"phone\":\"0912 000 001\",\"programCode\":\"WEB-FS\"}", 20L);
        assertEquals(201, created.status());
        long id = created.json().getAsJsonObject("data").get("id").getAsLong();
        // ApiResponse bỏ trường null khỏi JSON, nên lead chưa phân công không có ownerId.
        assertFalse(created.json().getAsJsonObject("data").has("ownerId"), "Quản lý thêm thì chưa phân công");

        assertEquals(0, call(s, "GET", "", null, 10L).json().getAsJsonObject("data").get("total").getAsInt());
        assertEquals(1, call(s, "GET", "/check-phone", null, 20L, Map.of("phone", "0912000001"))
                .json().getAsJsonArray("data").size());

        Result assigned = call(s, "POST", "/assign", "{\"leadIds\":[" + id + "],\"counselorId\":10,\"note\":\"Cuối tuần\"}", 20L);
        assertEquals(1, assigned.json().getAsJsonObject("data").get("assigned").getAsInt());
        Result mine = call(s, "GET", "", null, 10L, Map.of("status", "NEW", "owner", "10"));
        assertEquals(1, mine.json().getAsJsonObject("data").get("total").getAsInt());

        assertEquals(400, call(s, "GET", "", null, 20L, Map.of("from", "28/09/2026")).status());
        Result updated = call(s, "PUT", "/" + id, "{\"name\":\"LEAD A\",\"phone\":\"0912000001\",\"status\":\"WON\"}", 10L);
        assertEquals("Đã nhập học", updated.json().getAsJsonObject("data").get("statusLabel").getAsString());
    }

    @Test
    @DisplayName("Đăng ký tư vấn công khai tạo lead; bẫy bot không lưu")
    void consultation() throws Exception {
        PublicApiServlet s = new PublicApiServlet();
        assertEquals(1, call(s, "GET", "/programs", null, null).json().getAsJsonArray("data").size());
        assertEquals(400, call(s, "POST", "/consultations", "{\"name\":\"A\",\"phone\":\"123\",\"programCode\":\"WEB-FS\"}", null).status());
        assertEquals(201, call(s, "POST", "/consultations", "{\"name\":\"A\",\"phone\":\"0912000001\",\"programCode\":\"WEB-FS\"}", null).status());
        assertEquals(201, call(s, "POST", "/consultations",
                "{\"name\":\"Bot\",\"phone\":\"0912000002\",\"programCode\":\"WEB-FS\",\"website\":\"http://spam\"}", null).status());
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM leads")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}
