package com.coursistant.lms.v2.dto;

import com.coursistant.lms.module.assignment.dto.AssignmentDTO;
import com.coursistant.lms.v2.dto.AssignmentGroupSubmissionRequest;

import lombok.Data;

@Data
public class GroupSubmissionResponse {

    private AssignmentDTO assignmentDTO;
    private AssignmentGroupSubmissionRequest assignmentGroupSubmissionRequest;

}
