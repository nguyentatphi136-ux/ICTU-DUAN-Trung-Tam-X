package com.ems.model;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model kết quả chấm điểm của một bài nộp (S6-01):
 * - Nhập điểm trong khoảng 0 đến điểm tối đa, kèm nhận xét
 * - Điểm chỉ hiển thị cho học viên sau khi giảng viên bấm Công bố
 * - Mọi lần sửa điểm được ghi nhật ký kèm người sửa và thời điểm
 */
public class SubmissionGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double score;
    private String comment;
    private String gradedBy;
    private String gradedAt;
    private boolean published;
    private String publishedAt;
    private String publishedBy;
    private List<GradeAuditLog> auditLogs = new ArrayList<>();

    public SubmissionGrade() {
        this.published = false;
    }

    public SubmissionGrade(Double score, String comment, String gradedBy, String gradedAt, boolean published) {
        this.score = score;
        this.comment = comment;
        this.gradedBy = gradedBy;
        this.gradedAt = gradedAt;
        this.published = published;
    }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getFormattedScore() {
        if (score == null) return "–";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("0.#", symbols);
        return df.format(score);
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getGradedBy() { return gradedBy; }
    public void setGradedBy(String gradedBy) { this.gradedBy = gradedBy; }

    public String getGradedAt() { return gradedAt; }
    public void setGradedAt(String gradedAt) { this.gradedAt = gradedAt; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

    public List<GradeAuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<GradeAuditLog> auditLogs) { this.auditLogs = auditLogs; }

    public void addAuditLog(GradeAuditLog log) {
        if (this.auditLogs == null) {
            this.auditLogs = new ArrayList<>();
        }
        // Thêm vào đầu danh sách để hiển thị mới nhất trước
        this.auditLogs.add(0, log);
    }
}
