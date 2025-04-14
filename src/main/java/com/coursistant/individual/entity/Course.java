package com.coursistant.individual.entity;

import java.io.Serializable;

/**
 * 部门信息表
 * Department Information Sheet
*/
public class Course implements Serializable {
    private static final long serialVersionUID = 1L;


    private Integer id;

    private String name;

    private String courseCode;

    private Integer teacherId;

    private String instructorName;

    private String school;

    private String semester;

    private String teacherName;

    private String teacherPhone;

    private String teacherEmail;





    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (name != null) sb.append("\"name\":\"").append(name).append("\",");
        if (courseCode != null) sb.append("\"courseCode\":\"").append(courseCode).append("\",");
        if (teacherId != null) sb.append("\"teacherId\":").append(teacherId).append(",");
        if (school != null) sb.append("\"school\":").append(school).append(",");
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherPhone() {
        return teacherPhone;
    }

    public void setTeacherPhone(String teacherPhone) {
        this.teacherPhone = teacherPhone;
    }

    public String getTeacherEmail() {
        return teacherEmail;
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }
}