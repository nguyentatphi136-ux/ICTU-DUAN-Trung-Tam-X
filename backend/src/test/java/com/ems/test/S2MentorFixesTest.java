package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.dao.AdminDAO;
import com.ems.dao.TrainingProgramDAO;
import com.ems.dao.UserDAO;
import com.ems.dto.TrainingProgramRequest;
import com.ems.exception.ConflictException;
import com.ems.model.TrainingProgram;
import com.ems.service.TrainingProgramService;
import com.ems.service.UserImportParser;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử các sửa đổi theo nhận xét mentor cho Sprint 2:
 * nhập Excel 3 nhóm, trùng số điện thoại, hồ sơ cá nhân, thùng rác, lưu thứ tự môn.
 */
class S2MentorFixesTest {
    private final AdminDAO adminDAO = new AdminDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainingProgramDAO programDAO = new TrainingProgramDAO();
    private final TrainingProgramService programService = new TrainingProgramService(programDAO);

    @BeforeAll
    static void initDatabase() {
        TestDatabaseHelper.initTestDatabase();
    }

    @BeforeEach
    void reset() throws SQLException {
        TestDatabaseHelper.clearData();
        exec("INSERT INTO roles (id, role_code, role_name) VALUES (1, 'ADMIN', 'Quản trị'), (2, 'STUDENT', 'Học viên'), (3, 'INSTRUCTOR', 'Giảng viên')");
    }

