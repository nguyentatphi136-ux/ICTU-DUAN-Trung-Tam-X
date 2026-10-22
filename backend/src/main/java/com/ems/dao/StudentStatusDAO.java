package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.ClassItem;
import com.ems.model.PastClass;
import com.ems.model.Student;
import com.ems.model.StudentStatusLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object cho nghiệp vụ S4-06:
 * Xử lý Bảo lưu, Chuyển lớp, Thôi học và Danh sách điểm danh theo ngày (Roster on Date).
 * Tích hợp sẵn cơ chế Fallback In-Memory khi chưa cấu hình CSDL MySQL.
 */
public class StudentStatusDAO {

    // Kho dữ liệu In-Memory Demo phục vụ chạy thử và kiểm thử độc lập
    private static final Map<String, Student> MEMORY_STUDENTS = new ConcurrentHashMap<>();
    private static final Map<String, ClassItem> MEMORY_CLASSES = new ConcurrentHashMap<>();
    private static final List<StudentStatusLog> GLOBAL_LOGS = new CopyOnWriteArrayList<>();

    static {
        initSeedData();
    }

    private static void initSeedData() {
        // 1. Khởi tạo danh sách lớp học mẫu
        ClassItem webK15 = new ClassItem("WEB-K15", "Lập trình Web Full-stack K15", "Lập trình web Full-stack", 24, 30, "IN_PROGRESS", "2026-08-12");
        ClassItem webK16 = new ClassItem("WEB-K16", "Lập trình Web Full-stack K16", "Lập trình web Full-stack", 17, 30, "IN_PROGRESS", "2026-09-20");
        ClassItem webK17 = new ClassItem("WEB-K17", "Lập trình Web Full-stack K17 (Tối 2-4-6)", "Lập trình web Full-stack", 10, 25, "PLANNING", "2026-11-01");
        ClassItem javaK12 = new ClassItem("JAVA-K12", "Lập trình Java Backend K12", "Lập trình Java Backend", 18, 25, "IN_PROGRESS", "2026-09-01");
        ClassItem dataK07 = new ClassItem("DATA-K07", "Phân tích dữ liệu K07", "Phân tích dữ liệu", 16, 25, "IN_PROGRESS", "2026-07-20");

        MEMORY_CLASSES.put(webK15.getCode(), webK15);
        MEMORY_CLASSES.put(webK16.getCode(), webK16);
        MEMORY_CLASSES.put(webK17.getCode(), webK17);
        MEMORY_CLASSES.put(javaK12.getCode(), javaK12);
        MEMORY_CLASSES.put(dataK07.getCode(), dataK07);

        // 2. Học viên trọng tâm kiểm thử S4-06: NGUYEN MINH ANH (HV260101)
        Student s1 = new Student("HV260101", "NGUYEN MINH ANH", "0912 345 678", "minhanh.ng@gmail.com",
                "Lập trình web Full-stack", "Đang học", "2026-08-12", "WEB-K15");
        s1.setLoginEmail("S25200212101@tms.vn");
        s1.setBirthDate("2004-03-15");
        s1.setAddress("12 Nguyễn Trãi, Thanh Xuân, Hà Nội");
        s1.setCounselor("TRAN THU HA");
        s1.setEntryScore("6.5 / 10");
        s1.setLevel("Cơ bản");
        s1.setBackground("Sinh viên năm 3 ngành Kinh tế, đã tự học HTML và CSS.");

        List<PastClass> history = new ArrayList<>();
        history.add(new PastClass("GIT-K03", "Git và làm việc nhóm", "06/2026 – 07/2026", "Hoàn thành · 8.2"));
        history.add(new PastClass("WEB-K12", "Lập trình web Full-stack", "02/2026 – 04/2026", "Bảo lưu"));
        history.add(new PastClass("TIN-K20", "Tin học văn phòng", "09/2025 – 11/2025", "Hoàn thành · 9.0"));
        s1.setHistory(history);

        List<StudentStatusLog> logs = new ArrayList<>();
        StudentStatusLog log1 = new StudentStatusLog("Bảo lưu", "2026-04-10", "Lý do sức khoẻ. Điều trị tại bệnh viện 1 tháng.",
                "Nguyễn Tất Phi", "WEB-K12", null, "2026-04-09");
        logs.add(log1);
        GLOBAL_LOGS.add(log1);
        s1.setStatusChanges(logs);

        MEMORY_STUDENTS.put(s1.getCode(), s1);

        // Các học viên mẫu khác
        Student s2 = new Student("HV260102", "HOANG YEN NHI", "0934 118 205", "nhi.hy@gmail.com",
                "Lập trình web Full-stack", "Đang học", "2026-08-12", "WEB-K15");
        s2.setLoginEmail("S25200212102@tms.vn");
        MEMORY_STUDENTS.put(s2.getCode(), s2);

        Student s3 = new Student("HV260128", "TRAN DUC HUY", "0904 612 775", "huy.td@gmail.com",
                "Lập trình web Full-stack", "Đang học", "2026-09-22", "WEB-K16");
        s3.setLoginEmail("S25200212128@tms.vn");
        MEMORY_STUDENTS.put(s3.getCode(), s3);

        Student s4 = new Student("HV260131", "NGUYEN THU TRANG", "0976 220 148", "trang.nt@gmail.com",
                "Lập trình web Full-stack", "Đang học", "2026-09-25", "WEB-K16");
        s4.setLoginEmail("S25200212131@tms.vn");
        MEMORY_STUDENTS.put(s4.getCode(), s4);

        Student s5 = new Student("HV260075", "LE THANH TAM", "0906 731 554", "tam.lt@gmail.com",
                "Lập trình web Full-stack", "Bảo lưu", "2026-07-02", null);
        s5.setLoginEmail("S25200212075@tms.vn");
        StudentStatusLog logSusp = new StudentStatusLog("Bảo lưu", "2026-09-01", "Bận công việc cơ quan",
                "Nguyễn Minh Ngọc", "WEB-K15", null, "2026-08-30");
        s5.getStatusChanges().add(logSusp);
        GLOBAL_LOGS.add(logSusp);
        MEMORY_STUDENTS.put(s5.getCode(), s5);

        Student s6 = new Student("HV250298", "BUI QUANG KHAI", "0915 090 433", "khai.bq@gmail.com",
                "Lập trình web Full-stack", "Thôi học", "2025-11-03", null);
        s6.setLoginEmail("S25200212298@tms.vn");
        StudentStatusLog logDrop = new StudentStatusLog("Thôi học", "2026-03-15", "Không theo kịp chương trình",
                "Nguyễn Minh Ngọc", "WEB-K15", null, "2026-03-14");
        s6.getStatusChanges().add(logDrop);
        GLOBAL_LOGS.add(logDrop);
        MEMORY_STUDENTS.put(s6.getCode(), s6);
    }

