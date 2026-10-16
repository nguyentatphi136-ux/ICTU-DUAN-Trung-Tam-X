package com.ems.service;

import com.ems.dao.LeadDAO;
import com.ems.dao.StudentDAO;
import com.ems.dao.TrainingProgramDAO;
import com.ems.dao.UserDAO;
import com.ems.exception.ApiException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.TrainingProgram;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * S2-08 đăng ký tư vấn công khai, S2-09 danh sách lead, S2-10 phân công, S2-11 tìm kiếm và lọc.
 * Người có quyền LEAD_ASSIGN (Quản trị, Quản lý đào tạo) thấy mọi lead; tư vấn viên chỉ thấy và sửa lead được giao cho mình,
 * lead tư vấn viên tự thêm được giao luôn cho người đó.
 */
public class LeadService {
    /**
     * Mã trạng thái lưu trong CSDL (ENUM của bảng leads) và nhãn hiển thị, theo đúng thứ tự cột phễu S3-01:
     * Mới → Đã liên hệ → Đang tư vấn → Hẹn học thử → Chốt → Từ chối. Trang Lead (S2) và Bảng phễu dùng chung nhãn này.
     */
    public static final Map<String, String> STATUSES = new LinkedHashMap<>();
    /** Mã nguồn lead và nhãn hiển thị. */
    public static final Map<String, String> SOURCES = new LinkedHashMap<>();

    static {
        STATUSES.put("NEW", "Mới");
        STATUSES.put("CONTACTED", "Đã liên hệ");
        STATUSES.put("CONSULTING", "Đang tư vấn");
        STATUSES.put("TRIAL_SCHEDULED", "Hẹn học thử");
        STATUSES.put("WON", "Chốt");
        STATUSES.put("REJECTED", "Từ chối");
        SOURCES.put("WEBSITE", "Biểu mẫu web");
        SOURCES.put("FACEBOOK", "Facebook");
        SOURCES.put("ZALO", "Zalo");
        SOURCES.put("REFERRAL", "Giới thiệu");
        SOURCES.put("HOTLINE", "Hotline");
        SOURCES.put("EVENT", "Sự kiện");
        SOURCES.put("TIKTOK", "TikTok");
    }

    public record LeadInput(String name, String phone, String email, String source, String programCode, String status, String note) {}

    private static final int MAX_ASSIGN = 500;

    private final LeadDAO leadDAO;
    private final TrainingProgramDAO programDAO;
    private final StudentDAO studentDAO = new StudentDAO();

    public LeadService() {
        this(new LeadDAO(), new TrainingProgramDAO());
    }

    public LeadService(LeadDAO leadDAO, TrainingProgramDAO programDAO) {
        this.leadDAO = leadDAO;
        this.programDAO = programDAO;
    }

    /**
     * Danh sách có bộ lọc S2-11. owner: rỗng là mọi người, "none" là chưa phân công, hoặc id tư vấn viên.
     * from, to dạng yyyy-MM-dd, tính cả hai ngày.
     */
    public Map<String, Object> search(String keyword, String status, String source, String owner, String from, String to,
                                      int page, int size, long actorId, boolean seeAll) throws SQLException {
        Long ownerFilter = null;
        if (owner != null && !owner.isBlank()) {
            if ("none".equalsIgnoreCase(owner.trim())) ownerFilter = 0L;
            else if (owner.trim().matches("\\d+")) ownerFilter = Long.parseLong(owner.trim());
            else throw new ValidationException("Tham số owner là id tư vấn viên hoặc none.");
        }
        LeadDAO.Filter filter = new LeadDAO.Filter(keyword, optionalCode(status, STATUSES, "Trạng thái"),
                optionalCode(source, SOURCES, "Nguồn"), ownerFilter, date(from, "from"), date(to, "to"));
        Map<String, Object> result = leadDAO.search(filter, seeAll ? null : actorId,
                Math.max(page, 1), Math.min(Math.max(size, 1), 200));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        items.forEach(LeadService::label);
        return result;
    }

