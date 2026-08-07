package com.coursistant.lms.module.course.schedule.controller;

import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.schedule.dto.CreateSessionRequest;
import com.coursistant.lms.module.course.schedule.dto.SessionResponse;
import com.coursistant.lms.module.course.schedule.dto.UpdateSessionRequest;
import com.coursistant.lms.module.course.schedule.service.CourseSessionService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
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
    private ActorContextResolver actorContextResolver;

    @Resource
    private CourseAuthorizationService courseAuthorizationService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        return ApiResponse.success(courseSessionService.listByCourseId(courseId));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> get(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer sessionId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        return ApiResponse.success(courseSessionService.getById(courseId, sessionId));
    }

    @Idempotent
    @PostMapping
    public ApiResponse<SessionResponse> create(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @RequestBody CreateSessionRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSessionService.create(actor, courseId, body));
    }

    @Idempotent
    @PutMapping("/{sessionId}")
    public ApiResponse<SessionResponse> update(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer sessionId,
                                               @RequestBody UpdateSessionRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseSessionService.update(actor, courseId, sessionId, body));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer sessionId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseSessionService.delete(actor, courseId, sessionId);
        return ApiResponse.success();
    }
}
