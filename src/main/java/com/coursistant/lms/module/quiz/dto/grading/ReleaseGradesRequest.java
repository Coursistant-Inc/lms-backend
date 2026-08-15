package com.coursistant.lms.module.quiz.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ReleaseGradesRequest",
        description = "Optional user filter for release/retract; omit or null = all eligible users. Empty array is rejected.")
public class ReleaseGradesRequest {
    @Schema(description = "User ids to release/retract; omit for all")
    private List<Integer> userIds;
}
