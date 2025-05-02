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
 
 
     @Override
     public String toString() {
         return "AssignmentGroup{" +
                 "id=" + id +
                 ", assignmentId=" + assignmentId +
                 ", courseId=" + courseId +
                 '}';
     }
 }