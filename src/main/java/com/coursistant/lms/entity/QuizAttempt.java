package com.coursistant.lms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * teach
*/
public class QuizAttempt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 测验尝试ID */
    private Integer id;
    /** 所属测验ID */
    private Integer quizId;
    /** 学生ID */
    private Integer studentId;
    /** 第几次尝试（从1开始） */
    private Integer attemptNo;
    /** 开始时间 */
    private LocalDateTime startedAt;
    /** 提交时间 */
    private LocalDateTime submittedAt;
    /** 作答时长（秒） */
    private Integer timeSpentSeconds;
    /** 状态：in_progress、submitted、graded */
    private String state;
    /** 自动评分分数 */
    private BigDecimal autoScore;
    /** 人工评分分数 */
    private BigDecimal manualScore;
    /** 最终得分（自动+人工） */
    private BigDecimal finalScore;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;



    @Override
    public String toString() {
        return "QuizAttempt{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", studentId=" + studentId +
                ", attemptNo=" + attemptNo +
                ", startedAt=" + startedAt +
                ", submittedAt=" + submittedAt +
                ", timeSpentSeconds=" + timeSpentSeconds +
                ", state='" + state + '\'' +
                ", autoScore=" + autoScore +
                ", manualScore=" + manualScore +
                ", finalScore=" + finalScore +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public BigDecimal getAutoScore() {
        return autoScore;
    }

    public void setAutoScore(BigDecimal autoScore) {
        this.autoScore = autoScore;
    }

    public BigDecimal getManualScore() {
        return manualScore;
    }

    public void setManualScore(BigDecimal manualScore) {
        this.manualScore = manualScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}