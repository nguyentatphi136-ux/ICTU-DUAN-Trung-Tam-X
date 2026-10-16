package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.config.DatabaseMigration;
import com.ems.dao.PermissionDAO;
import com.ems.exception.ApiException;
import com.ems.security.PermissionPolicy;
import com.ems.service.LeadService;
import com.ems.service.LeadService.LeadInput;
import com.ems.service.SubjectService;
import com.ems.service.SubjectService.SubjectInput;
import com.ems.service.TrainingProgramService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** API S2-05 đến S2-11: môn học, lộ trình, buổi học, lead, phân công, đăng ký tư vấn. */
class S2ApiTest {
    private final SubjectService subjects = new SubjectService();
    private final TrainingProgramService programs = new TrainingProgramService();
    private final LeadService leads = new LeadService();

    @BeforeAll
    static void initDatabase() throws SQLException {
        TestDatabaseHelper.initTestDatabase();
        exec("CREATE TABLE IF NOT EXISTS class_subjects (id BIGINT AUTO_INCREMENT PRIMARY KEY, class_id BIGINT NOT NULL, subject_id INT NOT NULL)");
    }

    @BeforeEach
    void reset() throws SQLException {
        TestDatabaseHelper.clearData();
        for (String t : new String[] {"class_subjects", "subject_lessons", "program_subject_prereqs", "lead_assignments", "leads"}) {
            exec("DELETE FROM " + t);
        }
        exec("INSERT INTO programs (id, program_code, program_name, status) VALUES (1, 'WEB-FS', 'Lập trình web', 'ACTIVE'), "
                + "(2, 'DATA-01', 'Phân tích dữ liệu', 'ACTIVE'), (3, 'PHP-OLD', 'PHP cũ', 'INACTIVE')");
        exec("INSERT INTO roles (id, role_code, role_name) VALUES (1, 'ADMIN', 'Quản trị'), (2, 'TRAINING_MANAGER', 'Quản lý đào tạo'), "
                + "(3, 'ADMISSIONS', 'Tư vấn tuyển sinh')");
        exec("INSERT INTO users (id, user_code, email, password_hash, full_name) VALUES (10, 'U10', 'ha@x.vn', 'x', 'TRAN THU HA'), "
                + "(11, 'U11', 'tu@x.vn', 'x', 'NGO THANH TU'), (20, 'U20', 'ql@x.vn', 'x', 'QUAN LY')");
        exec("INSERT INTO user_roles (user_id, role_id) VALUES (10, 3), (11, 3), (20, 2)");
    }

