package com.coursistant.lms.module.assignment.repository;

import com.coursistant.lms.module.assignment.entity.AssignmentGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AssignmentGradeMapper {

    int insert(AssignmentGrade grade);

    int upsert(AssignmentGrade grade);

    int updateById(AssignmentGrade grade);

    /**
     * Always writes status/released_at (including a {@code null} released_at on retract),
     * which the dynamic updateById cannot express.
     */
    int updateStatus(@Param("id") Integer id,
                     @Param("status") String status,
                     @Param("releasedAt") LocalDateTime releasedAt,
                     @Param("editedBy") Integer editedBy);

    /**
     * Clears the annotated-file columns unconditionally.
     */
    int clearAnnotatedFile(@Param("id") Integer id, @Param("editedBy") Integer editedBy);

    AssignmentGrade selectById(@Param("id") Integer id);

    AssignmentGrade selectByAssignmentIdAndStudentUserId(@Param("assignmentId") Integer assignmentId,
                                                         @Param("studentUserId") Integer studentUserId);

    AssignmentGrade selectByAssignmentIdAndGroupId(@Param("assignmentId") Integer assignmentId,
                                                   @Param("groupId") Integer groupId);

    List<AssignmentGrade> selectByAssignmentId(@Param("assignmentId") Integer assignmentId);

    int countByAssignmentId(@Param("assignmentId") Integer assignmentId);
}
