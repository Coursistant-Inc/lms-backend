package com.coursistant.individual.entity.DTO;

import java.io.Serializable;

/**
 * teach 数据传输对象
 * Teach Data Transfer Object (DTO)
 */
public class LearnDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Integer id;

    /**
     * 课程 ID
     * Course ID
     */
    private Integer courseId;

    /**
     * 用户名
     * Username
     */
    private String username;

    /**
     * 备用用户名
     * Secondary username
     */
    private String username2;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
