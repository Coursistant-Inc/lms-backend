package com.coursistant.lms.module.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * chat
*/
public class Chat implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer dialogueId;

    private String queryText;

    private String queryImage;

    private String answerText;

    private String answerImage;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime time;

    private Boolean delete;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime deleteTime;

    //
    //private String base64QueryImage;

    //private String base64AnswerImage;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDialogueId() {
        return dialogueId;
    }

    public void setDialogueId(Integer dialogueId) {
        this.dialogueId = dialogueId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getQueryImage() {
        return queryImage;
    }

    public void setQueryImage(String queryImage) {
        this.queryImage = queryImage;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getAnswerImage() {
        return answerImage;
    }

    public void setAnswerImage(String answerImage) {
        this.answerImage = answerImage;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Boolean getDelete() {
        return delete;
    }

    public void setDelete(Boolean delete) {
        this.delete = delete;
    }
/*
    public String getBase64QueryImage() {
        return base64QueryImage;
    }

    public void setBase64QueryImage(String base64QueryImage) {
        this.base64QueryImage = base64QueryImage;
    }

    public String getBase64AnswerImage() {
        return base64AnswerImage;
    }

    public void setBase64AnswerImage(String base64AnswerImage) {
        this.base64AnswerImage = base64AnswerImage;
    }*/

    public LocalDateTime getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(LocalDateTime deleteTime) {
        this.deleteTime = deleteTime;
    }
}