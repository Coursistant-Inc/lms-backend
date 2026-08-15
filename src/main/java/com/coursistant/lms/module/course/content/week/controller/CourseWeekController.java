package com.coursistant.lms.module.course.content.week.controller;

import com.coursistant.lms.module.course.content.week.dto.CreateWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.RenameWeekRequest;
import com.coursistant.lms.module.course.content.week.dto.ReorderWeeksRequest;
import com.coursistant.lms.module.course.content.week.dto.WeekResponse;
import com.coursistant.lms.module.course.content.week.service.CourseWeekService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/weeks")
@Tag(name = "Weeks", description = "Course content weeks and zip download")
public class CourseWeekController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private CourseWeekService courseWeekService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @GetMapping
    @Operation(operationId = "courseWeekList", summary = "List weeks for a course")
    public ApiResponse<List<WeekResponse>> list(HttpServletRequest request, @PathVariable Integer courseId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.list(actor, courseId));
    }

    @Idempotent
    @PostMapping
    @Operation(
            operationId = "courseWeekCreate",
            summary = "Create a week",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<WeekResponse> create(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @RequestBody CreateWeekRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.create(actor, courseId, body));
    }

    @Idempotent
    @PatchMapping("/{weekId}")
    @Operation(
            operationId = "courseWeekRename",
            summary = "Rename a week",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<WeekResponse> rename(HttpServletRequest request,
                                            @PathVariable Integer courseId,
                                            @PathVariable Integer weekId,
                                            @RequestBody RenameWeekRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.rename(actor, courseId, weekId, body));
    }

    @Idempotent
    @PutMapping("/reorder")
    @Operation(
            operationId = "courseWeekReorder",
            summary = "Reorder weeks",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<List<WeekResponse>> reorder(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @RequestBody ReorderWeeksRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.reorder(actor, courseId, body));
    }

    @Idempotent
    @PostMapping("/{weekId}/publish")
    @Operation(
            operationId = "courseWeekPublish",
            summary = "Publish a week",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<WeekResponse> publish(HttpServletRequest request,
                                             @PathVariable Integer courseId,
                                             @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.publish(actor, courseId, weekId));
    }

    @Idempotent
    @PostMapping("/{weekId}/unpublish")
    @Operation(
            operationId = "courseWeekUnpublish",
            summary = "Unpublish a week",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<WeekResponse> unpublish(HttpServletRequest request,
                                               @PathVariable Integer courseId,
                                               @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseWeekService.unpublish(actor, courseId, weekId));
    }

    @DeleteMapping("/{weekId}")
    @Operation(operationId = "courseWeekDelete", summary = "Delete a week")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseWeekService.delete(actor, courseId, weekId);
        return ApiResponse.success();
    }

    @GetMapping("/{weekId}/download.zip")
    @Operation(
            operationId = "courseWeekDownloadZip",
            summary = "Download week materials as ZIP",
            description = "Streams a ZIP archive (not JSON ApiResponse). Content-Disposition is attachment."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "ZIP stream of week materials",
                    content = @Content(mediaType = "application/zip",
                            schema = @Schema(type = "string", format = "binary")),
                    headers = @Header(
                            name = "Content-Disposition",
                            description = "attachment; filename=\"...\"",
                            schema = @Schema(type = "string", example = "attachment; filename=\"week-1.zip\"")))
    })
    public ResponseEntity<StreamingResponseBody> downloadZip(HttpServletRequest request,
                                                             @PathVariable Integer courseId,
                                                             @PathVariable Integer weekId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseWeekService.downloadZip(actor, courseId, weekId);
    }
}
