package com.ems.model;

import java.io.Serializable;

/**
 * Mục môn học trong bảng điểm toàn khoá (S8-03):
 * Gồm mã môn, tên môn, số buổi, trọng số, điểm tổng kết và kết quả (Đạt / Không đạt).
 */
public class SubjectGradeItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String subjectCode;
    private String subjectName;
    private int sessions;
    private int weight; // Trọng số môn học để tính điểm trung bình toàn khoá (GPA)
    private Double attendanceScore; // Điểm chuyên cần (nếu có)
    private Double assignmentScore; // Điểm bài tập / thực hành
    private Double examScore;       // Điểm thi / Đồ án cuối môn
    private Double finalScore;      // Điểm tổng kết môn
    private String result;          // 'Đạt' hoặc 'Không đạt' (hoặc 'Chưa học')
    private boolean isLocked;       // Trạng thái đã chốt điểm chính thức

    public SubjectGradeItem() {}

    public SubjectGradeItem(String subjectCode, String subjectName, int sessions, int weight, Double finalScore, String result) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.sessions = sessions;
        this.weight = weight > 0 ? weight : 1;
        this.finalScore = finalScore;
        this.result = result;
        this.isLocked = finalScore != null;
    }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getSessions() { return sessions; }
    public void setSessions(int sessions) { this.sessions = sessions; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight > 0 ? weight : 1; }

    public Double getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(Double attendanceScore) { this.attendanceScore = attendanceScore; }

    public Double getAssignmentScore() { return assignmentScore; }
    public void setAssignmentScore(Double assignmentScore) { this.assignmentScore = assignmentScore; }

    public Double getExamScore() { return examScore; }
    public void setExamScore(Double examScore) { this.examScore = examScore; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
}
