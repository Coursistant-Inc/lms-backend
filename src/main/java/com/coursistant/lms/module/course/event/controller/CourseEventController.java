package com.coursistant.lms.module.course.event.controller;

import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.event.dto.CourseEventResponse;
import com.coursistant.lms.module.course.event.dto.CreateCourseEventRequest;
import com.coursistant.lms.module.course.event.dto.UpdateCourseEventRequest;
import com.coursistant.lms.module.course.event.service.CourseEventService;
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

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/events")
public class CourseEventController {

    @Resource
    private CourseEventService courseEventService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @GetMapping
    public ApiResponse<List<CourseEventResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        if (!coursePermissionService.isAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(courseId, currentUserId(request));
        }
        return ApiResponse.success(courseEventService.listByCourseId(courseId));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<CourseEventResponse> get(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer eventId) {
        if (!coursePermissionService.isAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(courseId, currentUserId(request));
        }
        return ApiResponse.success(courseEventService.getById(courseId, eventId));
    }

    @Idempotent
    @PostMapping
    public ApiResponse<CourseEventResponse> create(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @RequestBody CreateCourseEventRequest body) {
        coursePermissionService.requireCanManageCourseEvents(courseId, currentUserId(request));
        return ApiResponse.success(courseEventService.create(courseId, body));
    }

    @Idempotent
    @PutMapping("/{eventId}")
    public ApiResponse<CourseEventResponse> update(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @PathVariable Integer eventId,
                                                   @RequestBody UpdateCourseEventRequest body) {
        coursePermissionService.requireCanManageCourseEvents(courseId, currentUserId(request));
        return ApiResponse.success(courseEventService.update(courseId, eventId, body));
    }

    @DeleteMapping("/{eventId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer eventId) {
        coursePermissionService.requireCanManageCourseEvents(courseId, currentUserId(request));
        courseEventService.delete(courseId, eventId);
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
