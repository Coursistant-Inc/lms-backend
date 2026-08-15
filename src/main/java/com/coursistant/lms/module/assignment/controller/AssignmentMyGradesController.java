package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.MyGradeResponse;
import com.coursistant.lms.module.assignment.service.AssignmentMyGradesService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A student's own grade list for a course. Only released grades carry a score.
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/my-grades")
@Tag(name = "MyGrades", description = "Student view of own released/unreleased assignment grades in a course")
public class AssignmentMyGradesController {

    @Resource
    private AssignmentMyGradesService assignmentMyGradesService;

    @GetMapping
    @Operation(
            operationId = "assignmentListMyGrades",
            summary = "List my grades for a course",
            description = "Student only. Score, feedback, and annotated file appear only when grade status is Released; "
                    + "otherwise released=false and score fields are omitted."
    )
    public ApiResponse<List<MyGradeResponse>> listMyGrades(HttpServletRequest request,
                                                           @PathVariable Integer courseId) {
        return ApiResponse.success(assignmentMyGradesService.listMyGrades(courseId, currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
