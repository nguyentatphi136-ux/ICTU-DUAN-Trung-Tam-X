package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.dao.ClassDAO;
import com.ems.dao.ClassroomDAO;
import com.ems.dao.ScheduleDAO;
import com.ems.dao.UserDAO;
import com.ems.dto.ConflictCheckResult;
import com.ems.dto.ConflictDetail;
import com.ems.dto.ConflictType;
import com.ems.dto.ScheduleRequest;
import com.ems.exception.ForbiddenAccessException;
import com.ems.exception.ScheduleConflictException;
import com.ems.exception.ValidationException;
import com.ems.model.AuditLog;
import com.ems.model.ClassSession;
import com.ems.model.Classroom;
import com.ems.model.SchoolClass;
import com.ems.model.User;
import com.ems.service.ScheduleConflictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bß╗Ö kiß╗âm thß╗¡ to├án diß╗çn cho User Story S4-02:
 * [BE] Database + API kiß╗âm tra xung ─æß╗Öt thß╗¥i kh├│a biß╗âu
 * Bao qu├ít ─æß║ºy ─æß╗º 15 ti├¬u ch├¡ kiß╗âm thß╗¡ theo ─æß║╖c tß║ú nghiß╗çp vß╗Ñ.
 */
public class ScheduleConflictTest {

