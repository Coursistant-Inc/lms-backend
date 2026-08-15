package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.authoring.CreateQuizRequest;
import com.coursistant.lms.module.quiz.dto.authoring.PatchQuizRequest;
import com.coursistant.lms.module.quiz.dto.authoring.QuizResponse;
import com.coursistant.lms.module.quiz.service.QuizAuthoringService;
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
@RequestMapping("/v2/courses/{courseId}/quizzes")
@Tag(name = "Quizzes", description = "Quiz authoring: list/create/patch/publish/delete")
public class QuizAuthoringController {

    @Resource
    private QuizAuthoringService quizAuthoringService;

    @GetMapping
    @Operation(operationId = "quizList", summary = "List quizzes in a course")
    public ApiResponse<List<QuizResponse>> list(HttpServletRequest request,
                                                @PathVariable Integer courseId) {
        return ApiResponse.success(quizAuthoringService.list(request, courseId, currentUserId(request)));
    }

    @GetMapping("/{quizId}")
    @Operation(operationId = "quizGet", summary = "Get quiz detail")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "QUIZ_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<QuizResponse> detail(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer quizId) {
        return ApiResponse.success(quizAuthoringService.detail(request, courseId, quizId,
                currentUserId(request)));
    }

    @PostMapping
    @Operation(operationId = "quizCreate", summary = "Create a draft quiz")
    public ApiResponse<QuizResponse> create(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @RequestBody CreateQuizRequest body) {
        return ApiResponse.success(quizAuthoringService.create(courseId, currentUserId(request), body),
                "Quiz created");
    }

    @PatchMapping("/{quizId}")
    @Operation(operationId = "quizPatch", summary = "Patch quiz settings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_VERSION_CONFLICT / QUIZ_CONTENT_LOCKED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<QuizResponse> patch(HttpServletRequest request,
                                           @PathVariable Integer courseId,
                                           @PathVariable Integer quizId,
                                           @RequestBody PatchQuizRequest body) {
        return ApiResponse.success(quizAuthoringService.patch(courseId, quizId, currentUserId(request), body));
    }

    @PostMapping("/{quizId}/publish")
    @Operation(operationId = "quizPublish", summary = "Publish a quiz")
    public ApiResponse<QuizResponse> publish(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer quizId) {
        return ApiResponse.success(quizAuthoringService.publish(courseId, quizId, currentUserId(request)));
    }

    @PostMapping("/{quizId}/unpublish")
    @Operation(operationId = "quizUnpublish", summary = "Unpublish a quiz (blocked if attempts exist)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_HAS_ATTEMPTS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<QuizResponse> unpublish(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer quizId) {
        return ApiResponse.success(quizAuthoringService.unpublish(courseId, quizId, currentUserId(request)));
    }

    @DeleteMapping("/{quizId}")
    @Operation(
            operationId = "quizDelete",
            summary = "Delete a quiz",
            description = "Requires confirm=true. Fails with QUIZ_HAS_ATTEMPTS when attempts exist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BAD_REQUEST when confirm is not true",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "QUIZ_HAS_ATTEMPTS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer quizId,
                                    @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Pass confirm=true to delete quiz");
        }
        quizAuthoringService.delete(courseId, quizId, currentUserId(request));
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
