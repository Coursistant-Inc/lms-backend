package com.coursistant.lms.module.assignment.dto;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmission;
import com.coursistant.lms.module.file.entity.SubmissionFile;

import java.io.Serializable;
import java.util.List;
import com.coursistant.lms.module.assignment.entity.Assignment;

/**
 * 公告实体类
 * Assignment Submission DTO
 */
public class AssignmentSubmissionDTO extends AssignmentSubmission {
    private static final long serialVersionUID = 1L;


    private List<SubmissionFile> files;

    public AssignmentSubmissionDTO() {}


    public List<SubmissionFile> getFiles() {
        return files;
    }

    public void setFiles(List<SubmissionFile> files) {
        this.files = files;
    }
}