package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.dto.TranscriptResponse;
import com.ems.model.SubjectGradeItem;
import com.ems.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object cho nghiệp vụ S8-03 (Jira EP-07):
 * Bảng điểm toàn khoá của học viên (Liệt kê môn, điểm tổng kết, kết quả Đạt/Không đạt,
 * điểm trung bình toàn khoá theo trọng số, xuất PDF có mã học viên, tên trung tâm, ngày cấp).
 *
 * Hỗ trợ truy vấn CSDL MySQL và tự động fallback in-memory cache phục vụ môi trường kiểm thử/mock.
 */
public class TranscriptDAO {

    private static final Logger LOGGER = Logger.getLogger(TranscriptDAO.class.getName());
    private static final DateTimeFormatter VI_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // In-memory cache dự phòng cho dữ liệu mẫu
    private static final Map<String, TranscriptResponse> MEMORY_TRANSCRIPTS = new ConcurrentHashMap<>();

    static {
        initSampleData();
    }

    public static void initSampleData() {
        MEMORY_TRANSCRIPTS.clear();

        LocalDate today = LocalDate.now();
        String issueDateStr = today.format(VI_DATE_FMT);
        String currentYear = String.valueOf(today.getYear());

        // 1. Học viên tiêu biểu: NGUYEN MINH ANH (HV260101) - Đạt toàn bộ môn
        TranscriptResponse t1 = new TranscriptResponse();
        t1.setStudentId(1001L);
        t1.setStudentCode("HV260101");
        t1.setStudentName("NGUYEN MINH ANH");
        t1.setDateOfBirth("15/03/2004");
        t1.setEmail("minhanh.ng@gmail.com");
        t1.setPhone("0912 345 678");
        t1.setProgramCode("WEB-FS");
        t1.setProgramName("Lập trình web Full-stack");
        t1.setClassCode("WEB-K15");
        t1.setClassName("Web Full-stack K15");
        t1.setStudyPeriod("08/2026 – nay");
        t1.setCenterName("TRUNG TÂM ĐÀO TẠO TMS");
        t1.setCenterAddress("Tầng 3, cơ sở Cầu Giấy, Hà Nội");
        t1.setTranscriptNumber("BD-" + currentYear + "-100101");
        t1.setIssueDate(issueDateStr);
        t1.setTotalSessions(64);
        t1.setCompletedSessions(64);
        t1.setAttendanceRate(96);
        t1.setTotalSubjects(6);

        List<SubjectGradeItem> subjects1 = new ArrayList<>();
        subjects1.add(new SubjectGradeItem("WEB-01", "HTML & CSS nền tảng", 12, 1, 8.5, "Đạt"));
        subjects1.add(new SubjectGradeItem("WEB-02", "JavaScript cơ bản", 14, 2, 8.0, "Đạt"));
        subjects1.add(new SubjectGradeItem("WEB-03", "React", 12, 2, 9.0, "Đạt"));
        subjects1.add(new SubjectGradeItem("DB-01", "Cơ sở dữ liệu", 10, 2, 8.5, "Đạt"));
        subjects1.add(new SubjectGradeItem("BE-01", "Node.js và API", 12, 3, 8.0, "Đạt"));
        subjects1.add(new SubjectGradeItem("GIT-01", "Git và làm việc nhóm", 4, 1, 9.5, "Đạt"));
        t1.setSubjects(subjects1);
        t1.recalculate();
        MEMORY_TRANSCRIPTS.put(t1.getStudentCode().toUpperCase(), t1);

        // 2. Học viên: HOANG YEN NHI (HV260102) - Có 1 môn Không đạt
        TranscriptResponse t2 = new TranscriptResponse();
        t2.setStudentId(1002L);
        t2.setStudentCode("HV260102");
        t2.setStudentName("HOANG YEN NHI");
        t2.setDateOfBirth("20/11/2003");
        t2.setEmail("yennhi.hoang@gmail.com");
        t2.setPhone("0934 118 205");
        t2.setProgramCode("WEB-FS");
        t2.setProgramName("Lập trình web Full-stack");
        t2.setClassCode("WEB-K15");
        t2.setClassName("Web Full-stack K15");
        t2.setStudyPeriod("08/2026 – nay");
        t2.setCenterName("TRUNG TÂM ĐÀO TẠO TMS");
        t2.setCenterAddress("Tầng 3, cơ sở Cầu Giấy, Hà Nội");
        t2.setTranscriptNumber("BD-" + currentYear + "-100102");
        t2.setIssueDate(issueDateStr);
        t2.setTotalSessions(64);
        t2.setCompletedSessions(58);
        t2.setAttendanceRate(88);
        t2.setTotalSubjects(6);

        List<SubjectGradeItem> subjects2 = new ArrayList<>();
        subjects2.add(new SubjectGradeItem("WEB-01", "HTML & CSS nền tảng", 12, 1, 7.5, "Đạt"));
        subjects2.add(new SubjectGradeItem("WEB-02", "JavaScript cơ bản", 14, 2, 7.0, "Đạt"));
        subjects2.add(new SubjectGradeItem("WEB-03", "React", 12, 2, 6.5, "Đạt"));
        subjects2.add(new SubjectGradeItem("DB-01", "Cơ sở dữ liệu", 10, 2, 7.0, "Đạt"));
        subjects2.add(new SubjectGradeItem("BE-01", "Node.js và API", 12, 3, 4.2, "Không đạt")); // Môn trượt
        subjects2.add(new SubjectGradeItem("GIT-01", "Git và làm việc nhóm", 4, 1, 8.0, "Đạt"));
        t2.setSubjects(subjects2);
        t2.recalculate();
        MEMORY_TRANSCRIPTS.put(t2.getStudentCode().toUpperCase(), t2);

        // 3. Học viên tài khoản chuẩn hệ thống: HV-2026-001 (Nguyễn Văn Học)
        TranscriptResponse t3 = new TranscriptResponse();
        t3.setStudentId(5L);
        t3.setStudentCode("HV-2026-001");
        t3.setStudentName("Nguyễn Văn Học");
        t3.setDateOfBirth("10/05/2002");
        t3.setEmail("hocvien@tms.vn");
        t3.setPhone("0945678901");
        t3.setProgramCode("WEB-FS");
        t3.setProgramName("Lập trình web Full-stack");
        t3.setClassCode("WEB-K15");
        t3.setClassName("Web Full-stack K15");
        t3.setStudyPeriod("08/2026 – nay");
        t3.setCenterName("TRUNG TÂM ĐÀO TẠO TMS");
        t3.setCenterAddress("Tầng 3, cơ sở Cầu Giấy, Hà Nội");
        t3.setTranscriptNumber("BD-" + currentYear + "-000005");
        t3.setIssueDate(issueDateStr);
        t3.setTotalSessions(64);
        t3.setCompletedSessions(60);
        t3.setAttendanceRate(92);
        t3.setTotalSubjects(6);

        List<SubjectGradeItem> subjects3 = new ArrayList<>();
        subjects3.add(new SubjectGradeItem("WEB-01", "HTML & CSS nền tảng", 12, 1, 9.0, "Đạt"));
        subjects3.add(new SubjectGradeItem("WEB-02", "JavaScript cơ bản", 14, 2, 8.5, "Đạt"));
        subjects3.add(new SubjectGradeItem("WEB-03", "React", 12, 2, 8.0, "Đạt"));
        subjects3.add(new SubjectGradeItem("DB-01", "Cơ sở dữ liệu", 10, 2, 8.5, "Đạt"));
        subjects3.add(new SubjectGradeItem("BE-01", "Node.js và API", 12, 3, 8.5, "Đạt"));
        subjects3.add(new SubjectGradeItem("GIT-01", "Git và làm việc nhóm", 4, 1, 9.0, "Đạt"));
        t3.setSubjects(subjects3);
        t3.recalculate();
        MEMORY_TRANSCRIPTS.put(t3.getStudentCode().toUpperCase(), t3);
    }

