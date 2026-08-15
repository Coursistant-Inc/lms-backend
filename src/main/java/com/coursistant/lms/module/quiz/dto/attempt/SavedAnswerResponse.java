package com.coursistant.lms.module.quiz.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "SavedAnswerResponse", description = "Persisted answer snapshot for one question")
public class SavedAnswerResponse {
    @Schema(description = "Question id", example = "10")
    private Integer questionId;
    @Schema(description = "Selected option ids")
    private List<Integer> selectedOptionIds;
    @Schema(description = "Short-answer text")
    private String textAnswer;
    @Schema(description = "Answer revision", example = "2")
    private Integer revision;
    @Schema(description = "Last saved at (UTC local)")
    private LocalDateTime savedAt;
}
