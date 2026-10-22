package com.ems.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho Bài nộp của học viên (S5-07)
 * Bảng: assignment_submissions
 */
public class AssignmentSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L; // 20MB theo tiêu chí S5-07

    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Integer version;
    private String repoUrl;
    private String submissionNote;
    private Boolean isLatest;
    private Boolean isLate;
    private String status; // SUBMITTED, NEED_RESUBMIT, GRADED
    private String resubmitReason;
    private LocalDateTime newDeadline;
    private LocalDateTime submittedAt;

    private List<SubmissionFile> files = new ArrayList<>();

    public AssignmentSubmission() {
        this.version = 1;
        this.isLatest = true;
        this.isLate = false;
        this.status = "SUBMITTED";
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getSubmissionNote() { return submissionNote; }
    public void setSubmissionNote(String submissionNote) { this.submissionNote = submissionNote; }

    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }

    public Boolean getIsLate() { return isLate; }
    public void setIsLate(Boolean isLate) { this.isLate = isLate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResubmitReason() { return resubmitReason; }
    public void setResubmitReason(String resubmitReason) { this.resubmitReason = resubmitReason; }

    public LocalDateTime getNewDeadline() { return newDeadline; }
    public void setNewDeadline(LocalDateTime newDeadline) { this.newDeadline = newDeadline; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public List<SubmissionFile> getFiles() { return files; }
    public void setFiles(List<SubmissionFile> files) { this.files = files; }

    public static class SubmissionFile implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Long submissionId;
        private String fileName;
        private String fileUrl;
        private Long fileSizeBytes;

        public SubmissionFile() {}

        public SubmissionFile(String fileName, String fileUrl, Long fileSizeBytes) {
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.fileSizeBytes = fileSizeBytes;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getSubmissionId() { return submissionId; }
        public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    }
}