    private static void exec(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.executeUpdate();
        }
    }

    private static long insertUser(String email, String name, String phone) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO users (user_code, email, password_hash, full_name, phone) VALUES (?, ?, 'x', ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            s.setString(1, "U-" + email);
            s.setString(2, email);
            s.setString(3, name);
            s.setString(4, phone);
            s.executeUpdate();
            try (ResultSet rs = s.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static Map<String, Object> row(String name, String email, String phone, String role) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("fullName", name);
        r.put("email", email);
        r.put("phone", phone);
        r.put("role", role);
        return r;
    }

    @SuppressWarnings("unchecked")
    private static String status(Map<String, Object> preview, int index) {
        return (String) ((List<Map<String, Object>>) preview.get("rows")).get(index).get("status");
    }

    // ---- S2-01: nhập Excel ----

    @Test
    @DisplayName("S2-01: xem trước tách 3 nhóm hợp lệ / trùng / lỗi")
    void previewSplitsValidDuplicateAndError() throws SQLException {
        insertUser("da.co@tms.vn", "Đã Có", "0911111111");
        List<Map<String, Object>> rows = List.of(
                row("Nguyễn An", "an@tms.vn", "0912345678", "Học viên"),        // ok
                row("Trùng Email Tệp", "an@tms.vn", "", "student"),              // trùng trong tệp
                row("Trùng Email DB", "da.co@tms.vn", "", ""),                   // trùng với hệ thống
                row("Trùng SĐT DB", "sdt@tms.vn", "091 111 1111", "Giảng viên"), // trùng số với hệ thống
                row("Sai Email", "khong-hop-le", "", ""),                        // lỗi
                row("Sai Vai Trò", "vt@tms.vn", "", "Quản trị hệ thống"),       // lỗi: không nhập được quản trị
                row("", "trong@tms.vn", "", ""));                                // lỗi: thiếu họ tên

        Map<String, Object> preview = adminDAO.previewUsersBatch(rows);

        assertEquals(1, preview.get("validRows"));
        assertEquals(3, preview.get("duplicateRows"));
        assertEquals(3, preview.get("errorRows"));
        assertEquals("ok", status(preview, 0));
        assertEquals("duplicate", status(preview, 1));
        assertEquals("duplicate", status(preview, 2));
        assertEquals("duplicate", status(preview, 3));
        assertEquals("error", status(preview, 4));
        assertEquals("error", status(preview, 5));
        assertEquals("error", status(preview, 6));
    }

    @Test
    @DisplayName("S2-01: nhập chỉ thêm dòng hợp lệ, báo cáo tách dòng trùng và dòng lỗi")
    @SuppressWarnings("unchecked")
    void importAddsOnlyValidRows() throws SQLException {
        insertUser("da.co@tms.vn", "Đã Có", null);
        Map<String, Object> summary = adminDAO.importUsersBatch(1, "test.xlsx", List.of(
                row("Mới Một", "moi1@tms.vn", "912000001", "Học viên"),   // số mất 0 đầu do Excel
                row("Mới Hai", "moi2@tms.vn", "0912000002", "Giảng viên"),
                row("Trùng", "da.co@tms.vn", "", ""),
                row("Lỗi", "sai", "", "")));

        assertEquals(2, summary.get("successRows"));
        assertEquals(1, summary.get("duplicateRows"));
        assertEquals(1, summary.get("errorRows"));
        List<Map<String, Object>> skipped = (List<Map<String, Object>>) summary.get("errors");
        assertEquals("DUPLICATE", skipped.get(0).get("type"));
        assertEquals("INVALID", skipped.get(1).get("type"));
        assertNotNull(userDAO.getUserProfileByEmail("moi1@tms.vn"));
        assertEquals("0912000001", userDAO.getUserProfileByEmail("moi1@tms.vn").get("phone"));
    }

    @Test
    @DisplayName("S2-01: đọc tệp .xlsx thật, giữ số 0 đầu và đổi ô ngày về YYYY-MM-DD")
    void parsesXlsx() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            String[] titles = {"Họ và tên", "Email", "Số điện thoại", "Vai trò", "Ngày sinh"};
            for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
            Row r = sheet.createRow(1);
            r.createCell(0).setCellValue("Trần Bình");
            r.createCell(1).setCellValue("binh@tms.vn");
            r.createCell(2).setCellValue(912345678d); // ô số: Excel bỏ số 0 đầu
            r.createCell(3).setCellValue("Học viên");
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            r.createCell(4).setCellValue(LocalDate.of(2003, 5, 15));
            r.getCell(4).setCellStyle(date);
            sheet.createRow(2); // dòng trống bị bỏ qua
            wb.write(out);
        }
        List<Map<String, Object>> rows = UserImportParser.parse("ds.xlsx", new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1, rows.size());
        assertEquals("912345678", rows.get(0).get("phone"));
        assertEquals("2003-05-15", rows.get(0).get("dateOfBirth"));
        assertEquals("0912345678", AdminDAO.importPhone(rows.get(0).get("phone")));
    }

    @Test
    @DisplayName("S2-01: đọc CSV có BOM và ngoặc kép; báo lỗi khi thiếu cột hoặc sai loại tệp")
    void parsesCsvAndRejectsBadFiles() throws Exception {
        String csv = "﻿Email,Họ và tên,Địa chỉ\r\nan@tms.vn,\"Nguyễn, An\",\"Số 1 \"\"A\"\"\"\r\n\r\n";
        List<Map<String, Object>> rows = UserImportParser.parse("a.csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, rows.size());
        assertEquals("Nguyễn, An", rows.get(0).get("fullName"));
        assertEquals("Số 1 \"A\"", rows.get(0).get("address"));

        assertThrows(IllegalArgumentException.class, () -> UserImportParser.parse("a.csv",
                new ByteArrayInputStream("Tên,SĐT\r\nA,1\r\n".getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class, () -> UserImportParser.parse("a.pdf", new ByteArrayInputStream(new byte[0])));
    }

    // ---- S2-02: hồ sơ cá nhân, trùng số điện thoại ----

    @Test
    @DisplayName("S2-02: lưu hồ sơ, chặn số điện thoại đã có người dùng")
    void profileSavesAndRejectsDuplicatePhone() throws SQLException {
        long me = insertUser("me@tms.vn", "Tôi", null);
        insertUser("other@tms.vn", "Người Khác", "0987654321");

        Map<String, Object> saved = userDAO.updateUserProfile(me, "Tên Mới", "0912 345 678", "2004-03-14", "female", "Hà Nội");
        assertEquals("Tên Mới", saved.get("fullName"));
        assertEquals("0912345678", saved.get("phone"));
        assertEquals("2004-03-14", saved.get("dateOfBirth"));
        assertEquals("FEMALE", saved.get("gender"));

        ConflictException dup = assertThrows(ConflictException.class,
                () -> userDAO.updateUserProfile(me, "Tên Mới", "0987.654.321", null, null, null));
        assertEquals("PHONE_DUPLICATE", dup.getErrorCode());
        assertThrows(IllegalArgumentException.class, () -> userDAO.updateUserProfile(me, "Tên", "12345", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> userDAO.updateUserProfile(me, " ", null, null, null, null));
        // Số của chính mình thì vẫn lưu lại được.
        assertDoesNotThrow(() -> userDAO.updateUserProfile(me, "Tên Mới", "0912345678", null, null, null));
    }

    @Test
    @DisplayName("S1-08: tạo và sửa tài khoản cũng chặn trùng số điện thoại")
    void adminCreateAndUpdateRejectDuplicatePhone() throws SQLException {
        insertUser("a@tms.vn", "A", "0911111111");
        long b = insertUser("b@tms.vn", "B", "0922222222");
        ConflictException create = assertThrows(ConflictException.class,
                () -> adminDAO.createUser(1, null, "c@tms.vn", "C", "0911111111", List.of("STUDENT"), "Abc12345"));
        assertEquals("PHONE_DUPLICATE", create.getErrorCode());
        assertThrows(ConflictException.class, () -> adminDAO.updateUserDetails(1, b, "B", "0911 111 111"));
        assertDoesNotThrow(() -> adminDAO.updateUserDetails(1, b, "B đổi tên", "0922222222"));
    }

    // ---- Thùng rác ----

    @Test
    @DisplayName("Thùng rác tài khoản: chuyển vào, ẩn khỏi danh sách, khôi phục, xoá vĩnh viễn")
    @SuppressWarnings("unchecked")
    void userTrashLifecycle() throws SQLException {
        long admin = insertUser("admin@tms.vn", "Admin", null);
        long target = insertUser("t@tms.vn", "Mục Tiêu", "0933333333");

        assertThrows(IllegalArgumentException.class, () -> adminDAO.trashUser(admin, admin));
        assertNotNull(adminDAO.trashUser(admin, target));
        assertNull(adminDAO.trashUser(admin, target)); // đã ở trong thùng rác
        List<Map<String, Object>> visible = (List<Map<String, Object>>) adminDAO.searchUsers(null, null, null, 1, 50).get("items");
        assertTrue(visible.stream().noneMatch(u -> u.get("id").equals(target)));
        assertNull(userDAO.getUserProfile(target));
        assertEquals("Admin", adminDAO.listTrashedUsers().get(0).get("deletedBy"));
        // Số điện thoại của tài khoản trong thùng rác không chặn người khác.
        assertNull(UserDAO.findPhoneOwner(DBConnection.getConnection(), "0933333333", 0));

        assertNotNull(adminDAO.restoreUser(admin, target));
        assertNotNull(userDAO.getUserProfile(target));

        assertFalse(adminDAO.purgeUser(admin, target)); // phải nằm trong thùng rác mới xoá vĩnh viễn
        adminDAO.trashUser(admin, target);
        assertTrue(adminDAO.purgeUser(admin, target));
        assertTrue(adminDAO.listTrashedUsers().isEmpty());
    }

    private int createProgram(String code) throws SQLException {
        TrainingProgramRequest req = new TrainingProgramRequest();
        req.setCode(code);
        req.setName("Chương trình " + code);
        req.setDuration(120);
        req.setStandardTuition(new BigDecimal("1000000"));
        return programService.create(req).getId();
    }

    @Test
    @DisplayName("Thùng rác chương trình: ẩn, chặn tạo trùng mã, khôi phục")
    void programTrashLifecycle() throws SQLException {
        int id = createProgram("WEB-FS");
        programService.delete(id, null);
        assertNull(programDAO.findById(id));
        assertEquals(1, programService.listTrash().size());

        ConflictException conflict = assertThrows(ConflictException.class, () -> createProgram("WEB-FS"));
        assertTrue(conflict.getMessage().contains("thùng rác"));

        programService.restore(id);
        TrainingProgram restored = programDAO.findById(id);
        assertNotNull(restored);
        assertEquals("WEB-FS", restored.getCode());
    }

    // ---- S2-06: lưu thứ tự môn ----

    @Test
    @DisplayName("S2-06: lưu thứ tự môn mới; danh sách không khớp lộ trình thì báo 409")
    void reorderCurriculum() throws SQLException {
        int programId = createProgram("DATA");
        exec("INSERT INTO subjects (id, subject_code, subject_name) VALUES (1, 'SQL', 'SQL'), (2, 'PY', 'Python'), (3, 'BI', 'Power BI')");
        exec("INSERT INTO program_subjects (program_id, subject_id, order_index) VALUES "
                + "(" + programId + ", 1, 1), (" + programId + ", 2, 2), (" + programId + ", 3, 3)");

        List<Map<String, Object>> after = programService.reorderSubjects("DATA", List.of("bi", "SQL", "PY"));
        assertEquals(List.of("BI", "SQL", "PY"), after.stream().map(s -> s.get("code")).toList());
        assertEquals(List.of("BI", "SQL", "PY"),
                programService.listSubjects(String.valueOf(programId)).stream().map(s -> s.get("code")).toList());

        ConflictException missing = assertThrows(ConflictException.class,
                () -> programService.reorderSubjects("DATA", List.of("SQL", "PY")));
        assertEquals("CURRICULUM_CHANGED", missing.getErrorCode());
        assertThrows(ConflictException.class, () -> programService.reorderSubjects("DATA", List.of("SQL", "PY", "XX")));
    }
}
