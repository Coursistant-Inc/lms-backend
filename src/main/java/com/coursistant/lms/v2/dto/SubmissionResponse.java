package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private Long assignmentId;
    private Integer submissionCount;
    private String submissionContent;
    private List<FileResponse> files;
}
