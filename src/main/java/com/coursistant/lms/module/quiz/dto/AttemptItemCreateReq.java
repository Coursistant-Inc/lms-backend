package com.coursistant.lms.module.quiz.dto;

/**
 * 公告实体类
 * Assignment DTO
 */
public class AttemptItemCreateReq {
    private Integer attemptId;
    private Integer quizItemId;
    private Integer questionId;
    private Object  answerPayload;   // 关键：Object 接收任意 JSON
    private Boolean needsGrading;

    // getter/setter 省略


    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getQuizItemId() {
        return quizItemId;
    }

    public void setQuizItemId(Integer quizItemId) {
        this.quizItemId = quizItemId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Object getAnswerPayload() {
        return answerPayload;
    }

    public void setAnswerPayload(Object answerPayload) {
        this.answerPayload = answerPayload;
    }

    public Boolean getNeedsGrading() {
        return needsGrading;
    }

    public void setNeedsGrading(Boolean needsGrading) {
        this.needsGrading = needsGrading;
    }
}
