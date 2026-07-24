package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentSubmissionVersionMapper {

    int insert(AssignmentSubmissionVersion version);

    AssignmentSubmissionVersion selectById(@Param("id") Integer id);

    List<AssignmentSubmissionVersion> selectBySubmissionIdOrderByVersionDesc(@Param("submissionId") Integer submissionId);

    Integer selectMaxVersionNo(@Param("submissionId") Integer submissionId);
}
