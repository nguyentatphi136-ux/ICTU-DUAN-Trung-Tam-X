package com.ems.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model Entity đại diện cho Môn học được gán vào Chương trình đào tạo (ProgramSubject)
 * User Story: S2-06 (EP-02)
 */
public class ProgramSubject implements Serializable {
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
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ProgramSubject() {}

    public ProgramSubject(Integer programId, Integer subjectId, Integer orderIndex, Integer prerequisiteSubjectId) {
        this.programId = programId;
        this.subjectId = subjectId;
        this.orderIndex = orderIndex;
        this.prerequisiteSubjectId = prerequisiteSubjectId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getSessions() {
        return sessions;
    }

    public void setSessions(Integer sessions) {
        this.sessions = sessions;
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

    public String getPrerequisiteSubjectCode() {
        return prerequisiteSubjectCode;
    }

    public void setPrerequisiteSubjectCode(String prerequisiteSubjectCode) {
        this.prerequisiteSubjectCode = prerequisiteSubjectCode;
    }

    public String getPrerequisiteSubjectName() {
        return prerequisiteSubjectName;
    }

    public void setPrerequisiteSubjectName(String prerequisiteSubjectName) {
        this.prerequisiteSubjectName = prerequisiteSubjectName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
