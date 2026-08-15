package com.coursistant.lms.module.course.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "AdminBatchEnrollRequest",
        description = "Batch enroll students by userIds and/or emails")
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