    /** Chi tiết lead kèm lịch sử phân công. Lead không thuộc tư vấn viên thì báo không tìm thấy. */
    public Map<String, Object> get(long id, long actorId, boolean seeAll) throws SQLException {
        Map<String, Object> lead = visible(id, actorId, seeAll);
        lead.put("assignments", leadDAO.assignments(id));
        return lead;
    }

    public Map<String, Object> create(LeadInput in, long actorId, boolean seeAll) throws SQLException {
        Clean c = clean(in, false);
        long id = leadDAO.create(c.name, c.phone, c.email, c.source, c.programId, seeAll ? null : actorId, c.status, c.note);
        return label(leadDAO.findById(id));
    }

    public Map<String, Object> update(long id, LeadInput in, long actorId, boolean seeAll) throws SQLException {
        Map<String, Object> lead = editable(id, actorId, seeAll);
        Clean c = clean(in, false);
        // S2-09 chỉ sửa thông tin lead; trạng thái giữ nguyên, đổi qua changeStage (S3-01: ghi nhật ký, Từ chối cần lý do).
        leadDAO.update(id, c.name, c.phone, c.email, c.source, c.programId, (String) lead.get("status"), c.note);
        return label(leadDAO.findById(id));
    }

    /** Lead khác cùng số điện thoại, để giao diện cảnh báo trước khi tạo trùng. */
    public List<Map<String, Object>> samePhone(String phone, Long excludeId) throws SQLException {
        List<Map<String, Object>> items = leadDAO.findByPhone(phone(phone), excludeId);
        items.forEach(LeadService::label);
        return items;
    }

    public List<Map<String, Object>> counselors() throws SQLException {
        return leadDAO.counselors();
    }

