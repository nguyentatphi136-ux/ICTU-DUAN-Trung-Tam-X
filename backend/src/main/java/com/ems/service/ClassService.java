package com.ems.service;

import com.ems.dao.ClassDAO;
import com.ems.dao.RoomDAO;
import com.ems.dao.TrainingProgramDAO;
import com.ems.exception.ApiException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.TrainingProgram;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * S3-05 mở lớp và vòng đời lớp, S3-06 ghi danh, S3-07 danh sách lớp, S3-08 phân công giảng viên và trợ giảng,
 * S3-09 phòng học và cảnh báo sĩ số vượt sức chứa phòng.
 */
public class ClassService {
    public static final Map<String, String> STATUSES = new LinkedHashMap<>();
    public static final Map<String, String> MODES = new LinkedHashMap<>();
    public static final Map<String, String> ROOM_TYPES = new LinkedHashMap<>();
    /** Vòng đời lớp: Chuẩn bị → Đang học → Kết thúc; Chuẩn bị hoặc Đang học → Huỷ. */
    private static final Map<String, Set<String>> NEXT = Map.of(
            "PLANNING", Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("FINISHED", "CANCELLED"));

    static {
        STATUSES.put("PLANNING", "Chuẩn bị");
        STATUSES.put("IN_PROGRESS", "Đang học");
        STATUSES.put("FINISHED", "Kết thúc");
        STATUSES.put("CANCELLED", "Huỷ");
        MODES.put("OFFLINE", "Trực tiếp");
        MODES.put("ONLINE", "Trực tuyến");
        MODES.put("HYBRID", "Kết hợp");
        ROOM_TYPES.put("PHYSICAL", "Vật lý");
        ROOM_TYPES.put("ONLINE", "Trực tuyến");
    }

    public record ClassInput(String code, String name, String programCode, String startDate, Integer maxCapacity, String studyMode, Integer roomId) {}

    public record RoomInput(String code, String name, String type, Integer capacity, String location, String meetingUrl) {}

    private final ClassDAO classDAO = new ClassDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final TrainingProgramDAO programDAO = new TrainingProgramDAO();

    // ===== S3-05: mở lớp, đổi trạng thái =====

