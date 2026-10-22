package com.ems.dto;

/**
 * DTO trả về thông tin buổi học trong danh sách thời khóa biểu
 * EP-04: Xem thời khóa biểu dạng lịch tuần và tháng
 */
public class ClassSessionResponse {
    private Long id;
    private Long classId;
    private String classCode;
    private String className;
    private Long classSubjectId;
    private Integer subjectId;
    private String subjectCode;
    private String subjectName;
    private Long instructorId;
    private String instructorName;
    private Integer classroomId;
    private String roomCode;
    private String roomName;
    private String sessionDate;   // yyyy-MM-dd
    private String startTime;     // HH:mm
    private String endTime;       // HH:mm
    private String startDateTime; // yyyy-MM-dd'T'HH:mm:ss – tiện Frontend
    private String endDateTime;   // yyyy-MM-dd'T'HH:mm:ss
    private String status;
    private Integer sessionNumber;
    private boolean hasConflict;

    public ClassSessionResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Long getClassSubjectId() { return classSubjectId; }
    public void setClassSubjectId(Long classSubjectId) { this.classSubjectId = classSubjectId; }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public Integer getClassroomId() { return classroomId; }
    public void setClassroomId(Integer classroomId) { this.classroomId = classroomId; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStartDateTime() { return startDateTime; }
    public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }

    public String getEndDateTime() { return endDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(Integer sessionNumber) { this.sessionNumber = sessionNumber; }

    public boolean isHasConflict() { return hasConflict; }
    public void setHasConflict(boolean hasConflict) { this.hasConflict = hasConflict; }
}
