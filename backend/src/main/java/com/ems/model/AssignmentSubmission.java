package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện một bài nộp của học viên trong lớp (Jira IDTTX-273 - S6-01):
 * Quản lý học viên, phiên bản mới nhất, tệp nộp, liên kết repo, trạng thái và điểm số.
 */
public class AssignmentSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int assignmentId;
    private String studentCode;      // HV260101...
    private String studentName;      // NGUYEN MINH ANH...
    private String studentEmail;     // minhanh.ng@gmail.com
    private String classCode;        // WEB-K15
    private boolean isLate;          // true nếu nộp sau hạn
    private double lateHours;        // Số giờ trễ hạn (làm tròn)
    private String state;            // "Chưa nộp", "Chưa chấm", "Đã chấm - Nháp", "Đã công bố"
    private SubmissionGrade grade;   // Điểm số và nhận xét
    private List<SubmissionVersion> versions = new ArrayList<>();

    public AssignmentSubmission() {
        this.state = "Chưa chấm";
    }

    public AssignmentSubmission(int id, int assignmentId, String studentCode, String studentName,
                                String studentEmail, String classCode, boolean isLate, double lateHours) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.classCode = classCode;
        this.isLate = isLate;
        this.lateHours = lateHours;
        this.state = "Chưa chấm";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public boolean isLate() { return isLate; }
    public void setLate(boolean late) { isLate = late; }

    public double getLateHours() { return lateHours; }
    public void setLateHours(double lateHours) { this.lateHours = lateHours; }

    public String getState() {
        updateState();
        return state;
    }

    public void setState(String state) { this.state = state; }

    public SubmissionGrade getGrade() { return grade; }
    public void setGrade(SubmissionGrade grade) {
        this.grade = grade;
        updateState();
    }

    public List<SubmissionVersion> getVersions() { return versions; }
    public void setVersions(List<SubmissionVersion> versions) { this.versions = versions; }

    public void addVersion(SubmissionVersion version) {
        if (this.versions == null) {
            this.versions = new ArrayList<>();
        }
        // Thêm bản mới nhất vào đầu danh sách (index 0) theo quy ước dự án
        this.versions.add(0, version);
    }

    /**
     * Lấy phiên bản mới nhất (Bản được chấm điểm theo đặc tả S6-01)
     */
    public SubmissionVersion getLatestVersion() {
        if (versions != null && !versions.isEmpty()) {
            return versions.get(0);
        }
        return null;
    }

    // Các tiện ích Getter ánh xạ trực tiếp cho EL trong JSP
    public int getVersionNo() {
        SubmissionVersion v = getLatestVersion();
        return v != null ? v.getVersionNo() : 1;
    }

    public String getSubmittedAt() {
        SubmissionVersion v = getLatestVersion();
        return v != null ? v.getSubmittedAt() : "";
    }

    public List<SubmissionFile> getFiles() {
        SubmissionVersion v = getLatestVersion();
        return v != null ? v.getFiles() : new ArrayList<>();
    }

    public String getRepoUrl() {
        SubmissionVersion v = getLatestVersion();
        return v != null ? v.getRepoUrl() : null;
    }

    public String getStudentNote() {
        SubmissionVersion v = getLatestVersion();
        return v != null ? v.getNote() : null;
    }

    public void updateState() {
        if (grade == null) {
            this.state = "Chưa chấm";
        } else if (grade.isPublished()) {
            this.state = "Đã công bố";
        } else {
            this.state = "Đã chấm - Nháp";
        }
    }

    public boolean isPublished() {
        return grade != null && grade.isPublished();
    }
}
