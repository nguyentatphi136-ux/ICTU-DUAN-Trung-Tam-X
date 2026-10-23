package com.ems.model;

import java.io.Serializable;

/**
 * Model ghi nhận nhật ký thao tác sửa điểm (Jira IDTTX-273 / S6-01 & S6-11):
 * "Mọi lần sửa điểm được ghi nhật ký kèm người sửa và thời điểm"
 * Đồng bộ với chuẩn Audit Trail của hệ thống EMS.
 */
public class GradeAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String timestamp;     // yyyy-MM-dd HH:mm:ss hoặc ISO
    private String actorName;     // Người thực hiện (VD: VU DUC LONG)
    private String role;          // Vai trò (VD: Giảng viên)
    private String entity;        // Thực thể: "Điểm"
    private String action;        // "Chấm lần đầu" / "Sửa điểm" / "Công bố điểm"
    private String target;        // VD: "HV260101 · WEB-K15 · Bài 14"
    private Double scoreBefore;   // Điểm trước sửa (null nếu chấm lần đầu)
    private Double scoreAfter;    // Điểm sau sửa
    private String reason;        // Lý do sửa điểm (bắt buộc khi sửa điểm đã công bố)
    private String logText;       // Chuỗi tóm tắt hiển thị trên giao diện

    public GradeAuditLog() {
        this.entity = "Điểm";
        this.role = "Giảng viên";
    }

    public GradeAuditLog(Long id, String timestamp, String actorName, String action,
                         Double scoreBefore, Double scoreAfter, String reason, String logText) {
        this.id = id;
        this.timestamp = timestamp;
        this.actorName = actorName;
        this.role = "Giảng viên";
        this.entity = "Điểm";
        this.action = action;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.reason = reason;
        this.logText = logText;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Double getScoreBefore() { return scoreBefore; }
    public void setScoreBefore(Double scoreBefore) { this.scoreBefore = scoreBefore; }

    public Double getScoreAfter() { return scoreAfter; }
    public void setScoreAfter(Double scoreAfter) { this.scoreAfter = scoreAfter; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getLogText() { return logText; }
    public void setLogText(String logText) { this.logText = logText; }
}
