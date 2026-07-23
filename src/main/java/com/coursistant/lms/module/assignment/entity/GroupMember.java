package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
 
 /**
  * 小组成员实体类
  * Entity for group member
  */
 public class GroupMember implements Serializable {
     private static final long serialVersionUID = 1L;
 
     /** 主键 ID */
     private Integer id;
 
     /** 小组 ID */
     private Integer groupId;
 
     /** 所属课程 ID */
     private Integer courseId;
 
     /** 用户 ID（学生） */
     private Integer userId;
 
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
 
     @Override
     public String toString() {
         return "GroupMember{" +
                 "id=" + id +
                 ", groupId=" + groupId +
                 ", courseId=" + courseId +
                 ", userId=" + userId +
                 '}';
     }
 }