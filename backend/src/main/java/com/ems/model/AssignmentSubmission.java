package com.ems.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model biểu diễn bài nộp của học viên (S5-07 & S6-01):
 * Hỗ trợ lưu trữ DB nộp bài, tải lên nhiều file (tối đa 20MB/file), liên kết repo,
 * phiên bản nộp và chấm điểm, công bố điểm của giảng viên.
 */
public class AssignmentSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L; // 20 MB

    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String studentEmail;
    private String classCode;
    private Integer version = 1;
    private String repoUrl;
    private String submissionNote;
    private Boolean isLatest = true;
    private boolean isLate = false;
    private double lateHours = 0.0;
    private String status = "SUBMITTED";
    private String state = "Chưa chấm";
    private String resubmitReason;
    private LocalDateTime newDeadline;
    private LocalDateTime submittedAt = LocalDateTime.now();
    private SubmissionGrade grade;
    private List<SubmissionVersion> versions = new ArrayList<>();
    private List<SubmissionFile> files = new ArrayList<>();

    public AssignmentSubmission() {
    }

    public AssignmentSubmission(Long assignmentId, Long studentId, String repoUrl, String submissionNote) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.repoUrl = repoUrl;
        this.submissionNote = submissionNote;
    }

    public AssignmentSubmission(int id, int assignmentId, String studentCode, String studentName,
                                String studentEmail, String classCode, boolean isLate, double lateHours) {
        this.id = (long) id;
        this.assignmentId = (long) assignmentId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.classCode = classCode;
        this.isLate = isLate;
        this.lateHours = lateHours;
        this.state = "Chưa chấm";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public void setId(int id) { this.id = (long) id; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = (long) assignmentId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public void setStudentId(int studentId) { this.studentId = (long) studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getRepoUrl() {
        SubmissionVersion v = getLatestVersion();
        if (v != null && v.getRepoUrl() != null) return v.getRepoUrl();
        return repoUrl;
    }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getSubmissionNote() {
        SubmissionVersion v = getLatestVersion();
        if (v != null && v.getNote() != null) return v.getNote();
        return submissionNote;
    }
    public void setSubmissionNote(String submissionNote) { this.submissionNote = submissionNote; }

    public String getStudentNote() {
        return getSubmissionNote();
    }

    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }

    public boolean isLate() { return isLate; }
    public void setLate(boolean late) { this.isLate = late; }
    public Boolean getIsLate() { return isLate; }
    public void setIsLate(Boolean isLate) { this.isLate = isLate != null && isLate; }

    public double getLateHours() { return lateHours; }
    public void setLateHours(double lateHours) { this.lateHours = lateHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getState() {
        updateState();
        return state;
    }
    public void setState(String state) { this.state = state; }

    public String getResubmitReason() { return resubmitReason; }
    public void setResubmitReason(String resubmitReason) { this.resubmitReason = resubmitReason; }

    public LocalDateTime getNewDeadline() { return newDeadline; }
    public void setNewDeadline(LocalDateTime newDeadline) { this.newDeadline = newDeadline; }

    public String getSubmittedAt() {
        SubmissionVersion v = getLatestVersion();
        if (v != null && v.getSubmittedAt() != null) return v.getSubmittedAt();
        return submittedAt != null ? submittedAt.toString() : "";
    }

    public LocalDateTime getSubmittedAtDateTime() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

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
        this.versions.add(0, version);
    }

    public SubmissionVersion getLatestVersion() {
        if (versions != null && !versions.isEmpty()) {
            return versions.get(0);
        }
        return null;
    }

    public int getVersionNo() {
        SubmissionVersion v = getLatestVersion();
        return v != null ? v.getVersionNo() : (version != null ? version : 1);
    }

    public List<SubmissionFile> getFiles() {
        SubmissionVersion v = getLatestVersion();
        if (v != null && v.getFiles() != null && !v.getFiles().isEmpty()) {
            return v.getFiles();
        }
        return files;
    }

    public void setFiles(List<SubmissionFile> files) { this.files = files; }

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

    /** Lớp con tương thích ngược với code S5-07 */
    public static class SubmissionFile extends com.ems.model.SubmissionFile {
        private static final long serialVersionUID = 1L;

        public SubmissionFile() {
            super();
        }

        public SubmissionFile(String fileName, String fileUrl, Long fileSizeBytes) {
            super(fileName, fileUrl, fileSizeBytes);
        }

        public SubmissionFile(String name, long sizeBytes, String formattedSize, String fileType, String previewContent) {
            super(name, sizeBytes, formattedSize, fileType, previewContent);
        }
    }
}
