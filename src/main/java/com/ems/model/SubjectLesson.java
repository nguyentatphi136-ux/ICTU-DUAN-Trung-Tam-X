package com.ems.model;

import java.io.Serializable;

/**
 * Model Entity đại diện cho Buổi học trong môn học (SubjectLesson)
 * User Story: S2-07 (EP-02)
 */
public class SubjectLesson implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer subjectId;
    private Integer sessionOrder;
    private String topic;
    private String objectives;

    public SubjectLesson() {}

    public SubjectLesson(Integer id, Integer subjectId, Integer sessionOrder, String topic, String objectives) {
        this.id = id;
        this.subjectId = subjectId;
        this.sessionOrder = sessionOrder;
        this.topic = topic;
        this.objectives = objectives;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getSessionOrder() {
        return sessionOrder;
    }

    public void setSessionOrder(Integer sessionOrder) {
        this.sessionOrder = sessionOrder;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }
}
