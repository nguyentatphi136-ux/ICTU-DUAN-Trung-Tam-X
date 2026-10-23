package com.ems.service;

import com.ems.dao.StudentDAO;
import com.ems.dao.UserDAO;
import com.ems.exception.ApiException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * S3-04 hồ sơ học viên: tìm theo mã, tên, số điện thoại; xem lớp đang học và lịch sử lớp; sửa thông tin cá nhân,
 * trình độ đầu vào, ghi chú. Học viên được tạo từ lead ở S3-03 (LeadService.convert).
 */
public class StudentService {
    public static final Map<String, String> STATUSES = new LinkedHashMap<>();

    static {
        STATUSES.put("STUDYING", "Đang học");
        STATUSES.put("SUSPENDED", "Bảo lưu");
        STATUSES.put("GRADUATED", "Đã tốt nghiệp");
        STATUSES.put("DROPPED_OUT", "Thôi học");
    }

    public record StudentInput(String name, String phone, String email, String birth, String address, String level, String notes) {}

    private final StudentDAO dao = new StudentDAO();

    public Map<String, Object> search(String keyword, String status, String programCode, int page, int size) throws SQLException {
        String s = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (s != null && !STATUSES.containsKey(s)) throw new ValidationException("Trạng thái không hợp lệ. Giá trị cho phép: " + STATUSES.keySet() + ".");
        String program = programCode == null || programCode.isBlank() ? null : programCode.trim();
        Map<String, Object> result = dao.search(new StudentDAO.Filter(keyword, s, program), Math.max(page, 1), Math.min(Math.max(size, 1), 200));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        items.forEach(StudentService::label);
        return result;
    }

    /** Hồ sơ kèm currentClasses (lớp chưa kết thúc đang ghi danh) và history (các lớp còn lại). */
    public Map<String, Object> get(String code) throws SQLException {
        Map<String, Object> student = find(code);
        List<Map<String, Object>> current = new ArrayList<>();
        List<Map<String, Object>> history = new ArrayList<>();
        for (Map<String, Object> c : dao.classes((long) student.get("id"))) {
            boolean running = "ENROLLED".equals(c.get("status"))
                    && ("PLANNING".equals(c.get("classStatus")) || "IN_PROGRESS".equals(c.get("classStatus")));
            (running ? current : history).add(c);
        }
        student.put("currentClasses", current);
        student.put("history", history);
        return student;
    }

    public Map<String, Object> update(String code, StudentInput in) throws SQLException {
        Map<String, Object> student = find(code);
        if (in.name() == null || in.name().isBlank()) throw new ValidationException("Nhập họ và tên.");
        if (in.name().trim().length() > 100) throw new ValidationException("Họ và tên tối đa 100 ký tự.");
        String phone = UserDAO.normalizePhone(in.phone());
        if (!UserDAO.VN_PHONE_PATTERN.matcher(phone).matches()) {
            throw new ApiException(400, "INVALID_PHONE_FORMAT", "Số điện thoại di động gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        }
        if (phone.startsWith("+84")) phone = "0" + phone.substring(3);
        String email = blank(in.email());
        if (email != null && (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new ApiException(400, "INVALID_EMAIL_FORMAT", "Email không đúng định dạng.");
        }
        LocalDate birth = null;
        if (blank(in.birth()) != null) {
            try {
                birth = LocalDate.parse(in.birth().trim());
            } catch (DateTimeParseException e) {
                throw new ValidationException("Ngày sinh phải có dạng yyyy-MM-dd.");
            }
            if (!birth.isBefore(LocalDate.now())) throw new ValidationException("Ngày sinh phải trước hôm nay.");
        }
        String address = max(blank(in.address()), 255, "Địa chỉ");
        String level = max(blank(in.level()), 100, "Trình độ đầu vào");
        String notes = max(blank(in.notes()), 2000, "Ghi chú");
        dao.update((long) student.get("id"), (long) student.get("userId"), in.name().trim(), phone, birth, address, email, level, notes);
        return get(code);
    }

    private Map<String, Object> find(String code) throws SQLException {
        Map<String, Object> student = code == null ? null : dao.findByCode(code.trim().toUpperCase(Locale.ROOT));
        if (student == null) throw new ResourceNotFoundException("Không tìm thấy học viên " + code + ".");
        return label(student);
    }

    private static Map<String, Object> label(Map<String, Object> s) {
        s.put("statusLabel", STATUSES.getOrDefault((String) s.get("status"), (String) s.get("status")));
        return s;
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String max(String v, int length, String field) {
        if (v != null && v.length() > length) throw new ValidationException(field + " tối đa " + length + " ký tự.");
        return v;
    }
}
