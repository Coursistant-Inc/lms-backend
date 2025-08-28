package com.coursistant.lms.entity;

import java.io.Serializable;
 
 /**
  * 作业小组实体类
  * Entity for assignment group
  */
 public class AssignmentGroup implements Serializable {
     private static final long serialVersionUID = 1L;

     /** 主键 ID */
     private Integer id;

     /** 作业 ID */
     private Integer assignmentId;

     /** 课程 ID */
     private Integer courseId;

     /** 小组名称 */
     private String groupName;

     /** 小组状态：'active', 'closed', 'archived' */
     private String groupStatus;

     /** 加入模式：'free', 'approval' */
     private String joinMode;

     public AssignmentGroup() {}

     public AssignmentGroup(Integer assignmentId, Integer courseId) {
         this.assignmentId = assignmentId;
         this.courseId = courseId;
         this.groupStatus = "active";
         this.joinMode = "free";
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

     public Integer getCourseId() {
         return courseId;
     }

     public void setCourseId(Integer courseId) {
         this.courseId = courseId;
     }

     public String getGroupName() {
         return groupName;
     }

     public void setGroupName(String groupName) {
         this.groupName = groupName;
     }

     public String getGroupStatus() {
         return groupStatus;
     }

     public void setGroupStatus(String groupStatus) {
         this.groupStatus = groupStatus;
     }

     public String getJoinMode() {
         return joinMode;
     }

     public void setJoinMode(String joinMode) {
         this.joinMode = joinMode;
     }

     @Override
     public String toString() {
         return "AssignmentGroup{" +
                 "id=" + id +
                 ", assignmentId=" + assignmentId +
                 ", courseId=" + courseId +
                 ", groupName='" + groupName + '\'' +
                 ", groupStatus='" + groupStatus + '\'' +
                 ", joinMode='" + joinMode + '\'' +
                 '}';
     }
 }