    private ScheduleConflictService service;
    private MockScheduleDAO mockScheduleDAO;
    private MockClassroomDAO mockClassroomDAO;
    private MockClassDAO mockClassDAO;
    private MockUserDAO mockUserDAO;

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 11, 1);
    private static final Long TRAINING_MGR_ID = 2L;
    private static final List<String> TRAINING_MGR_ROLES = List.of(RoleConstant.TRAINING_MANAGER);
    private static final List<String> INSTRUCTOR_ROLES = List.of(RoleConstant.INSTRUCTOR);

    @BeforeEach
    void setUp() {
        mockScheduleDAO = new MockScheduleDAO();
        mockClassroomDAO = new MockClassroomDAO();
        mockClassDAO = new MockClassDAO();
        mockUserDAO = new MockUserDAO();

        service = new ScheduleConflictService(
                mockScheduleDAO,
                mockClassroomDAO,
                mockClassDAO,
                mockUserDAO,
                () -> createMockConnection(mockScheduleDAO)
        );

        // Khß╗ƒi tß║ío ph├▓ng hß╗ìc mß║½u
        mockClassroomDAO.save(new Classroom(1, "PH-101", "Ph├▓ng 101", 40, "PHYSICAL", "AVAILABLE"));
        mockClassroomDAO.save(new Classroom(2, "LAB-202", "Ph├▓ng m├íy 202", 35, "PHYSICAL", "AVAILABLE"));

        // Khß╗ƒi tß║ío lß╗¢p hß╗ìc mß║½u
        mockClassDAO.save(new SchoolClass(1L, "CNTT-K23-01", "Lß╗¢p Java 01", 1, "IN_PROGRESS"));
        mockClassDAO.save(new SchoolClass(2L, "CNTT-K23-02", "Lß╗¢p Web 02", 1, "IN_PROGRESS"));

        // Khß╗ƒi tß║ío ng╞░ß╗¥i d├╣ng / giß║úng vi├¬n mß║½u
        mockUserDAO.save(new User(3L, "NV-GV-01", "giangvien@edumanager.vn", "Trß║ºn Thß╗ï Giß║úng", RoleConstant.INSTRUCTOR));
        mockUserDAO.save(new User(4L, "NV-GV-02", "giangvien2@edumanager.vn", "Nguyß╗àn V─ân Thß║ºy", RoleConstant.INSTRUCTOR));
    }

    // -------------------------------------------------------------------------
    // 1. Tß║ío lß╗ïch hß╗úp lß╗ç th├ánh c├┤ng
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 1: Tß║ío lß╗ïch hß╗úp lß╗ç th├ánh c├┤ng khi kh├┤ng c├│ xung ─æß╗Öt")
    void testCreateValidScheduleSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        ClassSession created = service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req);

        assertNotNull(created, "Buß╗òi hß╗ìc ─æ╞░ß╗úc tß║ío kh├┤ng ─æ╞░ß╗úc null");
        assertNotNull(created.getId(), "Phß║úi sinh ID cho buß╗òi hß╗ìc mß╗¢i");
        assertEquals("SCHEDULED", created.getStatus());
        assertEquals(TEST_DATE, created.getSessionDate());
        assertEquals(LocalTime.of(8, 0), created.getStartTime());
        assertEquals(LocalTime.of(10, 0), created.getEndTime());
        assertEquals(1, mockScheduleDAO.auditLogs.size(), "Phß║úi ghi nhß║¡n 1 audit log cho thao t├íc tß║ío lß╗ïch");
    }

    // -------------------------------------------------------------------------
    // 2. Tr├╣ng giß║úng vi├¬n c├╣ng khung giß╗¥ bß╗ï chß║╖n
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 2: Tr├╣ng giß║úng vi├¬n c├╣ng khung giß╗¥ bß╗ï chß║╖n l╞░u lß╗ïch (HTTP 409)")
    void testInstructorConflictBlocked() {
        // ─É├ú c├│ lß╗ïch cß╗ºa Giß║úng vi├¬n 3 ß╗ƒ Ph├▓ng 1 tß╗½ 09:00 ─æß║┐n 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Y├¬u cß║ºu xß║┐p lß╗ïch Giß║úng vi├¬n 3 ß╗ƒ Ph├▓ng 2 (kh├íc ph├▓ng) tß╗½ 08:30 ─æß║┐n 10:00 (giao nhau 09:00 - 10:00)
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T08:30:00", "2026-11-01T10:00:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        ConflictCheckResult result = ex.getConflictResult();
        assertTrue(result.isHasConflict(), "Phß║úi b├ío c├│ xung ─æß╗Öt");
        assertEquals(1, result.getConflicts().size(), "Phß║úi c├│ ─æ├║ng 1 xung ─æß╗Öt");
        assertEquals(ConflictType.INSTRUCTOR_CONFLICT.getCode(), result.getConflicts().get(0).getType());
        assertTrue(result.getConflicts().get(0).getMessage().contains("Trß║ºn Thß╗ï Giß║úng"));
    }

    // -------------------------------------------------------------------------
    // 3. Tr├╣ng ph├▓ng c├╣ng khung giß╗¥ bß╗ï chß║╖n
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 3: Tr├╣ng ph├▓ng c├╣ng khung giß╗¥ bß╗ï chß║╖n l╞░u lß╗ïch (HTTP 409)")
    void testRoomConflictBlocked() {
        // ─É├ú c├│ lß╗ïch ß╗ƒ Ph├▓ng 1 cß╗ºa Giß║úng vi├¬n 3 tß╗½ 09:00 ─æß║┐n 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Y├¬u cß║ºu xß║┐p lß╗ïch cho Giß║úng vi├¬n 4 (kh├íc giß║úng vi├¬n) ß╗ƒ Ph├▓ng 1 tß╗½ 10:00 ─æß║┐n 12:00 (giao nhau 10:00 - 11:00)
        ScheduleRequest req = new ScheduleRequest(1L, 4L, 1,
                "2026-11-01T10:00:00", "2026-11-01T12:00:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        ConflictCheckResult result = ex.getConflictResult();
        assertTrue(result.isHasConflict());
        assertEquals(1, result.getConflicts().size());
        assertEquals(ConflictType.ROOM_CONFLICT.getCode(), result.getConflicts().get(0).getType());
        assertTrue(result.getConflicts().get(0).getMessage().contains("PH-101"));
    }

    // -------------------------------------------------------------------------
    // 4. Tr├╣ng cß║ú giß║úng vi├¬n v├á ph├▓ng trß║ú ─æß╗º th├┤ng tin xung ─æß╗Öt
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 4: Tr├╣ng cß║ú giß║úng vi├¬n v├á ph├▓ng trß║ú ─æß╗º cß║ú 2 th├┤ng tin xung ─æß╗Öt")
    void testBothInstructorAndRoomConflictReported() {
        // Buß╗òi 1: Giß║úng vi├¬n 3, Ph├▓ng 1, 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Y├¬u cß║ºu xß║┐p c├╣ng Giß║úng vi├¬n 3 V├Ç c├╣ng Ph├▓ng 1 tß╗½ 09:30 ─æß║┐n 11:30
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:30:00", "2026-11-01T11:30:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertTrue(result.isHasConflict());
        assertEquals(2, result.getConflicts().size(), "Phß║úi trß║ú vß╗ü 2 xung ─æß╗Öt: 1 cho GV, 1 cho Ph├▓ng");

        List<String> types = result.getConflicts().stream().map(ConflictDetail::getType).toList();
        assertTrue(types.contains(ConflictType.INSTRUCTOR_CONFLICT.getCode()));
        assertTrue(types.contains(ConflictType.ROOM_CONFLICT.getCode()));
    }

    // -------------------------------------------------------------------------
    // 5. Hai lß╗ïch nß╗æi tiß║┐p nhau kh├┤ng bß╗ï coi l├á xung ─æß╗Öt
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 5: Hai lß╗ïch nß╗æi tiß║┐p nhau (buß╗òi tr╞░ß╗¢c kß║┐t th├║c ─æ├║ng l├║c buß╗òi sau bß║»t ─æß║ºu) KH├öNG xung ─æß╗Öt")
    void testConsecutiveSchedulesNotConflicting() {
        // Buß╗òi 1: 08:00 - 10:00 (Giß║úng vi├¬n 3, Ph├▓ng 1)
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED");

        // Buß╗òi sau: 10:00 - 12:00 (C├╣ng Giß║úng vi├¬n 3 hoß║╖c c├╣ng Ph├▓ng 1)
        ScheduleRequest nextReq = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T10:00:00", "2026-11-01T12:00:00");

        ConflictCheckResult nextResult = service.detectConflicts(nextReq, mockScheduleDAO.sessions);
        assertFalse(nextResult.isHasConflict(), "Nß╗æi tiß║┐p sau 10:00 kh├┤ng ─æ╞░ß╗úc b├ío xung ─æß╗Öt");

        // Buß╗òi tr╞░ß╗¢c: 06:00 - 08:00 (C├╣ng Giß║úng vi├¬n 3 hoß║╖c c├╣ng Ph├▓ng 1)
        ScheduleRequest prevReq = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T06:00:00", "2026-11-01T08:00:00");

        ConflictCheckResult prevResult = service.detectConflicts(prevReq, mockScheduleDAO.sessions);
        assertFalse(prevResult.isHasConflict(), "Nß╗æi tiß║┐p tr╞░ß╗¢c 08:00 kh├┤ng ─æ╞░ß╗úc b├ío xung ─æß╗Öt");
    }

    // -------------------------------------------------------------------------
    // 6. Lß╗ïch giao nhau mß╗Öt phß║ºn bß╗ï ph├ít hiß╗çn
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 6: Lß╗ïch giao nhau mß╗Öt phß║ºn (bß║»t ─æß║ºu tr╞░ß╗¢c kß║┐t th├║c giß╗»a chß╗½ng, hoß║╖c bß║»t ─æß║ºu giß╗»a chß╗½ng) bß╗ï ph├ít hiß╗çn")
    void testPartialOverlapDetected() {
        // Buß╗òi hiß╗çn tß║íi: 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Tr╞░ß╗¥ng hß╗úp a: 08:00 - 09:30 (bß║»t ─æß║ºu tr╞░ß╗¢c, kß║┐t th├║c giß╗»a chß╗½ng)
        ScheduleRequest reqA = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T08:00:00", "2026-11-01T09:30:00");
        assertTrue(service.detectConflicts(reqA, mockScheduleDAO.sessions).isHasConflict());

        // Tr╞░ß╗¥ng hß╗úp b: 10:30 - 12:00 (bß║»t ─æß║ºu giß╗»a chß╗½ng, kß║┐t th├║c sau)
        ScheduleRequest reqB = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T10:30:00", "2026-11-01T12:00:00");
        assertTrue(service.detectConflicts(reqB, mockScheduleDAO.sessions).isHasConflict());
    }

    // -------------------------------------------------------------------------
    // 7. Lß╗ïch lß╗ông ho├án to├án v├áo lß╗ïch kh├íc bß╗ï ph├ít hiß╗çn
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 7: Lß╗ïch lß╗ông ho├án to├án b├¬n trong hoß║╖c bao tr├╣m ho├án to├án lß╗ïch kh├íc bß╗ï ph├ít hiß╗çn")
    void testCompleteNestingDetected() {
        // Buß╗òi 1: 08:00 - 12:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), "SCHEDULED");

        // Lß╗ông b├¬n trong: 09:00 - 11:00
        ScheduleRequest innerReq = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");
        assertTrue(service.detectConflicts(innerReq, mockScheduleDAO.sessions).isHasConflict());

        // Bao tr├╣m b├¬n ngo├ái: 07:00 - 13:00
        ScheduleRequest outerReq = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T07:00:00", "2026-11-01T13:00:00");
        assertTrue(service.detectConflicts(outerReq, mockScheduleDAO.sessions).isHasConflict());
    }

    // -------------------------------------------------------------------------
    // 8. Cß║¡p nhß║¡t lß╗ïch kh├┤ng so s├ính vß╗¢i ch├¡nh n├│
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 8: Cß║¡p nhß║¡t lß╗ïch kh├┤ng so s├ính vß╗¢i ch├¡nh n├│ (loß║íi trß╗½ scheduleId)")
    void testUpdateDoesNotCompareWithItself() throws SQLException {
        // Buß╗òi hß╗ìc ID 15: 09:00 - 11:00
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Cß║¡p nhß║¡t buß╗òi hß╗ìc ID 15 dß╗¥i giß╗¥ sang 09:00 - 11:30 (vß║½n giß║úng vi├¬n 3, ph├▓ng 1)
        ScheduleRequest req = new ScheduleRequest(2L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T11:30:00");

        ClassSession updated = service.updateSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, 15L, req);

        assertNotNull(updated);
        assertEquals(LocalTime.of(11, 30), updated.getEndTime());
    }

    // -------------------------------------------------------------------------
    // 9. Cß║¡p nhß║¡t tß║ío ra xung ─æß╗Öt vß╗¢i buß╗òi kh├íc bß╗ï tß╗½ chß╗æi
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 9: Cß║¡p nhß║¡t tß║ío ra xung ─æß╗Öt vß╗¢i buß╗òi hß╗ìc KH├üC bß╗ï tß╗½ chß╗æi")
    void testUpdateCausingConflictIsRejected() {
        // Buß╗òi hß╗ìc 15: 09:00 - 11:00 (GV 3, Ph├▓ng 1)
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");
        // Buß╗òi hß╗ìc 16: 14:00 - 16:00 (GV 3, Ph├▓ng 2)
        addExistingSession(16L, 1L, 3L, 2, LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");

        // Thß╗¡ cß║¡p nhß║¡t Buß╗òi 15 dß╗¥i sang 14:30 - 16:30 -> ─Éß╗Ñng Giß║úng vi├¬n 3 ß╗ƒ Buß╗òi 16
        ScheduleRequest req = new ScheduleRequest(2L, 3L, 1,
                "2026-11-01T14:30:00", "2026-11-01T16:30:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.updateSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, 15L, req));

        assertTrue(ex.getConflictResult().isHasConflict());
        assertEquals(16L, ex.getConflictResult().getConflicts().get(0).getConflictingSchedule().get("scheduleId"));
    }

    // -------------------------------------------------------------------------
    // 10. Giß║úng vi├¬n kh├íc nhau v├á ph├▓ng kh├íc nhau kh├┤ng bß╗ï b├ío nhß║ºm
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 10: Giß║úng vi├¬n kh├íc v├á ph├▓ng kh├íc trong c├╣ng khung giß╗¥ kh├┤ng bß╗ï b├ío nhß║ºm xung ─æß╗Öt")
    void testDifferentInstructorAndRoomNotReported() {
        // Buß╗òi 1: Giß║úng vi├¬n 3, Ph├▓ng 1, 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Buß╗òi mß╗¢i: Giß║úng vi├¬n 4, Ph├▓ng 2, 09:00 - 11:00 (kh├íc cß║ú GV lß║½n ph├▓ng)
        ScheduleRequest req = new ScheduleRequest(1L, 4L, 2,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Kh├íc cß║ú giß║úng vi├¬n lß║½n ph├▓ng hß╗ìc kh├┤ng ─æ╞░ß╗úc b├ío xung ─æß╗Öt");
    }

    // -------------------------------------------------------------------------
    // 11. Lß╗ïch ─æ├ú hß╗ºy kh├┤ng bß╗ï t├¡nh l├á xung ─æß╗Öt
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 11: Buß╗òi hß╗ìc c├│ trß║íng th├íi CANCELLED kh├┤ng bß╗ï t├¡nh l├á xung ─æß╗Öt")
    void testCancelledScheduleExcludedFromConflict() {
        // Buß╗òi 1: Giß║úng vi├¬n 3, Ph├▓ng 1, 09:00 - 11:00 nh╞░ng trß║íng th├íi CANCELLED
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "CANCELLED");

        // Buß╗òi mß╗¢i: Giß║úng vi├¬n 3, Ph├▓ng 1, 09:00 - 11:00
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Buß╗òi hß╗ìc ─æ├ú hß╗ºy kh├┤ng ─æ╞░ß╗úc t├¡nh l├á chiß║┐m dß╗Ñng lß╗ïch");
    }

    // -------------------------------------------------------------------------
    // 12. Ng╞░ß╗¥i kh├┤ng c├│ quyß╗ün kh├┤ng thß╗â ghi ─æ├¿
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 12: Ng╞░ß╗¥i kh├┤ng c├│ vai tr├▓ Quß║ún l├╜ ─æ├áo tß║ío (v├¡ dß╗Ñ Giß║úng vi├¬n/Hß╗ìc vi├¬n) kh├┤ng thß╗â ghi ─æ├¿ xung ─æß╗Öt (HTTP 403)")
    void testUnauthorizedUserCannotOverrideConflict() {
        // ─É├ú c├│ lß╗ïch 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Request cß╗æ t├¼nh truyß╗ün overrideConflict = true
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:30:00", "2026-11-01T11:30:00");
        req.setOverrideConflict(true);
        req.setOverrideReason("Tß╗▒ ├╜ ghi ─æ├¿");

        // Giß║úng vi├¬n thß╗¡ ghi ─æ├¿ -> Phß║úi bß╗ï tß╗½ chß╗æi 403 Forbidden
        ForbiddenAccessException ex = assertThrows(ForbiddenAccessException.class, () ->
                service.createSchedule(3L, INSTRUCTOR_ROLES, req));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Training Manager") || ex.getMessage().contains("Quản lý đào tạo"));
    }

    // -------------------------------------------------------------------------
    // 13. Ghi ─æ├¿ hß╗úp lß╗ç phß║úi c├│ audit log
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 13: Quß║ún l├╜ ─æ├áo tß║ío ghi ─æ├¿ hß╗úp lß╗ç k├¿m l├╜ do ─æ╞░ß╗úc l╞░u th├ánh c├┤ng v├á ghi nhß║¡n Audit Log")
    void testAuthorizedOverrideRecordsAuditLog() throws SQLException {
        // ─É├ú c├│ lß╗ïch 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:30:00", "2026-11-01T11:30:00");
        req.setOverrideConflict(true);
        req.setOverrideReason("Giß║úng vi├¬n thß╗Åa thuß║¡n dß║íy gh├⌐p b├╣ theo ph├¬ duyß╗çt cß╗ºa Ban gi├ím ─æß╗æc");

        ClassSession created = service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req);

        assertNotNull(created, "Ghi ─æ├¿ hß╗úp lß╗ç phß║úi l╞░u ─æ╞░ß╗úc buß╗òi hß╗ìc");
        assertEquals(1, mockScheduleDAO.auditLogs.size(), "Phß║úi ghi nhß║¡n 1 bß║ún ghi audit log");
        AuditLog log = mockScheduleDAO.auditLogs.get(0);
        assertEquals("OVERRIDE_SCHEDULE_CONFLICT", log.getAction());
        assertEquals("SCHEDULE", log.getEntityType());
        assertEquals(String.valueOf(created.getId()), log.getEntityId());
        assertTrue(log.getNewValues().contains("Ban gi├ím ─æß╗æc"));
    }

    // -------------------------------------------------------------------------
    // 14. Kiß╗âm tra hai request ─æß╗ông thß╗¥i tß║ío lß╗ïch tr├╣ng nhau (Concurrency / Race condition)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 14: Hai request ─æß╗ông thß╗¥i tß║ío lß╗ïch tr├╣ng nhau - chß╗ë 1 request th├ánh c├┤ng, request c├▓n lß║íi bß╗ï chß║╖n xung ─æß╗Öt")
    void testConcurrentRequestsRaceConditionHandled() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final long classId = i + 1;
            tasks.add(() -> {
                barrier.await(); // ─Éß║úm bß║úo cß║ú 2 luß╗ông c├╣ng xuß║Ñt ph├ít ─æß╗ông thß╗¥i
                ScheduleRequest req = new ScheduleRequest(classId, 3L, 1,
                        "2026-11-01T09:00:00", "2026-11-01T11:00:00");
                try {
                    service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req);
                    successCount.incrementAndGet();
                } catch (ScheduleConflictException e) {
                    conflictCount.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) f.get();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chß╗ë duy nhß║Ñt 1 buß╗òi hß╗ìc ─æ╞░ß╗úc tß║ío th├ánh c├┤ng");
        assertEquals(1, conflictCount.get(), "Buß╗òi hß╗ìc thß╗⌐ hai bß╗ï ph├ít hiß╗çn v├á chß║╖n v├¼ xung ─æß╗Öt");
    }

    // -------------------------------------------------------------------------
    // 15. Lß╗ùi l╞░u dß╗» liß╗çu kh├┤ng ─æß╗â lß║íi lß╗ïch ß╗ƒ trß║íng th├íi dß╗ƒ dang (Rollback)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 15: Lß╗ùi khi xß╗¡ l├╜ l╞░u dß╗» liß╗çu phß║úi rollback, kh├┤ng ─æß╗â lß║íi bß║ún ghi dß╗ƒ dang")
    void testDataConsistencyRollbackOnError() {
        mockScheduleDAO.setSimulateDatabaseError(true);

        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(SQLException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        assertEquals(0, mockScheduleDAO.sessions.size(), "Dß╗» liß╗çu phß║úi sß║ích, kh├┤ng c├│ lß╗ïch dß╗ƒ dang sau rollback");
    }

    // -------------------------------------------------------------------------
    // Test bổ sung: Hai lịch cùng giảng viên và trùng hoàn toàn thời gian
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 2: Hai lịch cùng giảng viên và trùng hoàn toàn thời gian bị chặn")
    void testExactTimeInstructorConflict() {
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest req = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        assertTrue(ex.getConflictResult().isHasConflict());
        assertEquals(ConflictType.INSTRUCTOR_CONFLICT.getCode(), ex.getConflictResult().getConflicts().get(0).getType());
    }

    // -------------------------------------------------------------------------
    // Tình huống 8: Cùng giảng viên nhưng khác ngày: không xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 8: Cùng giảng viên nhưng khác ngày -> không xung đột")
    void testSameInstructorDifferentDateNotConflicting() {
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Ngày 2026-11-02 cùng giờ 09:00 - 11:00 cùng giảng viên 3
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 2,
                "2026-11-02T09:00:00", "2026-11-02T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Khác ngày học không được tính là xung đột lịch");
    }

    // -------------------------------------------------------------------------
    // Tình huống 9: Cùng phòng nhưng khác ngày: không xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 9: Cùng phòng nhưng khác ngày -> không xung đột")
    void testSameRoomDifferentDateNotConflicting() {
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Ngày 2026-11-02 cùng giờ 09:00 - 11:00 cùng phòng 1
        ScheduleRequest req = new ScheduleRequest(1L, 4L, 1,
                "2026-11-02T09:00:00", "2026-11-02T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Khác ngày học không được tính là xung đột phòng");
    }

    // -------------------------------------------------------------------------
    // Tình huống 12: Dữ liệu có thời gian kết thúc trước hoặc bằng thời gian bắt đầu
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 12a: Thời gian kết thúc < thời gian bắt đầu -> từ chối (HTTP 400)")
    void testEndTimeBeforeStartTimeThrowsValidationException() {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T10:00:00", "2026-11-01T08:00:00");

        ValidationException ex = assertThrows(ValidationException.class, req::validate);
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    @DisplayName("Tình huống 12b: Thời gian kết thúc = thời gian bắt đầu -> từ chối (HTTP 400)")
    void testEndTimeEqualsStartTimeThrowsValidationException() {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T09:00:00");

        ValidationException ex = assertThrows(ValidationException.class, req::validate);
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    // -------------------------------------------------------------------------
    // Tình huống 13: Giảng viên hoặc phòng không tồn tại -> trả lỗi ResourceNotFoundException
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 13a: Giảng viên không tồn tại -> ném ResourceNotFoundException")
    void testNonExistentInstructorThrowsNotFound() {
        ScheduleRequest req = new ScheduleRequest(1L, 9999L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(com.ems.exception.ResourceNotFoundException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));
    }

    @Test
    @DisplayName("Tình huống 13b: Phòng học không tồn tại -> ném ResourceNotFoundException")
    void testNonExistentRoomThrowsNotFound() {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 9999,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(com.ems.exception.ResourceNotFoundException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));
    }

    @Test
    @DisplayName("Tình huống 13c: Lớp học không tồn tại -> ném ResourceNotFoundException")
    void testNonExistentClassThrowsNotFound() {
        ScheduleRequest req = new ScheduleRequest(9999L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(com.ems.exception.ResourceNotFoundException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));
    }

    private void addExistingSession(Long id, Long classId, Long instructorId, Integer roomId,
                                    LocalTime start, LocalTime end, String status) {
        ClassSession s = new ClassSession();
        s.setId(id);
        s.setClassId(classId);
        s.setClassCode("Lß╗ÜP-TEST-" + classId);
        s.setClassName("T├¬n lß╗¢p " + classId);
        s.setInstructorId(instructorId);
        s.setInstructorName("Trß║ºn Thß╗ï Giß║úng");
        s.setClassroomId(roomId);
        s.setRoomCode("PH-10" + roomId);
        s.setSessionDate(TEST_DATE);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setStatus(status);
        mockScheduleDAO.sessions.add(s);
    }

    // =========================================================================
    // Mock DAOs cho m├┤i tr╞░ß╗¥ng Test ─æß╗Öc lß║¡p, tß╗æc ─æß╗Ö cao, thread-safe
    // =========================================================================
    static class MockScheduleDAO extends ScheduleDAO {
        final List<ClassSession> sessions = new CopyOnWriteArrayList<>();
        final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();
        final AtomicLong idGen = new AtomicLong(100);
        private volatile boolean simulateDatabaseError = false;

        public void setSimulateDatabaseError(boolean val) {
            this.simulateDatabaseError = val;
        }

        @Override
        public synchronized List<ClassSession> findConflictingSessions(
                Connection connection, Long excludeScheduleId, Long instructorId,
                Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
            List<ClassSession> result = new ArrayList<>();
            for (ClassSession s : sessions) {
                if (excludeScheduleId != null && excludeScheduleId.equals(s.getId())) continue;
                if ("CANCELLED".equalsIgnoreCase(s.getStatus())) continue;
                if (!date.equals(s.getSessionDate())) continue;

                boolean overlap = s.getStartTime().isBefore(endTime) && s.getEndTime().isAfter(startTime);
                if (overlap) {
                    if ((instructorId != null && instructorId.equals(s.getInstructorId()))
                            || (roomId != null && roomId.equals(s.getClassroomId()))) {
                        result.add(s);
                    }
                }
            }
            return result;
        }

        @Override
        public synchronized long createSchedule(Connection connection, ClassSession session) throws SQLException {
            if (simulateDatabaseError) {
                throw new SQLException("Simulated database failure during insert");
            }
            long id = idGen.incrementAndGet();
            session.setId(id);
            sessions.add(session);
            return id;
        }

        @Override
        public synchronized boolean updateSchedule(Connection connection, ClassSession session) {
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getId().equals(session.getId())) {
                    sessions.set(i, session);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized ClassSession findById(Connection connection, long id) {
            return sessions.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        }

        private final Set<Long> attendedSessionIds = ConcurrentHashMap.newKeySet();
        private final Map<Long, List<Long>> enrolledStudents = new ConcurrentHashMap<>();
        public final List<com.ems.model.Notification> sentNotifications = Collections.synchronizedList(new ArrayList<>());
        public final List<com.ems.model.ClassSessionChangeLog> changeLogs = Collections.synchronizedList(new ArrayList<>());

        public void markAttended(long sessionId) {
            attendedSessionIds.add(sessionId);
        }

        public void addEnrolledStudent(long classId, long studentUserId) {
            enrolledStudents.computeIfAbsent(classId, k -> new CopyOnWriteArrayList<>()).add(studentUserId);
        }

        @Override
        public boolean hasAttendance(Connection connection, long sessionId) {
            return attendedSessionIds.contains(sessionId);
        }

        @Override
        public boolean hasAttendance(long sessionId) {
            return attendedSessionIds.contains(sessionId);
        }

        @Override
        public List<Long> getEnrolledStudentUserIds(Connection connection, long classId) {
            return enrolledStudents.getOrDefault(classId, List.of());
        }

        @Override
        public List<Long> getEnrolledStudentUserIds(long classId) {
            return enrolledStudents.getOrDefault(classId, List.of());
        }

        @Override
        public int sendNotificationToStudents(Connection connection, List<Long> recipientUserIds, Long senderId,
                                              String title, String content, String type, String linkUrl) {
            if (recipientUserIds == null) return 0;
            for (Long rId : recipientUserIds) {
                sentNotifications.add(new com.ems.model.Notification(rId, senderId, title, content, type, linkUrl));
            }
            return recipientUserIds.size();
        }

        @Override
        public void recordChangeLog(Connection connection, long sessionId, String actionType,
                                    long performedBy, String reason, String oldValues, String newValues) {
            changeLogs.add(new com.ems.model.ClassSessionChangeLog(sessionId, actionType, performedBy, reason, oldValues, newValues));
        }

        @Override
        public synchronized boolean cancelSchedule(Connection connection, long id, String cancelReason) {
            for (ClassSession s : sessions) {
                if (s.getId().equals(id)) {
                    s.setStatus("CANCELLED");
                    s.setCancelReason(cancelReason);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized void recordAuditLog(Connection connection, Long userId, String action,
                                                String entityType, String entityId, String oldVal, String newVal) {
            AuditLog log = new AuditLog(userId, action, entityType, entityId, oldVal, newVal);
            auditLogs.add(log);
        }

        private final java.util.concurrent.locks.ReentrantLock roomInstructorLock = new java.util.concurrent.locks.ReentrantLock();

        @Override
        public void lockInstructorAndRoom(Connection connection, Long instructorId, Integer roomId) {
            roomInstructorLock.lock();
        }

        public void releaseLock() {
            if (roomInstructorLock.isHeldByCurrentThread()) {
                roomInstructorLock.unlock();
            }
        }
    }

    static class MockClassroomDAO extends ClassroomDAO {
        private final Map<Integer, Classroom> map = new ConcurrentHashMap<>();

        public void save(Classroom c) { map.put(c.getId(), c); }

        @Override
        public Classroom findById(Connection conn, int id) { return map.get(id); }
    }

    static class MockClassDAO extends ClassDAO {
        private final Map<Long, SchoolClass> map = new ConcurrentHashMap<>();

        public void save(SchoolClass c) { map.put(c.getId(), c); }

        @Override
        public SchoolClass findById(Connection conn, long id) { return map.get(id); }
    }

    static class MockUserDAO extends UserDAO {
        private final Map<Long, User> map = new ConcurrentHashMap<>();

        public void save(User u) { map.put(u.getId(), u); }

        @Override
        public User findById(long id) { return map.get(id); }
    }

    static Connection createMockConnection(MockScheduleDAO dao) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                        dao.releaseLock();
                    }
                    if ("isClosed".equals(name)) return false;
                    return null;
                }
        );
    }
}
