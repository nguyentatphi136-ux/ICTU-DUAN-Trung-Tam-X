package com.ems.model;

import java.io.Serializable;

public class Classroom implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String roomCode;
    private String roomName;
    private Integer capacity = 30;
    private String roomType = "PHYSICAL";
    private String meetingUrl;
    private String status = "AVAILABLE";

    public Classroom() {}

    public Classroom(Integer id, String roomCode, String roomName, Integer capacity, String roomType, String status) {
        this.id = id;
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.capacity = capacity;
        this.roomType = roomType;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
