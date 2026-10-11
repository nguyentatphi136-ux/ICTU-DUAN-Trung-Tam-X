package com.ems.dto;

import java.io.Serializable;

/**
 * DTO yêu cầu tạo phòng học mới (POST /api/rooms)
 */
public class CreateRoomRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roomCode;
    private String roomName;
    private String roomType; // PHYSICAL hoặc ONLINE
    private Integer capacity; // Bắt buộc > 0 đối với PHYSICAL
    private String location; // Vị trí phòng vật lý
    private String onlineMeetingUrl; // URL phòng học trực tuyến (bắt buộc với ONLINE)
    private String description;
    private Boolean isActive; // Mặc định true

    public CreateRoomRequest() {
        this.isActive = true;
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

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOnlineMeetingUrl() {
        return onlineMeetingUrl;
    }

    public void setOnlineMeetingUrl(String onlineMeetingUrl) {
        this.onlineMeetingUrl = onlineMeetingUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
