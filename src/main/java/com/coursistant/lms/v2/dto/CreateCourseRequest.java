package com.coursistant.lms.v2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {
    private String courseCode;
    private String name;
    private String description;
    private String school;
    private String semester;
}
