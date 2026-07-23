package com.coursistant.lms.v2.dto;

import com.coursistant.lms.v2.entity.AssignmentEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentRequest {
    private String title;
    private String type;
    private Instant dueTime;
    private String description;
    private Integer isGroup;
    private Integer submissionRequired;
    private AssignmentEntity.AssignmentSettings settings;
}
