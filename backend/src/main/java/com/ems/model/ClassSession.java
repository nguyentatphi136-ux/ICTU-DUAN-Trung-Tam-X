package com.ems.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClassSession implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long id;
    private Long classId;
    private String classCode;
    private String className;
    private Long classSubjectId;
    private Integer subjectLessonId;
    private Integer sessionNumber = 1;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer classroomId;
    private String roomCode;
    private String roomName;
    private Long instructorId;
    private String instructorName;
    private String instructorEmail;
    private String status = "SCHEDULED";
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ClassSession() {}

    public ClassSession(Long id, Long classId, Long instructorId, Integer classroomId,
                        LocalDate sessionDate, LocalTime startTime, LocalTime endTime, String status) {
        this.id = id;
        this.classId = classId;
        this.instructorId = instructorId;
        this.classroomId = classroomId;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : "SCHEDULED";
    }

    public LocalDateTime getStartDateTime() {
        if (sessionDate == null || startTime == null) return null;
        return LocalDateTime.of(sessionDate, startTime);
    }

    public LocalDateTime getEndDateTime() {
        if (sessionDate == null || endTime == null) return null;
        return LocalDateTime.of(sessionDate, endTime);
    }

    public String getFormattedStartDateTime() {
        LocalDateTime dt = getStartDateTime();
        return dt != null ? dt.format(ISO_FORMATTER) : null;
    }

    public String getFormattedEndDateTime() {
        LocalDateTime dt = getEndDateTime();
        return dt != null ? dt.format(ISO_FORMATTER) : null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Long getClassSubjectId() {
        return classSubjectId;
    }

    public void setClassSubjectId(Long classSubjectId) {
        this.classSubjectId = classSubjectId;
    }

    public Integer getSubjectLessonId() {
        return subjectLessonId;
    }

    public void setSubjectLessonId(Integer subjectLessonId) {
        this.subjectLessonId = subjectLessonId;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Integer classroomId) {
        this.classroomId = classroomId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getInstructorEmail() {
        return instructorEmail;
    }

    public void setInstructorEmail(String instructorEmail) {
        this.instructorEmail = instructorEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
