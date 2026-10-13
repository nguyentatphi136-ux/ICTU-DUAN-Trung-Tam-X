package com.ems.dto;

import java.io.Serializable;

public class CloneLessonsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer sourceSubjectId;
    private String sourceSubjectCode;

    public CloneLessonsRequest() {}

    public Integer getSourceSubjectId() {
        return sourceSubjectId;
    }

    public void setSourceSubjectId(Integer sourceSubjectId) {
        this.sourceSubjectId = sourceSubjectId;
    }

    public String getSourceSubjectCode() {
        return sourceSubjectCode;
    }

    public void setSourceSubjectCode(String sourceSubjectCode) {
        this.sourceSubjectCode = sourceSubjectCode;
    }
}
