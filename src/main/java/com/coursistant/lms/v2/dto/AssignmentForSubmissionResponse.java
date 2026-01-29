package com.coursistant.lms.v2.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentForSubmissionResponse {
    private AssignmentForEditResponse assignment;
    @Nullable private SubmissionResponse submission;
}
