package com.ems.dto;

import com.ems.exception.ValidationException;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("HH:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm")
    );

    private Long scheduleId;
    private Long classId;
    private Long classSubjectId;
    private Integer subjectLessonId;
    private Integer sessionNumber = 1;
    private Long instructorId;
    private Integer roomId;
    private Integer classroomId;
    private String startDateTime;
    private String endDateTime;
    private String sessionDate;
    private String startTime;
    private String endTime;
    private String status = "SCHEDULED";
    private boolean overrideConflict;
    private String overrideReason;
    private String cancelReason;

    public ScheduleRequest() {}

    public ScheduleRequest(Long classId, Long instructorId, Integer roomId,
                           String startDateTime, String endDateTime) {
        this.classId = classId;
        this.instructorId = instructorId;
        this.roomId = roomId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public Integer getResolvedRoomId() {
        return roomId != null ? roomId : classroomId;
    }

    public LocalDate getResolvedDate() {
        if (sessionDate != null && !sessionDate.trim().isEmpty()) {
            try {
                return LocalDate.parse(sessionDate.trim());
            } catch (DateTimeParseException ignored) {}
        }
        if (startDateTime != null && !startDateTime.trim().isEmpty()) {
            LocalDateTime dt = parseDateTime(startDateTime.trim());
            if (dt != null) return dt.toLocalDate();
        }
        return null;
    }

    public LocalTime getResolvedStartTime() {
        if (startTime != null && !startTime.trim().isEmpty()) {
            LocalTime t = parseTime(startTime.trim());
            if (t != null) return t;
        }
        if (startDateTime != null && !startDateTime.trim().isEmpty()) {
            LocalDateTime dt = parseDateTime(startDateTime.trim());
            if (dt != null) return dt.toLocalTime();
        }
        return null;
    }

    public LocalTime getResolvedEndTime() {
        if (endTime != null && !endTime.trim().isEmpty()) {
            LocalTime t = parseTime(endTime.trim());
            if (t != null) return t;
        }
        if (endDateTime != null && !endDateTime.trim().isEmpty()) {
            LocalDateTime dt = parseDateTime(endDateTime.trim());
            if (dt != null) return dt.toLocalTime();
        }
        return null;
    }

    public LocalDateTime getResolvedStartDateTime() {
        LocalDate date = getResolvedDate();
        LocalTime time = getResolvedStartTime();
        return (date != null && time != null) ? LocalDateTime.of(date, time) : null;
    }

    public LocalDateTime getResolvedEndDateTime() {
        LocalDate date = getResolvedDate();
        LocalTime time = getResolvedEndTime();
        return (date != null && time != null) ? LocalDateTime.of(date, time) : null;
    }

    public void validate() {
        if (classId == null || classId <= 0) {
            throw new ValidationException("Mã lớp học (classId) không hợp lệ hoặc bị thiếu.");
        }
        if (instructorId == null || instructorId <= 0) {
            throw new ValidationException("Mã giảng viên (instructorId) không hợp lệ hoặc bị thiếu.");
        }
        Integer resolvedRoom = getResolvedRoomId();
        if (resolvedRoom == null || resolvedRoom <= 0) {
            throw new ValidationException("Mã phòng học (roomId/classroomId) không hợp lệ hoặc bị thiếu.");
        }

        LocalDate date = getResolvedDate();
        if (date == null) {
            throw new ValidationException("Ngày học không hợp lệ hoặc không xác định được từ yêu cầu.");
        }

        LocalTime start = getResolvedStartTime();
        LocalTime end = getResolvedEndTime();
        if (start == null) {
            throw new ValidationException("Thời gian bắt đầu (startTime/startDateTime) không hợp lệ.");
        }
        if (end == null) {
            throw new ValidationException("Thời gian kết thúc (endTime/endDateTime) không hợp lệ.");
        }

        if (!end.isAfter(start)) {
            throw new ValidationException("Thời gian kết thúc phải lớn hơn thời gian bắt đầu.");
        }
    }

    private LocalDateTime parseDateTime(String text) {
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private LocalTime parseTime(String text) {
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // Getters and Setters
    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
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
        return sessionNumber != null ? sessionNumber : 1;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Integer classroomId) {
        this.classroomId = classroomId;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOverrideConflict() {
        return overrideConflict;
    }

    public void setOverrideConflict(boolean overrideConflict) {
        this.overrideConflict = overrideConflict;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    private String reason;

    public String getReason() {
        return reason != null ? reason : cancelReason;
    }

    public void setReason(String reason) {
        this.reason = reason;
        if (this.cancelReason == null) {
            this.cancelReason = reason;
        }
    }

    public String getCancelReason() {
        return cancelReason != null ? cancelReason : reason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
        if (this.reason == null) {
            this.reason = cancelReason;
        }
    }
}
