package com.ems.dto;

import com.ems.model.ProgramSubject;

import java.io.Serializable;

public class ProgramSubjectResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer programId;
    private Integer subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer sessions;
    private Integer orderIndex;
    private Integer prerequisiteSubjectId;
    private String prerequisiteSubjectCode;
    private String prerequisiteSubjectName;

    public static ProgramSubjectResponse fromEntity(ProgramSubject entity) {
        if (entity == null) return null;
        ProgramSubjectResponse res = new ProgramSubjectResponse();
        res.id = entity.getId();
        res.programId = entity.getProgramId();
        res.subjectId = entity.getSubjectId();
        res.subjectCode = entity.getSubjectCode();
        res.subjectName = entity.getSubjectName();
        res.sessions = entity.getSessions();
        res.orderIndex = entity.getOrderIndex();
        res.prerequisiteSubjectId = entity.getPrerequisiteSubjectId();
        res.prerequisiteSubjectCode = entity.getPrerequisiteSubjectCode();
        res.prerequisiteSubjectName = entity.getPrerequisiteSubjectName();
        return res;
    }

    public Integer getId() {
        return id;
    }

    public Integer getProgramId() {
        return programId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public Integer getSessions() {
        return sessions;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public Integer getPrerequisiteSubjectId() {
        return prerequisiteSubjectId;
    }

    public String getPrerequisiteSubjectCode() {
        return prerequisiteSubjectCode;
    }

    public String getPrerequisiteSubjectName() {
        return prerequisiteSubjectName;
    }
}
