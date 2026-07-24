package com.coursistant.lms.module.assignment.dto;

import java.util.List;

/**
 * Selection payload for release / retract of individual grades.
 */
public class GradeStudentSelectionRequest {

    private List<Integer> studentUserIds;
    private List<Integer> groupIds;

    public List<Integer> getStudentUserIds() {
        return studentUserIds;
    }

    public void setStudentUserIds(List<Integer> studentUserIds) {
        this.studentUserIds = studentUserIds;
    }

    public List<Integer> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Integer> groupIds) {
        this.groupIds = groupIds;
    }
}
