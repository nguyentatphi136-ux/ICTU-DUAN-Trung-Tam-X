package com.ems.service;

import com.ems.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service xử lý thuật toán sinh thời khoá biểu tự động theo mẫu lặp cho lớp học
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Tiêu chí nghiệm thu:
 *   1. Khai báo mẫu: thứ trong tuần, ca học, ngày bắt đầu, số buổi (ví dụ 60 buổi)
 *   2. Tự động bỏ qua các ngày nghỉ lễ đã khai báo và sinh đủ số lượng buổi
 *   3. Gắn từng buổi với bài học tương ứng theo thứ tự môn trong chương trình đào tạo của lớp
 *   4. Lấy đúng giảng viên được phân công cho từng môn
 *   5. Xem trước danh sách buổi và xác nhận lưu thời khoá biểu
 * 
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ScheduleService {

    private static final Map<String, List<ClassSession>> SESSIONS_MAP = new ConcurrentHashMap<>();

    private static class LessonSlot {
        String subjectCode;
        int lesson;
        String topic;
        String teacher;

        LessonSlot(String subjectCode, int lesson, String topic, String teacher) {
            this.subjectCode = subjectCode;
            this.lesson = lesson;
            this.topic = topic;
            this.teacher = teacher;
        }
    }

    /**
     * Sinh thời khoá biểu tự động theo mẫu lặp, tự động bỏ qua ngày nghỉ lễ
     */
    public ScheduleGenerationResult generateSchedule(SchedulePattern pattern) {
        validatePattern(pattern);

        Klass klass = ClassStore.findByCode(pattern.getClassCode());
        if (klass == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin lớp học có mã: " + pattern.getClassCode());
        }

        // 1. Chuẩn bị danh sách bài học tuần tự từ các môn của lớp
        List<LessonSlot> slots = new ArrayList<>();
        if (klass.getSubjects() != null && !klass.getSubjects().isEmpty()) {
            for (ClassSubject s : klass.getSubjects()) {
                for (int i = 1; i <= s.getSessions(); i++) {
                    String topic = ClassStore.getTopicForLesson(s.getCode(), i);
                    slots.add(new LessonSlot(s.getCode(), i, topic, s.getTeacher()));
                }
            }
        }

        if (slots.isEmpty()) {
            // Trường hợp lớp chưa có môn, tạo các slot mẫu
            for (int i = 1; i <= pattern.getCount(); i++) {
                slots.add(new LessonSlot("CHUNG", i, "Bài học số " + i, "Chưa phân công"));
            }
        }

        // 2. Thuật toán duyệt ngày và sinh lịch tự động
        LocalDate current = LocalDate.parse(pattern.getFrom());
        List<ClassSession> sessions = new ArrayList<>();
        List<SkippedDate> skipped = new ArrayList<>();

        int guard = 0;
        int targetCount = pattern.getCount();

        while (sessions.size() < targetCount && guard++ < 3000) {
            // Trong Java: Thứ 2 = 1, ..., Thứ 7 = 6, Chủ nhật = 7. Quy đổi: Chủ nhật = 0
            int dow = current.getDayOfWeek().getValue() % 7;

            // Kiểm tra xem ngày hiện tại có phải thứ được chọn hay không
            if (!pattern.hasDay(dow)) {
                current = current.plusDays(1);
                continue;
            }

            // Kiểm tra ngày nghỉ lễ
            Holiday holiday = HolidayStore.findHolidayCovering(current);
            if (holiday != null) {
                // Tự động bỏ qua ngày nghỉ lễ, ghi nhận lý do và không tính vào số buổi đã sinh
                skipped.add(new SkippedDate(current.toString(), holiday.getName()));
                current = current.plusDays(1);
                continue;
            }

            // Sinh buổi học hợp lệ
            int sessionIndex = sessions.size();
            LessonSlot slot = (sessionIndex < slots.size()) ? slots.get(sessionIndex) : slots.get(slots.size() - 1);
            int sessionNo = sessionIndex + 1;
            String sessionId = pattern.getClassCode() + "-" + sessionNo;

            ClassSession session = new ClassSession(
                    sessionId,
                    pattern.getClassCode(),
                    sessionNo,
                    current.toString(),
                    pattern.getStart(),
                    pattern.getEnd(),
                    slot.subjectCode,
                    slot.lesson,
                    slot.topic,
                    slot.teacher,
                    pattern.getRoom()
            );

            sessions.add(session);
            current = current.plusDays(1);
        }

        return new ScheduleGenerationResult(klass, pattern, sessions, skipped);
    }

    /**
     * Lưu thời khoá biểu chính thức sau khi Quản lý đào tạo xem trước và xác nhận
     */
    public synchronized void saveSchedule(String classCode, List<ClassSession> sessions, SchedulePattern pattern) {
        if (classCode == null || classCode.isBlank()) {
            throw new IllegalArgumentException("Mã lớp không được để trống khi lưu.");
        }
        if (sessions == null || sessions.isEmpty()) {
            throw new IllegalArgumentException("Danh sách buổi học không được rỗng.");
        }

        SESSIONS_MAP.put(classCode.trim().toUpperCase(), new ArrayList<>(sessions));

        // Cập nhật thông tin khung giờ và phòng học của lớp
        Klass klass = ClassStore.findByCode(classCode);
        if (klass != null) {
            if (pattern != null) {
                String shiftName = determineShiftName(pattern.getStart());
                String daysStr = pattern.getDays().stream()
                        .sorted((a, b) -> ((a + 6) % 7) - ((b + 6) % 7))
                        .map(d -> (d == 0) ? "CN" : String.valueOf(d + 1))
                        .collect(Collectors.joining("-"));
                klass.setSlot(shiftName + " " + daysStr);
                if (pattern.getRoom() != null && !pattern.getRoom().isBlank()) {
                    klass.setRoom(pattern.getRoom());
                }
            }
            ClassStore.saveClass(klass);
        }
    }

    public List<ClassSession> getSessionsByClass(String classCode) {
        if (classCode == null) return Collections.emptyList();
        List<ClassSession> list = SESSIONS_MAP.get(classCode.trim().toUpperCase());
        return (list != null) ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public boolean hasSchedule(String classCode) {
        List<ClassSession> list = SESSIONS_MAP.get(classCode.trim().toUpperCase());
        return list != null && !list.isEmpty();
    }

    public synchronized void clearSchedule(String classCode) {
        if (classCode != null) {
            SESSIONS_MAP.remove(classCode.trim().toUpperCase());
        }
    }

    private String determineShiftName(String startTime) {
        if (startTime == null || startTime.isBlank()) return "Ca học";
        try {
            LocalTime t = LocalTime.parse(startTime);
            if (t.isBefore(LocalTime.of(12, 0))) return "Sáng";
            if (t.isBefore(LocalTime.of(17, 0))) return "Chiều";
            return "Tối";
        } catch (Exception e) {
            return "Ca học";
        }
    }

    private void validatePattern(SchedulePattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Thông tin mẫu lặp không được để trống.");
        }
        if (pattern.getClassCode() == null || pattern.getClassCode().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn lớp học cần sinh thời khoá biểu.");
        }
        if (pattern.getDays() == null || pattern.getDays().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một thứ trong tuần.");
        }
        if (pattern.getFrom() == null || pattern.getFrom().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu khai giảng.");
        }
        try {
            LocalDate.parse(pattern.getFrom());
        } catch (Exception e) {
            throw new IllegalArgumentException("Ngày bắt đầu không đúng định dạng YYYY-MM-DD.");
        }
        if (pattern.getStart() == null || pattern.getStart().isBlank() ||
                pattern.getEnd() == null || pattern.getEnd().isBlank()) {
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc ca học không được để trống.");
        }
        try {
            LocalTime startTime = LocalTime.parse(pattern.getStart());
            LocalTime endTime = LocalTime.parse(pattern.getEnd());
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("Giờ kết thúc ca học (" + pattern.getEnd() + ") phải sau giờ bắt đầu (" + pattern.getStart() + ").");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Giờ học không đúng định dạng HH:mm.");
        }
        if (pattern.getCount() < 1 || pattern.getCount() > 300) {
            throw new IllegalArgumentException("Số buổi của lớp phải nằm trong khoảng từ 1 đến 300 buổi.");
        }
    }
}
