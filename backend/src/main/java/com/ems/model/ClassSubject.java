package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model biểu diễn môn học thuộc một lớp học cụ thể kèm phân công giảng viên
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ClassSubject implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private int sessions; // Số buổi của môn
    private String teacher; // Giảng viên chính được phân công
    private List<String> tas = new ArrayList<>(); // Trợ giảng hỗ trợ

    public ClassSubject() {
    }

    public ClassSubject(String code, String name, int sessions, String teacher) {
        this.code = code;
        this.name = name;
        this.sessions = sessions;
        this.teacher = teacher;
    }

    public ClassSubject(String code, String name, int sessions, String teacher, List<String> tas) {
        this.code = code;
        this.name = name;
        this.sessions = sessions;
        this.teacher = teacher;
        if (tas != null) {
            this.tas = tas;
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSessions() {
        return sessions;
    }

    public void setSessions(int sessions) {
        this.sessions = sessions;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public List<String> getTas() {
        return tas;
    }

    public void setTas(List<String> tas) {
        this.tas = tas;
    }
}
