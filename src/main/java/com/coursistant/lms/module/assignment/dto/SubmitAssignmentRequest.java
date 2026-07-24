package com.coursistant.lms.module.assignment.dto;

import java.util.List;

/**
 * Turns staged uploads into a submission version. When {@code stagingFileIds} is omitted,
 * every active staging file of the caller is consumed.
 */
public class SubmitAssignmentRequest {

    private List<Integer> stagingFileIds;

    public List<Integer> getStagingFileIds() {
        return stagingFileIds;
    }

    public void setStagingFileIds(List<Integer> stagingFileIds) {
        this.stagingFileIds = stagingFileIds;
    }
}
