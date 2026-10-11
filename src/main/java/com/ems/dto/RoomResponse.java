package com.ems.dto;

import com.ems.model.Room;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * DTO trả về thông tin phòng học cho Client
 */
public class RoomResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String roomCode;
    private String roomName;
    private String roomType;
    private Integer capacity;
    private String location;
    private String onlineMeetingUrl;
    private String description;
    private Boolean isActive;
    private String status;
    private String createdAt;
    private String updatedAt;

    public RoomResponse() {}

    public static RoomResponse fromEntity(Room room) {
        if (room == null) return null;
        RoomResponse resp = new RoomResponse();
        resp.setId(room.getId());
        resp.setRoomCode(room.getRoomCode());
        resp.setRoomName(room.getRoomName());
        resp.setRoomType(room.getRoomType());
        resp.setCapacity(room.getCapacity());
        resp.setLocation(room.getLocation());
        resp.setOnlineMeetingUrl(room.getOnlineMeetingUrl());
        resp.setDescription(room.getDescription());
        resp.setIsActive(room.getIsActive());
        resp.setStatus(room.getStatus());
        resp.setCreatedAt(formatDate(room.getCreatedAt()));
        resp.setUpdatedAt(formatDate(room.getUpdatedAt()));
        return resp;
    }

    private static String formatDate(Date date) {
        if (date == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
