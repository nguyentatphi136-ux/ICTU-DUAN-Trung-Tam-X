package com.ems.service;

import com.ems.dao.SubjectDAO;
import com.ems.dao.TrainingProgramDAO;
import com.ems.exception.ApiException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.TrainingProgram;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * S2-05 môn học và S2-07 buổi học của môn.
 * Luật: mã môn duy nhất (kể cả môn trong thùng rác); môn đã mở lớp thì không xoá được;
 * số buổi đã khai báo không vượt số buổi của môn.
 */
public class SubjectService {
    /** Dữ liệu form môn học. programCodes = null nghĩa là không đổi chương trình đang dùng môn. */
    public record SubjectInput(String code, String name, Integer sessions, BigDecimal weight, String outcome, List<String> programCodes) {}

    private static final int MAX_SESSIONS = 200;

    private final SubjectDAO subjectDAO;
    private final TrainingProgramDAO programDAO;

    public SubjectService() {
        this(new SubjectDAO(), new TrainingProgramDAO());
    }

    public SubjectService(SubjectDAO subjectDAO, TrainingProgramDAO programDAO) {
        this.subjectDAO = subjectDAO;
        this.programDAO = programDAO;
    }

    public Map<String, Object> search(String keyword, String programCode, int page, int size) throws SQLException {
        return subjectDAO.search(keyword, programCode, page, Math.min(size, 500));
    }

    public Map<String, Object> get(String code) throws SQLException {
        Map<String, Object> subject = subjectDAO.findByCode(code);
        if (subject == null) throw new ResourceNotFoundException("Không tìm thấy môn học: " + code);
        return subject;
    }

    public Map<String, Object> create(SubjectInput in) throws SQLException {
        String code = validate(in);
        ensureCodeFree(code, null);
        int id = subjectDAO.create(code, in.name().trim(), in.sessions(), weight(in), outcome(in));
        syncPrograms(id, List.of(), in.programCodes());
        return subjectDAO.findByCode(code);
    }

    /** Sửa môn; đổi mã thì lộ trình và buổi học đi theo vì đều tham chiếu bằng id. */
    public Map<String, Object> update(String currentCode, SubjectInput in) throws SQLException {
        Map<String, Object> current = get(currentCode);
        int id = (Integer) current.get("id");
        String code = validate(in);
        ensureCodeFree(code, id);
        int lessons = subjectDAO.countLessons(id);
        if (in.sessions() < lessons) {
            throw new ConflictException("SESSIONS_BELOW_LESSONS",
                    "Môn đã khai báo " + lessons + " buổi học. Hãy xoá bớt buổi trước khi giảm số buổi xuống " + in.sessions() + ".");
        }
        subjectDAO.update(id, code, in.name().trim(), in.sessions(), weight(in), outcome(in));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> using = (List<Map<String, Object>>) current.get("programs");
        syncPrograms(id, using.stream().map(p -> (String) p.get("code")).toList(), in.programCodes());
        return subjectDAO.findByCode(code);
    }

    /** Chuyển vào thùng rác; môn đã có lớp học thì giữ lại (409 SUBJECT_HAS_CLASSES). */
    public void trash(String code, long actorId) throws SQLException {
        Map<String, Object> subject = get(code);
        int classes = (Integer) subject.get("openedClasses");
        if (classes > 0) {
            throw new ConflictException("SUBJECT_HAS_CLASSES",
                    "Không xoá được vì môn đã có " + classes + " lớp học. Bạn vẫn sửa được tên và mô tả.");
        }
        subjectDAO.softDelete((Integer) subject.get("id"), actorId);
    }

    public List<Map<String, Object>> listTrash() throws SQLException {
        return subjectDAO.listTrash();
    }

    public void restore(int id) throws SQLException {
        if (!subjectDAO.restore(id)) throw new ResourceNotFoundException("Môn học không có trong thùng rác.");
    }

    public void purge(int id) throws SQLException {
        if (subjectDAO.countClasses(id) > 0) {
            throw new ConflictException("SUBJECT_HAS_CLASSES", "Không xoá vĩnh viễn được vì môn đã có lớp học. Hãy để trong thùng rác.");
        }
        if (!subjectDAO.purge(id)) throw new ResourceNotFoundException("Môn học không có trong thùng rác.");
    }

    // ===== Buổi học (S2-07) =====

    public List<Map<String, Object>> lessons(String code) throws SQLException {
        return subjectDAO.listLessons(idOf(get(code)));
    }

    public List<Map<String, Object>> addLesson(String code, String topic, String goal) throws SQLException {
        Map<String, Object> subject = get(code);
        int sessions = (Integer) subject.get("sessions");
        if (!subjectDAO.addLesson(idOf(subject), sessions, lessonTopic(topic), lessonGoal(goal))) {
            throw new ConflictException("LESSON_LIMIT_REACHED",
                    "Môn chỉ có " + sessions + " buổi và đã khai báo đủ. Tăng số buổi của môn nếu cần thêm.");
        }
        return subjectDAO.listLessons(idOf(subject));
    }

    public List<Map<String, Object>> updateLesson(String code, int order, String topic, String goal) throws SQLException {
        int id = idOf(get(code));
        if (!subjectDAO.updateLesson(id, order, lessonTopic(topic), lessonGoal(goal))) {
            throw new ResourceNotFoundException("Không có buổi " + order + ".");
        }
        return subjectDAO.listLessons(id);
    }

