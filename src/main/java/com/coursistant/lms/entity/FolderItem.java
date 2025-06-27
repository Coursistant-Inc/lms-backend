package com.coursistant.lms.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Folder 实体类
 * 表示课程中的 Lecture 文件夹
 */
public class FolderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Integer id;

    /** 所属文件夹 ID */
    private Integer folderId;

    /** 资源标题，例如 Lecture 1 Slides */
    private String title;

    /** 资源类型：file=文件，link=链接，text=文本 */
    private String type;

    /** 文件 ID，仅 type=file 时使用，关联 Diskfiles 表 */
    private Integer fileId;

    /** 资源内容，如果是 link/text 类型则存放链接或文本内容 */
    private String content;

    /** 上传用户 ID */
    private Integer uploadedBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    public FolderItem() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "FolderItem{" +
                "id=" + id +
                ", folderId=" + folderId +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", fileId=" + fileId +
                ", content='" + content + '\'' +
                ", uploadedBy=" + uploadedBy +
                ", createTime=" + createTime +
                '}';
    }
}