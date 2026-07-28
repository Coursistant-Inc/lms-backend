package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.grading.*;
import com.coursistant.lms.module.quiz.service.QuizGradingService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/quizzes/{quizId}")
public class QuizGradingController {

    @Resource
    private QuizGradingService quizGradingService;

    @GetMapping("/grading-summary")
    public ApiResponse<GradingSummaryResponse> summary(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer quizId) {
        return ApiResponse.success(quizGradingService.summary(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/grading/questions/{questionId}/answers")
    public ApiResponse<List<ShortAnswerGradingItem>> listAnswers(HttpServletRequest request,
                                                                   @PathVariable Integer courseId,
                                                                   @PathVariable Integer quizId,
                                                                   @PathVariable Integer questionId) {
        return ApiResponse.success(quizGradingService.listShortAnswers(courseId, quizId, questionId,
                currentUserId(request)));
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}/grade")
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
    public ApiResponse<Void> release(HttpServletRequest request,
                                     @PathVariable Integer courseId,
                                     @PathVariable Integer quizId,
                                     @RequestBody(required = false) ReleaseGradesRequest body) {
        quizGradingService.release(courseId, quizId, currentUserId(request), body);
        return ApiResponse.success();
    }

    @PostMapping("/grades/retract")
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
