package com.ems.dto;

import java.io.Serializable;
import java.util.List;

public class ReorderSubjectsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Integer> subjectIds;

    public ReorderSubjectsRequest() {}

    public List<Integer> getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(List<Integer> subjectIds) {
        this.subjectIds = subjectIds;
    }
}
