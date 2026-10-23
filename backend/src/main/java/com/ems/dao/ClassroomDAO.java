package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.Classroom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClassroomDAO {

    public Classroom findById(Connection connection, int id) throws SQLException {
        String sql = "SELECT id, room_code, room_name, capacity, room_type, meeting_url, status "
                + "FROM classrooms WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Classroom room = new Classroom();
                    room.setId(rs.getInt("id"));
                    room.setRoomCode(rs.getString("room_code"));
                    room.setRoomName(rs.getString("room_name"));
                    room.setCapacity(rs.getInt("capacity"));
                    room.setRoomType(rs.getString("room_type"));
                    room.setMeetingUrl(rs.getString("meeting_url"));
                    room.setStatus(rs.getString("status"));
                    return room;
                }
            }
        }
        return null;
    }

    public Classroom findById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public boolean exists(Connection connection, int id) throws SQLException {
        String sql = "SELECT 1 FROM classrooms WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Classroom> listAvailable(Connection connection) throws SQLException {
        List<Classroom> list = new ArrayList<>();
        String sql = "SELECT id, room_code, room_name, capacity, room_type, meeting_url, status "
                + "FROM classrooms WHERE status = 'AVAILABLE' ORDER BY room_code";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Classroom room = new Classroom();
                room.setId(rs.getInt("id"));
                room.setRoomCode(rs.getString("room_code"));
                room.setRoomName(rs.getString("room_name"));
                room.setCapacity(rs.getInt("capacity"));
                room.setRoomType(rs.getString("room_type"));
                room.setMeetingUrl(rs.getString("meeting_url"));
                room.setStatus(rs.getString("status"));
                list.add(room);
            }
        }
        return list;
    }
}
