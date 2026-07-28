package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.authoring.*;
import com.coursistant.lms.module.quiz.service.QuizQuestionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/quizzes/{quizId}/questions")
public class QuizQuestionController {

    @Resource
    private QuizQuestionService quizQuestionService;

    @GetMapping
    public ApiResponse<List<?>> list(HttpServletRequest request,
                                     @PathVariable Integer courseId,
                                     @PathVariable Integer quizId) {
        return ApiResponse.success(quizQuestionService.listQuestions(request, courseId, quizId, currentUserId(request)));
    }

    @PostMapping
    public ApiResponse<QuestionResponse> create(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer quizId,
                                                @RequestBody CreateQuestionRequest body) {
        return ApiResponse.success(quizQuestionService.create(courseId, quizId, currentUserId(request), body));
    }

    @GetMapping("/{questionId}")
    public ApiResponse<Object> get(HttpServletRequest request,
                                   @PathVariable Integer courseId,
                                   @PathVariable Integer quizId,
                                   @PathVariable Integer questionId) {
        return ApiResponse.success(quizQuestionService.getQuestion(request, courseId, quizId, questionId,
                currentUserId(request)));
    }

    @PatchMapping("/{questionId}")
    public ApiResponse<QuestionResponse> patch(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer quizId,
                                               @PathVariable Integer questionId,
                                               @RequestBody PatchQuestionRequest body) {
        return ApiResponse.success(quizQuestionService.patch(courseId, quizId, questionId, currentUserId(request), body));
    }

    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                      @PathVariable Integer courseId,
                                      @PathVariable Integer quizId,
                                      @PathVariable Integer questionId) {
        quizQuestionService.delete(courseId, quizId, questionId, currentUserId(request));
        return ApiResponse.success();
    }

    @PutMapping("/order")
    public ApiResponse<Void> reorder(HttpServletRequest request,
                                     @PathVariable Integer courseId,
                                     @PathVariable Integer quizId,
                                     @RequestBody ReorderQuestionsRequest body) {
        quizQuestionService.reorder(courseId, quizId, currentUserId(request), body);
        return ApiResponse.success();
    }

    @PatchMapping("/{questionId}/answer-key")
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
