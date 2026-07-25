package com.coursistant.lms.module.course.enrollment.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchEnrollResponse {

    private List<MemberResponse> succeeded = new ArrayList<>();
    private List<BatchEnrollFailure> failed = new ArrayList<>();

    public List<MemberResponse> getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(List<MemberResponse> succeeded) {
        this.succeeded = succeeded;
    }

    public List<BatchEnrollFailure> getFailed() {
        return failed;
    }

    public void setFailed(List<BatchEnrollFailure> failed) {
        this.failed = failed;
    }
}
