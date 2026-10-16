package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.dao.UserDAO;
import com.ems.exception.ApiException;
import com.ems.security.PermissionPolicy;
import com.ems.service.ClassService;
import com.ems.service.ClassService.ClassInput;
import com.ems.service.ClassService.RoomInput;
import com.ems.service.LeadService;
import com.ems.service.StudentService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** S3-01 đến S3-09 trên H2: phễu, nhật ký, chuyển học viên, hồ sơ, mở lớp, ghi danh, danh sách lớp, phân công, phòng học. */
class S3ApiTest {
    private final LeadService leads = new LeadService();
    private final ClassService classes = new ClassService();
    private final StudentService students = new StudentService();

    @BeforeAll
    static void initDatabase() {
        TestDatabaseHelper.initTestDatabase();
    }

    @BeforeEach
    void reset() throws SQLException {
        TestDatabaseHelper.clearData();
        for (String t : new String[] {"enrollments", "class_subject_assistants", "class_subjects", "class_sessions", "classrooms",
                "students", "lead_interactions", "lead_assignments", "leads"}) exec("DELETE FROM " + t);
        exec("INSERT INTO roles (id, role_code, role_name) VALUES (3, 'ADMISSIONS', 'Tư vấn'), (4, 'INSTRUCTOR', 'Giảng viên'), "
                + "(5, 'TA', 'Trợ giảng'), (6, 'STUDENT', 'Học viên')");
        exec("INSERT INTO users (id, user_code, email, password_hash, full_name, status) VALUES "
                + "(10, 'U10', 'ha@x.vn', 'x', 'TRAN THU HA', 'ACTIVE'), (30, 'U30', 'gv@x.vn', 'x', 'VU DUC LONG', 'ACTIVE'), "
                + "(40, 'U40', 'tg@x.vn', 'x', 'PHAM GIA HUY', 'ACTIVE')");
        exec("INSERT INTO user_roles (user_id, role_id) VALUES (10, 3), (30, 4), (40, 5)");
        exec("INSERT INTO programs (id, program_code, program_name, status) VALUES (1, 'WEB-FS', 'Lập trình web', 'ACTIVE')");
        exec("INSERT INTO subjects (id, subject_code, subject_name, total_sessions) VALUES (1, 'GIT-01', 'Git', 4), (2, 'WEB-01', 'HTML', 12)");
        exec("INSERT INTO program_subjects (program_id, subject_id, order_index) VALUES (1, 2, 1), (1, 1, 2)");
        exec("INSERT INTO leads (id, full_name, phone, email, status, assigned_counselor_id, interested_program_id) "
                + "VALUES (100, 'DO TUAN KIET', '0971846320', 'kiet@gmail.com', 'NEW', 10, 1)");
    }

    @Test
    void funnelCareLogAndConvert() throws Exception {
        expectCode("REJECT_REASON_REQUIRED", () -> leads.changeStage(100, "REJECTED", " ", 10, false));
        assertEquals("CONTACTED", leads.changeStage(100, "CONTACTED", null, 10, false).get("status"));
        leads.addLog(100, "CALL", "Khách hẹn gọi lại", null, LocalDate.now().toString(), 10, false);
        List<Map<String, Object>> logs = leads.logs(100, 10, false);
        assertEquals("CALL", logs.get(0).get("type"));
        assertEquals("Mới → Đã liên hệ", logs.get(1).get("content"));

        exec("UPDATE leads SET callback_date = '" + LocalDate.now().minusDays(2) + "' WHERE id = 100");
        Map<String, Object> funnel = leads.funnel(null, 10, false);
        assertEquals(1, funnel.get("overdue"));
        Map<String, Object> contacted = column(funnel, "CONTACTED");
        assertEquals(1, contacted.get("count"));
        assertEquals(2, item(contacted).get("overdueDays"));
        assertEquals(0, column(leads.funnel(null, 99, false), "CONTACTED").get("count"), "tư vấn viên khác không thấy lead");
        expectCode("RESOURCE_NOT_FOUND", () -> leads.logs(100, 99, false));

        expectCode("LEAD_NOT_WON", () -> leads.convert(100, null, 10, false));
        leads.changeStage(100, "WON", null, 10, false);
        Map<String, Object> student = leads.convert(100, null, 10, false);
        String code = (String) student.get("code");
        assertEquals(String.format("HV%02d0001", LocalDate.now().getYear() % 100), code);
        assertEquals(code.toLowerCase() + "@tms.vn", student.get("login"));
        assertEquals("kiet@gmail.com", student.get("email"));
        assertNotNull(new UserDAO().authenticate((String) student.get("login"), UserDAO.DEFAULT_PASSWORD), "tài khoản học viên đăng nhập được");
        assertEquals(code, leads.get(100, 10, false).get("studentCode"));
        expectCode("LEAD_CONVERTED", () -> leads.changeStage(100, "NEW", null, 10, false));
        expectCode("LEAD_ALREADY_CONVERTED", () -> leads.convert(100, null, 10, false));

        Map<String, Object> profile = students.get(code);
        assertEquals("WEB-FS", profile.get("programCode"));
        assertEquals("TRAN THU HA", profile.get("counselor"));
        assertEquals(1L, students.search("846320", null, null, 1, 20).get("total"));
        students.update(code, new StudentService.StudentInput("DO TUAN KIET", "0971846320", "kiet@gmail.com", "2004-05-01", "Hà Nội", "Đã học HTML", null));
        assertEquals("Đã học HTML", students.get(code).get("level"));
    }

