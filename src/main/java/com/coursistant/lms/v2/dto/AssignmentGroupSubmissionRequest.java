package com.coursistant.lms.v2.dto;
import java.util.List;

import com.coursistant.lms.v2.entity.AssignmentEntity;
import com.coursistant.lms.v2.dto.FileResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentGroupSubmissionRequest extends AssignmentEntity {

    private List<FileResponse> files;

}
