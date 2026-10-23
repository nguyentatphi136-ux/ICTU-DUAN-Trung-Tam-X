package com.ems.model;

import java.io.Serializable;

/**
 * Model biểu diễn một lớp học viên đã từng học hoặc đã rời (S4-06)
 * Giữ nguyên kết quả học tập và trạng thái (Hoàn thành, Bảo lưu, Chuyển lớp,...)
 */
public class PastClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private String classCode;
    private String programName;
    private String period; // Ví dụ: "02/2026 – 04/2026"
    private String result; // Ví dụ: "Hoàn thành · 8.2", "Bảo lưu", "Chuyển sang WEB-K15"

    public PastClass() {
    }

    public PastClass(String classCode, String programName, String period, String result) {
        this.classCode = classCode;
        this.programName = programName;
        this.period = period;
        this.result = result;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
