package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.AssignmentAttachmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentResponse;
import com.coursistant.lms.module.assignment.dto.AssignmentSummaryResponse;
import com.coursistant.lms.module.assignment.dto.CreateAssignmentRequest;
import com.coursistant.lms.module.assignment.dto.DueDateChangePreviewRequest;
import com.coursistant.lms.module.assignment.dto.DueDateChangePreviewResponse;
import com.coursistant.lms.module.assignment.dto.PatchAssignmentRequest;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Assignment CRUD and instructor attachments.
 *
 * <p>Reads are role-shaped: students only ever see Published assignments and receive
 * {@code ASSIGNMENT_NOT_FOUND} for drafts. Write endpoints interpret date fields as
 * wall-clock times in the course tenant timezone.</p>
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments")
@Tag(name = "Assignments", description = "Assignment CRUD, publish lifecycle, and instructor attachments")
public class AssignmentController {

    @Resource
    private AssignmentService assignmentService;

    @GetMapping
    @Operation(
            operationId = "assignmentList",
            summary = "List assignments in a course",
            description = "Instructor/TA see all states including Draft. Students only see Published; "
                    + "drafts are omitted. Response shape is role-specific (staff counters vs student submission state)."
    )
    public ApiResponse<List<AssignmentResponse>> list(HttpServletRequest request,
                                                      @PathVariable Integer courseId) {
        return ApiResponse.success(assignmentService.list(request, courseId, currentUserId(request)));
    }

    /**
     * Slim list cards for any course member: title / due / type / submissionStatus.
     * Declared before {@code /{assignmentId}} so {@code summaries} is not parsed as an id.
     */
    @GetMapping("/summaries")
    @Operation(
            operationId = "assignmentListSummaries",
            summary = "List slim assignment summary cards",
            description = "Any course member. Students only see Published; submissionStatus is student-oriented."
    )
    public ApiResponse<List<AssignmentSummaryResponse>> listSummaries(
            HttpServletRequest request,
            @PathVariable Integer courseId) {
        return ApiResponse.success(assignmentService.listSummaries(request, courseId, currentUserId(request)));
    }

    @GetMapping("/{assignmentId}")
    @Operation(
            operationId = "assignmentGet",
            summary = "Get assignment detail",
            description = "Instructor/TA may open Draft. Students receive ASSIGNMENT_NOT_FOUND for non-Published "
                    + "assignments. Staff get roster/grading counters; students get submission/grade view fields."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND (including student access to Draft) or COURSE_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AssignmentResponse> detail(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentService.detail(request, courseId, assignmentId,
                currentUserId(request)));
    }

    @PostMapping
    @Operation(
            operationId = "assignmentCreate",
            summary = "Create an assignment",
            description = "Instructor/TA with edit permission. dueAt/lateUntil are wall-clock times in the course "
                    + "tenant timezone. Group assignments require submissionType=Group and groupSetId."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING, BAD_REQUEST, ASSIGNMENT_FILE_CONSTRAINT_INVALID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "ASSIGNMENT_GROUP_SET_REQUIRED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AssignmentResponse> create(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @RequestBody CreateAssignmentRequest body) {
        return ApiResponse.success(
                assignmentService.create(courseId, currentUserId(request), body),
                "Assignment created");
    }

    @PatchMapping("/{assignmentId}")
    @Operation(
            operationId = "assignmentPatch",
            summary = "Patch an assignment",
            description = "Instructor/TA. Partial update; only non-null fields apply. Shortening due date may "
                    + "require confirmShortenDueDate=true."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "ASSIGNMENT_TYPE_LOCKED, ASSIGNMENT_GROUP_SET_REQUIRED, "
                            + "ASSIGNMENT_DUE_SHORTEN_CONFIRM_REQUIRED, ASSIGNMENT_HAS_SUBMISSIONS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AssignmentResponse> patch(HttpServletRequest request,
                                                 @PathVariable Integer courseId,
                                                 @PathVariable Integer assignmentId,
                                                 @RequestBody PatchAssignmentRequest body) {
        return ApiResponse.success(assignmentService.patch(courseId, assignmentId, currentUserId(request), body));
    }

    @PostMapping("/{assignmentId}/publish")
    @Operation(
            operationId = "assignmentPublish",
            summary = "Publish an assignment",
            description = "Instructor/TA. Makes the assignment visible to students."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AssignmentResponse> publish(HttpServletRequest request,
                                                   @PathVariable Integer courseId,
                                                   @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentService.publish(courseId, assignmentId, currentUserId(request)));
    }

