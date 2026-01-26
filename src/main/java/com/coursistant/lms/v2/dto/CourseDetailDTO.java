package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDetailDTO {
    private CourseInfo courseInfo;
    private List<CourseUnit> courseUnits;
    private List<Assignment> assignments;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseInfo {
        private Long id;
        private Instant createdAt;
        private Instant updatedAt;
        private String courseCode;
        private String name;
        private String description;
        private String school;
        private String semester;
        private String teacherName;
        private String teacherPhone;
        private String teacherEmail;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseUnit {
        private Long id;
        private Instant createdAt;
        private Instant updatedAt;
        private Integer sortOrder;
        private String title;
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Assignment {
        private Long id;
        private Instant createdAt;
        private Instant updatedAt;
        private Long courseUnitId;
        private String title;
        private String type;
        private Instant dueTime;
    }
}
