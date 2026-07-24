package com.coursistant.lms.module.course.group.dto;

public class MoveMemberRequest {
    private Integer targetGroupId;
    private Boolean confirmCapacityOverfill;
    private Boolean confirmAcademicImpact;

    public Integer getTargetGroupId() {
        return targetGroupId;
    }

    public void setTargetGroupId(Integer targetGroupId) {
        this.targetGroupId = targetGroupId;
    }

    public Boolean getConfirmCapacityOverfill() {
        return confirmCapacityOverfill;
    }

    public void setConfirmCapacityOverfill(Boolean confirmCapacityOverfill) {
        this.confirmCapacityOverfill = confirmCapacityOverfill;
    }

    public Boolean getConfirmAcademicImpact() {
        return confirmAcademicImpact;
    }

    public void setConfirmAcademicImpact(Boolean confirmAcademicImpact) {
        this.confirmAcademicImpact = confirmAcademicImpact;
    }
}
