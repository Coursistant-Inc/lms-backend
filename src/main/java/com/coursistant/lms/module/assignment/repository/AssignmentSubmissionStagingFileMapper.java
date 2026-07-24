package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionStagingFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentSubmissionStagingFileMapper {

    int insert(AssignmentSubmissionStagingFile stagingFile);

    AssignmentSubmissionStagingFile selectById(@Param("id") Integer id);

    List<AssignmentSubmissionStagingFile> selectByAssignmentId(@Param("assignmentId") Integer assignmentId);

    List<AssignmentSubmissionStagingFile> selectByAssignmentIdAndOwnerUserIdAndNotConsumed(
            @Param("assignmentId") Integer assignmentId,
            @Param("ownerUserId") Integer ownerUserId);

    int updateConsumed(@Param("id") Integer id, @Param("consumed") Boolean consumed);

    int deleteById(@Param("id") Integer id);
}
