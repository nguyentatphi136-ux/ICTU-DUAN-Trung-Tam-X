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

import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service xử lý nghiệp vụ quản lý phòng học và kiểm tra sức chứa xếp lớp (S3-09)
 */
public class RoomService {

    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+", Pattern.CASE_INSENSITIVE);

    private final RoomDAO roomDAO;

    public RoomService() {
        this.roomDAO = new RoomDAO();
    }

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = roomDAO;
    }

    /**
     * Lấy danh sách phòng học có bộ lọc và phân trang
     */
    public Map<String, Object> getRooms(String keyword, String roomType, Boolean isActive, int page, int pageSize)
            throws SQLException {
        Map<String, Object> data = roomDAO.searchRooms(keyword, roomType, isActive, page, pageSize);
        List<?> rawItems = (List<?>) data.get("items");
        List<RoomResponse> responses = new ArrayList<>();
        if (rawItems != null) {
            for (Object obj : rawItems) {
                if (obj instanceof Room r) {
                    responses.add(RoomResponse.fromEntity(r));
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", responses);
        result.put("totalCount", data.get("totalCount"));
        result.put("page", data.get("page"));
        result.put("pageSize", data.get("pageSize"));
        result.put("totalPages", data.get("totalPages"));
        return result;
    }

    /**
     * Lấy chi tiết phòng học theo ID
     */
    public RoomResponse getRoomById(long id) throws SQLException {
        Room room = roomDAO.findById(id);
        if (room == null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng học với mã ID: " + id);
        }
        return RoomResponse.fromEntity(room);
    }

    /**
     * Tạo phòng học mới
     */
    public RoomResponse createRoom(CreateRoomRequest req) throws SQLException {
        validateCreateRequest(req);

        String roomCode = req.getRoomCode().trim().toUpperCase();
        if (roomDAO.existsByCode(roomCode, null)) {
            throw new DuplicateResourceException("Mã phòng học '" + roomCode + "' đã tồn tại trong hệ thống.");
        }

        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setRoomName(req.getRoomName().trim());
        String type = req.getRoomType().trim().toUpperCase();
        room.setRoomType(type);

        if (Room.TYPE_PHYSICAL.equals(type)) {
            room.setCapacity(req.getCapacity());
            room.setLocation(req.getLocation() != null ? req.getLocation().trim() : null);
            room.setOnlineMeetingUrl(null);
        } else {
            room.setCapacity(req.getCapacity() != null && req.getCapacity() > 0 ? req.getCapacity() : null);
            room.setLocation(null);
            room.setOnlineMeetingUrl(req.getOnlineMeetingUrl() != null ? req.getOnlineMeetingUrl().trim() : null);
        }

        room.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        room.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        room.setStatus(Boolean.TRUE.equals(room.getIsActive()) ? Room.STATUS_AVAILABLE : Room.STATUS_MAINTENANCE);

        Room created = roomDAO.createRoom(room);
        return RoomResponse.fromEntity(created);
    }

    /**
     * Cập nhật thông tin phòng học
     */
    public RoomResponse updateRoom(long id, UpdateRoomRequest req) throws SQLException {
        Room existing = roomDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng học với mã ID: " + id);
        }

        validateUpdateRequest(req);

        String newRoomCode = req.getRoomCode().trim().toUpperCase();
        if (roomDAO.existsByCode(newRoomCode, id)) {
            throw new DuplicateResourceException("Mã phòng học '" + newRoomCode + "' đã được sử dụng bởi phòng khác.");
        }

        String newType = req.getRoomType().trim().toUpperCase();

        // Kiểm tra an toàn khi giảm sức chứa phòng vật lý:
        if (Room.TYPE_PHYSICAL.equals(newType) && req.getCapacity() != null) {
            int newCap = req.getCapacity();
            if (existing.getCapacity() != null && newCap < existing.getCapacity()) {
                List<Map<String, Object>> violations = roomDAO.getAssignedClassesExceedingCapacity(id, newCap);
                if (!violations.isEmpty()) {
                    Map<String, Object> firstViolation = violations.get(0);
                    int studentCount = (int) firstViolation.get("studentCount");
                    String className = (String) firstViolation.get("className");
                    String classCode = (String) firstViolation.get("classCode");

                    RoomCapacityValidationResult valResult = RoomCapacityValidationResult.exceeded(
                            newRoomCode, newType, studentCount, newCap);

                    String message = "Không thể giảm sức chứa xuống " + newCap + " chỗ vì lớp '" + className
                            + "' (" + classCode + ") đang xếp vào phòng này có sĩ số " + studentCount + " học viên.";
                    throw new RoomCapacityException(message, valResult);
                }
            }
        }

        existing.setRoomCode(newRoomCode);
        existing.setRoomName(req.getRoomName().trim());
        existing.setRoomType(newType);

        if (Room.TYPE_PHYSICAL.equals(newType)) {
            existing.setCapacity(req.getCapacity());
            existing.setLocation(req.getLocation() != null ? req.getLocation().trim() : null);
            existing.setOnlineMeetingUrl(null);
        } else {
            existing.setCapacity(req.getCapacity() != null && req.getCapacity() > 0 ? req.getCapacity() : null);
            existing.setLocation(null);
            existing.setOnlineMeetingUrl(req.getOnlineMeetingUrl() != null ? req.getOnlineMeetingUrl().trim() : null);
        }

        existing.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);
        if (req.getIsActive() != null) {
            existing.setIsActive(req.getIsActive());
            existing.setStatus(Boolean.TRUE.equals(req.getIsActive()) ? Room.STATUS_AVAILABLE : Room.STATUS_MAINTENANCE);
        }

        Room updated = roomDAO.updateRoom(existing);
        return RoomResponse.fromEntity(updated);
    }

    /**
     * Xóa phòng học:
     * - Nếu phòng chưa từng được tham chiếu trong lớp / lịch học: xóa vật lý.
     * - Nếu phòng đang được tham chiếu: soft-delete (đặt isActive = false, status = MAINTENANCE).
     */
    public Map<String, Object> deleteRoom(long id) throws SQLException {
        Room room = roomDAO.findById(id);
        if (room == null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng học với mã ID: " + id);
        }

        boolean referenced = roomDAO.isRoomReferenced(id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roomId", id);
        response.put("roomCode", room.getRoomCode());

        if (referenced) {
            // Soft delete
            roomDAO.softDeleteRoom(id);
            response.put("action", "DEACTIVATED");
            response.put("message", "Phòng học '" + room.getRoomCode()
                    + "' đang được liên kết trong lớp/lịch học nên đã chuyển sang trạng thái ngừng hoạt động thay vì xóa dữ liệu lịch sử.");
            response.put("isActive", false);
        } else {
            // Physical delete
            roomDAO.deleteRoom(id);
            response.put("action", "DELETED");
            response.put("message", "Đã xóa phòng học '" + room.getRoomCode() + "' thành công.");
        }
        return response;
    }

    /**
     * Quy tắc nghiệp vụ trọng tâm (User Story S3-09):
     * Kiểm tra sức chứa phòng khi xếp lớp
     */
    public RoomCapacityValidationResult validateCapacity(long roomId, int studentCount) throws SQLException {
        Room room = roomDAO.findById(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng học với mã ID: " + roomId);
        }

        if (studentCount <= 0) {
            throw new IllegalArgumentException("Sĩ số lớp học phải lớn hơn 0.");
        }

        if (room.isOnline()) {
            return RoomCapacityValidationResult.valid(
                    room.getRoomCode(),
                    room.getRoomType(),
                    studentCount,
                    room.getCapacity(),
                    "Phòng học trực tuyến không giới hạn sức chứa vật lý."
            );
        }

        int capacity = room.getCapacity() != null ? room.getCapacity() : 0;
        if (studentCount > capacity) {
            return RoomCapacityValidationResult.exceeded(
                    room.getRoomCode(),
                    room.getRoomType(),
                    studentCount,
                    capacity
            );
        }

        return RoomCapacityValidationResult.valid(
                room.getRoomCode(),
                room.getRoomType(),
                studentCount,
                capacity,
                "Sĩ số lớp (" + studentCount + ") phù hợp với sức chứa của phòng " + room.getRoomCode() + " (" + capacity + " chỗ)."
        );
    }

    private void validateCreateRequest(CreateRoomRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu không được để trống.");
        }
        validateCommonFields(req.getRoomCode(), req.getRoomName(), req.getRoomType(), req.getCapacity(), req.getOnlineMeetingUrl());
    }

    private void validateUpdateRequest(UpdateRoomRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu không được để trống.");
        }
        validateCommonFields(req.getRoomCode(), req.getRoomName(), req.getRoomType(), req.getCapacity(), req.getOnlineMeetingUrl());
    }

    private void validateCommonFields(String roomCode, String roomName, String roomType, Integer capacity, String onlineMeetingUrl) {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phòng học (roomCode) không được để trống.");
        }
        if (roomCode.trim().length() > 50) {
            throw new IllegalArgumentException("Mã phòng học không được vượt quá 50 ký tự.");
        }

        if (roomName == null || roomName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên phòng học (roomName) không được để trống.");
        }
        if (roomName.trim().length() > 100) {
            throw new IllegalArgumentException("Tên phòng học không được vượt quá 100 ký tự.");
        }

        if (roomType == null || roomType.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại phòng học (roomType) không được để trống.");
        }
        String typeUpper = roomType.trim().toUpperCase();
        if (!Room.TYPE_PHYSICAL.equals(typeUpper) && !Room.TYPE_ONLINE.equals(typeUpper)) {
            throw new IllegalArgumentException("Loại phòng học không hợp lệ. Chỉ chấp nhận 'PHYSICAL' hoặc 'ONLINE'.");
        }

        if (Room.TYPE_PHYSICAL.equals(typeUpper)) {
            if (capacity == null || capacity <= 0) {
                throw new IllegalArgumentException("Phòng học vật lý phải có sức chứa (capacity) lớn hơn 0.");
            }
        }

        if (Room.TYPE_ONLINE.equals(typeUpper)) {
            if (onlineMeetingUrl == null || onlineMeetingUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("Phòng học trực tuyến phải có đường dẫn họp (onlineMeetingUrl).");
            }
            String url = onlineMeetingUrl.trim();
            if (!URL_PATTERN.matcher(url).matches()) {
                throw new IllegalArgumentException("Đường dẫn phòng trực tuyến (onlineMeetingUrl) không đúng định dạng. Phải bắt đầu bằng http:// hoặc https://.");
            }
            try {
                new URL(url).toURI();
            } catch (Exception e) {
                throw new IllegalArgumentException("Đường dẫn phòng trực tuyến (onlineMeetingUrl) không hợp lệ.");
            }
        }
    }
}
