package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model biểu diễn môn học thuộc một lớp học cụ thể kèm phân công giảng viên và thứ tự học
 * Hỗ trợ các chức năng: S3-08, S4-01, S4-04
 */
public class ClassSubject implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private Long id;
    private Long classId;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private int sessions;
    private int orderIndex = 1;
    private Long primaryInstructorId;
    private String primaryInstructorName;
    private List<String> tas = new ArrayList<>();
    private String status = STATUS_NOT_STARTED;

    public ClassSubject() {
    }

    public ClassSubject(Long classId, Integer subjectId, int orderIndex) {
        this.classId = classId;
        this.subjectId = subjectId != null ? subjectId.longValue() : null;
        this.orderIndex = orderIndex;
        this.status = STATUS_NOT_STARTED;
    }

    public ClassSubject(String code, String name, int sessions, String teacher) {
        this.subjectCode = code;
        this.subjectName = name;
        this.sessions = sessions;
        this.primaryInstructorName = teacher;
    }

    public ClassSubject(String code, String name, int sessions, String teacher, List<String> tas) {
        this(code, name, sessions, teacher);
        if (tas != null) {
            this.tas = tas;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassSubjectId() {
        return id;
    }

    public void setClassSubjectId(Long classSubjectId) {
        this.id = classSubjectId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId != null ? subjectId.longValue() : null;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getCode() {
        return subjectCode;
    }

    public void setCode(String code) {
        this.subjectCode = code;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getName() {
        return subjectName;
    }

    public void setName(String name) {
        this.subjectName = name;
    }

    public int getSessions() {
        return sessions;
    }

    public void setSessions(int sessions) {
        this.sessions = sessions;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Long getPrimaryInstructorId() {
        return primaryInstructorId;
    }

    public void setPrimaryInstructorId(Long primaryInstructorId) {
        this.primaryInstructorId = primaryInstructorId;
    }

    public Long getInstructorId() {
        return primaryInstructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.primaryInstructorId = instructorId;
    }

    public String getPrimaryInstructorName() {
        return primaryInstructorName;
    }

    public void setPrimaryInstructorName(String primaryInstructorName) {
        this.primaryInstructorName = primaryInstructorName;
    }

    public String getTeacher() {
        return primaryInstructorName;
    }

    public void setTeacher(String teacher) {
        this.primaryInstructorName = teacher;
    }

    public List<String> getTas() {
        return tas;
    }

    public void setTas(List<String> tas) {
        this.tas = tas != null ? tas : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
