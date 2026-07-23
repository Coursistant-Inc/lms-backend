package com.coursistant.lms.v2.dto;

import com.coursistant.lms.v2.common.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalFileUploadDTO {
    private MultipartFile file;
    private EntityType entityType;
    private Long entityId;
    private Integer userId;
    private String directory;
}
