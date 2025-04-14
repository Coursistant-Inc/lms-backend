package com.coursistant.lms.entity;

import java.io.Serializable;

/**
 * 公告实体类
 * Assignment Entity
 */
public class AssignmentFile implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公告 ID
     * Assignment ID
     */
    private Integer id;

    private Integer assignmentId;

    private String path;

    private String name;



    public AssignmentFile() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (assignmentId != null) sb.append("\"assignmentId\":").append(assignmentId).append(",");
        if (name != null) sb.append("\"name\":\"").append(name).append("\",");
        if (path != null) sb.append("\"path\":\"").append(path).append("\",");


        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("}");
        return sb.toString();
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
