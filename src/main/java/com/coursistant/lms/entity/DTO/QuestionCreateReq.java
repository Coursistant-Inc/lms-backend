package com.coursistant.lms.entity.DTO;

import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentFile;

import java.util.List;

/**
 * 公告实体类
 * Assignment DTO
 */
public class QuestionCreateReq {
    private String type;
    private String stem;
    private Object choices;      // 数组/对象皆可
    private Object answerKey;    // 对象
    private String status;       // draft/published

    // getter/setter 省略


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public Object getChoices() {
        return choices;
    }

    public void setChoices(Object choices) {
        this.choices = choices;
    }

    public Object getAnswerKey() {
        return answerKey;
    }

    public void setAnswerKey(Object answerKey) {
        this.answerKey = answerKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
