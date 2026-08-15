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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Courses", description = "Course lifecycle, archive, and primary instructor")
public class CourseController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private CourseService courseService;

    @Resource
    private CourseAuthorizationService courseAuthorizationService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @Idempotent
    @PostMapping
    @Operation(
            operationId = "courseCreate",
            summary = "Create a course",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<CourseResponse> create(HttpServletRequest request,
                                              @RequestBody CreateCourseRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.create(actor, body, requestId(request)));
    }

    @GetMapping
    @Operation(operationId = "courseList", summary = "Browse/list courses with optional filters")
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
    @Operation(operationId = "courseGetById", summary = "Get course by id")
    public ApiResponse<CourseResponse> getById(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseAuthorizationService.requireVisibleCourse(actor, id);
        return ApiResponse.success(courseService.getById(id));
    }

    @Idempotent
    @PatchMapping("/{id}")
    @Operation(
            operationId = "coursePatch",
            summary = "Partial update of course fields",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<CourseResponse> patch(HttpServletRequest request,
                                             @PathVariable Integer id,
                                             @RequestBody PatchCourseRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.patch(actor, id, body, requestId(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "courseDelete", summary = "Delete a course")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseService.delete(actor, id, requestId(request));
        return ApiResponse.success();
    }

    @Idempotent
    @PostMapping("/{id}/archive")
    @Operation(
            operationId = "courseArchive",
            summary = "Archive a course",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<CourseResponse> archive(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.archive(actor, id, requestId(request)));
    }

    @Idempotent
    @PostMapping("/{id}/unarchive")
    @Operation(
            operationId = "courseUnarchive",
            summary = "Unarchive a course",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<CourseResponse> unarchive(HttpServletRequest request, @PathVariable Integer id) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseService.unarchive(actor, id, requestId(request)));
    }

    @Idempotent
    @PostMapping("/{id}/primary-instructor")
    @Operation(
            operationId = "courseReassignPrimaryInstructor",
            summary = "Reassign the primary instructor",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
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