    public List<Map<String, Object>> deleteLesson(String code, int order) throws SQLException {
        int id = idOf(get(code));
        if (!subjectDAO.deleteLesson(id, order)) throw new ResourceNotFoundException("Không có buổi " + order + ".");
        return subjectDAO.listLessons(id);
    }

    /**
     * Nhân bản buổi học từ môn khác. Môn đích đã có buổi mà chưa xác nhận ghi đè thì trả 409 LESSONS_EXIST để giao diện hỏi lại.
     * Môn nguồn nhiều buổi hơn số buổi của môn đích thì chỉ lấy các buổi đầu.
     */
    public List<Map<String, Object>> cloneLessons(String code, String fromCode, boolean overwrite) throws SQLException {
        Map<String, Object> target = get(code);
        if (fromCode == null || fromCode.isBlank()) throw new ValidationException("Chọn môn để nhân bản buổi học (fromSubjectCode).");
        Map<String, Object> source = get(fromCode);
        if (idOf(source) == idOf(target)) throw new ValidationException("Không nhân bản buổi học từ chính môn này.");
        int existing = subjectDAO.countLessons(idOf(target));
        if (existing > 0 && !overwrite) {
            throw new ConflictException("LESSONS_EXIST",
                    "Môn đã có " + existing + " buổi học. Gửi lại với overwrite: true để thay bằng buổi học của môn " + source.get("code") + ".");
        }
        List<Map<String, Object>> copied = subjectDAO.listLessons(idOf(source));
        int sessions = (Integer) target.get("sessions");
        subjectDAO.replaceLessons(idOf(target), copied.subList(0, Math.min(sessions, copied.size())));
        return subjectDAO.listLessons(idOf(target));
    }

    // ===== Kiểm tra dữ liệu =====

    private String validate(SubjectInput in) {
        if (in.code() == null || in.code().isBlank()) throw new ValidationException("Nhập mã môn.");
        String code = in.code().trim().toUpperCase(Locale.ROOT);
        if (code.length() > 50 || !code.matches("^[A-Z0-9_.-]+$")) {
            throw new ValidationException("Mã môn tối đa 50 ký tự, chỉ gồm chữ, số và dấu - _ .");
        }
        if (in.name() == null || in.name().isBlank()) throw new ValidationException("Nhập tên môn học.");
        if (in.name().trim().length() > 150) throw new ValidationException("Tên môn học tối đa 150 ký tự.");
        if (in.sessions() == null || in.sessions() <= 0 || in.sessions() > MAX_SESSIONS) {
            throw new ValidationException("Số buổi phải từ 1 đến " + MAX_SESSIONS + ".");
        }
        if (in.weight() != null && (in.weight().signum() <= 0 || in.weight().compareTo(new BigDecimal("99.99")) > 0)) {
            throw new ValidationException("Trọng số phải lớn hơn 0 và nhỏ hơn 100.");
        }
        if (in.outcome() != null && in.outcome().length() > 2000) {
            throw new ValidationException("Mô tả chuẩn đầu ra tối đa 2000 ký tự.");
        }
        return code;
    }

    private void ensureCodeFree(String code, Integer exceptId) throws SQLException {
        if (subjectDAO.codeTaken(code, exceptId)) {
            throw new ConflictException("SUBJECT_CODE_CONFLICT", subjectDAO.codeInTrash(code)
                    ? "Mã " + code + " thuộc một môn trong thùng rác. Hãy khôi phục môn đó."
                    : "Mã " + code + " đã dùng cho môn khác.");
        }
    }

    private static BigDecimal weight(SubjectInput in) {
        return (in.weight() == null ? BigDecimal.ONE : in.weight()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String outcome(SubjectInput in) {
        return in.outcome() == null ? "" : in.outcome().trim();
    }

    /** Thêm môn vào cuối lộ trình các chương trình mới chọn, gỡ khỏi chương trình bỏ chọn. */
    private void syncPrograms(int subjectId, List<String> current, List<String> wanted) throws SQLException {
        if (wanted == null) return;
        Set<String> want = new HashSet<>();
        for (String c : wanted) want.add(c.trim().toUpperCase(Locale.ROOT));
        Set<String> have = new HashSet<>();
        for (String c : current) have.add(c.toUpperCase(Locale.ROOT));
        List<TrainingProgram> toAdd = new ArrayList<>();
        for (String c : want) {
            if (have.contains(c)) continue;
            TrainingProgram p = programDAO.findByCode(c);
            if (p == null) throw new ApiException(400, "PROGRAM_NOT_FOUND", "Không tìm thấy chương trình " + c + ".");
            toAdd.add(p);
        }
        for (TrainingProgram p : toAdd) programDAO.addSubject(p.getId(), subjectId);
        for (String c : have) {
            if (want.contains(c)) continue;
            TrainingProgram p = programDAO.findByCode(c);
            if (p != null) programDAO.removeSubject(p.getId(), subjectId);
        }
    }

    private static String lessonTopic(String topic) {
        if (topic == null || topic.isBlank()) throw new ValidationException("Nhập chủ đề buổi học.");
        if (topic.trim().length() > 255) throw new ValidationException("Chủ đề tối đa 255 ký tự.");
        return topic.trim();
    }

    private static String lessonGoal(String goal) {
        if (goal != null && goal.length() > 2000) throw new ValidationException("Mục tiêu tối đa 2000 ký tự.");
        return goal == null ? "" : goal.trim();
    }

    private static int idOf(Map<String, Object> subject) {
        return (Integer) subject.get("id");
    }
}
