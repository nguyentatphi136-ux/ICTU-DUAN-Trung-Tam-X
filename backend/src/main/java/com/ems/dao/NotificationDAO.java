package com.ems.dao;

import com.ems.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thông báo trong ứng dụng (bảng notifications, S6-09 chuông, S6-10 trang Thông báo). S4-03 ghi qua ScheduleDAO;
 * S4-07, S4-08 ghi qua send(). Loại: SCHEDULE, ABSENCE, ASSIGNMENT, GRADE, TUITION, SYSTEM.
 */
public class NotificationDAO {

    /** Ghi cùng giao dịch với thao tác gốc; người gửi không nhận thông báo của chính mình. */
    public static void send(Connection conn, Collection<Long> to, Long senderId, String type, String title, String content, String link)
            throws SQLException {
        Set<Long> ids = new LinkedHashSet<>(to);
        if (senderId != null) ids.remove(senderId);
        if (ids.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO notifications (recipient_id, sender_id, title, content, "
                + "notification_type, link_url, is_read) VALUES (?, ?, ?, ?, ?, ?, FALSE)")) {
            for (Long id : ids) {
                ps.setLong(1, id);
                ps.setObject(2, senderId);
                ps.setString(3, title);
                ps.setString(4, content);
                ps.setString(5, type);
                ps.setString(6, link);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Giảng viên buổi học, giảng viên phụ trách và trợ giảng các môn của lớp có buổi đó. */
    public static List<Long> teachersOf(Connection conn, long sessionId) throws SQLException {
        return ids(conn, "SELECT cs.instructor_id FROM class_sessions cs WHERE cs.id = ? AND cs.instructor_id IS NOT NULL "
                + "UNION SELECT t.primary_instructor_id FROM class_subjects t JOIN class_sessions cs ON cs.class_id = t.class_id "
                + " WHERE cs.id = ? AND t.primary_instructor_id IS NOT NULL "
                + "UNION SELECT a.ta_user_id FROM class_subject_assistants a JOIN class_subjects t ON t.id = a.class_subject_id "
                + " JOIN class_sessions cs ON cs.class_id = t.class_id WHERE cs.id = ?", sessionId, sessionId, sessionId);
    }

    /** Tài khoản Quản lý đào tạo còn hoạt động. */
    public static List<Long> managers(Connection conn) throws SQLException {
        return ids(conn, "SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id = ur.role_id JOIN users u ON u.id = ur.user_id "
                + "WHERE r.role_code = 'TRAINING_MANAGER' AND u.deleted_at IS NULL");
    }

    private static List<Long> ids(Connection conn, String sql, Object... params) throws SQLException {
        List<Long> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getLong(1));
            }
        }
        return out;
    }

    /** 100 thông báo mới nhất của người dùng. */
    public List<Map<String, Object>> inbox(long userId) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, title, content, notification_type, link_url, is_read, created_at "
                     + "FROM notifications WHERE recipient_id = ? ORDER BY created_at DESC, id DESC LIMIT 100")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("title", rs.getString("title"));
                    row.put("body", rs.getString("content"));
                    row.put("type", rs.getString("notification_type"));
                    row.put("link", rs.getString("link_url"));
                    row.put("read", rs.getBoolean("is_read"));
                    row.put("at", rs.getTimestamp("created_at").toLocalDateTime().toString().substring(0, 16));
                    out.add(row);
                }
            }
        }
        return out;
    }

    /** Đánh dấu đã đọc; ids rỗng là tất cả. Chỉ đụng tới thông báo của chính người dùng. */
    public void markRead(long userId, List<Long> ids) throws SQLException {
        String in = ids.isEmpty() ? "" : " AND id IN (" + String.join(",", ids.stream().map(x -> "?").toList()) + ")";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE notifications SET is_read = TRUE WHERE recipient_id = ?" + in)) {
            ps.setLong(1, userId);
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 2, ids.get(i));
            ps.executeUpdate();
        }
    }
}
