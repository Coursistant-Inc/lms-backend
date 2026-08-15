package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.grading.*;
import com.coursistant.lms.module.quiz.service.QuizGradingService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/quizzes/{quizId}")
@Tag(name = "Quiz Grading", description = "Manual short-answer grading and grade release/retract")
public class QuizGradingController {

    @Resource
    private QuizGradingService quizGradingService;

    @GetMapping("/grading-summary")
    @Operation(
            operationId = "quizGradingSummary",
            summary = "Get grading progress summary",
            description = "Instructor/TA. Main errors: QUIZ_GRADING_FORBIDDEN, QUIZ_TA_SELF_CONFLICT.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "QUIZ_GRADING_FORBIDDEN / QUIZ_TA_SELF_CONFLICT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradingSummaryResponse> summary(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer quizId) {
        return ApiResponse.success(quizGradingService.summary(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/grading/questions/{questionId}/answers")
    @Operation(
            operationId = "quizListShortAnswers",
            summary = "List short-answer submissions for a question",
            description = "Main errors: QUIZ_QUESTION_NOT_FOUND, QUIZ_GRADING_FORBIDDEN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_QUESTION_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "QUIZ_GRADING_FORBIDDEN / QUIZ_TA_SELF_CONFLICT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<List<ShortAnswerGradingItem>> listAnswers(HttpServletRequest request,
                                                                   @PathVariable Integer courseId,
                                                                   @PathVariable Integer quizId,
                                                                   @PathVariable Integer questionId) {
        return ApiResponse.success(quizGradingService.listShortAnswers(courseId, quizId, questionId,
                currentUserId(request)));
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}/grade")
    @Operation(
            operationId = "quizGradeAnswer",
            summary = "Grade a short-answer response",
            description = "Main errors: QUIZ_GRADING_FORBIDDEN, QUIZ_TA_SELF_CONFLICT, QUIZ_QUESTION_NOT_FOUND, "
                    + "QUIZ_ATTEMPT_NOT_FOUND, QUIZ_ANSWER_INVALID, BAD_REQUEST (score/reason).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "QUIZ_GRADING_FORBIDDEN / QUIZ_TA_SELF_CONFLICT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_QUESTION_NOT_FOUND / QUIZ_ATTEMPT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "QUIZ_ANSWER_INVALID / BAD_REQUEST",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> grade(HttpServletRequest request,
                                   @PathVariable Integer courseId,
                                   @PathVariable Integer quizId,
                                   @PathVariable Integer attemptId,
                                   @PathVariable Integer questionId,
                                   @RequestBody GradeAnswerRequest body) {
        quizGradingService.gradeAnswer(courseId, quizId, attemptId, questionId, currentUserId(request), body);
        return ApiResponse.success();
    }

    @PostMapping("/grades/release")
    @Operation(
            operationId = "quizReleaseGrades",
            summary = "Release grades",
            description = "Optional body.userIds filter; omit for all. Empty array rejected (BAD_REQUEST). "
                    + "Main errors: QUIZ_GRADING_FORBIDDEN, QUIZ_TA_SELF_CONFLICT, BAD_REQUEST.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "QUIZ_GRADING_FORBIDDEN / QUIZ_TA_SELF_CONFLICT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BAD_REQUEST when userIds is an empty array",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> release(HttpServletRequest request,
                                     @PathVariable Integer courseId,
                                     @PathVariable Integer quizId,
                                     @RequestBody(required = false) ReleaseGradesRequest body) {
        quizGradingService.release(courseId, quizId, currentUserId(request), body);
        return ApiResponse.success();
    }

    @PostMapping("/grades/retract")
    @Operation(
            operationId = "quizRetractGrades",
            summary = "Retract released grades",
            description = "Optional body.userIds filter; omit for all. Empty array rejected (BAD_REQUEST). "
                    + "Main errors: QUIZ_GRADING_FORBIDDEN, QUIZ_TA_SELF_CONFLICT, BAD_REQUEST.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "QUIZ_GRADING_FORBIDDEN / QUIZ_TA_SELF_CONFLICT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BAD_REQUEST when userIds is an empty array",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> retract(HttpServletRequest request,
                                     @PathVariable Integer courseId,
                                     @PathVariable Integer quizId,
                                     @RequestBody(required = false) ReleaseGradesRequest body) {
        quizGradingService.retract(courseId, quizId, currentUserId(request), body);
        return ApiResponse.success();
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
