package com.coursistant.lms.module.file.entity;

import java.io.Serializable;

/**
 * 网盘
 * DiskFiles
 */
public class DiskFiles implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /**
     * 部门名称
     * Department Name
     */
    private String name;
    /** 描述
     * describe
     * */

    private String path;

    private Integer userId;

    private Integer courseId;

    private String type;

    private Double size;

    private String createTime;


    private Boolean delete;


    private String category;

    private String summary;

    public DiskFiles() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (name != null) sb.append("\"name\":\"").append(name).append("\",");
        if (path != null) sb.append("\"path\":\"").append(path).append("\",");
        if (userId != null) sb.append("\"userId\":").append(userId).append(",");
        if (courseId != null) sb.append("\"courseId\":\"").append(courseId).append("\",");
        if (type != null) sb.append("\"type\":\"").append(type).append("\",");
        if (size != null) sb.append("\"size\":").append(size).append(",");
        if (createTime != null) sb.append("\"createTime\":\"").append(createTime).append("\",");
        if (delete != null) sb.append("\"delete\":").append(delete).append(",");
        if (category != null) sb.append("\"category\":\"").append(category).append("\",");
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Boolean getDelete() {
        return delete;
    }

    public void setDelete(Boolean delete) {
        this.delete = delete;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}