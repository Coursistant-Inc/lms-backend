package com.coursistant.lms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公告实体类
 * Assignment Entity
 */
public class AssignmentSubmission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 提交 ID
     * Submission ID
     */
    private Integer id;

    /** 作业 ID
     * Assignment ID
     */
    private Integer assignmentId;

    /** 学生 ID
     * Student ID
     */
    private Integer studentId;

    /** 批改者 ID
     * Grader ID
     */
    private Integer graderId;

    /** 提交日期
     * Submission date
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;







    /** 状态
     * Submission status
     */
    private String status;

    /** 成绩
     * Grade
     */
    private BigDecimal grade;

    /** 评语
     * Comment
     */
    private String comment;

    private Boolean isFinal;

    private Boolean isLate;

    private String lateTime;



    public AssignmentSubmission() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (assignmentId != null) sb.append("\"assignmentId\":").append(assignmentId).append(",");
        if (studentId != null) sb.append("\"studentId\":").append(studentId).append(",");
        if (graderId != null) sb.append("\"graderId\":").append(graderId).append(",");
        if (date != null) sb.append("\"date\":\"").append(date).append("\",");
        if (status != null) sb.append("\"status\":\"").append(status).append("\",");
        if (grade != null) sb.append("\"grade\":\"").append(grade).append("\",");
        if (comment != null) sb.append("\"comment\":\"").append(comment).append("\",");
        if (isFinal != null) sb.append("\"isFinal\":").append(isFinal).append(",");
        if (isLate != null) sb.append("\"isLate\":").append(isLate).append(",");
        if (lateTime != null) sb.append("\"lateTime\":").append(lateTime).append(",");

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("}");
        return sb.toString();
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getGraderId() {
        return graderId;
    }

    public void setGraderId(Integer graderId) {
        this.graderId = graderId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getGrade() {
        return grade;
    }

    public void setGrade(BigDecimal grade) {
        this.grade = grade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getFinal() {
        return isFinal;
    }

    public void setFinal(Boolean aFinal) {
        isFinal = aFinal;
    }

    public Boolean getLate() {
        return isLate;
    }

    public void setLate(Boolean late) {
        isLate = late;
    }

    public String getLateTime() {
        return lateTime;
    }

    public void setLateTime(String lateTime) {
        this.lateTime = lateTime;
    }
}