package com.coursistant.lms.module.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Selection payload for release / retract of individual grades.
 */
@Schema(name = "GradeStudentSelectionRequest",
        description = "Select students and/or groups for grade release/retract")
public class GradeStudentSelectionRequest {

    @Schema(description = "Student user ids to release/retract", example = "[385, 386]")
    private List<Integer> studentUserIds;
    @Schema(description = "Group ids (group assignments); when non-empty, group path is used", example = "[11]")
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
