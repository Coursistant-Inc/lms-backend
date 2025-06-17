package com.coursistant.lms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告实体类
 * Assignment Entity
 */
public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公告 ID
     * Assignment ID
     */
    private Integer id;

    /** 课程 ID
     * Course ID
     */
    private Integer courseId;

    /** 用户 ID
     * User ID
     */
    private Integer userId;

    /** 公告内容
     * Assignment title
     */
    private String title;

    /** 公告日期
     * Assignment start and due
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime due;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime start;


    private String description;

    private String criteria;

    /** 提交人数 */
    private Integer submissionNum;

    /** 允许提交次数 */
    private Integer allowedSubmissionTimes;

    /** 最高分 */
    private Integer highestGrade;

    /** 最低分 */
    private Integer lowestGrade;

    /** 平均分 */
    private Integer averageGrade;

    /** 是否发布成绩（0-未发布，1-已发布） */
    private Boolean gradePublish;

    private String groupMode;

    private Integer groupSize;





    public Assignment() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");

        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (courseId != null) sb.append("\"courseId\":").append(courseId).append(",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (title != null) sb.append("\"title\":\"").append(title).append("\",");
        if (start != null) sb.append("\"start\":\"").append(start).append("\",");
        if (due != null) sb.append("\"due\":\"").append(due).append("\",");
        if (description != null) sb.append("\"description\":\"").append(description).append("\",");
        if (criteria != null) sb.append("\"criteria\":\"").append(criteria).append("\",");

        if (submissionNum != null) sb.append("\"submissionNum\":").append(submissionNum).append(",");
        if (highestGrade != null) sb.append("\"highestGrade\":").append(highestGrade).append(",");
        if (lowestGrade != null) sb.append("\"lowestGrade\":").append(lowestGrade).append(",");
        if (averageGrade != null) sb.append("\"averageGrade\":").append(averageGrade).append(",");
        if (gradePublish != null) sb.append("\"gradePublish\":").append(gradePublish).append(",");

        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1); // 去掉最后一个逗号
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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getDue() {
        return due;
    }

    public void setDue(LocalDateTime due) {
        this.due = due;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }



    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    public Integer getSubmissionNum() {
        return submissionNum;
    }

    public void setSubmissionNum(Integer submissionNum) {
        this.submissionNum = submissionNum;
    }

    public Integer getAllowedSubmissionTimes() {
        return allowedSubmissionTimes;
    }

    public void setAllowedSubmissionTimes(Integer allowedSubmissionTimes) {
        this.allowedSubmissionTimes = allowedSubmissionTimes;
    }

    public Integer getHighestGrade() {
        return highestGrade;
    }

    public void setHighestGrade(Integer highestGrade) {
        this.highestGrade = highestGrade;
    }

    public Integer getLowestGrade() {
        return lowestGrade;
    }

    public void setLowestGrade(Integer lowestGrade) {
        this.lowestGrade = lowestGrade;
    }

    public Integer getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(Integer averageGrade) {
        this.averageGrade = averageGrade;
    }

    public Boolean getGradePublish() {
        return gradePublish;
    }

    public void setGradePublish(Boolean gradePublish) {
        this.gradePublish = gradePublish;
    }

    public String getGroupMode() {
        return groupMode;
    }

    public void setGroupMode(String groupMode) {
        this.groupMode = groupMode;
    }

    public Integer getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(Integer groupSize) {
        this.groupSize = groupSize;
    }
}
