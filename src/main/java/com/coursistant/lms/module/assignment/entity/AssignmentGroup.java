package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
 
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

     /** 小组标题 */
     private String title;

     /** 小组名称 */
     private String groupName;

     /** 小组状态：'active', 'closed', 'archived' */
     private String groupStatus;

     /** 加入模式：'free', 'approval' */
     private String joinMode;

     /** 小组描述 */
     private String description;

     /** 当前学生数量 */
     private Integer currStudentCount;

     /** 最大学生数量 */
     private Integer maxStudent;

     /** 创建时间 */
     private LocalDateTime createAt;

     public AssignmentGroup() {}

     public AssignmentGroup(Integer assignmentId, Integer courseId) {
         this.assignmentId = assignmentId;
         this.courseId = courseId;
         this.groupStatus = "active";
         this.joinMode = "free";
         this.currStudentCount = 0;
         this.createAt = LocalDateTime.now();
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

     public String getTitle() {
         return title;
     }

     public void setTitle(String title) {
         this.title = title;
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

     public String getDescription() {
         return description;
     }

     public void setDescription(String description) {
         this.description = description;
     }

     public Integer getCurrStudentCount() {
         return currStudentCount;
     }

     public void setCurrStudentCount(Integer currStudentCount) {
         this.currStudentCount = currStudentCount;
     }

     public Integer getMaxStudent() {
         return maxStudent;
     }

     public void setMaxStudent(Integer maxStudent) {
         this.maxStudent = maxStudent;
     }

     public LocalDateTime getCreateAt() {
         return createAt;
     }

     public void setCreateAt(LocalDateTime createAt) {
         this.createAt = createAt;
     }

     @Override
     public String toString() {
         return "AssignmentGroup{" +
                 "id=" + id +
                 ", assignmentId=" + assignmentId +
                 ", courseId=" + courseId +
                 ", title='" + title + '\'' +
                 ", groupName='" + groupName + '\'' +
                 ", groupStatus='" + groupStatus + '\'' +
                 ", joinMode='" + joinMode + '\'' +
                 ", description='" + description + '\'' +
                 ", currStudentCount=" + currStudentCount +
                 ", maxStudent=" + maxStudent +
                 ", createAt=" + createAt +
                 '}';
     }
 }