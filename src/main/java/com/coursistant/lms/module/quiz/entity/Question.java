package com.coursistant.lms.module.quiz.entity;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Question 实体
 * 对应表：Question
 * JSON 字段（choices / answerKey）建议在 Service 层使用 Jackson/Gson 做序列化/反序列化。
 *
 * Entity for table "Question".
 * JSON fields (choices / answerKey) are kept as raw String; parse/serialize in service layer as needed.
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 题目ID | Primary Key */
    private Integer id;

    /** 题目类型：'MCQ_SINGLE' | 'MCQ_MULTI' | 'FREE_RESPONSE' */
    private String type;

    /** 题干内容（支持HTML或Markdown） | Stem/content (HTML/Markdown supported) */
    private String stem;

    /**
     * 选择题选项内容（JSON 数组，仅 MCQ 使用）
     * 示例：
     *   [{"label":"A","text":"Option 1"}, {"label":"B","text":"Option 2"}]
     * Raw JSON string.
     */
    private String choices;

    /**
     * 标准答案或评分规则（JSON 结构）
     * 示例：
     *   单选：{"correct":["A"]}
     *   多选：{"correct":["A","C"]}
     *   简答：{"rubric":[{"kw":"binary search","pts":2.0}]}
     * Raw JSON string.
     */
    private String answerKey;

    /** 题目状态：'draft' | 'published' */
    private String status = "draft";

    /** 最后更新时间 | Last updated time */
    private LocalDateTime updatedAt;

    // ============== Constructors ==============

    public Question() {}

    public Question(Integer id,
                    String type,
                    String stem,
                    String choices,
                    String answerKey,
                    BigDecimal points,
                    String status,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.type = type;
        this.stem = stem;
        this.choices = choices;
        this.answerKey = answerKey;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    // ============== Getters / Setters (手写) ==============


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    /**
     * 可在调用处做取值校验（MCQ_SINGLE/MCQ_MULTI/FREE_RESPONSE）
     */
    public void setType(String type) {
        this.type = type;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getChoices() {
        return choices;
    }

    public void setChoices(String choices) {
        this.choices = choices;
    }

    public String getAnswerKey() {
        return answerKey;
    }

    public void setAnswerKey(String answerKey) {
        this.answerKey = answerKey;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ============== toString (手写) ==============

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", stem='" + stem + '\'' +
                ", choices=" + (choices == null ? "null" : "[json]") +
                ", answerKey=" + (answerKey == null ? "null" : "[json]") +
                ", status='" + status + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
