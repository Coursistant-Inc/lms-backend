package com.coursistant.lms.module.quiz.controller;

import com.coursistant.lms.module.quiz.dto.attempt.*;
import com.coursistant.lms.module.quiz.service.QuizAttemptService;
import com.coursistant.lms.module.quiz.service.QuizAutosaveService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/quizzes/{quizId}")
public class QuizAttemptController {

    @Resource
    private QuizAttemptService quizAttemptService;
    @Resource
    private QuizAutosaveService quizAutosaveService;

    @Idempotent
    @PostMapping("/attempts")
    public ApiResponse<AttemptResponse> start(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer quizId) {
        return ApiResponse.success(quizAttemptService.start(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/attempts/current")
    public ApiResponse<AttemptResponse> current(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer quizId) {
        return ApiResponse.success(quizAttemptService.getCurrent(courseId, quizId, currentUserId(request)));
    }

    @GetMapping("/attempts")
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
    public ApiResponse<AttemptResponse> detail(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer quizId,
                                               @PathVariable Integer attemptId) {
        return ApiResponse.success(quizAttemptService.getAttempt(request, courseId, quizId, attemptId,
                currentUserId(request)));
    }

    @PutMapping("/attempts/{attemptId}/answers/{questionId}")
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
    public ApiResponse<AttemptResponse> submit(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer quizId,
                                             @PathVariable Integer attemptId) {
        return ApiResponse.success(quizAttemptService.submit(courseId, quizId, attemptId, currentUserId(request)),
                "Submitted");
    }

    @GetMapping("/attempts/{attemptId}/receipt")
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
