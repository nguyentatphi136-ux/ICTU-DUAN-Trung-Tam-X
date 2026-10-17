package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.exception.ApiException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hồ sơ học viên: bảng students gắn với một tài khoản users (họ tên, số điện thoại, ngày sinh, địa chỉ nằm ở users).
 * S3-03 tạo học viên từ lead, S3-04 tìm kiếm, xem và sửa hồ sơ, xem các lớp đã và đang học.
 */
public class StudentDAO {
    public record Filter(String keyword, String status, String programCode) {}

    /** Lớp đang chạy mà học viên đang ghi danh, lấy lớp khai giảng gần nhất. */
    private static final String CURRENT_CLASS = "(SELECT c.class_code FROM enrollments e JOIN classes c ON c.id = e.class_id "
            + "WHERE e.student_id = s.id AND e.status = 'ENROLLED' AND c.status IN ('PLANNING', 'IN_PROGRESS') "
            + "ORDER BY c.start_date DESC LIMIT 1) AS current_class";

    private static final String SELECT = "SELECT s.id, s.student_code, s.enrollment_status, s.enrolled_at, s.contact_email, "
            + "s.entry_level, s.notes, s.lead_id, s.user_id, u.full_name, u.phone, u.email AS login, u.date_of_birth, u.address, "
            + "p.program_code, p.program_name, " + CURRENT_CLASS + " "
            + "FROM students s JOIN users u ON u.id = s.user_id LEFT JOIN programs p ON p.id = s.program_id ";

    // ===== S3-03: chuyển lead thành học viên =====

