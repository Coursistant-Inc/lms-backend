package com.coursistant.lms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * teach
*/
public class QuizItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 测验题目ID */
    private Integer id;
    /** 所属测验ID */
    private Integer quizId;
    /** 题目ID */
    private Integer questionId;
    /** 该题在本测验中的分值 */
    private BigDecimal points;
    /** 在测验中的顺序（从1开始） */
    private Integer orderIndex;
    /** 题目快照（JSON） */
    private String questionSnapshot;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "QuizItem{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", questionId=" + questionId +
                ", points=" + points +
                ", orderIndex=" + orderIndex +
                ", questionSnapshot=" + (questionSnapshot == null ? "null" : "[json]") +
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

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getQuestionSnapshot() {
        return questionSnapshot;
    }

    public void setQuestionSnapshot(String questionSnapshot) {
        this.questionSnapshot = questionSnapshot;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}