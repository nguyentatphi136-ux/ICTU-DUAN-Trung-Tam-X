package com.ems.model;

import java.io.Serializable;

public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int studentId;
    private String studentName;
    private String subjectName;
    private String componentName;
    private double score;
    private String notes;
    private String updatedBy;

    public Grade() {}

    public Grade(int id, int studentId, String studentName, String subjectName, String componentName, double score, String notes, String updatedBy) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectName = subjectName;
        this.componentName = componentName;
        this.score = score;
        this.notes = notes;
        this.updatedBy = updatedBy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
