package com.coursistant.lms.module.quiz.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * teach
*/
public class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;

    // ======= variables only =======
    /** 测验ID | Primary Key */
    private Integer id;

    /** 所属课程ID */
    private Integer courseId;

    /** 测验标题 */
    private String title;

    /** 测验描述 */
    private String description;

    /** 限时（分钟） */
    private Integer timeLimitMinutes;

    /** 允许尝试次数（1、3、-1表示无限次） */
    private Integer attemptsAllowed;

    /** 成绩计算策略：latest、highest、average */
    private String gradingPolicy;

    /** 开始时间 */
    private LocalDateTime startAt;

    /** 截止时间 */
    private LocalDateTime dueAt;

    /** 发布状态：draft、published */
    private String publishState;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    private Integer courseContentId;

    private Boolean integrity;

    // ======= toString only =======
    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", courseId=" + courseId +
                ", title='" + title + '\'' +
                ", description=" + (description == null ? "null" : "'" + description + "'") +
                ", timeLimitMinutes=" + timeLimitMinutes +
                ", attemptsAllowed=" + attemptsAllowed +
                ", gradingPolicy='" + gradingPolicy + '\'' +
                ", startAt=" + startAt +
                ", dueAt=" + dueAt +
                ", publishState='" + publishState + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Integer getAttemptsAllowed() {
        return attemptsAllowed;
    }

    public void setAttemptsAllowed(Integer attemptsAllowed) {
        this.attemptsAllowed = attemptsAllowed;
    }

    public String getGradingPolicy() {
        return gradingPolicy;
    }

    public void setGradingPolicy(String gradingPolicy) {
        this.gradingPolicy = gradingPolicy;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getPublishState() {
        return publishState;
    }

    public void setPublishState(String publishState) {
        this.publishState = publishState;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCourseContentId() {
        return courseContentId;
    }

    public void setCourseContentId(Integer courseContentId) {
        this.courseContentId = courseContentId;
    }

    public Boolean getIntegrity() {
        return integrity;
    }

    public void setIntegrity(Boolean integrity) {
        this.integrity = integrity;
    }
}