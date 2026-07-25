package com.coursistant.lms.module.course.schedule.controller;

import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.schedule.dto.CreateSessionRequest;
import com.coursistant.lms.module.course.schedule.dto.SessionResponse;
import com.coursistant.lms.module.course.schedule.dto.UpdateSessionRequest;
import com.coursistant.lms.module.course.schedule.service.CourseSessionService;
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
@RequestMapping("/v2/courses/{courseId}/sessions")
public class CourseSessionController {

    @Resource
    private CourseSessionService courseSessionService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        if (!coursePermissionService.isAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(courseId, currentUserId(request));
        }
        return ApiResponse.success(courseSessionService.listByCourseId(courseId));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> get(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer sessionId) {
        if (!coursePermissionService.isAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(courseId, currentUserId(request));
        }
        return ApiResponse.success(courseSessionService.getById(courseId, sessionId));
    }

    @Idempotent
    @PostMapping
    public ApiResponse<SessionResponse> create(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @RequestBody CreateSessionRequest body) {
        coursePermissionService.requireInstructor(courseId, currentUserId(request));
        return ApiResponse.success(courseSessionService.create(courseId, body));
    }

    @Idempotent
    @PutMapping("/{sessionId}")
    public ApiResponse<SessionResponse> update(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer sessionId,
                                               @RequestBody UpdateSessionRequest body) {
        coursePermissionService.requireInstructor(courseId, currentUserId(request));
        return ApiResponse.success(courseSessionService.update(courseId, sessionId, body));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer sessionId) {
        coursePermissionService.requireInstructor(courseId, currentUserId(request));
        courseSessionService.delete(courseId, sessionId);
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
