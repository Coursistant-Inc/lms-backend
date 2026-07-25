package com.coursistant.lms.module.course.course.controller;

import com.coursistant.lms.module.course.course.dto.CoursePageResponse;
import com.coursistant.lms.module.course.course.dto.CourseResponse;
import com.coursistant.lms.module.course.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.course.dto.TransferInstructorRequest;
import com.coursistant.lms.module.course.course.dto.UpdateCourseRequest;
import com.coursistant.lms.module.course.course.service.CourseService;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/courses")
public class CourseController {

    @Resource
    private CourseService courseService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Idempotent
    @PostMapping
    public ApiResponse<CourseResponse> create(HttpServletRequest request,
                                              @RequestBody CreateCourseRequest body) {
        return ApiResponse.success(courseService.create(currentUserId(request), body));
    }

    @GetMapping
    public ApiResponse<CoursePageResponse> list(HttpServletRequest request,
                                                @RequestParam(value = "q", required = false) String q,
                                                @RequestParam(value = "state", required = false) String state,
                                                @RequestParam(value = "page", required = false) Integer page,
                                                @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.success(courseService.listForBrowse(
                coursePermissionService.isAdmin(request),
                currentUserId(request),
                q,
                state,
                page,
                size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getById(HttpServletRequest request, @PathVariable Integer id) {
        if (!coursePermissionService.isAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(id, currentUserId(request));
        }
        return ApiResponse.success(courseService.getById(id));
    }

    @Idempotent
    @PutMapping("/{id}")
    public ApiResponse<CourseResponse> update(HttpServletRequest request,
                                              @PathVariable Integer id,
                                              @RequestBody UpdateCourseRequest body) {
        return ApiResponse.success(courseService.update(currentUserId(request), id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Integer id) {
        courseService.delete(currentUserId(request), id);
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/{id}/archive")
    public ApiResponse<CourseResponse> archive(HttpServletRequest request, @PathVariable Integer id) {
        return ApiResponse.success(courseService.archive(currentUserId(request), id));
    }

    @Idempotent
    @PostMapping("/{id}/unarchive")
    public ApiResponse<CourseResponse> unarchive(HttpServletRequest request, @PathVariable Integer id) {
        return ApiResponse.success(courseService.unarchive(currentUserId(request), id));
    }

    @Idempotent
    @PostMapping("/{id}/transfer-instructor")
    public ApiResponse<CourseResponse> transferInstructor(HttpServletRequest request,
                                                          @PathVariable Integer id,
                                                          @RequestBody TransferInstructorRequest body) {
        return ApiResponse.success(courseService.transferInstructor(currentUserId(request), id, body));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}