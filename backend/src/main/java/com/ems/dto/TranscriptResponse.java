package com.ems.dto;

import com.ems.model.SubjectGradeItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO dữ liệu Bảng điểm toàn khoá của học viên (S8-03):
 *  - AC1: Liệt kê toàn bộ môn đã học, điểm tổng kết, kết quả Đạt hoặc Không đạt
 *  - AC2: Điểm trung bình toàn khoá (GPA tính theo trọng số môn)
 *  - AC3: Hỗ trợ xuất PDF với mã học viên, tên trung tâm, ngày cấp và số quyết định/tra cứu
 */
public class TranscriptResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // Thông tin định danh học viên & lớp học
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String dateOfBirth; // dd/MM/yyyy
    private String email;
    private String phone;
    private String programCode;
    private String programName;
    private String classCode;
    private String className;
    private String studyPeriod;

    // AC3: Thông tin chính thức của trung tâm phục vụ xuất PDF
    private String centerName = "TRUNG TÂM ĐÀO TẠO TMS";
    private String centerAddress = "Tầng 3, cơ sở Cầu Giấy, Hà Nội";
    private String transcriptNumber; // VD: BD-2026-100101
    private String issueDate;        // Ngày cấp bảng điểm: dd/MM/yyyy
    private String signatoryTitle = "Quản lý đào tạo";
    private String signatoryName = "LE HOANG NAM";

    // AC2: Điểm trung bình toàn khoá (GPA) & Thống kê
    private Double gpa;              // Thang điểm 10, làm tròn 1 chữ số thập phân
    private String gpaFormula;       // Công thức chi tiết để học viên đối soát
    private int totalSubjects;       // Tổng số môn trong chương trình
    private int passedSubjects;      // Số môn đã đạt
    private int failedSubjects;      // Số môn chưa đạt
    private int totalSessions;       // Tổng số buổi học
    private int completedSessions;   // Số buổi đã học
    private Integer attendanceRate;  // Tỷ lệ chuyên cần (%)
    private boolean isCompleted;     // Đủ điều kiện hoàn thành khoá học (đạt 100% môn)

    // AC1: Danh sách các môn học, điểm tổng kết, kết quả
    private List<SubjectGradeItem> subjects = new ArrayList<>();

    public TranscriptResponse() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getStudyPeriod() { return studyPeriod; }
    public void setStudyPeriod(String studyPeriod) { this.studyPeriod = studyPeriod; }

    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }

    public String getCenterAddress() { return centerAddress; }
    public void setCenterAddress(String centerAddress) { this.centerAddress = centerAddress; }

    public String getTranscriptNumber() { return transcriptNumber; }
    public void setTranscriptNumber(String transcriptNumber) { this.transcriptNumber = transcriptNumber; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getSignatoryTitle() { return signatoryTitle; }
    public void setSignatoryTitle(String signatoryTitle) { this.signatoryTitle = signatoryTitle; }

    public String getSignatoryName() { return signatoryName; }
    public void setSignatoryName(String signatoryName) { this.signatoryName = signatoryName; }

    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }

    public String getGpaFormula() { return gpaFormula; }
    public void setGpaFormula(String gpaFormula) { this.gpaFormula = gpaFormula; }

    public int getTotalSubjects() { return totalSubjects; }
    public void setTotalSubjects(int totalSubjects) { this.totalSubjects = totalSubjects; }

    public int getPassedSubjects() { return passedSubjects; }
    public void setPassedSubjects(int passedSubjects) { this.passedSubjects = passedSubjects; }

    public int getFailedSubjects() { return failedSubjects; }
    public void setFailedSubjects(int failedSubjects) { this.failedSubjects = failedSubjects; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public int getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }

    public Integer getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(Integer attendanceRate) { this.attendanceRate = attendanceRate; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public List<SubjectGradeItem> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectGradeItem> subjects) { this.subjects = subjects != null ? subjects : new ArrayList<>(); }

    /**
     * Tự động tính toán lại GPA, số môn đạt, tỷ lệ hoàn thành theo trọng số
     */
    public void recalculate() {
        if (subjects == null || subjects.isEmpty()) {
            this.gpa = null;
            this.passedSubjects = 0;
            this.failedSubjects = 0;
            this.isCompleted = false;
            return;
        }

        double weightedSum = 0;
        int totalWeight = 0;
        int passed = 0;
        int failed = 0;
        StringBuilder formula = new StringBuilder("(");
        boolean first = true;

        for (SubjectGradeItem item : subjects) {
            if ("Đạt".equalsIgnoreCase(item.getResult()) || (item.getFinalScore() != null && item.getFinalScore() >= 5.0)) {
                passed++;
                if (item.getResult() == null) item.setResult("Đạt");
            } else if (item.getFinalScore() != null) {
                failed++;
                if (item.getResult() == null) item.setResult("Không đạt");
            }

            if (item.getFinalScore() != null) {
                int w = item.getWeight() > 0 ? item.getWeight() : 1;
                weightedSum += item.getFinalScore() * w;
                totalWeight += w;

                if (!first) formula.append(" + ");
                formula.append(String.format("%.1f", item.getFinalScore())).append("×").append(w);
                first = false;
            }
        }

        this.passedSubjects = passed;
        this.failedSubjects = failed;
        this.isCompleted = (totalSubjects > 0 && passed == totalSubjects);

        if (totalWeight > 0) {
            double rawGpa = weightedSum / totalWeight;
            this.gpa = Math.round(rawGpa * 10.0) / 10.0;
            formula.append(") ÷ ").append(totalWeight).append(" = ").append(String.format("%.1f", this.gpa));
            this.gpaFormula = formula.toString();
        } else {
            this.gpa = null;
            this.gpaFormula = "";
        }
    }
}
