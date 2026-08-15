package com.coursistant.lms.module.quiz.dto.grading;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "GradeAnswerRequest", description = "Manual grade for a short-answer question")
public class GradeAnswerRequest {
    @Schema(description = "Score awarded (0..question points)", example = "1.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal score;
    @Schema(description = "Feedback shown to student when released")
    private String feedback;
    @Schema(description = "Required when changing a score after grades were released")
    private String reason;
}
