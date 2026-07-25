package com.coursistant.lms.module.course.content.material.service;

import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.material.dto.MaterialResponse;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts {@link CourseMaterial} entities into {@link MaterialResponse} DTOs,
 * including preview/download URLs. Shared by the week and material services so
 * neither needs to depend on the other.
 */
@Component
public class MaterialResponseAssembler {

    private static final String FILE_TYPE = "FILE";

    @Resource
    private CourseContentFilePolicy courseContentFilePolicy;

    public MaterialResponse toResponse(CourseMaterial material) {
        MaterialResponse response = new MaterialResponse();
        response.setId(material.getId());
        response.setWeekId(material.getWeekId());
        response.setCourseId(material.getCourseId());
        response.setMaterialType(material.getMaterialType());
        response.setDisplayName(material.getDisplayName());
        response.setOrderPosition(material.getOrderPosition());
        response.setOriginalFilename(material.getOriginalFilename());
        response.setContentType(material.getContentType());
        response.setExtension(material.getExtension());
        response.setSizeBytes(material.getSizeBytes());
        response.setLinkUrl(material.getLinkUrl());
        response.setUploadedBy(material.getUploadedBy());
        response.setCreatedAt(material.getCreatedAt());
        response.setUpdatedAt(material.getUpdatedAt());

        boolean isFile = FILE_TYPE.equals(material.getMaterialType());
        boolean previewable = isFile && courseContentFilePolicy.isPreviewable(material.getContentType(), material.getExtension());
        response.setPreviewAvailable(previewable);
        response.setDownloadUrl(buildActionUrl(material, "download"));
        if (previewable) {
            response.setPreviewUrl(buildActionUrl(material, "preview"));
        }
        return response;
    }

    public List<MaterialResponse> toResponses(List<CourseMaterial> materials) {
        return materials.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private String buildActionUrl(CourseMaterial material, String action) {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/v2/courses/{courseId}/weeks/{weekId}/materials/{id}/" + action)
                    .buildAndExpand(material.getCourseId(), material.getWeekId(), material.getId())
                    .toUriString();
        } catch (IllegalStateException ex) {
            return "/api/v2/courses/" + material.getCourseId() + "/weeks/" + material.getWeekId()
                    + "/materials/" + material.getId() + "/" + action;
        }
    }
}
