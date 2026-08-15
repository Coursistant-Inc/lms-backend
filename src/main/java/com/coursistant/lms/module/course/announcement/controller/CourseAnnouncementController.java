package com.coursistant.lms.module.course.announcement.controller;

import com.coursistant.lms.module.course.announcement.dto.AnnouncementResponse;
import com.coursistant.lms.module.course.announcement.dto.AnnouncementSummaryResponse;
import com.coursistant.lms.module.course.announcement.dto.CreateAnnouncementRequest;
import com.coursistant.lms.module.course.announcement.dto.UpdateAnnouncementRequest;
import com.coursistant.lms.module.course.announcement.service.CourseAnnouncementService;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/announcements")
@Tag(name = "Announcements", description = "Course announcements")
public class CourseAnnouncementController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private CourseAnnouncementService courseAnnouncementService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @GetMapping
    @Operation(operationId = "courseAnnouncementList", summary = "List course announcements")
    public ApiResponse<List<AnnouncementSummaryResponse>> list(HttpServletRequest request,
                                                               @PathVariable Integer courseId) {
        Integer userId = currentUserId(request);
        if (!coursePermissionService.isSystemAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(courseId, userId);
        }
        return ApiResponse.success(courseAnnouncementService.listByCourse(courseId, userId));
    }

    @GetMapping("/{announcementId}")
    @Operation(operationId = "courseAnnouncementGet", summary = "Get an announcement by id")
    public ApiResponse<AnnouncementResponse> get(HttpServletRequest request,
                                                 @PathVariable Integer courseId,
                                                 @PathVariable Integer announcementId) {
        Integer userId = currentUserId(request);
        if (!coursePermissionService.isSystemAdmin(request)) {
            coursePermissionService.requireActiveEnrollment(courseId, userId);
        }
        return ApiResponse.success(courseAnnouncementService.getById(courseId, announcementId, userId));
    }

    @Idempotent
    @PostMapping
    @Operation(
            operationId = "courseAnnouncementCreate",
            summary = "Create an announcement",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<AnnouncementResponse> create(HttpServletRequest request,
                                                    @PathVariable Integer courseId,
                                                    @RequestBody CreateAnnouncementRequest body) {
        Integer userId = currentUserId(request);
        return ApiResponse.success(courseAnnouncementService.create(courseId, userId, body));
    }

    @Idempotent
    @PatchMapping("/{announcementId}")
    @Operation(
            operationId = "courseAnnouncementUpdate",
            summary = "Update an announcement",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<AnnouncementResponse> update(HttpServletRequest request,
                                                    @PathVariable Integer courseId,
                                                    @PathVariable Integer announcementId,
                                                    @RequestBody UpdateAnnouncementRequest body) {
        Integer userId = currentUserId(request);
        return ApiResponse.success(courseAnnouncementService.update(courseId, announcementId, userId, body));
    }

    @DeleteMapping("/{announcementId}")
    @Operation(
            operationId = "courseAnnouncementDelete",
            summary = "Delete an announcement",
            description = "Returns 204 No Content with an empty body on success."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Announcement deleted; no response body",
                    content = @Content)
    })
    public ResponseEntity<Void> delete(HttpServletRequest request,
                                       @PathVariable Integer courseId,
                                       @PathVariable Integer announcementId,
                                       @RequestParam(required = false) Boolean confirm) {
        Integer userId = currentUserId(request);
        courseAnnouncementService.delete(courseId, announcementId, userId, confirm);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
