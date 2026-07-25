package com.coursistant.lms.module.course.enrollment.dto;

import java.util.List;

public class AdminBatchEnrollRequest {

    private List<Integer> userIds;
    private List<String> emails;

    public List<Integer> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Integer> userIds) {
        this.userIds = userIds;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }
}
