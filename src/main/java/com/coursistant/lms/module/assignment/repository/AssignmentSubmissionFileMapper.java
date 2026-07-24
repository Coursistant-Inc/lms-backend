package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentSubmissionFileMapper {

    int insert(AssignmentSubmissionFile file);

    AssignmentSubmissionFile selectById(@Param("id") Integer id);

    List<AssignmentSubmissionFile> selectBySubmissionVersionId(@Param("submissionVersionId") Integer submissionVersionId);
}