    private static void exec(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static int count(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static SubjectInput subject(String code, int sessions, List<String> programCodes) {
        return new SubjectInput(code, "Môn " + code, sessions, new BigDecimal("2"), "Chuẩn đầu ra", programCodes);
    }

    private static List<String> codes(List<Map<String, Object>> curriculum) {
        return curriculum.stream().map(m -> (String) m.get("code")).toList();
    }

    private static String errorCode(Runnable call) {
        return assertThrows(ApiException.class, call::run).getErrorCode();
    }

    private interface Sql {
        void run() throws SQLException;
    }

    private static Runnable wrap(Sql call) {
        return () -> {
            try {
                call.run();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

    // ===== S2-05 môn học =====

    @Test
    @DisplayName("S2-05: tạo môn gắn chương trình, trùng mã báo 409, đổi mã giữ nguyên lộ trình và buổi học")
    void subjectCrud() throws SQLException {
        Map<String, Object> created = subjects.create(subject("web-01", 3, List.of("WEB-FS")));
        assertEquals("WEB-01", created.get("code"));
        assertEquals(0, new BigDecimal("2").compareTo((BigDecimal) created.get("weight")));
        assertEquals(List.of("WEB-01"), codes(programs.listSubjects("WEB-FS")));
        assertEquals("SUBJECT_CODE_CONFLICT", errorCode(wrap(() -> subjects.create(subject("WEB-01", 3, null)))));
        assertEquals("VALIDATION_ERROR", errorCode(wrap(() -> subjects.create(subject("X 1", 3, null)))));

        subjects.addLesson("WEB-01", "HTML", "Dựng trang");
        Map<String, Object> renamed = subjects.update("WEB-01", subject("WEB-10", 3, List.of("DATA-01")));
        assertEquals("WEB-10", renamed.get("code"));
        assertEquals(1, subjects.lessons("WEB-10").size());
        assertEquals(List.of(), codes(programs.listSubjects("WEB-FS")), "Bỏ chọn chương trình thì gỡ khỏi lộ trình");
        assertEquals(List.of("WEB-10"), codes(programs.listSubjects("DATA-01")));

        subjects.addLesson("WEB-10", "CSS", "");
        assertEquals("SESSIONS_BELOW_LESSONS", errorCode(wrap(() -> subjects.update("WEB-10", subject("WEB-10", 1, null)))));
    }

    @Test
    @DisplayName("S2-05: môn đã mở lớp không xoá được; xoá thì ẩn khỏi danh sách và lộ trình, khôi phục thì hiện lại")
    void subjectTrash() throws SQLException {
        subjects.create(subject("DB-01", 5, List.of("WEB-FS")));
        subjects.create(subject("GIT-01", 4, List.of("WEB-FS")));
        int dbId = (Integer) subjects.get("DB-01").get("id");
        exec("INSERT INTO class_subjects (class_id, subject_id) VALUES (1, " + dbId + ")");
        assertEquals("SUBJECT_HAS_CLASSES", errorCode(wrap(() -> subjects.trash("DB-01", 20))));

        subjects.trash("GIT-01", 20);
        assertEquals(1L, subjects.search(null, null, 1, 0).get("total"));
        assertEquals(List.of("DB-01"), codes(programs.listSubjects("WEB-FS")));
        assertEquals("SUBJECT_CODE_CONFLICT", errorCode(wrap(() -> subjects.create(subject("GIT-01", 4, null)))));

        int gitId = (Integer) subjects.listTrash().get(0).get("id");
        subjects.restore(gitId);
        assertEquals(List.of("DB-01", "GIT-01"), codes(programs.listSubjects("WEB-FS")));

        Map<String, Object> page = subjects.search("db", "WEB-FS", 1, 0);
        assertEquals(1L, page.get("total"));
        @SuppressWarnings("unchecked")
        Map<String, Object> db = ((List<Map<String, Object>>) page.get("items")).get(0);
        assertEquals(1, db.get("openedClasses"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> programs = (List<Map<String, Object>>) db.get("programs");
        assertEquals("WEB-FS", programs.get(0).get("code"));
    }

    // ===== S2-06 lộ trình =====

    @Test
    @DisplayName("S2-06: tiên quyết phải đứng trước; đổi thứ tự hoặc gỡ môn thì bỏ quan hệ không còn đúng")
    void curriculumPrerequisites() throws SQLException {
        for (String c : new String[] {"WEB-01", "WEB-02", "DB-01"}) subjects.create(subject(c, 5, null));
        programs.addSubject("WEB-FS", "WEB-01");
        programs.addSubject("WEB-FS", "WEB-02");
        programs.addSubject("WEB-FS", "DB-01");
        assertEquals("SUBJECT_ALREADY_IN_PROGRAM", errorCode(wrap(() -> programs.addSubject("WEB-FS", "WEB-01"))));

        programs.setPrerequisites("WEB-FS", "DB-01", List.of("WEB-01", "WEB-02"));
        assertEquals("PREREQ_NOT_BEFORE", errorCode(wrap(() -> programs.setPrerequisites("WEB-FS", "WEB-01", List.of("DB-01")))));

        // DB-01 lên đầu: WEB-01 và WEB-02 không còn đứng trước nên bị bỏ.
        List<Map<String, Object>> list = programs.reorderSubjects("WEB-FS", List.of("DB-01", "WEB-01", "WEB-02"));
        assertEquals(List.of(), list.get(0).get("prereq"));

        programs.setPrerequisites("WEB-FS", "WEB-02", List.of("DB-01", "WEB-01"));
        list = programs.removeSubject("WEB-FS", "DB-01");
        assertEquals(List.of("WEB-01", "WEB-02"), codes(list));
        assertEquals(List.of("WEB-01"), list.get(1).get("prereq"));
    }

    // ===== S2-07 buổi học =====

    @Test
    @DisplayName("S2-07: không khai báo quá số buổi; xoá thì dồn thứ tự; nhân bản cần xác nhận ghi đè")
    void lessons() throws SQLException {
        subjects.create(subject("WEB-01", 3, null));
        subjects.create(subject("WEB-00", 2, null));
        subjects.addLesson("WEB-01", "Buổi A", "a");
        subjects.addLesson("WEB-01", "Buổi B", "b");
        subjects.addLesson("WEB-01", "Buổi C", "c");
        assertEquals("LESSON_LIMIT_REACHED", errorCode(wrap(() -> subjects.addLesson("WEB-01", "Buổi D", ""))));

        List<Map<String, Object>> after = subjects.deleteLesson("WEB-01", 2);
        assertEquals(List.of(1, 2), after.stream().map(m -> m.get("order")).toList());
        assertEquals("Buổi C", after.get(1).get("topic"));

        subjects.addLesson("WEB-00", "Cũ", "");
        assertEquals("LESSONS_EXIST", errorCode(wrap(() -> subjects.cloneLessons("WEB-00", "WEB-01", false))));
        subjects.addLesson("WEB-01", "Buổi E", "");
        // WEB-00 chỉ có 2 buổi nên lấy 2 buổi đầu của WEB-01.
        List<Map<String, Object>> cloned = subjects.cloneLessons("WEB-00", "WEB-01", true);
        assertEquals(List.of("Buổi A", "Buổi C"), cloned.stream().map(m -> m.get("topic")).toList());
    }

    // ===== S2-08 đến S2-11 lead =====

    @Test
    @DisplayName("S2-09, S2-11: số điện thoại được chuẩn hoá, tìm theo tên hoặc số, lọc trạng thái và người phụ trách")
    void leadSearch() throws SQLException {
        Map<String, Object> a = leads.create(new LeadInput("PHAM THI HANH", "0912 480 227", null, "WEBSITE", "WEB-FS", null, null), 20, true);
        assertEquals("0912480227", a.get("phone"));
        assertEquals("Mới", a.get("statusLabel"));
        assertEquals("Lập trình web", a.get("programName"));
        leads.create(new LeadInput("DO QUANG MINH", "+84987115630", "minh@x.vn", "FACEBOOK", null, "CONSULTING", "Gọi tối"), 20, true);
        assertEquals("INVALID_PHONE_FORMAT",
                errorCode(wrap(() -> leads.create(new LeadInput("A", "12345", null, null, null, null, null), 20, true))));
        assertEquals("VALIDATION_ERROR",
                errorCode(wrap(() -> leads.create(new LeadInput("A", "0912480227", null, "TIVI", null, null, null), 20, true))));

        assertEquals(1L, leads.search("480 22", null, null, null, null, null, 1, 20, 20, true).get("total"));
        assertEquals(1L, leads.search("minh", null, null, null, null, null, 1, 20, 20, true).get("total"));
        assertEquals(1L, leads.search(null, "consulting", null, null, null, null, 1, 20, 20, true).get("total"));
        assertEquals(2L, leads.search(null, null, null, "none", "2000-01-01", null, 1, 20, 20, true).get("total"));
        assertEquals(1, leads.samePhone("0912.480.227", null).size());
    }

    @Test
    @DisplayName("S2-10: phân công ghi lịch sử, tư vấn viên chỉ thấy lead của mình và lead tự thêm được giao cho mình")
    void leadAssign() throws SQLException {
        long a = (Long) leads.create(new LeadInput("LEAD A", "0912000001", null, null, null, null, null), 20, true).get("id");
        long b = (Long) leads.create(new LeadInput("LEAD B", "0912000002", null, null, null, null, null), 20, true).get("id");

        assertEquals("COUNSELOR_INVALID", errorCode(wrap(() -> leads.assign(List.of(a), 20L, null, 20))));
        assertEquals("RESOURCE_NOT_FOUND", errorCode(wrap(() -> leads.assign(List.of(a, 999L), 10L, null, 20))));
        assertEquals(0, count("SELECT COUNT(*) FROM lead_assignments"), "Lỗi giữa chừng thì không giao lead nào");

        assertEquals(2, leads.assign(List.of(a, b), 10L, "Lead cuối tuần", 20).get("assigned"));
        assertEquals("CONSULTING", leads.get(b, 20, true).get("status"), "Lead Mới được giao thì sang Đang chăm sóc");
        assertEquals(0, leads.assign(List.of(a), 10L, null, 20).get("assigned"), "Giao lại cho đúng người cũ thì bỏ qua");
        leads.assign(List.of(a), 11L, null, 20);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) leads.get(a, 20, true).get("assignments");
        assertEquals(2, history.size());
        assertEquals("TRAN THU HA", history.get(0).get("from"));
        assertEquals("NGO THANH TU", history.get(0).get("to"));
        assertEquals("QUAN LY", history.get(0).get("by"));

        // Tư vấn viên 10 giờ chỉ còn lead B.
        assertEquals(1L, leads.search(null, null, null, null, null, null, 1, 20, 10, false).get("total"));
        assertEquals("RESOURCE_NOT_FOUND", errorCode(wrap(() -> leads.get(a, 10, false))));
        Map<String, Object> own = leads.create(new LeadInput("LEAD C", "0912000003", null, null, null, null, null), 10, false);
        assertEquals(10L, own.get("ownerId"));

        List<Map<String, Object>> counselors = leads.counselors();
        assertEquals(List.of("NGO THANH TU", "TRAN THU HA"), counselors.stream().map(m -> m.get("name")).toList());
        assertEquals(2, counselors.get(1).get("openLeads"));
    }

    @Test
    @DisplayName("S2-08: đăng ký tư vấn tạo lead Mới chưa phân công, chỉ nhận chương trình đang tuyển sinh")
    void consultation() throws SQLException {
        assertEquals(List.of("DATA-01", "WEB-FS"), leads.publicPrograms().stream().map(m -> m.get("code")).sorted().toList());
        assertEquals("PROGRAM_NOT_FOUND", errorCode(wrap(() -> leads.submitConsultation("A", "0912000001", null, "PHP-OLD", null))));
        assertEquals("VALIDATION_ERROR", errorCode(wrap(() -> leads.submitConsultation("A", "0912000001", null, null, null))));

        leads.submitConsultation("Nguyễn Văn A", "0912000001", "a@x.vn", "WEB-FS", "Học buổi tối");
        @SuppressWarnings("unchecked")
        Map<String, Object> lead = ((List<Map<String, Object>>) leads.search(null, null, null, null, null, null, 1, 20, 20, true)
                .get("items")).get(0);
        assertEquals("NEW", lead.get("status"));
        assertEquals("WEBSITE", lead.get("source"));
        assertNull(lead.get("ownerId"));
        assertEquals("Học buổi tối", lead.get("note"));
    }

    // ===== Phân quyền =====

    @Test
    @DisplayName("Phân quyền: môn học cần PROGRAM_MANAGE; phân công, xoá, thùng rác lead cần LEAD_ASSIGN")
    void permissionPolicy() {
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/subjects"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/subjects/WEB-01/lessons/clone"));
        assertEquals("LEAD_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/leads"));
        assertEquals("LEAD_MANAGE", PermissionPolicy.requiredPermission("PUT", "/api/leads/5"));
        assertEquals("LEAD_ASSIGN", PermissionPolicy.requiredPermission("POST", "/api/leads/assign"));
        assertEquals("LEAD_ASSIGN", PermissionPolicy.requiredPermission("DELETE", "/api/leads/5"));
        assertEquals("LEAD_ASSIGN", PermissionPolicy.requiredPermission("GET", "/api/leads/trash"));
        assertNull(PermissionPolicy.requiredPermission("GET", "/api/leadsX"));
    }

    @Test
    @DisplayName("Migration: LEAD_ASSIGN chỉ gán ở lần đầu, quyền đã thu hồi không bị gán lại khi khởi động")
    void seedLeadAssignOnce() throws SQLException {
        exec("INSERT INTO permissions (permission_code, permission_name, module) VALUES ('LEAD_MANAGE', 'Lead', 'ADMISSIONS')");
        try (Connection c = DBConnection.getConnection()) {
            DatabaseMigration.runMigration(c);
        }
        PermissionDAO dao = new PermissionDAO();
        assertTrue(dao.hasPermission(20, "LEAD_ASSIGN"));
        assertTrue(dao.hasPermission(20, "LEAD_MANAGE"));
        assertFalse(dao.hasPermission(10, "LEAD_ASSIGN"));

        exec("DELETE FROM role_permissions WHERE role_id = 2");
        try (Connection c = DBConnection.getConnection()) {
            DatabaseMigration.runMigration(c);
        }
        assertFalse(dao.hasPermission(20, "LEAD_ASSIGN"));
    }
}
