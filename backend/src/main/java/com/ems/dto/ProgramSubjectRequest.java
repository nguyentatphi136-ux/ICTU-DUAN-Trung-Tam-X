package com.ems.dto;

import java.io.Serializable;

public class ProgramSubjectRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer subjectId;
    private Integer orderIndex;
    private Integer prerequisiteSubjectId;

    public ProgramSubjectRequest() {}

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getPrerequisiteSubjectId() {
        return prerequisiteSubjectId;
    }

    public void setPrerequisiteSubjectId(Integer prerequisiteSubjectId) {
        this.prerequisiteSubjectId = prerequisiteSubjectId;
    }
}
