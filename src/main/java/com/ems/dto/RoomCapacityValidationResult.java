package com.ems.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kết quả kiểm tra sức chứa phòng học khi xếp lớp (User Story S3-09 Acceptance Criteria 3)
 */
public class RoomCapacityValidationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean valid;
    private String code;
    private String message;
    private Integer studentCount;
    private Integer roomCapacity;
    private String roomType;
    private String roomCode;

    public RoomCapacityValidationResult() {}

    public static RoomCapacityValidationResult valid(String roomCode, String roomType, Integer studentCount, Integer roomCapacity, String message) {
        RoomCapacityValidationResult res = new RoomCapacityValidationResult();
        res.setValid(true);
        res.setCode("ROOM_CAPACITY_VALID");
        res.setMessage(message != null ? message : "Sức chứa phòng học phù hợp với sĩ số lớp.");
        res.setRoomCode(roomCode);
        res.setRoomType(roomType);
        res.setStudentCount(studentCount);
        res.setRoomCapacity(roomCapacity);
        return res;
    }

    public static RoomCapacityValidationResult exceeded(String roomCode, String roomType, Integer studentCount, Integer roomCapacity) {
        RoomCapacityValidationResult res = new RoomCapacityValidationResult();
        res.setValid(false);
        res.setCode("ROOM_CAPACITY_EXCEEDED");
        res.setMessage("Sĩ số lớp vượt quá sức chứa của phòng học.");
        res.setRoomCode(roomCode);
        res.setRoomType(roomType);
        res.setStudentCount(studentCount);
        res.setRoomCapacity(roomCapacity);
        return res;
    }

    public Map<String, Object> toDetailsMap() {
        Map<String, Object> details = new LinkedHashMap<>();
        if (studentCount != null) details.put("studentCount", studentCount);
        if (roomCapacity != null) details.put("roomCapacity", roomCapacity);
        if (roomCode != null) details.put("roomCode", roomCode);
        if (roomType != null) details.put("roomType", roomType);
        return details;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public Integer getRoomCapacity() {
        return roomCapacity;
    }

    public void setRoomCapacity(Integer roomCapacity) {
        this.roomCapacity = roomCapacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}
