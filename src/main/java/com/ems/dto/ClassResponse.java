package com.ems.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO trả dữ liệu lớp học sau khi mở thành công (User Story S3-05)
 */
public class ClassResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String classCode;
    private String className;
    private Integer programId;
    private String programName;
    private String expectedStartDate;
    private int maxStudents;
    private String studyMode;
    private String status;
    private String statusLabel;
    private int subjectCount;
    private List<SubjectItem> subjects = new ArrayList<>();
    private String createdAt;

    public ClassResponse() {
    }

    public static class SubjectItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private Integer subjectId;
        private String subjectCode;
        private String subjectName;
        private int orderIndex;
        private String status;

        public SubjectItem() {
        }

        public SubjectItem(Long id, Integer subjectId, String subjectCode, String subjectName, int orderIndex, String status) {
            this.id = id;
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.orderIndex = orderIndex;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public int getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getExpectedStartDate() {
        return expectedStartDate;
    }

    public void setExpectedStartDate(String expectedStartDate) {
        this.expectedStartDate = expectedStartDate;
    }

    public int getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(int maxStudents) {
        this.maxStudents = maxStudents;
    }

    public String getStudyMode() {
        return studyMode;
    }

    public void setStudyMode(String studyMode) {
        this.studyMode = studyMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public int getSubjectCount() {
        return subjectCount;
    }

    public void setSubjectCount(int subjectCount) {
        this.subjectCount = subjectCount;
    }

    public List<SubjectItem> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectItem> subjects) {
        this.subjects = subjects;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
