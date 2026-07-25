package com.coursistant.lms.module.course.content.syllabus.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row per course. {@code currentVersionId}/{@code previousVersionId} reference
 * {@link CourseSyllabusVersion#getId()} but are NOT enforced by a database foreign
 * key (to avoid a circular FK between CourseSyllabus and CourseSyllabusVersion);
 * integrity is maintained by {@code CourseSyllabusService}.
 */
public class CourseSyllabus implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer courseId;
    private Integer currentVersionId;
    private Integer previousVersionId;
    private LocalDateTime updatedAt;

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(Integer currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public Integer getPreviousVersionId() {
        return previousVersionId;
    }

    public void setPreviousVersionId(Integer previousVersionId) {
        this.previousVersionId = previousVersionId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
