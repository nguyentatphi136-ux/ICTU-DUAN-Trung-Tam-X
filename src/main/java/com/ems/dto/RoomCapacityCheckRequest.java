package com.ems.dto;

import java.io.Serializable;

/**
 * DTO yêu cầu kiểm tra sức chứa phòng khi xếp lớp
 */
public class RoomCapacityCheckRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer studentCount;
    private Long classId;

    public RoomCapacityCheckRequest() {}

    public RoomCapacityCheckRequest(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public Integer getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }
}
