package com.coursistant.lms.module.course.event.controller;

import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.event.dto.CourseEventResponse;
import com.coursistant.lms.module.course.event.dto.CreateCourseEventRequest;
import com.coursistant.lms.module.course.event.dto.UpdateCourseEventRequest;
import com.coursistant.lms.module.course.event.service.CourseEventService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Events", description = "Course calendar events")
public class CourseEventController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private CourseEventService courseEventService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @Resource
    private CourseAuthorizationService courseAuthorizationService;

    @GetMapping
    @Operation(operationId = "courseEventList", summary = "List events for a course")
    public ApiResponse<List<CourseEventResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        return ApiResponse.success(courseEventService.listByCourseId(courseId));
    }

    @GetMapping("/{eventId}")
    @Operation(operationId = "courseEventGet", summary = "Get a course event by id")
    public ApiResponse<CourseEventResponse> get(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer eventId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        return ApiResponse.success(courseEventService.getById(courseId, eventId));
    }

    @Idempotent
    @PostMapping
    @Operation(
            operationId = "courseEventCreate",
            summary = "Create a course event",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<CourseEventResponse> create(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @RequestBody CreateCourseEventRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseEventService.create(actor, courseId, body));
    }

    @Idempotent
    @PutMapping("/{eventId}")
    @Operation(
            operationId = "courseEventUpdate",
            summary = "Update a course event",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<CourseEventResponse> update(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @PathVariable Integer eventId,
                                                   @RequestBody UpdateCourseEventRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseEventService.update(actor, courseId, eventId, body));
    }

    @DeleteMapping("/{eventId}")
    @Operation(operationId = "courseEventDelete", summary = "Delete a course event")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer eventId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseEventService.delete(actor, courseId, eventId);
        return ApiResponse.success();
    }
}