    @Test
    void openEnrollAssignAndRooms() throws Exception {
        expectCode("VALIDATION_ERROR", () -> classes.createRoom(new RoomInput("Meet 1", null, "ONLINE", null, null, null)));
        int room = (int) classes.createRoom(new RoomInput("P.302", null, "PHYSICAL", 2, "Tầng 3", null)).get("id");

        String start = LocalDate.now().plusDays(7).toString();
        Map<String, Object> k = classes.open(new ClassInput("web-k16", null, "WEB-FS", start, 3, "OFFLINE", room));
        assertEquals("PLANNING", k.get("status"));
        assertNotNull(k.get("roomWarning"), "sĩ số 3 vượt phòng 2 chỗ");
        List<?> subjects = (List<?>) k.get("subjects");
        assertEquals("WEB-01", ((Map<?, ?>) subjects.get(0)).get("code"), "môn sao chép theo thứ tự lộ trình");
        expectCode("CLASS_CODE_EXISTS", () -> classes.open(new ClassInput("WEB-K16", null, "WEB-FS", start, 3, "OFFLINE", null)));

        for (int i = 1; i <= 4; i++) {
            exec("INSERT INTO users (id, user_code, email, password_hash, full_name, status) VALUES (" + (500 + i) + ", 'S" + i + "', 's" + i
                    + "@x.vn', 'x', 'HOC VIEN " + i + "', 'ACTIVE')");
            exec("INSERT INTO students (id, user_id, student_code, enrollment_status) VALUES (" + i + ", " + (500 + i) + ", 'HV26000" + i + "', 'STUDYING')");
        }
        assertEquals(3, classes.enroll("WEB-K16", List.of("HV260001", "HV260002", "HV260003"), 10).get("enrolled"));
        expectCode("ALREADY_ENROLLED", () -> classes.enroll("WEB-K16", List.of("HV260001"), 10));
        expectCode("CLASS_FULL", () -> classes.enroll("WEB-K16", List.of("HV260004"), 10));
        assertEquals("TRAN THU HA", classes.enrollments("WEB-K16").get(0).get("by"));
        assertEquals(1, ((List<?>) students.get("HV260001").get("currentClasses")).size());

        expectCode("INSTRUCTOR_INVALID", () -> classes.assign("WEB-K16", "WEB-01", 40L, List.of()));
        expectCode("ASSISTANT_INVALID", () -> classes.assign("WEB-K16", "WEB-01", null, List.of(30L)));
        classes.assign("WEB-K16", "WEB-01", 30L, List.of(40L));
        assertEquals("INSTRUCTOR", ((Map<?, ?>) ((List<?>) classes.mine(30).get(0).get("mySubjects")).get(0)).get("role"));
        assertEquals("TA", ((Map<?, ?>) ((List<?>) classes.mine(40).get(0).get("mySubjects")).get(0)).get("role"));
        assertEquals(1, classes.staff().get("instructors") instanceof List<?> l ? l.size() : -1);

        expectCode("CLASS_STATUS_INVALID", () -> classes.changeStatus("WEB-K16", "FINISHED"));
        classes.changeStatus("WEB-K16", "IN_PROGRESS");
        Map<String, Object> list = classes.search(null, null, "WEB-FS", "30", null, null, 1, 20);
        Map<?, ?> row = (Map<?, ?>) ((List<?>) list.get("items")).get(0);
        assertEquals(3, row.get("enrolled"));
        assertEquals(16, row.get("sessionsTotal"));
        assertEquals("VU DUC LONG", row.get("instructor"));
        assertEquals(1L, ((Map<?, ?>) list.get("summary")).get("IN_PROGRESS"));
        assertEquals(0L, classes.search(null, "PLANNING", null, null, null, null, 1, 20).get("total"));

        expectCode("ROOM_IN_USE", () -> classes.deleteRoom(room));
        assertEquals(false, classes.checkCapacity(room, 3).get("ok"));
    }

    @Test
    void permissions() {
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/classes/mine"));
        assertEquals("CLASS_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/classes"));
        assertEquals("CLASS_MANAGE", PermissionPolicy.requiredPermission("PUT", "/api/classes/WEB-K16/subjects/WEB-01"));
        assertEquals("STUDENT_READ", PermissionPolicy.requiredPermission("GET", "/api/students"));
        assertEquals("STUDENT_READ", PermissionPolicy.requiredPermission("GET", "/api/students/HV260001"));
        assertEquals("STUDENT_WRITE", PermissionPolicy.requiredPermission("PUT", "/api/students/HV260001"));
        assertEquals("CLASS_READ", PermissionPolicy.requiredPermission("GET", "/api/classes"));
        assertEquals("CLASS_READ", PermissionPolicy.requiredPermission("GET", "/api/rooms"));
        assertEquals("CLASS_MANAGE", PermissionPolicy.requiredPermission("DELETE", "/api/rooms/3"));
        assertEquals("LEAD_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/leads/5/convert"));
        assertEquals("LEAD_MANAGE|LEAD_READ", PermissionPolicy.requiredPermission("GET", "/api/leads/funnel"));
        assertNull(PermissionPolicy.requiredPermission("GET", "/api/classesX"));
    }

    private static Map<String, Object> column(Map<String, Object> funnel, String status) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) funnel.get("columns");
        return columns.stream().filter(c -> status.equals(c.get("status"))).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> item(Map<String, Object> column) {
        return ((List<Map<String, Object>>) column.get("items")).get(0);
    }

    private static void expectCode(String code, Executable action) {
        ApiException e = assertThrows(ApiException.class, action);
        assertEquals(code, e.getErrorCode(), e.getMessage());
    }

    private static void exec(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }
}