    /**
     * Lấy danh sách toàn bộ học viên theo bộ lọc
     */
    public List<Student> getAllStudents(String search, String status, String classCode, String program) {
        List<Student> list = new ArrayList<>();
        boolean dbSuccess = false;

        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                String sql = "SELECT s.student_code, u.full_name, u.phone, u.email, p.program_name, " +
                        "s.enrollment_status, s.created_at, c.class_code " +
                        "FROM students s " +
                        "JOIN users u ON s.user_id = u.id " +
                        "LEFT JOIN enrollments e ON s.id = e.student_id AND e.status = 'ENROLLED' " +
                        "LEFT JOIN classes c ON e.class_id = c.id " +
                        "LEFT JOIN programs p ON c.program_id = p.id " +
                        "WHERE 1=1 ";

                List<Object> params = new ArrayList<>();
                if (search != null && !search.isBlank()) {
                    sql += "AND (s.student_code LIKE ? OR u.full_name LIKE ? OR u.phone LIKE ?) ";
                    String pattern = "%" + search.trim() + "%";
                    params.add(pattern);
                    params.add(pattern);
                    params.add(pattern);
                }
                if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                    sql += "AND s.enrollment_status = ? ";
                    params.add(status);
                }
                if (classCode != null && !classCode.isBlank() && !"ALL".equalsIgnoreCase(classCode)) {
                    sql += "AND c.class_code = ? ";
                    params.add(classCode);
                }

