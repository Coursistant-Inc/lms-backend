package com.coursistant.lms.module.course.entity;

import java.io.Serializable;

/**
 * teach
*/
public class Teach implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;

    /** 老师id
     * Teacher ID
     * */
    private Integer userId;

    /** courseid */
    private Integer courseId;

    private String courseName;

    private String courseCode;
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (courseId != null) sb.append("\"courseId\":").append(courseId).append(",");
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); // 删除最后的逗号
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
}