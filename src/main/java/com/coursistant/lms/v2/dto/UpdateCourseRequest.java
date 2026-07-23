package com.coursistant.lms.v2.dto;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.Map;

public record UpdateCourseRequest(
        @Nullable @Valid CourseUpdate courseUpdate,
        Map<@Positive Long, @Valid CourseUnitUpdate> courseUnitUpdateMap,
        Map<@Positive Long, @Valid AssignmentUpdate> assignmentUpdateMap
) {
    public boolean hasUpdates() {
        return (courseUpdate != null && courseUpdate.hasUpdates()) ||
                (!CollectionUtils.isEmpty(courseUnitUpdateMap)) ||
                (!CollectionUtils.isEmpty(assignmentUpdateMap));
    }

    public record CourseUpdate(
            @Nullable @Size(max = 15) @NotBlank String courseCode,
            @Nullable @Size(max = 127) @NotBlank String name,
            @Nullable @Size(max = 1000) String description,
            @Nullable @Size(max = 127) String school,
            @Nullable @Size(max = 127) String semester
    ) {
        public boolean hasUpdates() {
            return courseCode != null || name != null || description != null
                    || school != null || semester != null;
        }
    }

    public record CourseUnitUpdate(
            @Nullable @Positive Integer sortOrder,
            @Nullable @Size(max = 63) @NotBlank String title,
            @Nullable @Size(max = 1000) String description
    ) {
        public boolean hasUpdates() {
            return sortOrder != null || title != null || description != null;
        }
    }

    public record AssignmentUpdate(
            @Nullable @Size(max = 63) @NotBlank String title,
            @Nullable @Size(max = 1000) String description,
            @Nullable @Size(max = 31) @NotBlank String type,
            @Nullable Instant dueTime,
            @Nullable AssignmentEntity.AssignmentSettings settings
    ) {
        public boolean hasUpdates() {
            return title != null || description != null || type != null
                    || dueTime != null || settings != null;
        }
    }
}
