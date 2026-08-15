package com.coursistant.lms.module.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Turns staged uploads into a submission version. When {@code stagingFileIds} is omitted,
 * every active staging file of the caller is consumed.
 */
@Schema(name = "SubmitAssignmentRequest",
        description = "Submit staged files; omit stagingFileIds to consume all active staging files")
public class SubmitAssignmentRequest {

    @Schema(description = "Staging file ids to include; omit to submit all active staging files",
            example = "[101, 102]")
    private List<Integer> stagingFileIds;

    public List<Integer> getStagingFileIds() {
        return stagingFileIds;
    }

    public void setStagingFileIds(List<Integer> stagingFileIds) {
        this.stagingFileIds = stagingFileIds;
    }
}
