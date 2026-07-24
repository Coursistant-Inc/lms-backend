package com.coursistant.lms.module.interaction.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.user.account.entity.User;

/**
 * 公告实体类
 * Announcement Entity
 */
public class Announcement implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公告 ID
     * Announcement ID
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
     * Announcement content
     */
    private String content;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    /** 公告标题
     * Announcement title
     */
    private String title;

    /** 发布者名称
     * Publisher name
     */
    private String name;

    public Announcement() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (courseId != null) sb.append("\"courseId\":").append(courseId).append(",");
        if (content != null) sb.append("\"content\":\"").append(content).append("\",");
        if (title != null) sb.append("\"title\":\"").append(title).append("\",");
        if (name != null) sb.append("\"name\":\"").append(name).append("\",");

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
