package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.GradeResponse;
import com.coursistant.lms.module.assignment.dto.GradeStudentSelectionRequest;
import com.coursistant.lms.module.assignment.dto.GradeTransitionResponse;
import com.coursistant.lms.module.assignment.dto.GradingRosterResponse;
import com.coursistant.lms.module.assignment.dto.GradingViewResponse;
import com.coursistant.lms.module.assignment.dto.UpsertGradeRequest;
import com.coursistant.lms.module.assignment.service.AssignmentGradingService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Grading endpoints for Instructors and TAs with the grade permission.
 *
 * <p>The roster is active Students only. Scores stay invisible to students until they are
 * released; retracting pulls them back without notifying anyone.</p>
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/assignments/{assignmentId}")
@Tag(name = "AssignmentGrading", description = "Instructor/TA grading roster, scores, release/retract")
public class AssignmentGradingController {

    @Resource
    private AssignmentGradingService assignmentGradingService;

    @GetMapping("/grading-roster")
    @Operation(
            operationId = "assignmentGetGradingRoster",
            summary = "Get grading roster",
            description = "Instructor/TA with grade permission. Students and system admins cannot use daily grading APIs."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_DENIED or FORBIDDEN (admins cannot perform daily grading)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradingRosterResponse> getRoster(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentGradingService.getRoster(courseId, assignmentId, currentUserId(request)));
    }

    @GetMapping("/students/{studentUserId}/grading")
    @Operation(
            operationId = "assignmentGetStudentGradingView",
            summary = "Get grading view for a student",
            description = "Instructor/TA. Includes submission history, rubric, and current grade (if any)."
    )
    public ApiResponse<GradingViewResponse> getGradingView(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer assignmentId,
                                                           @PathVariable Integer studentUserId) {
        return ApiResponse.success(assignmentGradingService.getGradingView(courseId, assignmentId, studentUserId,
                currentUserId(request)));
    }

