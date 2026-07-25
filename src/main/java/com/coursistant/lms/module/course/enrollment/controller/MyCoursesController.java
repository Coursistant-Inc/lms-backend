package com.coursistant.lms.module.course.enrollment.controller;

import com.coursistant.lms.module.course.enrollment.dto.MyCourseResponse;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/me/courses")
public class MyCoursesController {

    @Resource
    private EnrollmentService enrollmentService;

    @GetMapping
    public ApiResponse<List<MyCourseResponse>> list(HttpServletRequest request) {
        return ApiResponse.success(enrollmentService.listMyCourses(currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
