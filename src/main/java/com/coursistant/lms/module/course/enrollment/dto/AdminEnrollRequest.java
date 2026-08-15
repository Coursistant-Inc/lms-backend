package com.coursistant.lms.module.course.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminEnrollRequest", description = "Enroll a single user by userId (student or TA)")
public class AdminEnrollRequest {

    private Integer userId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