    public Map<String, Object> open(ClassInput in) throws SQLException {
        String code = in.code() == null ? "" : in.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("^[A-Z0-9_-]{2,50}$")) throw new ValidationException("Mã lớp gồm 2 đến 50 chữ cái, chữ số, dấu - hoặc _.");
        if (classDAO.codeExists(code)) throw new ConflictException("CLASS_CODE_EXISTS", "Mã lớp " + code + " đã được dùng.");
        String name = in.name() == null || in.name().isBlank() ? code : in.name().trim();
        if (name.length() > 150) throw new ValidationException("Tên lớp tối đa 150 ký tự.");
        TrainingProgram program = in.programCode() == null || in.programCode().isBlank() ? null : programDAO.findByCode(in.programCode().trim());
        if (program == null || !"ACTIVE".equals(program.getStatus())) {
            throw new ApiException(400, "PROGRAM_NOT_FOUND", "Chọn một chương trình đào tạo đang áp dụng.");
        }
        LocalDate start = date(in.startDate(), "Ngày khai giảng");
        if (start == null) throw new ValidationException("Nhập ngày khai giảng dự kiến (yyyy-MM-dd).");
        if (start.isBefore(LocalDate.now())) throw new ValidationException("Ngày khai giảng dự kiến phải từ hôm nay trở đi.");
        if (in.maxCapacity() == null || in.maxCapacity() < 1 || in.maxCapacity() > 500) throw new ValidationException("Sĩ số tối đa từ 1 đến 500.");
        String mode = code(in.studyMode(), MODES, "Hình thức học");
        if (in.roomId() != null) room(in.roomId());
        classDAO.create(code, name, program.getId(), start, in.maxCapacity(), mode, in.roomId());
        return get(code);
    }

    public Map<String, Object> changeStatus(String code, String status) throws SQLException {
        Map<String, Object> k = find(code);
        String from = (String) k.get("status");
        String to = code(status, STATUSES, "Trạng thái");
        if (!NEXT.getOrDefault(from, Set.of()).contains(to) || !classDAO.changeStatus((long) k.get("id"), from, to)) {
            throw new ConflictException("CLASS_STATUS_INVALID", "Không chuyển được lớp từ " + STATUSES.get(from) + " sang " + STATUSES.get(to) + ".");
        }
        return get(code);
    }

    // ===== S3-07: danh sách, chi tiết =====

    public Map<String, Object> search(String keyword, String status, String programCode, String instructorId, String from, String to,
                                      int page, int size) throws SQLException {
        Long instructor = null;
        if (instructorId != null && !instructorId.isBlank()) {
            if (!instructorId.trim().matches("\\d+")) throw new ValidationException("Tham số instructorId phải là số.");
            instructor = Long.parseLong(instructorId.trim());
        }
        ClassDAO.Filter filter = new ClassDAO.Filter(keyword, status == null || status.isBlank() ? null : code(status, STATUSES, "Trạng thái"),
                programCode == null || programCode.isBlank() ? null : programCode.trim(), instructor, date(from, "from"), date(to, "to"));
        Map<String, Object> result = classDAO.search(filter, Math.max(page, 1), Math.min(Math.max(size, 1), 200));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        items.forEach(ClassService::label);
        return result;
    }

    /** Chi tiết lớp kèm danh sách môn và người phụ trách từng môn. */
    public Map<String, Object> get(String code) throws SQLException {
        Map<String, Object> k = find(code);
        k.put("subjects", classDAO.subjects((long) k.get("id")));
        return k;
    }

    // ===== S3-06: ghi danh =====

    public Map<String, Object> enroll(String code, List<String> studentCodes, long actorId) throws SQLException {
        Map<String, Object> k = find(code);
        if (studentCodes == null || studentCodes.isEmpty()) throw new ValidationException("Chọn ít nhất một học viên (studentCodes).");
        List<String> codes = studentCodes.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).filter(s -> !s.isEmpty()).distinct().toList();
        if (codes.isEmpty()) throw new ValidationException("Chọn ít nhất một học viên (studentCodes).");
        if (codes.size() > 200) throw new ValidationException("Mỗi lần ghi danh tối đa 200 học viên.");
        int added = classDAO.enroll((long) k.get("id"), codes, actorId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enrolled", added);
        result.put("class", get(code));
        return result;
    }

    public List<Map<String, Object>> enrollments(String code) throws SQLException {
        return classDAO.enrollments((long) find(code).get("id"));
    }

    // ===== S3-08: phân công =====

    /** Danh sách chọn: chỉ tài khoản có vai trò Giảng viên, Trợ giảng. */
    public Map<String, Object> staff() throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("instructors", classDAO.staff("INSTRUCTOR"));
        result.put("assistants", classDAO.staff("TA"));
        return result;
    }

    /** Một giảng viên chính (null là bỏ phân công) và nhiều trợ giảng cho một môn của lớp. */
    public Map<String, Object> assign(String code, String subjectCode, Long instructorId, List<Long> assistantIds) throws SQLException {
        Map<String, Object> k = open(find(code));
        Long classSubjectId = subjectCode == null ? null : classDAO.classSubjectId((long) k.get("id"), subjectCode.trim().toUpperCase(Locale.ROOT));
        if (classSubjectId == null) throw new ResourceNotFoundException("Môn " + subjectCode + " không thuộc lớp " + code + ".");
        if (instructorId != null && !classDAO.hasActiveRole(instructorId, "INSTRUCTOR")) {
            throw new ApiException(400, "INSTRUCTOR_INVALID", "Giảng viên chính phải là tài khoản có vai trò Giảng viên đang hoạt động.");
        }
        List<Long> tas = assistantIds == null ? List.of() : assistantIds.stream().distinct().toList();
        if (tas.size() > 10) throw new ValidationException("Mỗi môn tối đa 10 trợ giảng.");
        for (long ta : tas) {
            if (instructorId != null && ta == instructorId) throw new ValidationException("Giảng viên chính không đồng thời là trợ giảng của môn.");
            if (!classDAO.hasActiveRole(ta, "TA")) {
                throw new ApiException(400, "ASSISTANT_INVALID", "Trợ giảng phải là tài khoản có vai trò Trợ giảng đang hoạt động.");
            }
        }
        classDAO.assign(classSubjectId, instructorId, tas);
        return get(code);
    }

    /** Lớp của giảng viên hoặc trợ giảng đang đăng nhập. */
    public List<Map<String, Object>> mine(long actorId) throws SQLException {
        List<Map<String, Object>> items = classDAO.classesOf(actorId);
        items.forEach(ClassService::label);
        return items;
    }

    // ===== S3-09: phòng học =====

    /** Gắn phòng mặc định cho lớp (roomId null là gỡ phòng). Sĩ số tối đa vượt sức chứa thì vẫn lưu, kèm roomWarning. */
    public Map<String, Object> setRoom(String code, Integer roomId) throws SQLException {
        Map<String, Object> k = open(find(code));
        if (roomId != null) room(roomId);
        classDAO.setRoom((long) k.get("id"), roomId);
        return get(code);
    }

    public List<Map<String, Object>> rooms(String type) throws SQLException {
        return roomDAO.list(type == null || type.isBlank() ? null : code(type, ROOM_TYPES, "Loại phòng"));
    }

    public Map<String, Object> room(int id) throws SQLException {
        Map<String, Object> room = roomDAO.find(id);
        if (room == null) throw new ResourceNotFoundException("Không tìm thấy phòng học " + id + ".");
        return room;
    }

    public Map<String, Object> createRoom(RoomInput in) throws SQLException {
        RoomDAO.Room r = clean(in);
        if (roomDAO.codeTaken(r.code(), -1)) throw new ConflictException("ROOM_CODE_EXISTS", "Mã phòng " + r.code() + " đã được dùng.");
        return room(roomDAO.create(r));
    }

    public Map<String, Object> updateRoom(int id, RoomInput in) throws SQLException {
        room(id);
        RoomDAO.Room r = clean(in);
        if (roomDAO.codeTaken(r.code(), id)) throw new ConflictException("ROOM_CODE_EXISTS", "Mã phòng " + r.code() + " đã được dùng.");
        roomDAO.update(id, r);
        return room(id);
    }

    public void deleteRoom(int id) throws SQLException {
        if ((int) room(id).get("openClasses") > 0) {
            throw new ConflictException("ROOM_IN_USE", "Phòng đang được xếp cho lớp chưa kết thúc, hãy đổi phòng cho lớp trước.");
        }
        roomDAO.delete(id);
    }

    /** Kiểm tra trước khi xếp lớp có students học viên vào phòng. */
    public Map<String, Object> checkCapacity(int id, int students) throws SQLException {
        Map<String, Object> room = room(id);
        Integer capacity = (Integer) room.get("capacity");
        boolean ok = capacity == null || students <= capacity;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", id);
        result.put("capacity", capacity);
        result.put("students", students);
        result.put("ok", ok);
        if (!ok) result.put("message", students + " học viên vượt sức chứa phòng " + room.get("code") + " (" + capacity + " chỗ).");
        return result;
    }

    // ===== Dùng chung =====

    private Map<String, Object> find(String code) throws SQLException {
        Map<String, Object> k = code == null ? null : classDAO.findByCode(code.trim().toUpperCase(Locale.ROOT));
        if (k == null) throw new ResourceNotFoundException("Không tìm thấy lớp " + code + ".");
        return label(k);
    }

    /** Lớp đã kết thúc hoặc huỷ thì không sửa phân công, phòng. */
    private static Map<String, Object> open(Map<String, Object> k) {
        if (!NEXT.containsKey((String) k.get("status"))) {
            throw new ConflictException("CLASS_NOT_OPEN", "Lớp đã kết thúc hoặc đã huỷ, không thay đổi được nữa.");
        }
        return k;
    }

    /** Nhãn trạng thái, hình thức học và cảnh báo khi sĩ số tối đa vượt sức chứa phòng mặc định. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> label(Map<String, Object> k) {
        k.put("statusLabel", STATUSES.getOrDefault((String) k.get("status"), (String) k.get("status")));
        k.put("studyModeLabel", MODES.getOrDefault((String) k.get("studyMode"), (String) k.get("studyMode")));
        Map<String, Object> room = (Map<String, Object>) k.get("room");
        Integer capacity = room == null ? null : (Integer) room.get("capacity");
        int max = (int) k.get("maxCapacity");
        if (capacity != null && max > capacity) {
            k.put("roomWarning", "Sĩ số tối đa " + max + " vượt sức chứa phòng " + room.get("code") + " (" + capacity + " chỗ).");
        }
        return k;
    }

    private static RoomDAO.Room clean(RoomInput in) {
        String code = in.code() == null ? "" : in.code().trim();
        if (code.isEmpty() || code.length() > 50) throw new ValidationException("Mã phòng bắt buộc, tối đa 50 ký tự.");
        String name = in.name() == null || in.name().isBlank() ? code : in.name().trim();
        if (name.length() > 100) throw new ValidationException("Tên phòng tối đa 100 ký tự.");
        String type = code(in.type(), ROOM_TYPES, "Loại phòng");
        String location = in.location() == null || in.location().isBlank() ? null : in.location().trim();
        String url = in.meetingUrl() == null || in.meetingUrl().isBlank() ? null : in.meetingUrl().trim();
        if (location != null && location.length() > 255) throw new ValidationException("Vị trí tối đa 255 ký tự.");
        if ("PHYSICAL".equals(type)) {
            if (in.capacity() == null || in.capacity() < 1 || in.capacity() > 1000) {
                throw new ValidationException("Phòng vật lý phải có sức chứa từ 1 đến 1000 chỗ.");
            }
            url = null;
        } else {
            if (url == null || url.length() > 255 || !url.matches("^(https?://)?[\\w-]+(\\.[\\w-]+)+(/\\S*)?$")) {
                throw new ValidationException("Phòng trực tuyến phải có liên kết hợp lệ, ví dụ https://meet.google.com/abc-defg-hij.");
            }
            if (in.capacity() != null && in.capacity() < 1) throw new ValidationException("Sức chứa phải lớn hơn 0, bỏ trống là không giới hạn.");
            location = null;
        }
        return new RoomDAO.Room(code, name, type, in.capacity(), location, url);
    }

    private static String code(String value, Map<String, String> allowed, String field) {
        String c = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.containsKey(c)) throw new ValidationException(field + " không hợp lệ. Giá trị cho phép: " + allowed.keySet() + ".");
        return c;
    }

    private static LocalDate date(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(field + " phải có dạng yyyy-MM-dd.");
        }
    }

    /** Danh sách id từ JSON, dùng cho assistantIds. */
    public static List<Long> ids(List<String> raw) {
        List<Long> out = new ArrayList<>();
        if (raw != null) for (String s : raw) out.add(Long.parseLong(s));
        return out;
    }
}
