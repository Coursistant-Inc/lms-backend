package com.coursistant.lms.module.assignment.entity;

import com.coursistant.lms.module.user.entity.User;

import java.io.Serializable;

/**
 * 小组成员详情DTO（包含用户信息）
 * DTO for group member with user details
 */
public class GroupMemberDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Integer id;
    
    /** 小组ID */
    private Integer groupId;
    
    /** 课程ID */
    private Integer courseId;
    
    /** 用户ID */
    private Integer userId;
    
    /** 用户名 */
    private String username;
    
    /** 用户姓名 */
    private String name;
    
    /** 用户邮箱 */
    private String email;
    
    /** 用户头像 */
    private String avatar;

    public GroupMemberDetail() {}

    public GroupMemberDetail(GroupMember member, User user) {
        if (member != null) {
            this.id = member.getId();
            this.groupId = member.getGroupId();
            this.courseId = member.getCourseId();
            this.userId = member.getUserId();
        }
        if (user != null) {
            this.username = user.getUsername();
            this.name = user.getName();
            this.email = user.getEmail();
            this.avatar = user.getAvatar();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String toString() {
        return "GroupMemberDetail{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", courseId=" + courseId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}
