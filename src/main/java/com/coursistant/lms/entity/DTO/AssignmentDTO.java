package com.coursistant.lms.entity.DTO;

import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.entity.AssignmentFile;

import java.io.Serializable;
import java.util.List;

/**
 * 公告实体类
 * Assignment DTO
 */
public class AssignmentDTO extends Assignment {
    private static final long serialVersionUID = 1L;

    private List<AssignmentFile> files;



    public AssignmentDTO() {}



    public List<AssignmentFile> getFiles() {
        return files;
    }

    public void setFiles(List<AssignmentFile> files) {
        this.files = files;
    }
}
