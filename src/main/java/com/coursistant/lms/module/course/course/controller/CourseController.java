package com.coursistant.lms.module.course.course.controller;

import com.coursistant.lms.module.course.course.dto.CoursePageResponse;
import com.coursistant.lms.module.course.course.dto.CourseResponse;
import com.coursistant.lms.module.course.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.course.dto.PatchCourseRequest;
import com.coursistant.lms.module.course.course.dto.ReassignPrimaryInstructorRequest;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.course.service.CourseService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private CourseAuthorizationService courseAuthorizationService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @Idempotent
    @PostMapping
    public ApiResponse<CourseResponse> create(HttpServletRequest request,
                                              @RequestBody CreateCourseRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.create(actor, body, requestId(request)));
    }

    @GetMapping
    public ApiResponse<CoursePageResponse> list(HttpServletRequest request,
                                                @RequestParam(value = "q", required = false) String q,
                                                @RequestParam(value = "state", required = false) String state,
                                                @RequestParam(value = "tenantId", required = false) Integer tenantId,
                                                @RequestParam(value = "page", required = false) Integer page,
                                                @RequestParam(value = "size", required = false) Integer size) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.listForBrowse(actor, q, state, tenantId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getById(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseAuthorizationService.requireVisibleCourse(actor, id);
        return ApiResponse.success(courseService.getById(id));
    }

    @Idempotent
    @PatchMapping("/{id}")
    public ApiResponse<CourseResponse> patch(HttpServletRequest request,
                                             @PathVariable Integer id,
                                             @RequestBody PatchCourseRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.patch(actor, id, body, requestId(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseService.delete(actor, id, requestId(request));
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/{id}/archive")
    public ApiResponse<CourseResponse> archive(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.archive(actor, id, requestId(request)));
    }

    @Idempotent
    @PostMapping("/{id}/unarchive")
    public ApiResponse<CourseResponse> unarchive(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.unarchive(actor, id, requestId(request)));
    }

    @Idempotent
    @PostMapping("/{id}/primary-instructor")
    public ApiResponse<CourseResponse> reassignPrimaryInstructor(HttpServletRequest request,
                                                                 @PathVariable Integer id,
                                                                 @RequestBody ReassignPrimaryInstructorRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.reassignPrimaryInstructor(actor, id, body, requestId(request)));
    }

    private static String requestId(HttpServletRequest request) {
        String idem = request.getHeader("Idempotency-Key");
        if (idem != null && !idem.isBlank()) {
            return idem.trim();
        }
        String req = request.getHeader("X-Request-Id");
        if (req != null && !req.isBlank()) {
            return req.trim();
        }
        return null;
    }
}
