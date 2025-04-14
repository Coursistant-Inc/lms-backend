package com.coursistant.lms.entity;

import java.io.Serializable;

/**
 * 公告实体类
 * Assignment Entity
 */
public class SubmissionFile implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公告 ID
     * Assignment ID
     */
    private Integer id;

    private Integer submissionId;

    private String path;

    private String name;



    public SubmissionFile() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) sb.append("\"id\":").append(id).append(",");
        if (submissionId != null) sb.append("\"submissionId\":").append(submissionId).append(",");
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

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
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
