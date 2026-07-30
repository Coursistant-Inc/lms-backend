package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.authoring.CreateQuizRequest;
import com.coursistant.lms.module.quiz.dto.authoring.PatchQuizRequest;
import com.coursistant.lms.module.quiz.dto.authoring.QuizResponse;
import com.coursistant.lms.module.quiz.service.QuizAuthoringService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/quizzes")
public class QuizAuthoringController {

    @Resource
    private QuizAuthoringService quizAuthoringService;

    @GetMapping
    public ApiResponse<List<QuizResponse>> list(HttpServletRequest request,
                                                @PathVariable Integer courseId) {
        return ApiResponse.success(quizAuthoringService.list(request, courseId, currentUserId(request)));
    }

    @GetMapping("/{quizId}")
    public ApiResponse<QuizResponse> detail(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer quizId) {
        return ApiResponse.success(quizAuthoringService.detail(request, courseId, quizId,
                currentUserId(request)));
    }

    @PostMapping
    public ApiResponse<QuizResponse> create(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @RequestBody CreateQuizRequest body) {
        return ApiResponse.success(quizAuthoringService.create(courseId, currentUserId(request), body),
                "Quiz created");
    }

    @PatchMapping("/{quizId}")
    public ApiResponse<QuizResponse> patch(HttpServletRequest request,
                                           @PathVariable Integer courseId,
                                           @PathVariable Integer quizId,
                                           @RequestBody PatchQuizRequest body) {
        return ApiResponse.success(quizAuthoringService.patch(courseId, quizId, currentUserId(request), body));
    }

    @PostMapping("/{quizId}/publish")
    public ApiResponse<QuizResponse> publish(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer quizId) {
        return ApiResponse.success(quizAuthoringService.publish(courseId, quizId, currentUserId(request)));
    }

    @PostMapping("/{quizId}/unpublish")
    public ApiResponse<QuizResponse> unpublish(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer quizId) {
        return ApiResponse.success(quizAuthoringService.unpublish(courseId, quizId, currentUserId(request)));
    }

    @DeleteMapping("/{quizId}")
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
