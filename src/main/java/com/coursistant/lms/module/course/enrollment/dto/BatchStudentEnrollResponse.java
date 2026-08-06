package com.coursistant.lms.module.course.enrollment.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchStudentEnrollResponse {

    private int requestedCount;
    private int successCount;
    private int failureCount;
    private List<BatchStudentItemResult> items = new ArrayList<>();

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<BatchStudentItemResult> getItems() {
        return items;
    }

    public void setItems(List<BatchStudentItemResult> items) {
        this.items = items;
    }
}
