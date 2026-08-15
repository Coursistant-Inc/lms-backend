package com.coursistant.lms.module.course.content.material.controller;

import com.coursistant.lms.module.course.content.material.dto.MaterialCreateRequest;
import com.coursistant.lms.module.course.content.material.dto.MaterialResponse;
import com.coursistant.lms.module.course.content.material.dto.MoveMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.RenameMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.ReorderMaterialsRequest;
import com.coursistant.lms.module.course.content.material.service.CourseMaterialService;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v2/courses/{courseId}/weeks/{weekId}/materials")
@Tag(name = "Materials", description = "Week materials: files, links, preview/download")
public class CourseMaterialController {

    private static final String IDEMPOTENCY_KEY_DESC =
            "Optional idempotency key for safe retries of this mutating request";

    @Resource
    private CourseMaterialService courseMaterialService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @Idempotent
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "courseMaterialCreate",
            summary = "Create file and/or link materials",
            description = "Multipart parts: `files` (optional array), `linkUrl` (optional), `linkDisplayName` (optional). "
                    + "Provide at least one file or a linkUrl.",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string")),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = MaterialCreateRequest.class))
            )
    )
    public ApiResponse<List<MaterialResponse>> create(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer weekId,
                                                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                       @RequestParam(value = "linkUrl", required = false) String linkUrl,
                                                       @RequestParam(value = "linkDisplayName", required = false) String linkDisplayName) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseMaterialService.create(
                actor, courseId, weekId, files, linkUrl, linkDisplayName, request));
    }

    @Idempotent
    @PatchMapping("/{materialId}")
    @Operation(
            operationId = "courseMaterialRename",
            summary = "Rename a material",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<MaterialResponse> rename(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer weekId,
                                                @PathVariable Integer materialId,
                                                @RequestBody RenameMaterialRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseMaterialService.rename(
                actor, courseId, weekId, materialId, body));
    }

    @Idempotent
    @PutMapping("/reorder")
    @Operation(
            operationId = "courseMaterialReorder",
            summary = "Reorder materials in a week",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<List<MaterialResponse>> reorder(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer weekId,
                                                        @RequestBody ReorderMaterialsRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseMaterialService.reorder(actor, courseId, weekId, body));
    }

    @Idempotent
    @PostMapping("/{materialId}/move")
    @Operation(
            operationId = "courseMaterialMove",
            summary = "Move a material to another week",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = false,
                    description = IDEMPOTENCY_KEY_DESC, schema = @Schema(type = "string"))
    )
    public ApiResponse<MaterialResponse> move(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer weekId,
                                              @PathVariable Integer materialId,
                                              @RequestBody MoveMaterialRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseMaterialService.move(
                actor, courseId, weekId, materialId, body));
    }

    @DeleteMapping("/{materialId}")
    @Operation(operationId = "courseMaterialDelete", summary = "Delete a material")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer weekId,
                                    @PathVariable Integer materialId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseMaterialService.delete(actor, courseId, weekId, materialId);
        return ApiResponse.success();
    }

    @GetMapping("/{materialId}/preview")
    @Operation(
            operationId = "courseMaterialPreview",
            summary = "Preview a file material (inline binary)",
            description = "Returns raw file bytes (not JSON ApiResponse). Only for previewable FILE materials."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Material file bytes for inline preview",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")),
                    headers = @Header(
                            name = "Content-Disposition",
                            description = "inline; filename=\"...\"",
                            schema = @Schema(type = "string")))
    })
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer weekId,
                                                        @PathVariable Integer materialId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseMaterialService.preview(actor, courseId, weekId, materialId);
    }

    @GetMapping("/{materialId}/download")
    @Operation(
            operationId = "courseMaterialDownload",
            summary = "Download a material (file bytes or link redirect)",
            description = "FILE materials return 200 binary with Content-Disposition attachment. "
                    + "LINK materials return 302 with Location set to the external URL."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "File material bytes",
                    content = @Content(mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")),
                    headers = @Header(
                            name = "Content-Disposition",
                            description = "attachment; filename=\"...\"",
                            schema = @Schema(type = "string"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Link material redirect to external URL",
                    content = @Content,
                    headers = @Header(
                            name = "Location",
                            description = "External link URL",
                            schema = @Schema(type = "string", format = "uri")))
    })
    public ResponseEntity<?> download(HttpServletRequest request,
                                      @PathVariable Integer courseId,
                                      @PathVariable Integer weekId,
                                      @PathVariable Integer materialId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseMaterialService.download(actor, courseId, weekId, materialId);
    }
}
