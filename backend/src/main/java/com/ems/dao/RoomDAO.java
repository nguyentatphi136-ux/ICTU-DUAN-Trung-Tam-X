package com.ems.dao;

import com.ems.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phòng học vật lý và trực tuyến (S3-09), bảng classrooms. Phòng trả về dạng Map
 * { id, code, name, type, capacity, location, meetingUrl, status, openClasses }; capacity null là không giới hạn.
 */
public class RoomDAO {
    public record Room(String code, String name, String type, Integer capacity, String location, String meetingUrl) {}

    private static final String SELECT = "SELECT r.id, r.room_code, r.room_name, r.room_type, r.capacity, r.location, r.meeting_url, r.status, "
            + "(SELECT COUNT(*) FROM classes c WHERE c.default_classroom_id = r.id AND c.status IN ('PLANNING', 'IN_PROGRESS')) AS open_classes "
            + "FROM classrooms r ";

    public List<Map<String, Object>> list(String type) throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + (type == null ? "" : "WHERE r.room_type = ? ") + "ORDER BY r.room_type, r.room_code")) {
            if (type != null) stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public Map<String, Object> find(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE r.id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Phòng khác (không phải excludeId) đang dùng mã này. */
    public boolean codeTaken(String code, int excludeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM classrooms WHERE room_code = ? AND id <> ?")) {
            stmt.setString(1, code);
            stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int create(Room r) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO classrooms (room_code, room_name, room_type, capacity, location, meeting_url, status) "
                             + "VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE')", Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, r);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(int id, Room r) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE classrooms SET room_code = ?, room_name = ?, room_type = ?, capacity = ?, location = ?, meeting_url = ? WHERE id = ?")) {
            bind(stmt, r);
            stmt.setInt(7, id);
            stmt.executeUpdate();
        }
    }

    /** Xoá phòng; lớp đã kết thúc hoặc huỷ từng dùng phòng này được gỡ phòng trước. */
    public void delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String sql : new String[] {"UPDATE classes SET default_classroom_id = NULL WHERE default_classroom_id = ?",
                        "DELETE FROM classrooms WHERE id = ?"}) {
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setInt(1, id);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void bind(PreparedStatement stmt, Room r) throws SQLException {
        stmt.setString(1, r.code());
        stmt.setString(2, r.name());
        stmt.setString(3, r.type());
        stmt.setObject(4, r.capacity());
        stmt.setString(5, r.location());
        stmt.setString(6, r.meetingUrl());
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getInt("id"));
        item.put("code", rs.getString("room_code"));
        item.put("name", rs.getString("room_name"));
        item.put("type", rs.getString("room_type"));
        int capacity = rs.getInt("capacity");
        item.put("capacity", rs.wasNull() ? null : capacity);
        item.put("location", rs.getString("location"));
        item.put("meetingUrl", rs.getString("meeting_url"));
        item.put("status", rs.getString("status"));
        item.put("openClasses", rs.getInt("open_classes"));
        return item;
    }
}
