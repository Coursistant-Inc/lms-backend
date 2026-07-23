package com.coursistant.lms.module.file.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentItem;

/**
 * 公告实体类
 * Assignment Entity
 */
public class SubmissionFile implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID / Primary key */
    private Integer id;

    /** 所属 Assignment ID / Belongs to assignment */
    private Integer submissionId;

    /** 资源类型：file=上传文件，link=外部链接，text=文字内容 / Resource type */
    private String type;

    /** 关联 Diskfiles 表的文件 ID（仅 type=file 时有值） / Linked file ID (only if type=file) */
    private Integer fileId;

    /** 链接 URL 或文本内容（link/text 类型使用） / Link URL or text content */
    private String content;

    /** 上传用户 ID / Uploader user ID */
    private Integer uploadedBy;

    /** 创建时间 / Creation time */
    private LocalDateTime createTime;

    /** 排序字段，值越小越靠前 / Display order, lower means higher priority */
    private Integer orderIndex;

    private String filePath;

    // ===== Getter & Setter =====

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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "AssignmentItem{" +
                "id=" + id +
                ", submissionId=" + submissionId +
                ", type='" + type + '\'' +
                ", fileId=" + fileId +
                ", filePath=" + filePath +
                ", content='" + content + '\'' +
                ", uploadedBy=" + uploadedBy +
                ", createTime=" + createTime +
                ", orderIndex=" + orderIndex +
                '}';
    }
}
