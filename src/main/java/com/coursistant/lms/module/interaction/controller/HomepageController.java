package com.coursistant.lms.module.interaction.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.interaction.entity.Announcement;
import com.coursistant.lms.module.assignment.dto.AssignmentDTO;
import com.coursistant.lms.module.assignment.service.AssignmentService;
import com.coursistant.lms.module.course.entity.Learn;
import com.coursistant.lms.module.course.entity.Teach;
import com.coursistant.lms.module.course.service.LearnService;
import com.coursistant.lms.module.course.service.TeachService;
import com.coursistant.lms.module.interaction.service.AnnouncementService;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/home")
public class HomepageController {

    @Resource
    private AssignmentService assignmentService;

    @Resource
    private AnnouncementService announcementService;

    @Resource
    private LearnService learnService;

    @Resource
    private TeachService teachService;

    @GetMapping("/courseAssignmentDetails/{userId}")
    public Result getAssignmentDetails(@PathVariable Integer userId, @RequestParam Integer courseId)
    {
        List<AssignmentDTO> assignmentDetails = assignmentService.selectAssignmentsByCourseAndUserId(userId,courseId);
        return Result.success(assignmentDetails);
    }

    @GetMapping("/announcement/{userId}")
    public Result getAnnouncements(@PathVariable Integer userId)
    {
        List<Integer> courseIds = collectCourseIdsByUserId(userId);
        List<Announcement> announcementList = announcementService.selectLatestAnnouncementByCourseId(courseIds);
        return Result.success(announcementList);
    }

    @GetMapping("/courseDetails/{userId}")
    public Result getCourseDetails(@PathVariable Integer userId)
    {
        return Result.success(Collections.emptyList());
    }

    private List<Integer> collectCourseIdsByUserId(Integer userId) {
        Set<Integer> courseIds = new LinkedHashSet<>();
        try {
            List<Learn> learns = learnService.selectByStudentId(userId);
            if (learns != null) {
                for (Learn learn : learns) {
                    if (learn.getCourseId() != null) {
                        courseIds.add(learn.getCourseId());
                    }
                }
            }
        } catch (Exception ignored) {
            // user may not be a student
        }
        try {
            List<Teach> teaches = teachService.selectByTeacherId(userId);
            if (teaches != null) {
                for (Teach teach : teaches) {
                    if (teach.getCourseId() != null) {
                        courseIds.add(teach.getCourseId());
                    }
                }
            }
        } catch (Exception ignored) {
            // user may not be a teacher
        }
        return new ArrayList<>(courseIds);
    }
}
