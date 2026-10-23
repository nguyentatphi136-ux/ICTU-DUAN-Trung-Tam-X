package com.ems.dto;

import java.io.Serializable;

public class SubjectLessonRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer sessionOrder;
    private String topic;
    private String objectives;

    public SubjectLessonRequest() {}

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
