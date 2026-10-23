package com.ems.dto;

import com.ems.model.AttendanceAuditLog;
import java.io.Serializable;
import java.util.*;

/**
 * DTO phản hồi chi tiết màn hình điểm danh một buổi học (S4-07):
 * Hỗ trợ tối ưu hóa hiển thị cho cả desktop và mobile (màn hình 360px).
 */
public class AttendanceSheetResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private Long classId;
    private String classCode;
    private String className;
    private int sessionNumber;
    private String sessionDate; // yyyy-MM-dd
    private String startTime;   // HH:mm
    private String endTime;     // HH:mm
    private String roomName;
    private String subjectName;
    private String lessonTopic;
    private String instructorName;

    private boolean isToday;
    private boolean isPast;
    private boolean isFuture;
    private boolean canEdit;
    private boolean isCancelled;
    private String cancelReason;

    private boolean saved;
    private String savedAt;
    private String savedBy;

    private Map<String, Integer> summary = new LinkedHashMap<>();
    private List<StudentAttendanceItem> students = new ArrayList<>();
    private List<AttendanceAuditLog> logs = new ArrayList<>();

    public AttendanceSheetResponse() {
        summary.put("Có mặt", 0);
        summary.put("Đi muộn", 0);
        summary.put("Vắng có phép", 0);
        summary.put("Vắng không phép", 0);
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public int getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(int sessionNumber) { this.sessionNumber = sessionNumber; }

    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getLessonTopic() { return lessonTopic; }
    public void setLessonTopic(String lessonTopic) { this.lessonTopic = lessonTopic; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public boolean isToday() { return isToday; }
    public void setToday(boolean today) { isToday = today; }

    public boolean isPast() { return isPast; }
    public void setPast(boolean past) { isPast = past; }

    public boolean isFuture() { return isFuture; }
    public void setFuture(boolean future) { isFuture = future; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public boolean isCancelled() { return isCancelled; }
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }

    public String getSavedAt() { return savedAt; }
    public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

    public String getSavedBy() { return savedBy; }
    public void setSavedBy(String savedBy) { this.savedBy = savedBy; }

    public Map<String, Integer> getSummary() { return summary; }
    public void setSummary(Map<String, Integer> summary) { this.summary = summary; }

    public List<StudentAttendanceItem> getStudents() { return students; }
    public void setStudents(List<StudentAttendanceItem> students) { this.students = students; }

    public List<AttendanceAuditLog> getLogs() { return logs; }
    public void setLogs(List<AttendanceAuditLog> logs) { this.logs = logs; }

    public void recalculateSummary() {
        int present = 0;
        int late = 0;
        int excused = 0;
        int unexcused = 0;
        for (StudentAttendanceItem item : students) {
            String st = item.getStatus();
            if (st == null) continue;
            if (st.contains("Có mặt") || st.equalsIgnoreCase("PRESENT")) present++;
            else if (st.contains("Đi muộn") || st.equalsIgnoreCase("LATE") || st.equalsIgnoreCase("Muộn")) late++;
            else if (st.contains("Vắng có phép") || st.equalsIgnoreCase("EXCUSED_ABSENCE") || st.equalsIgnoreCase("Có phép")) excused++;
            else if (st.contains("Vắng không phép") || st.equalsIgnoreCase("UNEXCUSED_ABSENCE") || st.equalsIgnoreCase("Vắng")) unexcused++;
        }
        summary.put("Có mặt", present);
        summary.put("Đi muộn", late);
        summary.put("Vắng có phép", excused);
        summary.put("Vắng không phép", unexcused);
    }

    public static class StudentAttendanceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long studentId;
        private String studentCode;
        private String studentName;
        private String status; // 'Có mặt', 'Đi muộn', 'Vắng có phép', 'Vắng không phép'
        private String notes;
        private boolean approvedLeave; // Có đơn xin phép được duyệt

        public StudentAttendanceItem() {}

        public StudentAttendanceItem(Long studentId, String studentCode, String studentName, String status, String notes) {
            this.studentId = studentId;
            this.studentCode = studentCode;
            this.studentName = studentName;
            this.status = status;
            this.notes = notes;
        }

        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }

        public String getStudentCode() { return studentCode; }
        public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public boolean isApprovedLeave() { return approvedLeave; }
        public void setApprovedLeave(boolean approvedLeave) { this.approvedLeave = approvedLeave; }
    }
}
