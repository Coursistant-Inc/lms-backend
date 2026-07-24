package com.coursistant.lms.module.assignment.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Membership snapshot taken when a group grade is released.
 *
 * <p>Group membership keeps moving after a grade is published, so visibility cannot be derived
 * from the live roster: a student who leaves the group afterwards must keep seeing the grade they
 * earned, and a student who joins afterwards must not inherit it. One row per (grade, student)
 * records exactly who the release reached.</p>
 */
public class AssignmentGradeReleaseRecipient implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer gradeId;
    private Integer assignmentId;
    private Integer groupId;
    private Integer studentUserId;
    private LocalDateTime releasedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGradeId() {
        return gradeId;
    }

    public void setGradeId(Integer gradeId) {
        this.gradeId = gradeId;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Integer studentUserId) {
        this.studentUserId = studentUserId;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }
}
