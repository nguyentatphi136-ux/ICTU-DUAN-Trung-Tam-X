package com.ems.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO yêu cầu lưu / sửa bảng điểm danh của một buổi học (S4-07):
 * Hỗ trợ lưu danh sách học viên, nút đánh dấu tất cả có mặt, và lý do sửa buổi quá khứ.
 */
public class SaveAttendanceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String reason; // Lý do sửa điểm danh (bắt buộc khi sửa buổi đã qua)
    private String defaultStatus; // Ví dụ: 'Có mặt' nếu dùng nút đánh dấu tất cả có mặt
    private List<AttendanceItem> items = new ArrayList<>();

    public SaveAttendanceRequest() {}

    public SaveAttendanceRequest(Long sessionId, String reason, List<AttendanceItem> items) {
        this.sessionId = sessionId;
        this.reason = reason;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDefaultStatus() { return defaultStatus; }
    public void setDefaultStatus(String defaultStatus) { this.defaultStatus = defaultStatus; }

    public List<AttendanceItem> getItems() { return items; }
    public void setItems(List<AttendanceItem> items) { this.items = items != null ? items : new ArrayList<>(); }

    public static class AttendanceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long studentId;
        private String studentCode;
        private String status; // 'Có mặt', 'Đi muộn', 'Vắng có phép', 'Vắng không phép'
        private String notes;

        public AttendanceItem() {}

        public AttendanceItem(Long studentId, String studentCode, String status, String notes) {
            this.studentId = studentId;
            this.studentCode = studentCode;
            this.status = status;
            this.notes = notes;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }

        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
