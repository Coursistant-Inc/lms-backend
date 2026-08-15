package com.coursistant.lms.module.quiz.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "AttemptResponse", description = "Full quiz attempt including saved answers")
public class AttemptResponse {
    @Schema(description = "Attempt id", example = "50")
    private Integer id;
    @Schema(description = "Quiz id", example = "3")
    private Integer quizId;
    @Schema(description = "Student user id", example = "21")
    private Integer userId;
    @Schema(description = "1-based attempt number for this user", example = "1")
    private Integer attemptNumber;
    @Schema(description = "Attempt status", example = "InProgress",
            allowableValues = {"InProgress", "Finalizing", "Submitted"})
    private String status;
    @Schema(description = "Why the attempt closed, when submitted", example = "MANUAL",
            allowableValues = {"MANUAL", "TIME_LIMIT_REACHED", "QUIZ_CLOSED", "COURSE_ARCHIVED", "MEMBERSHIP_INELIGIBLE"})
    private String closeReason;
    @Schema(description = "Opaque receipt id after submit")
    private String receiptId;
    @Schema(description = "Start time (UTC local)")
    private LocalDateTime startedAt;
    @Schema(description = "Hard deadline for this attempt (UTC local)")
    private LocalDateTime deadlineAt;
    @Schema(description = "Submit instant (UTC)")
    private Instant submittedAt;
    @Schema(description = "Server clock at response time (UTC local)")
    private LocalDateTime serverNowUtc;
    @Schema(description = "Auto-scored points")
    private BigDecimal autoScore;
    @Schema(description = "Manual (short-answer) points")
    private BigDecimal manualScore;
    @Schema(description = "Total points")
    private BigDecimal totalScore;
    @Schema(description = "Whether all short answers have been graded")
    private Boolean manualGradingComplete;
    @Schema(description = "Saved answers for this attempt")
    private List<SavedAnswerResponse> answers;
}
