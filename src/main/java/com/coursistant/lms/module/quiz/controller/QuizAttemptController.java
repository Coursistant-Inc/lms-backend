package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.attempt.*;
import com.coursistant.lms.module.quiz.service.QuizAttemptService;
import com.coursistant.lms.module.quiz.service.QuizAutosaveService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
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
@Tag(name = "Quiz Attempts", description = "Start, autosave, submit, and inspect quiz attempts")
public class QuizAttemptController {

    @Resource
    private QuizAttemptService quizAttemptService;
    @Resource
    private QuizAutosaveService quizAutosaveService;

    @Idempotent
    @PostMapping("/attempts")
    @Operation(
            operationId = "quizStartAttempt",
            summary = "Start a new attempt",
            description = "Idempotent via Idempotency-Key header. Students only. "
                    + "Main errors: QUIZ_NOT_FOUND, QUIZ_NOT_PUBLISHED, QUIZ_WINDOW_CLOSED, "
                    + "QUIZ_ATTEMPTS_EXCEEDED, QUIZ_FEATURE_DISABLED, ACCESS_DENIED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_NOT_FOUND / QUIZ_NOT_PUBLISHED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_WINDOW_CLOSED / QUIZ_ATTEMPTS_EXCEEDED / QUIZ_FEATURE_DISABLED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED / FORBIDDEN",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AttemptResponse> start(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer quizId) {
        return ApiResponse.success(quizAttemptService.start(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/attempts/current")
    @Operation(
            operationId = "quizGetCurrentAttempt",
            summary = "Get current in-progress attempt for the caller",
            description = "Main errors: QUIZ_NOT_FOUND, QUIZ_ATTEMPT_NOT_FOUND.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_NOT_FOUND / QUIZ_ATTEMPT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AttemptResponse> current(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer quizId) {
        return ApiResponse.success(quizAttemptService.getCurrent(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/attempts")
    @Operation(
            operationId = "quizListAttempts",
            summary = "List attempts",
            description = "Students see own attempts; instructors may filter by userId.")
    public ApiResponse<List<AttemptSummaryResponse>> list(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer quizId,
                                                          @RequestParam(required = false) Integer userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.success(quizAttemptService.listAttempts(request, courseId, quizId,
                currentUserId(request), userId, page, pageSize));
    }

    @GetMapping("/attempts/{attemptId}")
    @Operation(
            operationId = "quizGetAttempt",
            summary = "Get attempt detail",
            description = "Main errors: QUIZ_ATTEMPT_NOT_FOUND, ACCESS_DENIED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_ATTEMPT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AttemptResponse> detail(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer quizId,
                                               @PathVariable Integer attemptId) {
        return ApiResponse.success(quizAttemptService.getAttempt(request, courseId, quizId, attemptId,
                currentUserId(request)));
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}")
    @Operation(
            operationId = "quizAutosave",
            summary = "Autosave one answer",
            description = "Main errors: QUIZ_NOT_FOUND, QUIZ_ATTEMPT_NOT_IN_PROGRESS, "
                    + "QUIZ_QUESTION_NOT_FOUND, QUIZ_ANSWER_INVALID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_NOT_FOUND / QUIZ_QUESTION_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_ATTEMPT_NOT_IN_PROGRESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "QUIZ_ANSWER_INVALID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AutosaveResponse> autosave(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer quizId,
                                                  @PathVariable Integer attemptId,
                                                  @PathVariable Integer questionId,
                                                  @RequestBody AutosaveRequest body) {
        return ApiResponse.success(quizAutosaveService.autosave(courseId, quizId, attemptId, questionId,
                currentUserId(request), body));
    }

    @Idempotent
    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(
            operationId = "quizSubmitAttempt",
            summary = "Submit an in-progress attempt",
            description = "Idempotent via Idempotency-Key header. "
                    + "Main errors: QUIZ_ATTEMPT_NOT_FOUND, QUIZ_ATTEMPT_NOT_IN_PROGRESS, ACCESS_DENIED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_ATTEMPT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_ATTEMPT_NOT_IN_PROGRESS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AttemptResponse> submit(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer quizId,
                                             @PathVariable Integer attemptId) {
        return ApiResponse.success(quizAttemptService.submit(courseId, quizId, attemptId, currentUserId(request)),
                "Submitted");
    }

    @GetMapping("/attempts/{attemptId}/receipt")
    @Operation(
            operationId = "quizGetReceipt",
            summary = "Get submission receipt",
            description = "Main errors: QUIZ_ATTEMPT_NOT_FOUND (or receipt not issued), ACCESS_DENIED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_ATTEMPT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<ReceiptResponse> receipt(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer quizId,
                                                @PathVariable Integer attemptId) {
        return ApiResponse.success(quizAttemptService.getReceipt(courseId, quizId, attemptId, currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
