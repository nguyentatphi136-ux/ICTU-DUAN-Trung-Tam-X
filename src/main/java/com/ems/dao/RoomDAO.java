package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) cho Room / Classroom
 * Quản lý tương tác CSDL với bảng `classrooms`
 */
public class RoomDAO {

    /**
     * Tìm phòng học theo ID
     */
    public Room findById(long id) throws SQLException {
        String sql = "SELECT id, room_code, room_name, room_type, capacity, location, meeting_url, "
                + "description, is_active, status, created_at, updated_at FROM classrooms WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRoom(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm phòng học theo RoomCode
     */
    public Room findByCode(String roomCode) throws SQLException {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, room_code, room_name, room_type, capacity, location, meeting_url, "
                + "description, is_active, status, created_at, updated_at FROM classrooms WHERE room_code = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, roomCode.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRoom(rs);
                }
            }
        }
        return null;
    }

    /**
     * Kiểm tra RoomCode đã tồn tại hay chưa (loại trừ excludeId nếu là thao tác cập nhật)
     */
    public boolean existsByCode(String roomCode, Long excludeId) throws SQLException {
        if (roomCode == null || roomCode.trim().isEmpty()) {
            return false;
        }
        StringBuilder sql = new StringBuilder("SELECT 1 FROM classrooms WHERE room_code = ?");
        if (excludeId != null) {
            sql.append(" AND id <> ?");
        }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            stmt.setString(1, roomCode.trim());
            if (excludeId != null) {
                stmt.setLong(2, excludeId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tìm kiếm, lọc và phân trang danh sách phòng học
     */
    public Map<String, Object> searchRooms(String keyword, String roomType, Boolean isActive, int page, int pageSize)
            throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            whereSql.append(" AND (room_code LIKE ? OR room_name LIKE ? OR location LIKE ?) ");
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (roomType != null && !roomType.trim().isEmpty() && !"ALL".equalsIgnoreCase(roomType.trim())) {
            whereSql.append(" AND UPPER(room_type) = ? ");
            params.add(roomType.trim().toUpperCase());
        }

        if (isActive != null) {
            whereSql.append(" AND is_active = ? ");
            params.add(isActive);
        }

        int totalCount = 0;
        String countSql = "SELECT COUNT(*) FROM classrooms " + whereSql;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalCount = rs.getInt(1);
                }
            }
        }

        int offset = (page - 1) * pageSize;
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        String selectSql = "SELECT id, room_code, room_name, room_type, capacity, location, meeting_url, "
                + "description, is_active, status, created_at, updated_at FROM classrooms "
                + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?";

        List<Room> items = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(selectSql)) {
            int paramIndex = 1;
            for (Object param : params) {
                stmt.setObject(paramIndex++, param);
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToRoom(rs));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        return result;
    }

    /**
     * Tạo phòng học mới
     */
    public Room createRoom(Room room) throws SQLException {
        String sql = "INSERT INTO classrooms (room_code, room_name, room_type, capacity, location, meeting_url, "
                + "description, is_active, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, room.getRoomCode());
            stmt.setString(2, room.getRoomName());
            stmt.setString(3, room.getRoomType());
            if (room.getCapacity() != null) {
                stmt.setInt(4, room.getCapacity());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setString(5, room.getLocation());
            stmt.setString(6, room.getOnlineMeetingUrl());
            stmt.setString(7, room.getDescription());
            stmt.setBoolean(8, room.getIsActive() != null ? room.getIsActive() : true);
            stmt.setString(9, room.getStatus() != null ? room.getStatus() : Room.STATUS_AVAILABLE);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    room.setId(rs.getLong(1));
                }
            }
        }
        return findById(room.getId());
    }

    /**
     * Cập nhật thông tin phòng học
     */
    public Room updateRoom(Room room) throws SQLException {
        String sql = "UPDATE classrooms SET room_code = ?, room_name = ?, room_type = ?, capacity = ?, "
                + "location = ?, meeting_url = ?, description = ?, is_active = ?, status = ?, updated_at = NOW() "
                + "WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomCode());
            stmt.setString(2, room.getRoomName());
            stmt.setString(3, room.getRoomType());
            if (room.getCapacity() != null) {
                stmt.setInt(4, room.getCapacity());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            stmt.setString(5, room.getLocation());
            stmt.setString(6, room.getOnlineMeetingUrl());
            stmt.setString(7, room.getDescription());
            stmt.setBoolean(8, room.getIsActive() != null ? room.getIsActive() : true);
            stmt.setString(9, room.getStatus() != null ? room.getStatus() : Room.STATUS_AVAILABLE);
            stmt.setLong(10, room.getId());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                return null;
            }
        }
        return findById(room.getId());
    }

    /**
     * Xóa vật lý phòng học (khi phòng chưa từng được tham chiếu)
     */
    public boolean deleteRoom(long id) throws SQLException {
        String sql = "DELETE FROM classrooms WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Soft-delete phòng học (ngừng hoạt động khi phòng đã có dữ liệu lịch sử)
     */
    public boolean softDeleteRoom(long id) throws SQLException {
        String sql = "UPDATE classrooms SET is_active = FALSE, status = 'MAINTENANCE', updated_at = NOW() WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Kiểm tra phòng có đang được tham chiếu trong bảng `classes` hoặc `class_sessions` hay không
     */
    public boolean isRoomReferenced(long id) throws SQLException {
        // Kiểm tra trong classes
        String sqlClass = "SELECT 1 FROM classes WHERE default_classroom_id = ? LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sqlClass)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return true;
            }
        } catch (SQLException e) {
            // Nếu bảng classes chưa có hoặc đang khởi tạo, tiếp tục kiểm tra class_sessions
        }

        // Kiểm tra trong class_sessions
        String sqlSession = "SELECT 1 FROM class_sessions WHERE classroom_id = ? LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sqlSession)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return true;
            }
        } catch (SQLException e) {
            // Bỏ qua nếu bảng class_sessions chưa có
        }

        return false;
    }

    /**
     * Lấy danh sách các lớp học đang gán cho phòng này mà có quy mô sĩ số vượt quá newCapacity
     * Phục vụ kiểm tra an toàn khi giảm sức chứa phòng học
     */
    public List<Map<String, Object>> getAssignedClassesExceedingCapacity(long roomId, int newCapacity) throws SQLException {
        List<Map<String, Object>> violations = new ArrayList<>();

        // 1. Kiểm tra các lớp có default_classroom_id = roomId và max_capacity > newCapacity
        String sqlClasses = "SELECT id, class_code, class_name, max_capacity FROM classes "
                + "WHERE default_classroom_id = ? AND status IN ('PLANNING', 'IN_PROGRESS') AND max_capacity > ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sqlClasses)) {
            stmt.setLong(1, roomId);
            stmt.setInt(2, newCapacity);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("classId", rs.getLong("id"));
                    item.put("classCode", rs.getString("class_code"));
                    item.put("className", rs.getString("class_name"));
                    item.put("studentCount", rs.getInt("max_capacity"));
                    item.put("reason", "Sĩ số tối đa quy định của lớp (" + rs.getInt("max_capacity")
                            + " học viên) vượt quá sức chứa mới (" + newCapacity + " chỗ).");
                    violations.add(item);
                }
            }
        } catch (SQLException ignored) {
            // Trường hợp DB chưa tạo bảng classes
        }

        // 2. Kiểm tra số lượng học viên thực tế đã ghi danh (enrolled) vào các lớp đang xếp phòng này
        String sqlEnrolled = "SELECT c.id, c.class_code, c.class_name, COUNT(e.id) AS enrolled_count "
                + "FROM classes c "
                + "JOIN enrollments e ON e.class_id = c.id AND e.status = 'ENROLLED' "
                + "WHERE c.default_classroom_id = ? AND c.status IN ('PLANNING', 'IN_PROGRESS') "
                + "GROUP BY c.id, c.class_code, c.class_name "
                + "HAVING COUNT(e.id) > ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sqlEnrolled)) {
            stmt.setLong(1, roomId);
            stmt.setInt(2, newCapacity);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long classId = rs.getLong("id");
                    boolean alreadyPresent = violations.stream().anyMatch(v -> Long.valueOf(classId).equals(v.get("classId")));
                    if (!alreadyPresent) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("classId", classId);
                        item.put("classCode", rs.getString("class_code"));
                        item.put("className", rs.getString("class_name"));
                        item.put("studentCount", rs.getInt("enrolled_count"));
                        item.put("reason", "Số lượng học viên thực tế đã ghi danh (" + rs.getInt("enrolled_count")
                                + " học viên) vượt quá sức chứa mới (" + newCapacity + " chỗ).");
                        violations.add(item);
                    }
                }
            }
        } catch (SQLException ignored) {
            // Bỏ qua nếu enrollments chưa tồn tại
        }

        return violations;
    }

    private Room mapRowToRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setId(rs.getLong("id"));
        room.setRoomCode(rs.getString("room_code"));
        room.setRoomName(rs.getString("room_name"));
        room.setRoomType(rs.getString("room_type"));
        int cap = rs.getInt("capacity");
        if (!rs.wasNull()) {
            room.setCapacity(cap);
        } else {
            room.setCapacity(null);
        }
        room.setLocation(rs.getString("location"));
        room.setOnlineMeetingUrl(rs.getString("meeting_url"));
        room.setDescription(rs.getString("description"));
        room.setIsActive(rs.getBoolean("is_active"));
        room.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            room.setCreatedAt(new java.util.Date(createdAt.getTime()));
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            room.setUpdatedAt(new java.util.Date(updatedAt.getTime()));
        }
        return room;
    }
}
