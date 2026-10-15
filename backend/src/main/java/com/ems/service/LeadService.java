package com.ems.service;

import com.ems.dao.LeadDAO;
import com.ems.dao.TrainingProgramDAO;
import com.ems.dao.UserDAO;
import com.ems.exception.ApiException;
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

/**
 * S2-08 đăng ký tư vấn công khai, S2-09 danh sách lead, S2-10 phân công, S2-11 tìm kiếm và lọc.
 * Người có quyền LEAD_ASSIGN (Quản trị, Quản lý đào tạo) thấy mọi lead; tư vấn viên chỉ thấy và sửa lead được giao cho mình,
 * lead tư vấn viên tự thêm được giao luôn cho người đó.
 */
public class LeadService {
    /** Mã trạng thái lưu trong CSDL (ENUM của bảng leads) và nhãn hiển thị trên giao diện. */
    public static final Map<String, String> STATUSES = new LinkedHashMap<>();
    /** Mã nguồn lead và nhãn hiển thị. */
    public static final Map<String, String> SOURCES = new LinkedHashMap<>();

    static {
        STATUSES.put("NEW", "Mới");
        STATUSES.put("CONSULTING", "Đang chăm sóc");
        STATUSES.put("CONTACTED", "Hẹn gọi lại");
        STATUSES.put("TRIAL_SCHEDULED", "Hẹn học thử");
        STATUSES.put("WON", "Đã nhập học");
        STATUSES.put("REJECTED", "Không quan tâm");
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
        visible(id, actorId, seeAll);
        Clean c = clean(in, false);
        leadDAO.update(id, c.name, c.phone, c.email, c.source, c.programId, c.status, c.note);
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
        lead.put("statusLabel", STATUSES.getOrDefault((String) lead.get("status"), (String) lead.get("status")));
        lead.put("sourceLabel", SOURCES.getOrDefault((String) lead.get("source"), (String) lead.get("source")));
        return lead;
    }
}
