package com.coursistant.lms.module.assignment.controller;

import com.coursistant.lms.module.assignment.dto.MyGradeResponse;
import com.coursistant.lms.module.assignment.service.AssignmentMyGradesService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A student's own grade list for a course. Only released grades carry a score.
 */
@RestController
@RequestMapping("/v2/courses/{courseId}/my-grades")
public class AssignmentMyGradesController {

    @Resource
    private AssignmentMyGradesService assignmentMyGradesService;

    @GetMapping
    public ApiResponse<List<MyGradeResponse>> listMyGrades(HttpServletRequest request,
                                                           @PathVariable Integer courseId,
                                                           @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return ApiResponse.success(assignmentMyGradesService.listMyGrades(courseId, currentUserId(request), timezone));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