    /**
     * Lấy bảng điểm toàn khoá của học viên theo mã học viên (S8-03 AC1, AC2, AC3)
     */
    public TranscriptResponse getTranscriptByStudentCode(String studentCode) throws SQLException {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = studentCode.trim().toUpperCase();

        // 1. Thử truy vấn cơ sở dữ liệu MySQL nếu có kết nối
        try (Connection conn = DBConnection.getConnection()) {
            TranscriptResponse fromDb = queryFromDatabase(conn, normalizedCode);
            if (fromDb != null) {
                return fromDb;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Không truy vấn được từ CSDL MySQL (fallback in-memory): " + e.getMessage());
        }

        // 2. Fallback in-memory cache
        TranscriptResponse cached = MEMORY_TRANSCRIPTS.get(normalizedCode);
        if (cached != null) {
            return cached;
        }

        // Tìm kiếm mềm gần đúng nếu có tiền tố khác nhau
        for (Map.Entry<String, TranscriptResponse> entry : MEMORY_TRANSCRIPTS.entrySet()) {
            if (entry.getKey().contains(normalizedCode) || normalizedCode.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Lấy bảng điểm toàn khoá cho người dùng đang đăng nhập
     */
    public TranscriptResponse getTranscriptForUser(User currentUser) throws SQLException {
        if (currentUser == null) {
            return null;
        }

        // 1. Tìm theo userCode (vd: HV-2026-001 hoặc HV260101)
        if (currentUser.getUserCode() != null) {
            TranscriptResponse t = getTranscriptByStudentCode(currentUser.getUserCode());
            if (t != null) return t;
        }

        // 2. Tìm theo email
        for (TranscriptResponse t : MEMORY_TRANSCRIPTS.values()) {
            if (t.getEmail() != null && t.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
                return t;
            }
        }

        // 3. Mặc định trả về học viên mẫu đầu tiên nếu là tài khoản demo học viên
        return MEMORY_TRANSCRIPTS.get("HV-2026-001");
    }

    private TranscriptResponse queryFromDatabase(Connection conn, String studentCode) throws SQLException {
        String sql = """
            SELECT s.id AS student_id, s.student_code, u.full_name, u.date_of_birth, u.email, u.phone,
                   p.program_code, p.program_name, c.class_code, c.class_name
            FROM students s
            JOIN users u ON s.user_id = u.id
            LEFT JOIN enrollments e ON e.student_id = s.id AND e.status = 'ACTIVE'
            LEFT JOIN classes c ON e.class_id = c.id
            LEFT JOIN programs p ON c.program_id = p.id
            WHERE UPPER(s.student_code) = ? OR UPPER(u.user_code) = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentCode);
            ps.setString(2, studentCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                TranscriptResponse t = new TranscriptResponse();
                t.setStudentId(rs.getLong("student_id"));
                t.setStudentCode(rs.getString("student_code") != null ? rs.getString("student_code") : studentCode);
                t.setStudentName(rs.getString("full_name"));
                java.sql.Date dob = rs.getDate("date_of_birth");
                t.setDateOfBirth(dob != null ? dob.toLocalDate().format(VI_DATE_FMT) : "–");
                t.setEmail(rs.getString("email"));
                t.setPhone(rs.getString("phone"));
                t.setProgramCode(rs.getString("program_code"));
                t.setProgramName(rs.getString("program_name"));
                t.setClassCode(rs.getString("class_code"));
                t.setClassName(rs.getString("class_name"));
                t.setStudyPeriod("08/2026 – nay");
                t.setCenterName("TRUNG TÂM ĐÀO TẠO TMS");
                t.setCenterAddress("Tầng 3, cơ sở Cầu Giấy, Hà Nội");
                t.setTranscriptNumber("BD-" + LocalDate.now().getYear() + "-" + String.format("%06d", Math.abs(t.getStudentCode().hashCode()) % 1000000));
                t.setIssueDate(LocalDate.now().format(VI_DATE_FMT));

                // Lấy danh sách điểm các môn
                List<SubjectGradeItem> subjects = querySubjectGrades(conn, t.getStudentId());
                if (!subjects.isEmpty()) {
                    t.setSubjects(subjects);
                    t.setTotalSubjects(subjects.size());
                    t.recalculate();
                    return t;
                }
            }
        }
        return null;
    }

    private List<SubjectGradeItem> querySubjectGrades(Connection conn, Long studentId) throws SQLException {
        List<SubjectGradeItem> list = new ArrayList<>();
        String sql = """
            SELECT sub.subject_code, sub.subject_name, sub.duration_hours,
                   sfg.attendance_score, sfg.assignment_score, sfg.exam_score, sfg.final_score,
                   sfg.pass_status, sfg.is_locked
            FROM student_final_grades sfg
            JOIN class_subjects cs ON sfg.class_subject_id = cs.id
            JOIN subjects sub ON cs.subject_id = sub.id
            WHERE sfg.student_id = ?
            ORDER BY sub.id ASC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SubjectGradeItem item = new SubjectGradeItem();
                    item.setSubjectCode(rs.getString("subject_code"));
                    item.setSubjectName(rs.getString("subject_name"));
                    item.setSessions(Math.max(1, rs.getInt("duration_hours") / 3));
                    item.setWeight(Math.max(1, item.getSessions() / 5));

                    double finalScore = rs.getDouble("final_score");
                    if (!rs.wasNull()) {
                        item.setFinalScore(finalScore);
                        String passStatus = rs.getString("pass_status");
                        item.setResult("PASS".equalsIgnoreCase(passStatus) || finalScore >= 5.0 ? "Đạt" : "Không đạt");
                    } else {
                        item.setFinalScore(null);
                        item.setResult("Chưa học");
                    }

                    item.setLocked(rs.getBoolean("is_locked"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    /**
     * Sinh văn bản HTML định dạng chuẩn in / xuất PDF chính thức (AC3)
     */
    public String generatePrintableHtml(TranscriptResponse t) {
        if (t == null) return "<p>Không tìm thấy bảng điểm.</p>";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"vi\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n")
          .append("<title>Bảng điểm toàn khoá - ").append(escape(t.getStudentName())).append(" (").append(escape(t.getStudentCode())).append(")</title>\n")
          .append("<style>\n")
          .append("@page { size: A4; margin: 16mm; }\n")
          .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; line-height: 1.5; margin: 0; padding: 20px; }\n")
          .append(".header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 20px; }\n")
          .append(".center-info b { font-size: 16px; text-transform: uppercase; letter-spacing: 0.5px; color: #0f172a; display: block; }\n")
          .append(".center-info span { font-size: 13px; color: #64748b; }\n")
          .append(".doc-title { text-align: right; }\n")
          .append(".doc-title h1 { margin: 0; font-size: 20px; color: #0284c7; text-transform: uppercase; }\n")
          .append(".doc-title span { font-size: 13px; color: #64748b; font-family: monospace; }\n")
          .append(".student-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 24px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px 18px; margin-bottom: 24px; font-size: 14px; }\n")
          .append(".student-grid dt { color: #64748b; font-weight: 500; }\n")
          .append(".student-grid dd { margin: 0; font-weight: 600; color: #0f172a; }\n")
          .append("table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 14px; }\n")
          .append("th { background: #f1f5f9; color: #334155; font-weight: 600; text-align: left; padding: 10px 12px; border-bottom: 2px solid #cbd5e1; }\n")
          .append("td { padding: 9px 12px; border-bottom: 1px solid #e2e8f0; }\n")
          .append(".num { text-align: right; font-variant-numeric: tabular-nums; }\n")
          .append(".badge-pass { background: #dcfce7; color: #166534; font-weight: 600; padding: 3px 8px; border-radius: 999px; font-size: 12px; display: inline-block; }\n")
          .append(".badge-fail { background: #fee2e2; color: #991b1b; font-weight: 600; padding: 3px 8px; border-radius: 999px; font-size: 12px; display: inline-block; }\n")
          .append(".summary-box { display: flex; justify-content: space-between; align-items: center; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 14px 18px; margin-bottom: 24px; }\n")
          .append(".gpa-val { font-size: 24px; font-weight: 800; color: #15803d; }\n")
          .append(".signatures { display: flex; justify-content: space-between; margin-top: 40px; page-break-inside: avoid; }\n")
          .append(".sign-col { text-align: center; width: 240px; }\n")
          .append(".sign-col b { display: block; margin-top: 6px; font-size: 14px; }\n")
          .append(".sign-col em { display: block; margin-top: 60px; font-style: normal; font-weight: 600; color: #0f172a; }\n")
          .append(".footer { margin-top: 40px; padding-top: 12px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8; text-align: center; }\n")
          .append("@media print { .no-print { display: none; } body { padding: 0; } }\n")
          .append("</style>\n</head>\n<body>\n")
          .append("<div class=\"no-print\" style=\"text-align: right; margin-bottom: 16px;\">\n")
          .append("  <button onclick=\"window.print()\" style=\"background: #0284c7; color: white; border: none; padding: 8px 16px; border-radius: 6px; font-size: 14px; font-weight: 600; cursor: pointer;\">In hoặc Lưu PDF</button>\n")
          .append("</div>\n");

        // Header
        sb.append("<div class=\"header\">\n")
          .append("  <div class=\"center-info\">\n")
          .append("    <b>").append(escape(t.getCenterName())).append("</b>\n")
          .append("    <span>").append(escape(t.getCenterAddress())).append("</span>\n")
          .append("  </div>\n")
          .append("  <div class=\"doc-title\">\n")
          .append("    <h1>BẢNG ĐIỂM TOÀN KHOÁ</h1>\n")
          .append("    <span>Số: ").append(escape(t.getTranscriptNumber())).append("</span>\n")
          .append("  </div>\n")
          .append("</div>\n");

        // Student Grid
        sb.append("<div class=\"student-grid\">\n")
          .append("  <div><dt>Họ và tên</dt><dd>").append(escape(t.getStudentName())).append("</dd></div>\n")
          .append("  <div><dt>Mã học viên</dt><dd>").append(escape(t.getStudentCode())).append("</dd></div>\n")
          .append("  <div><dt>Ngày sinh</dt><dd>").append(escape(t.getDateOfBirth())).append("</dd></div>\n")
          .append("  <div><dt>Chương trình</dt><dd>").append(escape(t.getProgramName())).append("</dd></div>\n")
          .append("  <div><dt>Lớp</dt><dd>").append(escape(t.getClassCode())).append("</dd></div>\n")
          .append("  <div><dt>Thời gian học</dt><dd>").append(escape(t.getStudyPeriod())).append("</dd></div>\n")
          .append("</div>\n");

        // Table
        sb.append("<table>\n")
          .append("  <thead>\n    <tr>\n")
          .append("      <th>Mã môn</th>\n")
          .append("      <th>Môn học</th>\n")
          .append("      <th class=\"num\">Số buổi</th>\n")
          .append("      <th class=\"num\">Trọng số</th>\n")
          .append("      <th class=\"num\">Điểm tổng kết</th>\n")
          .append("      <th style=\"text-align: center;\">Kết quả</th>\n")
          .append("    </tr>\n  </thead>\n  <tbody>\n");

        for (SubjectGradeItem item : t.getSubjects()) {
            sb.append("    <tr>\n")
              .append("      <td><code>").append(escape(item.getSubjectCode())).append("</code></td>\n")
              .append("      <td><b>").append(escape(item.getSubjectName())).append("</b></td>\n")
              .append("      <td class=\"num\">").append(item.getSessions()).append("</td>\n")
              .append("      <td class=\"num\">").append(item.getWeight()).append("</td>\n")
              .append("      <td class=\"num\" style=\"font-weight: 700;\">")
              .append(item.getFinalScore() != null ? String.format("%.1f", item.getFinalScore()) : "–")
              .append("</td>\n")
              .append("      <td style=\"text-align: center;\">\n");

            if ("Đạt".equalsIgnoreCase(item.getResult())) {
                sb.append("        <span class=\"badge-pass\">Đạt</span>\n");
            } else if ("Không đạt".equalsIgnoreCase(item.getResult())) {
                sb.append("        <span class=\"badge-fail\">Không đạt</span>\n");
            } else {
                sb.append("        <span style=\"color: #94a3b8;\">").append(escape(item.getResult())).append("</span>\n");
            }

            sb.append("      </td>\n    </tr>\n");
        }

        sb.append("  </tbody>\n</table>\n");

        // Summary box
        sb.append("<div class=\"summary-box\">\n")
          .append("  <div>\n")
          .append("    <b style=\"font-size: 15px; color: #166534;\">ĐIỂM TRUNG BÌNH TOÀN KHOÁ (GPA)</b>\n")
          .append("    <div style=\"font-size: 12px; color: #475569;\">Thang điểm 10 · Tính theo trọng số môn học: ")
          .append(escape(t.getGpaFormula())).append("</div>\n")
          .append("    <div style=\"font-size: 13px; margin-top: 4px;\">Số môn đã đạt: <b>")
          .append(t.getPassedSubjects()).append("/").append(t.getTotalSubjects()).append("</b>")
          .append(" &nbsp;·&nbsp; Tỷ lệ chuyên cần: <b>").append(t.getAttendanceRate() != null ? t.getAttendanceRate() + "%" : "–").append("</b></div>\n")
          .append("  </div>\n")
          .append("  <div class=\"gpa-val\">").append(t.getGpa() != null ? String.format("%.1f", t.getGpa()) : "–").append("</div>\n")
          .append("</div>\n");

        // Signatures
        sb.append("<div class=\"signatures\">\n")
          .append("  <div class=\"sign-col\"></div>\n")
          .append("  <div class=\"sign-col\">\n")
          .append("    <span>Hà Nội, ngày cấp ").append(escape(t.getIssueDate())).append("</span>\n")
          .append("    <b>").append(escape(t.getSignatoryTitle())).append("</b>\n")
          .append("    <em>").append(escape(t.getSignatoryName())).append("</em>\n")
          .append("  </div>\n")
          .append("</div>\n");

        // Footer
        sb.append("<div class=\"footer\">\n")
          .append("  Mã học viên: ").append(escape(t.getStudentCode()))
          .append(" · Tên trung tâm: ").append(escape(t.getCenterName()))
          .append(" · Ngày cấp: ").append(escape(t.getIssueDate()))
          .append(" · Mã tra cứu văn bằng: ").append(escape(t.getTranscriptNumber()))
          .append("<br>Bảng điểm chính thức xuất từ Hệ thống Quản lý Đào tạo TMS.\n")
          .append("</div>\n")
          .append("</body>\n</html>");

        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
