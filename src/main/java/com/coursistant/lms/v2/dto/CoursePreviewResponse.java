package com.coursistant.lms.v2.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoursePreviewResponse {
    private Long id;
    private String courseCode;
    private String name;
    private String teacherName;
    private Integer courseUnitsCount;
    @Nullable private String avatarUrl;
}
