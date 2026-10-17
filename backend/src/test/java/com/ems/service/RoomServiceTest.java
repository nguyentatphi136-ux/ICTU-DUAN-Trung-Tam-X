package com.ems.service;

import com.ems.dao.RoomDAO;
import com.ems.dto.CreateRoomRequest;
import com.ems.dto.RoomCapacityValidationResult;
import com.ems.dto.RoomResponse;
import com.ems.dto.UpdateRoomRequest;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.RoomCapacityException;
import com.ems.model.Room;
import com.ems.test.MockRoomDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho RoomService theo User Story S3-09
 */
public class RoomServiceTest {

    private RoomService roomService;
    private MockRoomDAO mockRoomDAO;

    @BeforeEach
    void setUp() {
        mockRoomDAO = new MockRoomDAO();
        roomService = new RoomService(mockRoomDAO);
    }

    @Test
    @DisplayName("TC1: Tạo phòng vật lý hợp lệ với sức chứa lớn hơn 0")
    void testCreatePhysicalRoomSuccess() throws SQLException {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomCode("PH-101");
        req.setRoomName("Phòng học lý thuyết 101");
        req.setRoomType("PHYSICAL");
        req.setCapacity(20);
        req.setLocation("Tầng 1 - Tòa A");
        req.setIsActive(true);

        RoomResponse res = roomService.createRoom(req);
        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("PH-101", res.getRoomCode());
        assertEquals(20, res.getCapacity());
        assertEquals("PHYSICAL", res.getRoomType());
        assertTrue(res.getIsActive());
    }

    @Test
    @DisplayName("TC2: Tạo phòng trực tuyến hợp lệ với URL cuộc họp")
    void testCreateOnlineRoomSuccess() throws SQLException {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomCode("OL-101");
        req.setRoomName("Phòng trực tuyến Google Meet");
        req.setRoomType("ONLINE");
        req.setOnlineMeetingUrl("https://meet.google.com/abc-defg-hij");
        req.setIsActive(true);

        RoomResponse res = roomService.createRoom(req);
        assertNotNull(res);
        assertEquals("OL-101", res.getRoomCode());
        assertEquals("ONLINE", res.getRoomType());
        assertEquals("https://meet.google.com/abc-defg-hij", res.getOnlineMeetingUrl());
    }

