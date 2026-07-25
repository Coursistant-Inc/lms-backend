package com.coursistant.lms.module.course.enrollment.dto;

public class UpdateTaPermissionsRequest {

    private Boolean canGrade;
    private Boolean canPostAnnouncements;
    private Boolean canManageGroups;
    private Boolean canManageCourseEvents;

    public Boolean getCanGrade() {
        return canGrade;
    }

    public void setCanGrade(Boolean canGrade) {
        this.canGrade = canGrade;
    }

    public Boolean getCanPostAnnouncements() {
        return canPostAnnouncements;
    }

    public void setCanPostAnnouncements(Boolean canPostAnnouncements) {
        this.canPostAnnouncements = canPostAnnouncements;
    }

    public Boolean getCanManageGroups() {
        return canManageGroups;
    }

    public void setCanManageGroups(Boolean canManageGroups) {
        this.canManageGroups = canManageGroups;
    }

    public Boolean getCanManageCourseEvents() {
        return canManageCourseEvents;
    }

    public void setCanManageCourseEvents(Boolean canManageCourseEvents) {
        this.canManageCourseEvents = canManageCourseEvents;
    }
}
