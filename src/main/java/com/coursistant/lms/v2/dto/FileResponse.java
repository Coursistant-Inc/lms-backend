package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private String filePath;
}