    @Test
    @DisplayName("TC3: Tạo phòng với RoomCode bị trùng lặp -> báo lỗi DuplicateResourceException (HTTP 409)")
    void testCreateDuplicateRoomCodeThrowsConflict() throws SQLException {
        CreateRoomRequest req1 = new CreateRoomRequest();
        req1.setRoomCode("PH-101");
        req1.setRoomName("Phòng 1");
        req1.setRoomType("PHYSICAL");
        req1.setCapacity(25);
        roomService.createRoom(req1);

        CreateRoomRequest req2 = new CreateRoomRequest();
        req2.setRoomCode("PH-101");
        req2.setRoomName("Phòng 2 trùng mã");
        req2.setRoomType("PHYSICAL");
        req2.setCapacity(30);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> roomService.createRoom(req2));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    @DisplayName("TC4: Tạo phòng thiếu RoomName -> báo lỗi IllegalArgumentException (HTTP 400)")
    void testCreateRoomMissingNameThrowsValidation() {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomCode("PH-102");
        req.setRoomName("   ");
        req.setRoomType("PHYSICAL");
        req.setCapacity(20);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.createRoom(req));
        assertTrue(ex.getMessage().contains("Tên phòng học"));
    }

    @Test
    @DisplayName("TC5: Tạo phòng vật lý với Capacity bằng 0 -> báo lỗi IllegalArgumentException (HTTP 400)")
    void testCreatePhysicalRoomCapacityZeroThrowsValidation() {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomCode("PH-103");
        req.setRoomName("Phòng 0 chỗ");
        req.setRoomType("PHYSICAL");
        req.setCapacity(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.createRoom(req));
        assertTrue(ex.getMessage().contains("lớn hơn 0"));
    }

    @Test
    @DisplayName("TC6: Tạo phòng vật lý với Capacity âm -> báo lỗi IllegalArgumentException (HTTP 400)")
    void testCreatePhysicalRoomCapacityNegativeThrowsValidation() {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomCode("PH-104");
        req.setRoomName("Phòng sức chứa âm");
        req.setRoomType("PHYSICAL");
        req.setCapacity(-5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.createRoom(req));
        assertTrue(ex.getMessage().contains("lớn hơn 0"));
    }

    @Test
    @DisplayName("TC7: Tạo phòng trực tuyến với URL sai định dạng -> báo lỗi IllegalArgumentException (HTTP 400)")
    void testCreateOnlineRoomInvalidUrlThrowsValidation() {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setRoomCode("OL-102");
        req.setRoomName("Phòng trực tuyến sai URL");
        req.setRoomType("ONLINE");
        req.setOnlineMeetingUrl("not-a-valid-url");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.createRoom(req));
        assertTrue(ex.getMessage().contains("onlineMeetingUrl"));
    }

    @Test
    @DisplayName("TC8: Lấy danh sách phòng học hỗ trợ phân trang và bộ lọc")
    void testGetRoomsListAndFilter() throws SQLException {
        CreateRoomRequest p1 = new CreateRoomRequest();
        p1.setRoomCode("PH-201");
        p1.setRoomName("Phòng vật lý 201");
        p1.setRoomType("PHYSICAL");
        p1.setCapacity(20);
        roomService.createRoom(p1);

        CreateRoomRequest o1 = new CreateRoomRequest();
        o1.setRoomCode("OL-201");
        o1.setRoomName("Phòng online 201");
        o1.setRoomType("ONLINE");
        o1.setOnlineMeetingUrl("https://meet.google.com/test");
        roomService.createRoom(o1);

        Map<String, Object> all = roomService.getRooms(null, null, null, 1, 10);
        assertEquals(2, all.get("totalCount"));

        Map<String, Object> onlyPhysical = roomService.getRooms(null, "PHYSICAL", null, 1, 10);
        assertEquals(1, onlyPhysical.get("totalCount"));

        Map<String, Object> search = roomService.getRooms("201", null, null, 1, 10);
        assertEquals(2, search.get("totalCount"));
    }

    @Test
    @DisplayName("TC9: Lấy phòng theo ID không tồn tại -> báo lỗi ResourceNotFoundException (HTTP 404)")
    void testGetRoomByIdNotFoundThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(9999L));
    }

    @Test
    @DisplayName("TC10: Cập nhật phòng học thành công")
    void testUpdateRoomSuccess() throws SQLException {
        CreateRoomRequest p = new CreateRoomRequest();
        p.setRoomCode("PH-301");
        p.setRoomName("Phòng ban đầu");
        p.setRoomType("PHYSICAL");
        p.setCapacity(25);
        RoomResponse created = roomService.createRoom(p);

        UpdateRoomRequest update = new UpdateRoomRequest();
        update.setRoomCode("PH-301");
        update.setRoomName("Phòng đã nâng cấp máy chiếu");
        update.setRoomType("PHYSICAL");
        update.setCapacity(30);
        update.setIsActive(true);

        RoomResponse updated = roomService.updateRoom(created.getId(), update);
        assertEquals("Phòng đã nâng cấp máy chiếu", updated.getRoomName());
        assertEquals(30, updated.getCapacity());
    }

    @Test
    @DisplayName("TC11: Cập nhật RoomCode thành mã đã tồn tại ở phòng khác -> báo lỗi DuplicateResourceException (HTTP 409)")
    void testUpdateRoomDuplicateCodeThrowsConflict() throws SQLException {
        CreateRoomRequest p1 = new CreateRoomRequest();
        p1.setRoomCode("PH-301");
        p1.setRoomName("Phòng 301");
        p1.setRoomType("PHYSICAL");
        p1.setCapacity(20);
        roomService.createRoom(p1);

        CreateRoomRequest p2 = new CreateRoomRequest();
        p2.setRoomCode("PH-302");
        p2.setRoomName("Phòng 302");
        p2.setRoomType("PHYSICAL");
        p2.setCapacity(20);
        RoomResponse created2 = roomService.createRoom(p2);

        UpdateRoomRequest update = new UpdateRoomRequest();
        update.setRoomCode("PH-301"); // trùng với p1
        update.setRoomName("Phòng 302 đổi mã");
        update.setRoomType("PHYSICAL");
        update.setCapacity(20);

        assertThrows(DuplicateResourceException.class, () -> roomService.updateRoom(created2.getId(), update));
    }

    @Test
    @DisplayName("TC12: Xóa phòng chưa được sử dụng -> thực hiện xóa vật lý")
    void testDeleteUnusedRoomPhysicalDelete() throws SQLException {
        CreateRoomRequest p = new CreateRoomRequest();
        p.setRoomCode("PH-401");
        p.setRoomName("Phòng chưa dùng");
        p.setRoomType("PHYSICAL");
        p.setCapacity(20);
        RoomResponse created = roomService.createRoom(p);

        Map<String, Object> delRes = roomService.deleteRoom(created.getId());
        assertEquals("DELETED", delRes.get("action"));
        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(created.getId()));
    }

    @Test
    @DisplayName("TC13: Xóa phòng đang được tham chiếu trong lịch học/lớp -> áp dụng soft delete (isActive=false)")
    void testDeleteReferencedRoomSoftDeletes() throws SQLException {
        CreateRoomRequest p = new CreateRoomRequest();
        p.setRoomCode("PH-402");
        p.setRoomName("Phòng đang có lớp học");
        p.setRoomType("PHYSICAL");
        p.setCapacity(20);
        RoomResponse created = roomService.createRoom(p);

        // Đánh dấu phòng đang được tham chiếu
        mockRoomDAO.markReferenced(created.getId(), true);

        Map<String, Object> delRes = roomService.deleteRoom(created.getId());
        assertEquals("DEACTIVATED", delRes.get("action"));

        // Bản ghi vẫn còn trong DB nhưng isActive = false
        RoomResponse after = roomService.getRoomById(created.getId());
        assertFalse(after.getIsActive());
    }

    @Test
    @DisplayName("TC14: Từ chối xếp lớp 30 học viên vào phòng vật lý 20 chỗ (ROOM_CAPACITY_EXCEEDED)")
    void testRejectClassCapacityExceededForPhysicalRoom() throws SQLException {
        CreateRoomRequest p = new CreateRoomRequest();
        p.setRoomCode("PH-501");
        p.setRoomName("Phòng 20 chỗ");
        p.setRoomType("PHYSICAL");
        p.setCapacity(20);
        RoomResponse room = roomService.createRoom(p);

        RoomCapacityValidationResult res = roomService.validateCapacity(room.getId(), 30);
        assertFalse(res.isValid());
        assertEquals("ROOM_CAPACITY_EXCEEDED", res.getCode());
        assertEquals(30, res.getStudentCount());
        assertEquals(20, res.getRoomCapacity());
        assertTrue(res.getMessage().contains("vượt quá sức chứa"));
    }

    @Test
    @DisplayName("TC15: Cho phép xếp lớp 20 học viên vào phòng vật lý 20 hoặc 30 chỗ (ROOM_CAPACITY_VALID)")
    void testAllowClassCapacityMatchesPhysicalRoom() throws SQLException {
        CreateRoomRequest p = new CreateRoomRequest();
        p.setRoomCode("PH-502");
        p.setRoomName("Phòng 20 chỗ");
        p.setRoomType("PHYSICAL");
        p.setCapacity(20);
        RoomResponse room = roomService.createRoom(p);

        RoomCapacityValidationResult res = roomService.validateCapacity(room.getId(), 20);
        assertTrue(res.isValid());
        assertEquals("ROOM_CAPACITY_VALID", res.getCode());
        assertEquals(20, res.getStudentCount());
        assertEquals(20, res.getRoomCapacity());
    }

    @Test
    @DisplayName("TC16: Quy tắc Capacity không áp dụng sai cho phòng trực tuyến (30 học viên vào phòng online vẫn hợp lệ)")
    void testCapacityCheckIgnoredForOnlineRoom() throws SQLException {
        CreateRoomRequest o = new CreateRoomRequest();
        o.setRoomCode("OL-501");
        o.setRoomName("Phòng trực tuyến Zoom");
        o.setRoomType("ONLINE");
        o.setOnlineMeetingUrl("https://zoom.us/j/123456");
        RoomResponse room = roomService.createRoom(o);

        RoomCapacityValidationResult res = roomService.validateCapacity(room.getId(), 30);
        assertTrue(res.isValid());
        assertEquals("ROOM_CAPACITY_VALID", res.getCode());
    }

    @Test
    @DisplayName("TC17: Giảm sức chứa phòng vật lý khi có lớp đang xếp vào phòng vượt sức chứa mới -> từ chối an toàn")
    void testRejectDecreasingCapacityWhenClassExceeds() throws SQLException {
        CreateRoomRequest p = new CreateRoomRequest();
        p.setRoomCode("PH-601");
        p.setRoomName("Phòng 30 chỗ");
        p.setRoomType("PHYSICAL");
        p.setCapacity(30);
        RoomResponse room = roomService.createRoom(p);

        // Mô phỏng có lớp 28 học viên đang xếp vào phòng này
        mockRoomDAO.addAssignedClassViolation(room.getId(), 28, "Lớp Java Fullstack 01", "JV-01");

        UpdateRoomRequest update = new UpdateRoomRequest();
        update.setRoomCode("PH-601");
        update.setRoomName("Phòng 30 chỗ");
        update.setRoomType("PHYSICAL");
        update.setCapacity(20); // Giảm từ 30 xuống 20, trong khi lớp đang có 28 học viên!

        RoomCapacityException ex = assertThrows(RoomCapacityException.class,
                () -> roomService.updateRoom(room.getId(), update));
        assertEquals("ROOM_CAPACITY_EXCEEDED", ex.getCode());
        assertTrue(ex.getMessage().contains("Không thể giảm sức chứa"));
    }
}
