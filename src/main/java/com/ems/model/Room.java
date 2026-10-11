package com.ems.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity Room đại diện cho phòng học (Vật lý hoặc Trực tuyến)
 * Đáp ứng User Story S3-09 (EP-04: Lớp học & Thời khóa biểu)
 */
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_PHYSICAL = "PHYSICAL";
    public static final String TYPE_ONLINE = "ONLINE";

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";

    private Long id;
    private String roomCode;
    private String roomName;
    private String roomType; // PHYSICAL hoặc ONLINE
    private Integer capacity; // Sức chứa tối đa (cho phòng vật lý)
    private String location; // Vị trí phòng (tầng, tòa nhà...)
    private String onlineMeetingUrl; // Đường dẫn học trực tuyến (Google Meet, Zoom...)
    private String description; // Mô tả phòng
    private Boolean isActive; // Trạng thái hoạt động
    private String status; // AVAILABLE, MAINTENANCE
    private Date createdAt;
    private Date updatedAt;

    public Room() {
        this.isActive = true;
        this.status = STATUS_AVAILABLE;
        this.roomType = TYPE_PHYSICAL;
    }

    public Room(Long id, String roomCode, String roomName, String roomType, Integer capacity,
                String location, String onlineMeetingUrl, String description, Boolean isActive) {
        this.id = id;
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.roomType = roomType;
        this.capacity = capacity;
        this.location = location;
        this.onlineMeetingUrl = onlineMeetingUrl;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
        this.status = Boolean.TRUE.equals(this.isActive) ? STATUS_AVAILABLE : STATUS_MAINTENANCE;
    }

    public boolean isPhysical() {
        return TYPE_PHYSICAL.equalsIgnoreCase(this.roomType);
    }

    public boolean isOnline() {
        return TYPE_ONLINE.equalsIgnoreCase(this.roomType);
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
