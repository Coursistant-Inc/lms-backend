package com.coursistant.lms.controller.interaction;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.Announcement;
import com.coursistant.lms.entity.Course;
import com.coursistant.lms.entity.DTO.AssignmentDTO;
import com.coursistant.lms.entity.DTO.CourseDetailsDTO;
import com.coursistant.lms.service.assignment.AssignmentService;
import com.coursistant.lms.service.course.CourseService;
import com.coursistant.lms.service.interaction.AnnouncementService;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/home")
public class HomepageController {

    @Resource
    private AssignmentService assignmentService;

    @Resource
    private CourseService courseService;

    @Resource
    private AnnouncementService announcementService;


    

    @GetMapping("/courseAssignmentDetails/{userId}")
    public Result getAssignmentDetails(@PathVariable Integer userId, @RequestParam Integer courseId)
    {
        List<AssignmentDTO> assignmentDetails = assignmentService.selectAssignmentsByCourseAndUserId(userId,courseId);
        courseService.updateLastSelectedCourse(userId,courseId);

        return Result.success(assignmentDetails);
    }

    @GetMapping("/announcement/{userId}")
    public Result getAnnouncements(@PathVariable Integer userId)
    {
        List<Course> courseList = courseService.selectCoursesByUserId(userId);
        List<Announcement> announcementList = announcementService.selectLatestAnnouncementByCourseId(courseList);
        return Result.success(announcementList);

    }

    @GetMapping("/courseDetails/{userId}")
    public Result getCourseDetails(@PathVariable Integer userId)
    {
        List<Course> courseList = courseService.selectCoursesByUserId(userId);
        List<CourseDetailsDTO> courseDetails = courseService.getCourseDetailsByUserId(userId, courseList);
        return Result.success(courseDetails);
    }

    // @GetMapping("/courseAssignmentDetails/{userId}")
    // public getCourseAssignmenttDetails(@PathVariable Inte)


}
