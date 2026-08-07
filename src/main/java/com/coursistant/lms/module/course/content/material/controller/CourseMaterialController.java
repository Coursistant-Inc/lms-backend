package com.coursistant.lms.module.course.content.material.controller;

import com.coursistant.lms.module.course.content.material.dto.MaterialResponse;
import com.coursistant.lms.module.course.content.material.dto.MoveMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.RenameMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.ReorderMaterialsRequest;
import com.coursistant.lms.module.course.content.material.service.CourseMaterialService;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.shared.security.ActorContextResolver;
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
public class CourseMaterialController {

    @Resource
    private CourseMaterialService courseMaterialService;

    @Resource
    private ActorContextResolver actorContextResolver;

    @Idempotent
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ApiResponse<List<MaterialResponse>> reorder(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer weekId,
                                                        @RequestBody ReorderMaterialsRequest body) {
        ActorContext actor = actorContextResolver.resolve(request);
        return ApiResponse.success(courseMaterialService.reorder(actor, courseId, weekId, body));
    }

    @Idempotent
    @PostMapping("/{materialId}/move")
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
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer weekId,
                                    @PathVariable Integer materialId) {
        ActorContext actor = actorContextResolver.resolve(request);
        courseMaterialService.delete(actor, courseId, weekId, materialId);
        return ApiResponse.success();
    }

    @GetMapping("/{materialId}/preview")
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer weekId,
                                                        @PathVariable Integer materialId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseMaterialService.preview(actor, courseId, weekId, materialId);
    }

    @GetMapping("/{materialId}/download")
    public ResponseEntity<?> download(HttpServletRequest request,
                                      @PathVariable Integer courseId,
                                      @PathVariable Integer weekId,
                                      @PathVariable Integer materialId) {
        ActorContext actor = actorContextResolver.resolve(request);
        return courseMaterialService.download(actor, courseId, weekId, materialId);
    }
}
