package com.coursistant.lms.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

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
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime date;

    private String timezone;

    /** 状态
     * Submission status
     */
    private String status;

    /** 成绩
     * Grade
     */
    private String grade;

    /** 评语
     * Comment
     */
    private String comment;



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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }


}