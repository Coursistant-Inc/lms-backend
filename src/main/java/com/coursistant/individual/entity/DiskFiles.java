package com.coursistant.individual.entity;

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

    private String courseName;

    private String type;

    private Double size;

    private String createTime;
    private String hadoopedTime;
    private String qdrantedTime;

    private Boolean delete;

    private Boolean hadooped;
    private Boolean qdranted;
    private String hadoopPath;

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
        if (courseName != null) sb.append("\"courseName\":\"").append(courseName).append("\",");
        if (type != null) sb.append("\"type\":\"").append(type).append("\",");
        if (size != null) sb.append("\"size\":").append(size).append(",");
        if (createTime != null) sb.append("\"createTime\":\"").append(createTime).append("\",");
        if (hadoopedTime != null) sb.append("\"hadoopedTime\":\"").append(hadoopedTime).append("\",");
        if (qdrantedTime != null) sb.append("\"qdrantedTime\":\"").append(qdrantedTime).append("\",");
        if (delete != null) sb.append("\"delete\":").append(delete).append(",");
        if (hadooped != null) sb.append("\"hadooped\":").append(hadooped).append(",");
        if (qdranted != null) sb.append("\"qdranted\":").append(qdranted).append(",");
        if (hadoopPath != null) sb.append("\"hadoopPath\":\"").append(hadoopPath).append("\",");
        if (category != null) sb.append("\"category\":\"").append(category).append("\",");
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1); // 删除最后的逗号
        sb.append("}");
        return sb.toString();
    }


    public Integer getId() {
        return id;
    }

    public Boolean getHadooped() {
        return hadooped;
    }

    public void setHadooped(Boolean hadooped) {
        this.hadooped = hadooped;
    }

    public String getHadoopedTime() {
        return hadoopedTime;
    }

    public void setHadoopedTime(String hadoopedTime) {
        this.hadoopedTime = hadoopedTime;
    }

    public String getQdrantedTime() {
        return qdrantedTime;
    }

    public void setQdrantedTime(String qdrantedTime) {
        this.qdrantedTime = qdrantedTime;
    }

    public Boolean getQdranted() {
        return qdranted;
    }

    public void setQdranted(Boolean qdranted) {
        this.qdranted = qdranted;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
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

    public String getHadoopPath() {
        return hadoopPath;
    }

    public void setHadoopPath(String hadoopPath) {
        this.hadoopPath = hadoopPath;
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