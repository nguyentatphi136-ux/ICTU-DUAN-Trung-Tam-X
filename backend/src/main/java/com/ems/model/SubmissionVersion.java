package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model biểu diễn một phiên bản bài nộp của học viên (S5-08 & S6-01):
 * Mỗi lần nộp tạo một phiên bản mới, không ghi đè bản cũ.
 * Màn hình chấm điểm S6-01 hiển thị phiên bản mới nhất của học viên.
 */
public class SubmissionVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    private int versionNo;              // Bản 1, Bản 2...
    private String submittedAt;         // Thời điểm nộp (yyyy-MM-dd HH:mm)
    private List<SubmissionFile> files = new ArrayList<>(); // Các tệp nộp
    private String repoUrl;             // Đường dẫn repository (GitHub, GitLab)
    private String note;                // Ghi chú của học viên cho giảng viên

    public SubmissionVersion() {
        this.versionNo = 1;
    }

    public SubmissionVersion(int versionNo, String submittedAt, List<SubmissionFile> files, String repoUrl, String note) {
        this.versionNo = versionNo;
        this.submittedAt = submittedAt;
        this.files = (files != null) ? files : new ArrayList<>();
        this.repoUrl = repoUrl;
        this.note = note;
    }

    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }

    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }

    public List<SubmissionFile> getFiles() { return files; }
    public void setFiles(List<SubmissionFile> files) { this.files = files; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
