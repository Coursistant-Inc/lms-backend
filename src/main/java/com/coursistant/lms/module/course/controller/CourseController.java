package com.coursistant.lms.module.course.controller;

import com.coursistant.lms.module.course.dto.CourseResponse;
import com.coursistant.lms.module.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.dto.UpdateCourseRequest;
import com.coursistant.lms.module.course.service.CourseService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/courses")
public class CourseController {

    @Resource
    private CourseService courseService;

    @Idempotent
    @PostMapping
    public ApiResponse<CourseResponse> create(HttpServletRequest request,
                                              @RequestBody CreateCourseRequest body) {
        return ApiResponse.success(courseService.create(currentUserId(request), body));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getById(@PathVariable Integer id) {
        return ApiResponse.success(courseService.getById(id));
    }

    @Idempotent
    @PutMapping("/{id}")
    public ApiResponse<CourseResponse> update(@PathVariable Integer id,
                                              @RequestBody UpdateCourseRequest body) {
        return ApiResponse.success(courseService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        courseService.delete(id);
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/{id}/archive")
    public ApiResponse<CourseResponse> archive(@PathVariable Integer id) {
        return ApiResponse.success(courseService.archive(id));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
