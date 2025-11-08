package com.coursistant.lms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * teach
*/
public class AttemptItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 单题作答记录ID */
    private Integer id;
    /** 所属测验尝试ID */
    private Integer attemptId;
    /** 对应测验题目ID */
    private Integer quizItemId;
    /** 题目ID（冗余） */
    private Integer questionId;
    /** 学生作答内容（JSON） */
    private String answerPayload;
    /** 自动评分分数 */
    private BigDecimal autoScore;
    /** 人工评分分数 */
    private BigDecimal manualScore;
    /** 最终得分（自动+人工） */
    private BigDecimal finalScore;
    /** 是否需要人工评分（1=是） */
    private Boolean needsGrading;
    /** 评分时间 */
    private LocalDateTime gradedAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "AttemptItem{" +
                "id=" + id +
                ", attemptId=" + attemptId +
                ", quizItemId=" + quizItemId +
                ", questionId=" + questionId +
                ", answerPayload=" + (answerPayload == null ? "null" : "[json]") +
                ", autoScore=" + autoScore +
                ", manualScore=" + manualScore +
                ", finalScore=" + finalScore +
                ", needsGrading=" + needsGrading +
                ", gradedAt=" + gradedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}