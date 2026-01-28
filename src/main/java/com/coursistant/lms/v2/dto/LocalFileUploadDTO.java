package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileUploadDTO {
    private MultipartFile file;
    private String entityType;
    private Long entityId;
    private Integer userId;
    private String directory;
}
