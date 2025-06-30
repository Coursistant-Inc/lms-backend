package com.coursistant.lms.controller.interaction;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Assignment;
import com.coursistant.lms.service.assignment.AssignmentService;

@RestController
@RequestMapping("/home")
public class HomepageController {

    @Resource
    private AssignmentService assignmentService;

    @GetMapping("/assignmentDetails/{userId}")
    public Result getAssignmentDetails(@PathVariable Integer userId)
    {
        List<Assignment> assignmentDetails = assignmentService.selectAssignmentsByUserId(userId);
        
        return Result.success(assignmentDetails);
    }



}