    /**
     * Trong một giao dịch: tạo tài khoản đăng nhập vai trò STUDENT (mật khẩu mặc định, phải đổi ở lần đăng nhập đầu),
     * tạo hồ sơ học viên với mã tự sinh, khoá lead (converted_student_id) và ghi nhật ký.
     * Trả về { studentId, code, login, name, email }.
     */
    public Map<String, Object> createFromLead(long leadId, String contactEmail, long actorId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String name;
                String phone;
                Object programId;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT full_name, phone, status, converted_student_id, interested_program_id FROM leads "
                                + "WHERE id = ? AND deleted_at IS NULL FOR UPDATE")) {
                    stmt.setLong(1, leadId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new ResourceNotFoundException("Không tìm thấy lead " + leadId + ".");
                        if (rs.getObject("converted_student_id") != null) {
                            throw new ConflictException("LEAD_ALREADY_CONVERTED", "Lead này đã được chuyển thành học viên.");
                        }
                        if (!"WON".equals(rs.getString("status"))) {
                            throw new ConflictException("LEAD_NOT_WON", "Chỉ chuyển được lead ở giai đoạn Chốt.");
                        }
                        name = rs.getString("full_name");
                        phone = rs.getString("phone");
                        programId = rs.getObject("interested_program_id");
                    }
                }
                String phoneOwner = UserDAO.findPhoneOwner(conn, phone, -1);
                if (phoneOwner != null) {
                    throw new ConflictException("PHONE_DUPLICATE", "Số điện thoại " + phone + " đã thuộc tài khoản của " + phoneOwner + ".");
                }

                String code = nextCode(conn);
                String login = code + "@tms.vn";
                long userId;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users (user_code, email, password_hash, full_name, display_name, phone, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, code);
                    stmt.setString(2, login);
                    stmt.setString(3, BCrypt.hashpw(UserDAO.DEFAULT_PASSWORD, BCrypt.gensalt(10)));
                    stmt.setString(4, UserDAO.normalizeName(name));
                    stmt.setString(5, name == null ? null : name.trim());
                    stmt.setString(6, phone);
                    stmt.executeUpdate();
                    userId = generatedId(stmt);
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE role_code = 'STUDENT'")) {
                    stmt.setLong(1, userId);
                    if (stmt.executeUpdate() != 1) {
                        throw new ApiException(500, "ROLE_MISSING", "CSDL chưa có vai trò STUDENT, không tạo được tài khoản học viên.");
                    }
                }
                long studentId;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO students (user_id, student_code, enrollment_status, program_id, lead_id, contact_email, enrolled_at) "
                                + "VALUES (?, ?, 'STUDYING', ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setLong(1, userId);
                    stmt.setString(2, code);
                    stmt.setObject(3, programId);
                    stmt.setLong(4, leadId);
                    stmt.setString(5, contactEmail);
                    stmt.setDate(6, Date.valueOf(LocalDate.now()));
                    stmt.executeUpdate();
                    studentId = generatedId(stmt);
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE leads SET converted_student_id = ?, email = COALESCE(email, ?), updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    stmt.setLong(1, studentId);
                    stmt.setString(2, contactEmail);
                    stmt.setLong(3, leadId);
                    stmt.executeUpdate();
                }
                LeadDAO.insertLog(conn, leadId, actorId, "CONVERT", "Chuyển thành học viên " + code + ", tài khoản " + login, null, null);
                conn.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("studentId", studentId);
                result.put("code", code);
                result.put("login", login);
                result.put("name", UserDAO.normalizeName(name));
                result.put("email", contactEmail);
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Mã học viên HV + 2 số cuối năm + số thứ tự 4 chữ số trong năm, ví dụ HV260143.
     * ponytail: hai lần chuyển cùng lúc có thể lấy trùng số; ràng buộc UNIQUE chặn lại và người dùng bấm lại là được.
     */
    static String nextCode(Connection conn) throws SQLException {
        String prefix = String.format("HV%02d", LocalDate.now().getYear() % 100);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT MAX(student_code) FROM students WHERE student_code LIKE ?")) {
            stmt.setString(1, prefix + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                String max = rs.next() ? rs.getString(1) : null;
                int seq = max == null ? 0 : Integer.parseInt(max.substring(prefix.length()).replaceAll("\\D", "0"));
                return prefix + String.format("%04d", seq + 1);
            }
        }
    }

    // ===== S3-04: hồ sơ học viên =====

    /** Tìm theo mã học viên, họ tên hoặc số điện thoại. */
    public Map<String, Object> search(Filter f, int page, int size) throws SQLException {
        StringBuilder where = new StringBuilder("WHERE u.deleted_at IS NULL");
        List<Object> params = new ArrayList<>();
        if (f.keyword() != null && !f.keyword().isBlank()) {
            String kw = f.keyword().trim().toLowerCase(Locale.ROOT);
            String digits = kw.replaceAll("\\D", "");
            where.append(" AND (LOWER(s.student_code) LIKE ? OR LOWER(u.full_name) LIKE ?");
            params.add("%" + kw + "%");
            params.add("%" + kw + "%");
            if (digits.length() >= 3) {
                where.append(" OR u.phone LIKE ?");
                params.add("%" + digits + "%");
            }
            where.append(")");
        }
        if (f.status() != null) {
            where.append(" AND s.enrollment_status = ?");
            params.add(f.status());
        }
        if (f.programCode() != null) {
            where.append(" AND p.program_code = ?");
            params.add(f.programCode());
        }
        try (Connection conn = DBConnection.getConnection()) {
            long total;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM students s JOIN users u ON u.id = s.user_id "
                    + "LEFT JOIN programs p ON p.id = s.program_id " + where)) {
                SubjectDAO.bind(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            List<Object> all = new ArrayList<>(params);
            all.add(size);
            all.add((page - 1) * size);
            List<Map<String, Object>> items = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(SELECT + where + " ORDER BY s.student_code DESC LIMIT ? OFFSET ?")) {
                SubjectDAO.bind(stmt, all);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) items.add(mapRow(rs));
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", items);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", size);
            return result;
        }
    }

    /** Hồ sơ theo mã học viên, kèm tư vấn viên và nguồn của lead gốc. null nếu không có. */
    public Map<String, Object> findByCode(String code) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT + "WHERE s.student_code = ? AND u.deleted_at IS NULL")) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> item = mapRow(rs);
                Date birth = rs.getDate("date_of_birth");
                item.put("birth", birth == null ? null : birth.toLocalDate().toString());
                item.put("address", rs.getString("address"));
                item.put("level", rs.getString("entry_level"));
                item.put("notes", rs.getString("notes"));
                item.put("userId", rs.getLong("user_id"));
                long leadId = rs.getLong("lead_id");
                if (!rs.wasNull()) {
                    item.put("leadId", leadId);
                    try (PreparedStatement lead = conn.prepareStatement(
                            "SELECT l.source, u.full_name FROM leads l LEFT JOIN users u ON u.id = l.assigned_counselor_id WHERE l.id = ?")) {
                        lead.setLong(1, leadId);
                        try (ResultSet lr = lead.executeQuery()) {
                            if (lr.next()) {
                                item.put("source", lr.getString("source"));
                                item.put("counselor", lr.getString("full_name"));
                            }
                        }
                    }
                }
                return item;
            }
        }
    }

    /** Mọi lần ghi danh của học viên, lớp mới nhất trước: lớp đang học và lịch sử các lớp đã học. */
    public List<Map<String, Object>> classes(long studentId) throws SQLException {
        String sql = "SELECT c.class_code, c.class_name, c.start_date, c.end_date, c.status AS class_status, p.program_name, "
                + "e.status, e.enrollment_date FROM enrollments e JOIN classes c ON c.id = e.class_id "
                + "LEFT JOIN programs p ON p.id = c.program_id WHERE e.student_id = ? ORDER BY c.start_date DESC, e.id DESC";
        List<Map<String, Object>> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("classCode", rs.getString("class_code"));
                    item.put("className", rs.getString("class_name"));
                    item.put("programName", rs.getString("program_name"));
                    item.put("startDate", rs.getDate("start_date").toLocalDate().toString());
                    Date end = rs.getDate("end_date");
                    item.put("endDate", end == null ? null : end.toLocalDate().toString());
                    item.put("classStatus", rs.getString("class_status"));
                    item.put("status", rs.getString("status"));
                    item.put("enrolledAt", rs.getDate("enrollment_date").toLocalDate().toString());
                    items.add(item);
                }
            }
        }
        return items;
    }

    /** Sửa thông tin cá nhân (bảng users) và hồ sơ học tập (bảng students) trong một giao dịch. */
    public void update(long studentId, long userId, String name, String phone, LocalDate birth, String address,
                       String contactEmail, String level, String notes) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String owner = UserDAO.findPhoneOwner(conn, phone, userId);
                if (owner != null) {
                    throw new ConflictException("PHONE_DUPLICATE", "Số điện thoại " + phone + " đã thuộc tài khoản của " + owner + ".");
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE users SET full_name = ?, phone = ?, date_of_birth = ?, address = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    stmt.setString(1, UserDAO.normalizeName(name));
                    stmt.setString(2, phone);
                    stmt.setDate(3, birth == null ? null : Date.valueOf(birth));
                    stmt.setString(4, address);
                    stmt.setLong(5, userId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE students SET contact_email = ?, entry_level = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    stmt.setString(1, contactEmail);
                    stmt.setString(2, level);
                    stmt.setString(3, notes);
                    stmt.setLong(4, studentId);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static long generatedId(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("code", rs.getString("student_code"));
        item.put("name", rs.getString("full_name"));
        item.put("phone", rs.getString("phone"));
        item.put("email", rs.getString("contact_email"));
        item.put("login", rs.getString("login"));
        item.put("programCode", rs.getString("program_code"));
        item.put("programName", rs.getString("program_name"));
        item.put("status", rs.getString("enrollment_status"));
        Date enrolled = rs.getDate("enrolled_at");
        item.put("enrolledAt", enrolled == null ? null : enrolled.toLocalDate().toString());
        item.put("currentClass", rs.getString("current_class"));
        return item;
    }
}
