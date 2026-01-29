package com.coursistant.lms.v2.dto;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentForReviewResponse {
    private Assignment assignment;
    private Map<Long, Submission> submissions;
    private Map<Long, Review> reviews;
    private Map<Long, FlatFile> files;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Assignment {
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
        private Instant createdAt;
        private Instant updatedAt;
        private Integer submissionCount;
        private String submissionContent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Review {
        private Long submissionId;
        private Instant createdAt;
        private Instant updatedAt;
        private Integer grade;
        private String teacherComment;
    }
}
