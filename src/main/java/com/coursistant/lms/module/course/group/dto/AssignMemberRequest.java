package com.coursistant.lms.module.course.group.dto;

public class AssignMemberRequest {
    private Integer userId;
    private Boolean confirmCapacityOverfill;
    private Boolean confirmAcademicImpact;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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
