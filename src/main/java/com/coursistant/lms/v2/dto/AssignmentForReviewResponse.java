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
public class AssignmentForReviewResponse {
    private Assignment assignment;
    private List<Submission> submissions;
    private List<Review> reviews;
    private List<FileResponse> files;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Assignment {
        private Long id;
        private Instant createdAt;
        private Instant updatedAt;
        private String title;
        private String description;
        private String type;
        private Instant dueTime;
        private AssignmentEntity.AssignmentSettings settings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Submission {
        private Long id;
        private Instant createdAt;
        private Instant updatedAt;
        private Long assignmentId;
        private String studentName;
        private Integer submissionCount;
        private String submissionContent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Review {
        private Long id;
        private Instant createdAt;
        private Instant updatedAt;
        private Long submissionId;
        private Integer grade;
        private String teacherComment;
    }
}
