package com.ems.dto;

import com.ems.model.SubjectLesson;

import java.io.Serializable;

public class SubjectLessonResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer subjectId;
    private Integer sessionOrder;
    private String topic;
    private String objectives;

    public static SubjectLessonResponse fromEntity(SubjectLesson entity) {
        if (entity == null) return null;
        SubjectLessonResponse res = new SubjectLessonResponse();
        res.id = entity.getId();
        res.subjectId = entity.getSubjectId();
        res.sessionOrder = entity.getSessionOrder();
        res.topic = entity.getTopic();
        res.objectives = entity.getObjectives();
        return res;
    }

    public Integer getId() {
        return id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public Integer getSessionOrder() {
        return sessionOrder;
    }

    public String getTopic() {
        return topic;
    }

    public String getObjectives() {
        return objectives;
    }
}
