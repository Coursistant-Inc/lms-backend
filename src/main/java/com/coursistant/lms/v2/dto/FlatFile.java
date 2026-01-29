package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatFile {
    private Instant createdAt;
    private Instant updatedAt;

    private String parentEntityType;
    private Long parentEntityId;

    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String filePath;
}