    @PutMapping("/students/{studentUserId}/grade")
    @Operation(
            operationId = "assignmentUpsertGrade",
            summary = "Upsert a student grade",
            description = "Instructor/TA. score is required. Students never see Entered scores until released."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING, GRADE_SCORE_OUT_OF_RANGE",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, NOT_IN_GRADING_ROSTER",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeResponse> upsertGrade(HttpServletRequest request,
                                                  @PathVariable Integer courseId,
                                                  @PathVariable Integer assignmentId,
                                                  @PathVariable Integer studentUserId,
                                                  @RequestBody UpsertGradeRequest body) {
        return ApiResponse.success(assignmentGradingService.upsertGrade(courseId, assignmentId, studentUserId,
                currentUserId(request), body));
    }

    @PostMapping(value = "/students/{studentUserId}/grade/annotated-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "assignmentUploadAnnotatedFile",
            summary = "Upload annotated feedback file for a student",
            description = "Instructor/TA. Multipart part name must be `file`.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object", requiredProperties = {"file"}),
                            schemaProperties = {
                                    @SchemaProperty(
                                            name = "file",
                                            schema = @Schema(type = "string", format = "binary"))
                            }
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, GRADE_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeResponse> uploadAnnotatedFile(HttpServletRequest request,
                                                          @PathVariable Integer courseId,
                                                          @PathVariable Integer assignmentId,
                                                          @PathVariable Integer studentUserId,
                                                          @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(assignmentGradingService.uploadAnnotatedFile(courseId, assignmentId, studentUserId,
                currentUserId(request), file));
    }

    @GetMapping("/students/{studentUserId}/grade/annotated-file")
    @Operation(
            operationId = "assignmentDownloadAnnotatedFile",
            summary = "Download annotated file for a student grade",
            description = "Instructor/TA. Returns raw bytes with Content-Disposition: attachment."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Annotated file bytes",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "attachment; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Stored content type or application/octet-stream",
                                    schema = @Schema(type = "string"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, GRADE_NOT_FOUND, or annotated file NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> downloadAnnotatedFile(HttpServletRequest request,
                                                                     @PathVariable Integer courseId,
                                                                     @PathVariable Integer assignmentId,
                                                                     @PathVariable Integer studentUserId) {
        return assignmentGradingService.downloadAnnotatedFile(courseId, assignmentId, studentUserId,
                currentUserId(request));
    }

    @Idempotent
    @PostMapping("/grades/release-all")
    @Operation(
            operationId = "assignmentReleaseAllGrades",
            summary = "Release all entered grades",
            description = "Instructor/TA. Makes Entered grades visible to students. Idempotent."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeTransitionResponse> releaseAll(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @PathVariable Integer assignmentId) {
        return ApiResponse.success(assignmentGradingService.releaseAll(courseId, assignmentId, currentUserId(request)));
    }

    @Idempotent
    @PostMapping("/grades/release")
    @Operation(
            operationId = "assignmentReleaseGrades",
            summary = "Release selected grades",
            description = "Instructor/TA. Provide studentUserIds and/or groupIds. Skips students not in Entered state."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING when selection is empty/invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, GRADE_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeTransitionResponse> release(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId,
                                                        @RequestBody GradeStudentSelectionRequest body) {
        Integer userId = currentUserId(request);
        if (body != null && body.getGroupIds() != null && !body.getGroupIds().isEmpty()) {
            return ApiResponse.success(assignmentGradingService.releaseGroups(courseId, assignmentId, userId,
                    body.getGroupIds()));
        }
        return ApiResponse.success(assignmentGradingService.release(courseId, assignmentId, userId,
                body == null ? null : body.getStudentUserIds()));
    }

    @Idempotent
    @PostMapping("/grades/retract")
    @Operation(
            operationId = "assignmentRetractGrades",
            summary = "Retract selected grades",
            description = "Instructor/TA. Pulls Released grades back to Entered without notifying students. "
                    + "Provide studentUserIds and/or groupIds."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING when selection is empty/invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, GRADE_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeTransitionResponse> retract(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer assignmentId,
                                                        @RequestBody GradeStudentSelectionRequest body) {
        Integer userId = currentUserId(request);
        if (body != null && body.getGroupIds() != null && !body.getGroupIds().isEmpty()) {
            return ApiResponse.success(assignmentGradingService.retractGroups(courseId, assignmentId, userId,
                    body.getGroupIds()));
        }
        return ApiResponse.success(assignmentGradingService.retract(courseId, assignmentId, userId,
                body == null ? null : body.getStudentUserIds()));
    }

    @GetMapping("/groups/{groupId}/grading")
    @Operation(
            operationId = "assignmentGetGroupGradingView",
            summary = "Get grading view for a group",
            description = "Instructor/TA. Group assignment grading view for one group."
    )
    public ApiResponse<GradingViewResponse> getGroupGradingView(HttpServletRequest request,
                                                                @PathVariable Integer courseId,
                                                                @PathVariable Integer assignmentId,
                                                                @PathVariable Integer groupId) {
        return ApiResponse.success(assignmentGradingService.getGroupGradingView(courseId, assignmentId, groupId,
                currentUserId(request)));
    }

    @PutMapping("/groups/{groupId}/grade")
    @Operation(
            operationId = "assignmentUpsertGroupGrade",
            summary = "Upsert a group grade",
            description = "Instructor/TA. score is required. Applies to the group submission target."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PARAM_MISSING, GRADE_SCORE_OUT_OF_RANGE, BAD_REQUEST",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeResponse> upsertGroupGrade(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer assignmentId,
                                                       @PathVariable Integer groupId,
                                                       @RequestBody UpsertGradeRequest body) {
        return ApiResponse.success(assignmentGradingService.upsertGroupGrade(courseId, assignmentId, groupId,
                currentUserId(request), body));
    }

    @PostMapping(value = "/groups/{groupId}/grade/annotated-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "assignmentUploadGroupAnnotatedFile",
            summary = "Upload annotated file for a group grade",
            description = "Instructor/TA. Multipart part name must be `file`.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object", requiredProperties = {"file"}),
                            schemaProperties = {
                                    @SchemaProperty(
                                            name = "file",
                                            schema = @Schema(type = "string", format = "binary"))
                            }
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, GRADE_NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ApiResponse<GradeResponse> uploadGroupAnnotatedFile(HttpServletRequest request,
                                                               @PathVariable Integer courseId,
                                                               @PathVariable Integer assignmentId,
                                                               @PathVariable Integer groupId,
                                                               @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(assignmentGradingService.uploadGroupAnnotatedFile(courseId, assignmentId, groupId,
                currentUserId(request), file));
    }

    @GetMapping("/groups/{groupId}/grade/annotated-file")
    @Operation(
            operationId = "assignmentDownloadGroupAnnotatedFile",
            summary = "Download annotated file for a group grade",
            description = "Instructor/TA. Returns raw bytes with Content-Disposition: attachment."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Annotated file bytes",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "attachment; filename=\"...\"; filename*=UTF-8''...",
                                    schema = @Schema(type = "string")),
                            @Header(
                                    name = "Content-Type",
                                    description = "Stored content type or application/octet-stream",
                                    schema = @Schema(type = "string"))
                    }
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "ASSIGNMENT_NOT_FOUND, GRADE_NOT_FOUND, or annotated file NOT_FOUND",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> downloadGroupAnnotatedFile(HttpServletRequest request,
                                                                          @PathVariable Integer courseId,
                                                                          @PathVariable Integer assignmentId,
                                                                          @PathVariable Integer groupId) {
        return assignmentGradingService.downloadGroupAnnotatedFile(courseId, assignmentId, groupId,
                currentUserId(request));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
