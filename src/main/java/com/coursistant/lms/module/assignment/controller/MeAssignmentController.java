package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.UpcomingAssignmentDeadlineResponse;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/me/assignments")
public class MeAssignmentController {

    @Resource
    private AssignmentService assignmentService;

    @GetMapping("/upcoming")
    public ApiResponse<List<UpcomingAssignmentDeadlineResponse>> upcoming(
            HttpServletRequest request,
            @RequestParam(required = false) Integer days) {
        return ApiResponse.success(
                assignmentService.listUpcomingDeadlines(currentUserId(request), days));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
