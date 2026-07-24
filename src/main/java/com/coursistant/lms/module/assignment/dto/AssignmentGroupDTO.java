package com.coursistant.lms.module.assignment.dto;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.assignment.entity.AssignmentFile;
import com.coursistant.lms.module.quiz.entity.Quiz;

import java.util.List;

/**
 * 公告实体类
 * Assignment DTO
 */
public class AssignmentGroupDTO {
    private static final long serialVersionUID = 1L;

    /** 课程内容 ID / Course content ID */
    private Integer courseContentId;

    /** 当前 courseContentId 下的作业列表 / Assignment list under this content */
    private List<Assignment> assignments;

    private List<Quiz> quizzes;

    public AssignmentGroupDTO() {}

    public AssignmentGroupDTO(Integer courseContentId, List<Assignment> assignments, List<Quiz> quizzes) {
        this.courseContentId = courseContentId;
        this.assignments = assignments;
        this.quizzes = quizzes;
    }

    public Integer getCourseContentId() {
        return courseContentId;
    }

    public void setCourseContentId(Integer courseContentId) {
        this.courseContentId = courseContentId;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    @Override
    public String toString() {
        return "AssignmentGroupDTO{" +
                "courseContentId=" + courseContentId +
                ", assignments=" + assignments +
                '}';
    }
}
