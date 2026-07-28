package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.result.MyResultResponse;
import com.coursistant.lms.module.quiz.service.QuizResultService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/quizzes/{quizId}")
public class QuizResultController {

    @Resource
    private QuizResultService quizResultService;

    @GetMapping("/my-result")
    public ApiResponse<MyResultResponse> myResult(HttpServletRequest request,
                                                    @PathVariable Integer courseId,
                                                    @PathVariable Integer quizId) {
        return ApiResponse.success(quizResultService.myResult(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/my-attempts")
    public ApiResponse<List<MyResultResponse>> myAttempts(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer quizId) {
        return ApiResponse.success(quizResultService.myAttemptsSummary(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/attempts/{attemptId}/result")
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
