package com.coursistant.lms.module.course.content.material.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAPI schema for multipart material create form parts.
 * Runtime binding uses {@code @RequestParam} on the controller.
 */
@Schema(name = "MaterialCreateRequest",
        description = "Multipart form: upload files and/or create an external link material")
public class MaterialCreateRequest {

    @ArraySchema(arraySchema = @Schema(description = "One or more files (materialType=FILE)"),
            schema = @Schema(type = "string", format = "binary"))
    private Object[] files;

    @Schema(description = "External URL for a link material (materialType=LINK)",
            example = "https://example.com/reading")
    private String linkUrl;

    @Schema(description = "Display name when creating a link material", example = "Week 1 reading")
    private String linkDisplayName;

    public Object[] getFiles() {
        return files;
    }

    public void setFiles(Object[] files) {
        this.files = files;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkDisplayName() {
        return linkDisplayName;
    }

    public void setLinkDisplayName(String linkDisplayName) {
        this.linkDisplayName = linkDisplayName;
    }
}