    /** S2-10: giao một hoặc nhiều lead cho tư vấn viên, ghi lịch sử chuyển giao. */
    public Map<String, Object> assign(List<Long> leadIds, Long counselorId, String note, long actorId) throws SQLException {
        if (leadIds == null || leadIds.isEmpty()) throw new ValidationException("Chọn ít nhất một lead (leadIds).");
        if (leadIds.size() > MAX_ASSIGN) throw new ValidationException("Mỗi lần phân công tối đa " + MAX_ASSIGN + " lead.");
        if (counselorId == null || !leadDAO.isActiveCounselor(counselorId)) {
            throw new ApiException(400, "COUNSELOR_INVALID", "Người được giao phải là tư vấn tuyển sinh đang hoạt động.");
        }
        if (note != null && note.length() > 255) throw new ValidationException("Ghi chú chuyển giao tối đa 255 ký tự.");
        try {
            int changed = leadDAO.assign(leadIds.stream().distinct().toList(), counselorId, actorId,
                    note == null || note.isBlank() ? null : note.trim());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("assigned", changed);
            return result;
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    public void trash(long id, long actorId) throws SQLException {
        if (!leadDAO.softDelete(id, actorId)) throw new ResourceNotFoundException("Không tìm thấy lead " + id + ".");
    }

    public List<Map<String, Object>> listTrash() throws SQLException {
        return leadDAO.listTrash();
    }

    public void restore(long id) throws SQLException {
        if (!leadDAO.restore(id)) throw new ResourceNotFoundException("Lead không có trong thùng rác.");
    }

    public void purge(long id) throws SQLException {
        if (!leadDAO.purge(id)) throw new ResourceNotFoundException("Lead không có trong thùng rác.");
    }

    // ===== S2-08: biểu mẫu công khai =====

    /** Chương trình đang áp dụng, cho ô "Bạn quan tâm khoá học nào?". */
    public List<Map<String, Object>> publicPrograms() throws SQLException {
        @SuppressWarnings("unchecked")
        List<TrainingProgram> programs = (List<TrainingProgram>) programDAO.search(null, "ACTIVE", 1, 500).get("items");
        List<Map<String, Object>> items = new ArrayList<>();
        for (TrainingProgram p : programs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", p.getCode());
            item.put("name", p.getName());
            items.add(item);
        }
        return items;
    }

    /** Tạo lead Mới, nguồn Biểu mẫu web, chưa phân công. Bắt buộc chọn chương trình đang áp dụng. */
    public void submitConsultation(String name, String phone, String email, String programCode, String message) throws SQLException {
        Clean c = clean(new LeadInput(name, phone, email, "WEBSITE", programCode, "NEW", message), true);
        leadDAO.create(c.name, c.phone, c.email, c.source, c.programId, null, c.status, c.note);
    }

    // ===== S3-01: phễu lead =====

    /** Loại dòng nhật ký S3-02. Ba loại đầu do tư vấn viên ghi, hai loại sau hệ thống tự ghi. */
    public static final Map<String, String> LOG_TYPES = new LinkedHashMap<>();

    static {
        LOG_TYPES.put("CALL", "Gọi");
        LOG_TYPES.put("MESSAGE", "Nhắn");
        LOG_TYPES.put("MEETING", "Gặp");
        LOG_TYPES.put("STAGE", "Đổi trạng thái");
        LOG_TYPES.put("CONVERT", "Chuyển thành học viên");
    }

    /** Sáu cột phễu, mỗi cột có số lượng và danh sách lead; overdue là số lead quá hạn gọi lại. */
    public Map<String, Object> funnel(String keyword, long actorId, boolean seeAll) throws SQLException {
        LocalDate today = LocalDate.now();
        Map<String, List<Map<String, Object>>> byStage = new LinkedHashMap<>();
        STATUSES.keySet().forEach(s -> byStage.put(s, new ArrayList<>()));
        int overdue = 0;
        for (Map<String, Object> lead : leadDAO.funnel(keyword, seeAll ? null : actorId)) {
            int days = overdueDays(lead, today);
            if (days > 0) {
                lead.put("overdueDays", days);
                overdue++;
            }
            byStage.computeIfAbsent((String) lead.get("status"), k -> new ArrayList<>()).add(label(lead));
        }
        List<Map<String, Object>> columns = new ArrayList<>();
        byStage.forEach((status, items) -> {
            // Lead quá hạn lâu nhất lên đầu cột để biết hôm nay cần gọi ai trước.
            items.sort((a, b) -> Integer.compare((int) b.getOrDefault("overdueDays", 0), (int) a.getOrDefault("overdueDays", 0)));
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("status", status);
            col.put("label", STATUSES.getOrDefault(status, status));
            col.put("count", items.size());
            col.put("items", items);
            columns.add(col);
        });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today", today.toString());
        result.put("overdue", overdue);
        result.put("columns", columns);
        return result;
    }

    /** Kéo thả sang cột khác. Sang Từ chối bắt buộc có lý do; rời Từ chối thì xoá lý do. Mỗi lần đổi ghi một dòng nhật ký. */
    public Map<String, Object> changeStage(long id, String status, String reason, long actorId, boolean seeAll) throws SQLException {
        Map<String, Object> lead = editable(id, actorId, seeAll);
        if (status == null || status.isBlank()) throw new ValidationException("Chọn giai đoạn mới (status).");
        String to = code(status, STATUSES, "Giai đoạn");
        String why = reason == null || reason.isBlank() ? null : reason.trim();
        if ("REJECTED".equals(to) && why == null) {
            throw new ApiException(400, "REJECT_REASON_REQUIRED", "Chuyển sang Từ chối phải chọn lý do.");
        }
        if (why != null && why.length() > 255) throw new ValidationException("Lý do tối đa 255 ký tự.");
        String from = (String) lead.get("status");
        if (to.equals(from) && !"REJECTED".equals(to)) return lead;
        String text = STATUSES.getOrDefault(from, from) + " → " + STATUSES.get(to) + ("REJECTED".equals(to) ? ": " + why : "");
        leadDAO.changeStage(id, to, "REJECTED".equals(to) ? why : null, actorId, text);
        return label(leadDAO.findById(id));
    }

    // ===== S3-02: nhật ký chăm sóc =====

    public List<Map<String, Object>> logs(long id, long actorId, boolean seeAll) throws SQLException {
        visible(id, actorId, seeAll);
        List<Map<String, Object>> items = leadDAO.logs(id);
        items.forEach(i -> i.put("typeLabel", LOG_TYPES.getOrDefault((String) i.get("type"), (String) i.get("type"))));
        return items;
    }

    /**
     * Ghi một lần tương tác: type CALL, MESSAGE hoặc MEETING, nội dung, thời điểm (bỏ trống là bây giờ),
     * callbackDate (yyyy-MM-dd) để đặt ngày nhắc gọi lại. Trả về lead sau khi ghi.
     */
    public Map<String, Object> addLog(long id, String type, String content, String at, String callbackDate,
                                      long actorId, boolean seeAll) throws SQLException {
        visible(id, actorId, seeAll);
        if (type == null || type.isBlank()) throw new ValidationException("Chọn loại tương tác (type).");
        String t = type.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CALL", "MESSAGE", "MEETING").contains(t)) {
            throw new ValidationException("Loại tương tác là CALL (gọi), MESSAGE (nhắn) hoặc MEETING (gặp).");
        }
        if (content == null || content.isBlank()) throw new ValidationException("Nhập nội dung trao đổi.");
        if (content.trim().length() > 2000) throw new ValidationException("Nội dung tối đa 2000 ký tự.");
        LocalDateTime when;
        try {
            when = at == null || at.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(at.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Thời điểm (at) phải có dạng yyyy-MM-ddTHH:mm.");
        }
        if (when.isAfter(LocalDateTime.now().plusMinutes(5))) throw new ValidationException("Thời điểm tương tác không được ở tương lai.");
        LocalDate callback = date(callbackDate, "callbackDate");
        if (callback != null && callback.isBefore(LocalDate.now())) throw new ValidationException("Ngày hẹn gọi lại phải từ hôm nay trở đi.");
        leadDAO.addLog(id, actorId, t, content.trim(), when, callback);
        return label(leadDAO.findById(id));
    }

    // ===== S3-03: chuyển lead thành học viên =====

    /** Tạo hồ sơ học viên và tài khoản từ lead ở cột Chốt. sendTo là email nhận thông tin đăng nhập, bỏ trống thì dùng email của lead. */
    public Map<String, Object> convert(long id, String sendTo, long actorId, boolean seeAll) throws SQLException {
        Map<String, Object> lead = visible(id, actorId, seeAll);
        String email = sendTo == null || sendTo.isBlank() ? (String) lead.get("email") : sendTo.trim();
        if (email == null || email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ApiException(400, "INVALID_EMAIL_FORMAT", "Nhập email hợp lệ để gửi thông tin đăng nhập cho học viên.");
        }
        return studentDAO.createFromLead(id, email, actorId);
    }

    /** Số ngày quá hạn gọi lại; lead đã chốt, từ chối hoặc đã thành học viên không tính. */
    static int overdueDays(Map<String, Object> lead, LocalDate today) {
        String callback = (String) lead.get("callbackDate");
        String status = (String) lead.get("status");
        if (callback == null || "WON".equals(status) || "REJECTED".equals(status)) return 0;
        return (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(callback), today));
    }

    /** Như visible nhưng lead đã chuyển thành học viên thì chỉ còn xem, không sửa được (S3-03). */
    private Map<String, Object> editable(long id, long actorId, boolean seeAll) throws SQLException {
        Map<String, Object> lead = visible(id, actorId, seeAll);
        if (lead.get("studentId") != null) {
            throw new ConflictException("LEAD_CONVERTED", "Lead đã chuyển thành học viên " + lead.get("studentCode") + " nên không sửa được nữa.");
        }
        return lead;
    }

    // ===== Kiểm tra dữ liệu =====

    private record Clean(String name, String phone, String email, String source, Integer programId, String status, String note) {}

    private Clean clean(LeadInput in, boolean isPublic) throws SQLException {
        if (in.name() == null || in.name().isBlank()) throw new ValidationException("Nhập họ và tên.");
        if (in.name().trim().length() > 100) throw new ValidationException("Họ và tên tối đa 100 ký tự.");
        String email = in.email() == null || in.email().isBlank() ? null : in.email().trim();
        if (email != null && (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new ApiException(400, "INVALID_EMAIL_FORMAT", "Email không đúng định dạng.");
        }
        String note = in.note() == null || in.note().isBlank() ? null : in.note().trim();
        if (note != null && note.length() > 2000) throw new ValidationException("Ghi chú tối đa 2000 ký tự.");
        Integer programId = null;
        if (in.programCode() != null && !in.programCode().isBlank()) {
            TrainingProgram p = programDAO.findByCode(in.programCode());
            if (p == null || isPublic && !"ACTIVE".equals(p.getStatus())) {
                throw new ApiException(400, "PROGRAM_NOT_FOUND", "Chương trình quan tâm không tồn tại hoặc đã ngừng tuyển sinh.");
            }
            programId = p.getId();
        } else if (isPublic) {
            throw new ValidationException("Chọn chương trình bạn quan tâm.");
        }
        String source = in.source() == null || in.source().isBlank() ? "WEBSITE" : code(in.source(), SOURCES, "Nguồn");
        String status = in.status() == null || in.status().isBlank() ? "NEW" : code(in.status(), STATUSES, "Trạng thái");
        return new Clean(in.name().trim(), phone(in.phone()), email, source, programId, status, note);
    }

    /** Số di động Việt Nam, lưu dạng 10 chữ số bắt đầu bằng 0 để tìm và so trùng. */
    private static String phone(String raw) {
        String p = UserDAO.normalizePhone(raw);
        if (!UserDAO.VN_PHONE_PATTERN.matcher(p).matches()) {
            throw new ApiException(400, "INVALID_PHONE_FORMAT",
                    "Số điện thoại di động gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        }
        return p.startsWith("+84") ? "0" + p.substring(3) : p;
    }

    private static String code(String value, Map<String, String> allowed, String field) {
        String c = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.containsKey(c)) throw new ValidationException(field + " không hợp lệ. Giá trị cho phép: " + allowed.keySet() + ".");
        return c;
    }

    private static String optionalCode(String value, Map<String, String> allowed, String field) {
        return value == null || value.isBlank() ? null : code(value, allowed, field);
    }

    private static LocalDate date(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Tham số " + field + " phải có dạng yyyy-MM-dd.");
        }
    }

    private Map<String, Object> visible(long id, long actorId, boolean seeAll) throws SQLException {
        Map<String, Object> lead = leadDAO.findById(id);
        if (lead == null || !seeAll && !Long.valueOf(actorId).equals(lead.get("ownerId"))) {
            throw new ResourceNotFoundException("Không tìm thấy lead " + id + ".");
        }
        return label(lead);
    }

    private static Map<String, Object> label(Map<String, Object> lead) {
        // S3-03: lead đã chuyển thành học viên hiện là "Đã nhập học" (vẫn là mã WON, nằm ở cột Chốt của phễu).
        lead.put("statusLabel", lead.get("studentCode") != null ? "Đã nhập học"
                : STATUSES.getOrDefault((String) lead.get("status"), (String) lead.get("status")));
        lead.put("sourceLabel", SOURCES.getOrDefault((String) lead.get("source"), (String) lead.get("source")));
        return lead;
    }
}
