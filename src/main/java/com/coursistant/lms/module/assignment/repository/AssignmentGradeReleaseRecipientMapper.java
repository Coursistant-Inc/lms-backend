package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentGradeReleaseRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentGradeReleaseRecipientMapper {

    int insert(AssignmentGradeReleaseRecipient recipient);

    /**
     * Drops the whole snapshot for one grade; used by Retract and by a re-release that has to
     * rewrite the snapshot from the current roster.
     */
    int deleteByGradeId(@Param("gradeId") Integer gradeId);

    List<AssignmentGradeReleaseRecipient> selectByGradeId(@Param("gradeId") Integer gradeId);

    /**
     * The grade this student was actually released, regardless of which group they are in now.
     */
    AssignmentGradeReleaseRecipient selectByAssignmentIdAndStudentUserId(
            @Param("assignmentId") Integer assignmentId,
            @Param("studentUserId") Integer studentUserId);

    int countByGradeId(@Param("gradeId") Integer gradeId);
}
