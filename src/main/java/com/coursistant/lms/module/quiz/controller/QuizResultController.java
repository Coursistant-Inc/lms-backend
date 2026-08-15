package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.result.MyResultResponse;
import com.coursistant.lms.module.quiz.service.QuizResultService;
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
@Tag(name = "Quiz Results", description = "Student-facing quiz results and attempt summaries")
public class QuizResultController {

    @Resource
    private QuizResultService quizResultService;

    @GetMapping("/my-result")
    @Operation(
            operationId = "quizMyResult",
            summary = "Get my counted quiz result",
            description = "Main errors: QUIZ_NOT_FOUND, NOT_FOUND (no grade yet).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_NOT_FOUND / NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<MyResultResponse> myResult(HttpServletRequest request,
                                                    @PathVariable Integer courseId,
                                                    @PathVariable Integer quizId) {
        return ApiResponse.success(quizResultService.myResult(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/my-attempts")
    @Operation(operationId = "quizMyAttempts", summary = "List my attempt result summaries")
    public ApiResponse<List<MyResultResponse>> myAttempts(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer quizId) {
        return ApiResponse.success(quizResultService.myAttemptsSummary(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/attempts/{attemptId}/result")
    @Operation(
            operationId = "quizAttemptResult",
            summary = "Get result for a specific attempt",
            description = "Main errors: QUIZ_NOT_FOUND, QUIZ_ATTEMPT_NOT_FOUND, ACCESS_DENIED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_NOT_FOUND / QUIZ_ATTEMPT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<MyResultResponse> attemptResult(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer quizId,
                                                       @PathVariable Integer attemptId) {
        return ApiResponse.success(quizResultService.attemptResult(courseId, quizId, attemptId,
                currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
