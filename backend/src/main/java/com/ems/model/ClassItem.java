package com.ems.model;

import java.io.Serializable;

/**
 * Model đại diện lớp học trong hệ thống EMS
 * Phục vụ nghiệp vụ chuyển lớp (S4-06): kiểm tra cùng chương trình, đang mở và còn chỗ
 */
public class ClassItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String program;
    private int currentSize;
    private int maxCapacity;
    private String status; // "PLANNING", "IN_PROGRESS", "FINISHED"
    private String startDate;

    public ClassItem() {
    }

    public ClassItem(String code, String name, String program, int currentSize, int maxCapacity, String status, String startDate) {
        this.code = code;
        this.name = name;
        this.program = program;
        this.currentSize = currentSize;
        this.maxCapacity = maxCapacity;
        this.status = status;
        this.startDate = startDate;
    }

    public int getAvailableSeats() {
        return Math.max(0, maxCapacity - currentSize);
    }

    public boolean hasAvailableSeats() {
        return currentSize < maxCapacity;
    }

    public boolean isOpen() {
        return "PLANNING".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
}
