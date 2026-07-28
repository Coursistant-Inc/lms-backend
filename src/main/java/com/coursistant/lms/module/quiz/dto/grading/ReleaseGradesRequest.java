package com.coursistant.lms.module.quiz.dto.grading;

import lombok.Data;

import java.util.List;

@Data
public class ReleaseGradesRequest {
    private List<Integer> userIds;
}
