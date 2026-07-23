package com.coursistant.lms.module.file.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Folder 实体类
 * 表示课程中的 Lecture 文件夹
 */
public class Folder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Integer id;

    /** 所属课程 ID */
    private Integer courseId;

    /** 文件夹名称，例如 Lecture 1 */
    private String name;

    /** 文件夹描述，支持 Markdown 格式 */
    private String description;

    /** 排序字段，值越小越靠前 */
    private Integer orderIndex;

    private String title;

    private Boolean submissionRequired;

    public Folder() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getSubmissionRequired() {
        return submissionRequired;
    }

    public void setSubmissionRequired(Boolean submissionRequired) {
        this.submissionRequired = submissionRequired;
    }

    @Override
    public String toString() {
        return "Folder{" +
                "id=" + id +
                ", courseId=" + courseId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", orderIndex=" + orderIndex +
                '}';
    }
}