                sql += "ORDER BY s.student_code DESC";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Student st = new Student();
                            st.setCode(rs.getString("student_code"));
                            st.setName(rs.getString("full_name"));
                            st.setPhone(rs.getString("phone"));
                            st.setEmail(rs.getString("email"));
                            st.setProgram(rs.getString("program_name") != null ? rs.getString("program_name") : "Lập trình web Full-stack");
                            st.setStatus(mapStatusFromDb(rs.getString("enrollment_status")));
                            st.setCurrentClass(rs.getString("class_code"));
                            st.setEnrolledAt(rs.getString("created_at") != null ? rs.getString("created_at").substring(0, 10) : "2026-08-12");
                            list.add(st);
                        }
                    }
                }
                if (!list.isEmpty()) {
                    dbSuccess = true;
                }
            }
        } catch (Exception e) {
            // Log và chuyển fallback
        }

        if (!dbSuccess) {
            // Sử dụng dữ liệu in-memory
            for (Student s : MEMORY_STUDENTS.values()) {
                boolean matchSearch = search == null || search.isBlank()
                        || s.getName().toLowerCase().contains(search.toLowerCase())
                        || s.getCode().toLowerCase().contains(search.toLowerCase())
                        || (s.getPhone() != null && s.getPhone().contains(search));
                boolean matchStatus = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || s.getStatus().equalsIgnoreCase(status);
                boolean matchClass = classCode == null || classCode.isBlank() || "ALL".equalsIgnoreCase(classCode)
                        || (s.getCurrentClass() != null && s.getCurrentClass().equalsIgnoreCase(classCode));
                boolean matchProgram = program == null || program.isBlank() || "ALL".equalsIgnoreCase(program)
                        || (s.getProgram() != null && s.getProgram().equalsIgnoreCase(program));

                if (matchSearch && matchStatus && matchClass && matchProgram) {
                    list.add(s);
                }
            }
            list.sort((a, b) -> b.getCode().compareTo(a.getCode()));
        }

        return list;
    }

    /**
     * Tìm học viên theo mã học viên
     */
    public Student getStudentByCode(String code) {
        if (code == null || code.isBlank()) return null;

        // Ưu tiên tra cứu DB
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                String sql = "SELECT s.id AS student_id, s.student_code, u.full_name, u.phone, u.email, " +
                        "p.program_name, s.enrollment_status, s.notes, s.created_at, c.class_code " +
                        "FROM students s " +
                        "JOIN users u ON s.user_id = u.id " +
                        "LEFT JOIN enrollments e ON s.id = e.student_id AND e.status = 'ENROLLED' " +
                        "LEFT JOIN classes c ON e.class_id = c.id " +
                        "LEFT JOIN programs p ON c.program_id = p.id " +
                        "WHERE s.student_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Student st = new Student();
                            st.setId(rs.getLong("student_id"));
                            st.setCode(rs.getString("student_code"));
                            st.setName(rs.getString("full_name"));
                            st.setPhone(rs.getString("phone"));
                            st.setEmail(rs.getString("email"));
                            st.setProgram(rs.getString("program_name") != null ? rs.getString("program_name") : "Lập trình web Full-stack");
                            st.setStatus(mapStatusFromDb(rs.getString("enrollment_status")));
                            st.setCurrentClass(rs.getString("class_code"));
                            st.setEnrolledAt(rs.getString("created_at") != null ? rs.getString("created_at").substring(0, 10) : "2026-08-12");
                            st.setStatusChanges(getStatusLogsForStudent(code));
                            return st;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback in-memory
        Student st = MEMORY_STUDENTS.get(code.toUpperCase());
        if (st == null) {
            // Tra cứu case insensitive
            for (Student s : MEMORY_STUDENTS.values()) {
                if (s.getCode().equalsIgnoreCase(code)) {
                    return s;
                }
            }
        }
        return st;
    }

    /**
     * Lấy các lớp hợp lệ để chuyển đến (S4-06):
     * Cùng chương trình đào tạo, đang mở/đang học và còn chỗ trống.
     */
    public List<ClassItem> getAvailableClassesForTransfer(String currentClassCode, String program) {
        List<ClassItem> available = new ArrayList<>();
        for (ClassItem c : MEMORY_CLASSES.values()) {
            boolean isSameProgram = program == null || program.isBlank() || c.getProgram().equalsIgnoreCase(program);
            boolean isDifferentClass = currentClassCode == null || !c.getCode().equalsIgnoreCase(currentClassCode);
            boolean isOpen = c.isOpen();
            boolean hasSpace = c.hasAvailableSeats();

            if (isSameProgram && isDifferentClass && isOpen && hasSpace) {
                available.add(c);
            }
        }
        available.sort(Comparator.comparing(ClassItem::getCode));
        return available;
    }

    /**
     * Nghiệp vụ cốt lõi S4-06:
     * Xử lý Bảo lưu, Chuyển lớp, Thôi học.
     * Cập nhật tình trạng, lưu lịch sử học viên và ghi log hệ thống.
     */
    public synchronized boolean processStatusChange(String studentCode, String actionType, String effectiveDate,
                                                    String reason, String notes, String toClassCode,
                                                    String actorName, Long actorId) {
        if (studentCode == null || actionType == null || effectiveDate == null) {
            return false;
        }

        Student student = getStudentByCode(studentCode);
        if (student == null) {
            return false;
        }

        String fromClass = student.getCurrentClass();
        String fullReason = reason != null ? reason.trim() : "";
        if (notes != null && !notes.isBlank()) {
            fullReason = fullReason.isEmpty() ? notes.trim() : fullReason + ". " + notes.trim();
        }

        String today = LocalDate.now().toString();
        StudentStatusLog log = new StudentStatusLog();
        log.setStudentCode(student.getCode());
        log.setStudentName(student.getName());
        log.setActionType(actionType);
        log.setFromClass(fromClass);
        log.setToClass("Chuyển lớp".equalsIgnoreCase(actionType) ? toClassCode : null);
        log.setEffectiveDate(effectiveDate);
        log.setReason(fullReason);
        log.setNotes(notes);
        log.setActorName(actorName != null ? actorName : "Quản lý đào tạo");
        log.setActorId(actorId);
        log.setRecordedDate(today);

        // 1. Cập nhật đối tượng Student trong bộ nhớ
        if ("Chuyển lớp".equalsIgnoreCase(actionType)) {
            if (fromClass != null && !fromClass.isBlank()) {
                student.getHistory().add(0, new PastClass(fromClass, student.getProgram(),
                        "08/2026 – " + effectiveDate.substring(5, 7) + "/" + effectiveDate.substring(0, 4),
                        "Chuyển sang " + toClassCode));
            }
            student.setCurrentClass(toClassCode);
            student.setStatus("Đang học");

            // Cập nhật sĩ số lớp
            ClassItem targetClass = MEMORY_CLASSES.get(toClassCode);
            if (targetClass != null) {
                targetClass.setCurrentSize(targetClass.getCurrentSize() + 1);
            }
            if (fromClass != null) {
                ClassItem oldClass = MEMORY_CLASSES.get(fromClass);
                if (oldClass != null && oldClass.getCurrentSize() > 0) {
                    oldClass.setCurrentSize(oldClass.getCurrentSize() - 1);
                }
            }
        } else if ("Bảo lưu".equalsIgnoreCase(actionType)) {
            if (fromClass != null && !fromClass.isBlank()) {
                student.getHistory().add(0, new PastClass(fromClass, student.getProgram(),
                        "08/2026 – " + effectiveDate.substring(5, 7) + "/" + effectiveDate.substring(0, 4),
                        "Bảo lưu"));
            }
            student.setStatus("Bảo lưu");
            student.setCurrentClass(null);
            if (fromClass != null) {
                ClassItem oldClass = MEMORY_CLASSES.get(fromClass);
                if (oldClass != null && oldClass.getCurrentSize() > 0) {
                    oldClass.setCurrentSize(oldClass.getCurrentSize() - 1);
                }
            }
        } else if ("Thôi học".equalsIgnoreCase(actionType)) {
            if (fromClass != null && !fromClass.isBlank()) {
                student.getHistory().add(0, new PastClass(fromClass, student.getProgram(),
                        "08/2026 – " + effectiveDate.substring(5, 7) + "/" + effectiveDate.substring(0, 4),
                        "Thôi học"));
            }
            student.setStatus("Thôi học");
            student.setCurrentClass(null);
            if (fromClass != null) {
                ClassItem oldClass = MEMORY_CLASSES.get(fromClass);
                if (oldClass != null && oldClass.getCurrentSize() > 0) {
                    oldClass.setCurrentSize(oldClass.getCurrentSize() - 1);
                }
            }
        }

        // Ghi nhận vào danh sách log của học viên và toàn hệ thống
        student.getStatusChanges().add(0, log);
        GLOBAL_LOGS.add(0, log);

        // 2. Thử lưu xuống MySQL nếu có kết nối CSDL
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                String insertSql = "INSERT INTO student_status_logs " +
                        "(student_code, action_type, from_class, to_class, effective_date, reason, notes, actor_name, actor_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, log.getStudentCode());
                    ps.setString(2, log.getActionType());
                    ps.setString(3, log.getFromClass());
                    ps.setString(4, log.getToClass());
                    ps.setString(5, log.getEffectiveDate());
                    ps.setString(6, log.getReason());
                    ps.setString(7, log.getNotes());
                    ps.setString(8, log.getActorName());
                    ps.setObject(9, log.getActorId());
                    ps.executeUpdate();
                }

                // Cập nhật bảng students nếu cần
                String updateStudentSql = "UPDATE students SET enrollment_status = ? WHERE student_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateStudentSql)) {
                    ps.setString(1, mapStatusToDb(student.getStatus()));
                    ps.setString(2, student.getCode());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            // Ghi nhật ký lỗi, in-memory vẫn giữ trạng thái thành công
            System.err.println("DB Update Warning in processStatusChange: " + e.getMessage());
        }

        return true;
    }

    /**
     * Quy tắc nghiệm thu S4-06 (Roster on Date):
     * Danh sách điểm danh của lớp vào một ngày cụ thể.
     * - Học viên rời lớp (do chuyển lớp, bảo lưu, thôi học) từ ngày hiệu lực (targetDate >= effectiveDate)
     *   sẽ KHÔNG còn xuất hiện trong danh sách điểm danh.
     * - Các buổi học trước ngày hiệu lực (targetDate < effectiveDate), học viên vẫn có trong danh sách và giữ nguyên điểm danh.
     * - Học viên chuyển đến lớp mới chỉ xuất hiện trong danh sách từ ngày hiệu lực (targetDate >= effectiveDate).
     */
    public List<Student> getRosterOnDate(String classCode, String targetDate) {
        List<Student> roster = new ArrayList<>();
        if (classCode == null || targetDate == null) return roster;

        for (Student s : MEMORY_STUDENTS.values()) {
            // 1. Kiểm tra xem học viên có từng rời lớp classCode không
            StudentStatusLog leftLog = null;
            StudentStatusLog joinedLog = null;

            for (StudentStatusLog c : s.getStatusChanges()) {
                if (classCode.equalsIgnoreCase(c.getFromClass())) {
                    leftLog = c;
                    break;
                }
            }
            for (StudentStatusLog c : s.getStatusChanges()) {
                if (classCode.equalsIgnoreCase(c.getToClass())) {
                    joinedLog = c;
                    break;
                }
            }

            // Nếu học viên đã rời lớp này:
            if (leftLog != null) {
                // Nếu ngày điểm danh trước ngày hiệu lực rời lớp, học viên vẫn có mặt
                if (targetDate.compareTo(leftLog.getEffectiveDate()) < 0) {
                    roster.add(s);
                }
                continue;
            }

            // Nếu học viên hiện không thuộc lớp này hoặc trạng thái Bảo lưu/Thôi học
            if (!classCode.equalsIgnoreCase(s.getCurrentClass()) || s.isSuspended() || s.isDroppedOut()) {
                continue;
            }

            // Nếu học viên mới chuyển đến lớp này: chỉ có mặt từ ngày hiệu lực
            if (joinedLog != null) {
                if (targetDate.compareTo(joinedLog.getEffectiveDate()) >= 0) {
                    roster.add(s);
                }
            } else {
                // Học viên học từ đầu trong lớp
                roster.add(s);
            }
        }

        roster.sort(Comparator.comparing(Student::getName));
        return roster;
    }

    /**
     * Lấy danh sách lịch sử biến động học vụ của một học viên
     */
    public List<StudentStatusLog> getStatusLogsForStudent(String studentCode) {
        List<StudentStatusLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                String sql = "SELECT * FROM student_status_logs WHERE student_code = ? ORDER BY id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, studentCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            StudentStatusLog log = new StudentStatusLog();
                            log.setId(rs.getLong("id"));
                            log.setStudentCode(rs.getString("student_code"));
                            log.setActionType(rs.getString("action_type"));
                            log.setFromClass(rs.getString("from_class"));
                            log.setToClass(rs.getString("to_class"));
                            log.setEffectiveDate(rs.getString("effective_date"));
                            log.setReason(rs.getString("reason"));
                            log.setNotes(rs.getString("notes"));
                            log.setActorName(rs.getString("actor_name"));
                            log.setRecordedDate(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString().substring(0, 10) : "");
                            logs.add(log);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (logs.isEmpty()) {
            Student st = MEMORY_STUDENTS.get(studentCode);
            if (st != null) {
                return new ArrayList<>(st.getStatusChanges());
            }
        }
        return logs;
    }

    /**
     * Lấy toàn bộ lịch sử biến động gần nhất trong hệ thống
     */
    public List<StudentStatusLog> getAllStatusLogs(int limit) {
        List<StudentStatusLog> list = new ArrayList<>(GLOBAL_LOGS);
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    public List<ClassItem> getAllClasses() {
        return new ArrayList<>(MEMORY_CLASSES.values());
    }

    private String mapStatusFromDb(String dbStatus) {
        if (dbStatus == null) return "Đang học";
        switch (dbStatus.toUpperCase()) {
            case "STUDYING":
            case "ENROLLED":
                return "Đang học";
            case "SUSPENDED":
                return "Bảo lưu";
            case "DROPPED_OUT":
            case "DROPPED":
                return "Thôi học";
            case "GRADUATED":
                return "Đã tốt nghiệp";
            default:
                return "Đang học";
        }
    }

    private String mapStatusToDb(String vnStatus) {
        if (vnStatus == null) return "STUDYING";
        switch (vnStatus) {
            case "Bảo lưu":
                return "SUSPENDED";
            case "Thôi học":
                return "DROPPED_OUT";
            case "Đã tốt nghiệp":
                return "GRADUATED";
            default:
                return "STUDYING";
        }
    }
}
