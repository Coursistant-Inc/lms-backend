package com.coursistant.lms.v2.dto;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentForEditResponse {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private String title;
    private String description;
    private String type;
    private Instant dueTime;
    private AssignmentEntity.AssignmentSettings settings;
    private List<FileResponse> attachments;
}
