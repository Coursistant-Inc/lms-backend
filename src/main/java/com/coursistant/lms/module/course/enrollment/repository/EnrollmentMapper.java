package com.coursistant.lms.module.course.enrollment.repository;

import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EnrollmentMapper {

    int insert(Enrollment enrollment);

    int updateById(Enrollment enrollment);

    Enrollment selectById(@Param("id") Integer id);

    Enrollment selectByCourseIdAndUserId(@Param("courseId") Integer courseId, @Param("userId") Integer userId);

    List<Enrollment> selectByCourseId(@Param("courseId") Integer courseId);

    List<Enrollment> selectActiveByUserId(@Param("userId") Integer userId);

    /**
     * Active Students only (TA/Instructor excluded), ordered by user_id ASC.
     * Used as the assignment grading roster.
     */
    List<Enrollment> selectActiveStudentsByCourseId(@Param("courseId") Integer courseId);

    /**
     * All active enrollments for a course (Instructor / TA / Student).
     */
    List<Enrollment> selectActiveByCourseId(@Param("courseId") Integer courseId);

    int countByCourseId(@Param("courseId") Integer courseId);

    int countActiveInstructorsByCourseId(@Param("courseId") Integer courseId);

    int countActiveInstructorEnrollmentsByUserId(@Param("userId") Integer userId);

    int countByUserId(@Param("userId") Integer userId);
}
