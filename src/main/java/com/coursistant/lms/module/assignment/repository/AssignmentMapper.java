package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.Assignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssignmentMapper {

    int insert(Assignment assignment);

    Assignment selectById(@Param("id") Integer id);

    List<Assignment> selectByCourseId(@Param("courseId") Integer courseId);

    List<Assignment> selectByCourseIdAndState(@Param("courseId") Integer courseId, @Param("state") String state);

    Assignment selectByCourseIdAndId(@Param("courseId") Integer courseId, @Param("id") Integer id);

    int updateById(Assignment assignment);

    /**
     * Always writes late_until, including {@code null} (the dynamic updateById skips nulls).
     */
    int updateLateUntil(@Param("id") Integer id, @Param("lateUntil") java.time.LocalDateTime lateUntil);

    int deleteById(@Param("id") Integer id);

    int countSubmissionsByAssignmentId(@Param("assignmentId") Integer assignmentId);

    int countGradesByAssignmentId(@Param("assignmentId") Integer assignmentId);

    int updateCurrentRubricVersionId(@Param("id") Integer id,
                                     @Param("currentRubricVersionId") Integer currentRubricVersionId);

    int updateState(@Param("id") Integer id, @Param("state") String state);
}