    @PostMapping("/{assignmentId}/unpublish")
    @Operation(
            operationId = "assignmentUnpublish",
            summary = "Unpublish an assignment",
            description = "Instructor/TA. Returns the assignment to Draft; students then see ASSIGNMENT_NOT_FOUND."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<AssignmentResponse> unpublish(HttpServletRequest request,
                                                     @PathVariable Integer courseId,
                                                     @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentService.unpublish(courseId, assignmentId, currentUserId(request)));
    }

    @DeleteMapping("/{assignmentId}")
    @Operation(
            operationId = "assignmentDelete",
            summary = "Delete an assignment",
            description = "Instructor/TA. May fail when submissions already exist."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "ASSIGNMENT_HAS_SUBMISSIONS",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer assignmentId) {
        assignmentService.delete(courseId, assignmentId, currentUserId(request));
        return ApiResponse.success();
    }

    /**
     * Dry run showing who is affected before a deadline is moved. Writes nothing.
     */
    @PostMapping("/{assignmentId}/due-date-change-preview")
    @Operation(
            operationId = "assignmentPreviewDueDateChange",
            summary = "Preview due-date change impact",
            description = "Instructor/TA dry-run. Writes nothing; reports who would become late / need confirmation."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<DueDateChangePreviewResponse> previewDueDateChange(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @RequestBody DueDateChangePreviewRequest body) {
        return ApiResponse.success(assignmentService.previewDueDateChange(courseId, assignmentId,
                currentUserId(request), body));
    }

    @PostMapping(value = "/{assignmentId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "assignmentUploadAttachments",
            summary = "Upload instructor attachments",
            description = "Instructor/TA. Multipart part `files` (array of binary files).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object"),
                            schemaProperties = {
                                    @SchemaProperty(
                                            name = "files",
                                            array = @ArraySchema(
                                                    schema = @Schema(type = "string", format = "binary")))
                            }
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "UNSUPPORTED_FILE_TYPE, FILE_TOO_LARGE, PARAM_MISSING",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<List<AssignmentAttachmentResponse>> uploadAttachments(
            HttpServletRequest request,
            @PathVariable Integer courseId,
            @PathVariable Integer assignmentId,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return ApiResponse.success(assignmentService.uploadAttachments(courseId, assignmentId,
                currentUserId(request), files));
    }

    @GetMapping("/{assignmentId}/attachments/{attachmentId}/download")
    @Operation(
            operationId = "assignmentDownloadAttachment",
            summary = "Download an assignment attachment",
            description = "Course members with assignment visibility. Returns raw bytes (not JSON ApiResponse). "
                    + "Students cannot download attachments on Draft assignments (ASSIGNMENT_NOT_FOUND)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Attachment bytes",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "attachment; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string",
                                            example = "attachment; filename=\"brief.pdf\"; filename*=UTF-8''brief.pdf")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Stored content type, or application/octet-stream when unknown",
                                    schema = @Schema(type = "string", example = "application/pdf"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or attachment NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> downloadAttachment(HttpServletRequest request,
                                                                  @PathVariable Integer courseId,
                                                                  @PathVariable Integer assignmentId,
                                                                  @PathVariable Integer attachmentId) {
        return assignmentService.downloadAttachment(request, courseId, assignmentId, attachmentId,
                currentUserId(request));
    }

    @GetMapping("/{assignmentId}/attachments/{attachmentId}/preview")
    @Operation(
            operationId = "assignmentPreviewAttachment",
            summary = "Preview an assignment attachment inline",
            description = "Course members with assignment visibility. Returns raw bytes with Content-Disposition: "
                    + "inline for PDF and images. Students cannot preview attachments on Draft assignments "
                    + "(ASSIGNMENT_NOT_FOUND). Non-previewable types return UNSUPPORTED_FILE_TYPE — use download."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Attachment bytes for inline preview",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "inline; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string",
                                            example = "inline; filename=\"brief.pdf\"; filename*=UTF-8''brief.pdf")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Stored content type, or application/octet-stream when unknown",
                                    schema = @Schema(type = "string", example = "application/pdf"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "UNSUPPORTED_FILE_TYPE when the attachment is not PDF or image",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or attachment NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> previewAttachment(HttpServletRequest request,
                                                                 @PathVariable Integer courseId,
                                                                 @PathVariable Integer assignmentId,
                                                                 @PathVariable Integer attachmentId) {
        return assignmentService.previewAttachment(request, courseId, assignmentId, attachmentId,
                currentUserId(request));
    }

    @DeleteMapping("/{assignmentId}/attachments/{attachmentId}")
    @Operation(
            operationId = "assignmentDeleteAttachment",
            summary = "Delete an instructor attachment",
            description = "Instructor/TA only."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND or attachment NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<Void> deleteAttachment(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer assignmentId,
                                              @PathVariable Integer attachmentId) {
        assignmentService.deleteAttachment(courseId, assignmentId, attachmentId, currentUserId(request));
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
