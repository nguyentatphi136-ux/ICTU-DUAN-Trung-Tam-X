package com.ems.model;

import java.io.Serializable;

/**
 * Model thông tin bài tập (Jira IDTTX-273 / S6-01):
 * Đồng bộ với đặc tả dữ liệu bài tập môn học và giao diện chấm điểm.
 */
public class AssignmentInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String classCode;
    private String className;
    private String subjectCode;
    private String subjectName;
    private String sessionCode;
    private String instructorName;
    private String dueDate;
    private double maxScore;
    private String description;
    private String latePolicy; // "Cho nộp" hoặc "Chặn"

    public AssignmentInfo() {
        this.maxScore = 10.0;
        this.latePolicy = "Cho nộp";
    }

    public AssignmentInfo(int id, String title, String classCode, String className,
                          String subjectCode, String subjectName, String sessionCode,
                          String instructorName, String dueDate, double maxScore,
                          String description, String latePolicy) {
        this.id = id;
        this.title = title;
        this.classCode = classCode;
        this.className = className;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.sessionCode = sessionCode;
        this.instructorName = instructorName;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
        this.description = description;
        this.latePolicy = latePolicy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getShortTitle() {
        if (title != null && title.contains(":")) {
            return title.split(":")[0].trim();
        }
        return title;
    }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLatePolicy() { return latePolicy; }
    public void setLatePolicy(String latePolicy) { this.latePolicy = latePolicy; }
}
