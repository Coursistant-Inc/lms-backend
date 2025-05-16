package com.coursistant.lms.entity.DTO;

import com.coursistant.lms.entity.AssignmentSubmission;
import com.coursistant.lms.entity.SubmissionFile;

import java.io.Serializable;
import java.util.List;

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