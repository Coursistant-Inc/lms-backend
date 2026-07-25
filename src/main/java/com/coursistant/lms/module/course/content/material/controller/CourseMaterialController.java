package com.coursistant.lms.module.course.content.material.controller;

import com.coursistant.lms.module.course.content.material.dto.MaterialResponse;
import com.coursistant.lms.module.course.content.material.dto.MoveMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.RenameMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.ReorderMaterialsRequest;
import com.coursistant.lms.module.course.content.material.service.CourseMaterialService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<MaterialResponse>> create(HttpServletRequest request,
                                                       @PathVariable Integer courseId,
                                                       @PathVariable Integer weekId,
                                                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                       @RequestParam(value = "linkUrl", required = false) String linkUrl,
                                                       @RequestParam(value = "linkDisplayName", required = false) String linkDisplayName) {
        return ApiResponse.success(courseMaterialService.create(
                courseId, weekId, currentUserId(request), files, linkUrl, linkDisplayName));
    }

    @Idempotent
    @PatchMapping("/{materialId}")
    public ApiResponse<MaterialResponse> rename(HttpServletRequest request,
                                                @PathVariable Integer courseId,
                                                @PathVariable Integer weekId,
                                                @PathVariable Integer materialId,
                                                @RequestBody RenameMaterialRequest body) {
        return ApiResponse.success(courseMaterialService.rename(
                courseId, weekId, materialId, currentUserId(request), body));
    }

    @Idempotent
    @PutMapping("/reorder")
    public ApiResponse<List<MaterialResponse>> reorder(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer weekId,
                                                        @RequestBody ReorderMaterialsRequest body) {
        return ApiResponse.success(courseMaterialService.reorder(
                courseId, weekId, currentUserId(request), body));
    }

    @Idempotent
    @PostMapping("/{materialId}/move")
    public ApiResponse<MaterialResponse> move(HttpServletRequest request,
                                              @PathVariable Integer courseId,
                                              @PathVariable Integer weekId,
                                              @PathVariable Integer materialId,
                                              @RequestBody MoveMaterialRequest body) {
        return ApiResponse.success(courseMaterialService.move(
                courseId, weekId, materialId, currentUserId(request), body));
    }

    @DeleteMapping("/{materialId}")
    public ApiResponse<Void> delete(HttpServletRequest request,
                                    @PathVariable Integer courseId,
                                    @PathVariable Integer weekId,
                                    @PathVariable Integer materialId) {
        courseMaterialService.delete(courseId, weekId, materialId, currentUserId(request));
        return ApiResponse.success();
    }

    @GetMapping("/{materialId}/preview")
    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request,
                                                        @PathVariable Integer courseId,
                                                        @PathVariable Integer weekId,
                                                        @PathVariable Integer materialId) {
        return courseMaterialService.preview(request, courseId, weekId, materialId, currentUserId(request));
    }

    @GetMapping("/{materialId}/download")
    public ResponseEntity<?> download(HttpServletRequest request,
                                      @PathVariable Integer courseId,
                                      @PathVariable Integer weekId,
                                      @PathVariable Integer materialId) {
        return courseMaterialService.download(request, courseId, weekId, materialId, currentUserId(request));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
