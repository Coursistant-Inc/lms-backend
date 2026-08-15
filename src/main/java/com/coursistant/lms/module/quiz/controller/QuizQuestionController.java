package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.authoring.*;
import com.coursistant.lms.module.quiz.service.QuizQuestionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@RequestMapping("/v2/courses/{courseId}/quizzes/{quizId}/questions")
@Tag(name = "Quiz Questions",
        description = "Question CRUD; list/get are role-shaped (QuestionResponse vs StudentQuestionResponse)")
public class QuizQuestionController {

    @Resource
    private QuizQuestionService quizQuestionService;

    @GetMapping
    @Operation(
            operationId = "quizQuestionList",
            summary = "List questions (role-shaped)",
            description = "Instructors receive QuestionResponse (includes answer keys / version). "
                    + "Students receive StudentQuestionResponse without answer-key fields.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "ApiResponse envelope; data is an array of QuestionResponse or StudentQuestionResponse",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(oneOf = {
                                    QuestionResponse.class,
                                    StudentQuestionResponse.class
                            }))))
    })
    public ApiResponse<List<? extends QuestionView>> list(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer quizId) {
        return ApiResponse.success(quizQuestionService.listQuestions(request, courseId, quizId, currentUserId(request)));
    }

    @PostMapping
    @Operation(operationId = "quizQuestionCreate", summary = "Create a question (instructor)")
    public ApiResponse<QuestionResponse> create(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer quizId,
                                                @RequestBody CreateQuestionRequest body) {
        return ApiResponse.success(quizQuestionService.create(courseId, quizId, currentUserId(request), body));
    }

    @GetMapping("/{questionId}")
    @Operation(
            operationId = "quizQuestionGet",
            summary = "Get a question (role-shaped)",
            description = "Instructors receive QuestionResponse (includes answer keys / version). "
                    + "Students receive StudentQuestionResponse without answer-key fields.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "ApiResponse envelope; data is oneOf QuestionResponse | StudentQuestionResponse",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {
                                    QuestionResponse.class,
                                    StudentQuestionResponse.class
                            }))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_QUESTION_NOT_FOUND / QUIZ_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<? extends QuestionView> get(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @PathVariable Integer quizId,
                                                   @PathVariable Integer questionId) {
        return ApiResponse.success(quizQuestionService.getQuestion(request, courseId, quizId, questionId,
                currentUserId(request)));
    }

    @PatchMapping("/{questionId}")
    @Operation(operationId = "quizQuestionPatch", summary = "Patch question stem/points/options (instructor)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_VERSION_CONFLICT / QUIZ_CONTENT_LOCKED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<QuestionResponse> patch(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer quizId,
                                               @PathVariable Integer questionId,
                                               @RequestBody PatchQuestionRequest body) {
        return ApiResponse.success(quizQuestionService.patch(courseId, quizId, questionId, currentUserId(request), body));
    }

    @DeleteMapping("/{questionId}")
    @Operation(operationId = "quizQuestionDelete", summary = "Delete a question (instructor)")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                      @PathVariable Integer courseId,
                                      @PathVariable Integer quizId,
                                      @PathVariable Integer questionId) {
        quizQuestionService.delete(courseId, quizId, questionId, currentUserId(request));
        return ApiResponse.success();
    }

    @PutMapping("/order")
    @Operation(operationId = "quizQuestionReorder", summary = "Reorder questions (instructor)")
    public ApiResponse<Void> reorder(HttpServletRequest request,
                                     @PathVariable Integer courseId,
                                     @PathVariable Integer quizId,
                                     @RequestBody ReorderQuestionsRequest body) {
        quizQuestionService.reorder(courseId, quizId, currentUserId(request), body);
        return ApiResponse.success();
    }

    @PatchMapping("/{questionId}/answer-key")
    @Operation(
            operationId = "quizQuestionPatchAnswerKey",
            summary = "Patch answer key (instructor)",
            description = "May trigger regrade when attempts exist. Main errors: QUIZ_VERSION_CONFLICT, QUIZ_QUESTION_NOT_FOUND.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_VERSION_CONFLICT",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_QUESTION_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<QuestionResponse> patchAnswerKey(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer quizId,
                                                        @PathVariable Integer questionId,
                                                        @RequestBody PatchAnswerKeyRequest body) {
        return ApiResponse.success(quizQuestionService.patchAnswerKey(courseId, quizId, questionId,
                currentUserId(request), body));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
