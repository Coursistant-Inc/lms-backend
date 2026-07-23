package com.coursistant.lms.module.interaction.controller;

import java.time.ZoneId;
import java.util.List;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.module.interaction.entity.Announcement;
import com.coursistant.lms.module.user.entity.User;
import com.coursistant.lms.module.interaction.service.AnnouncementService;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import com.coursistant.lms.shared.security.RequiresPermission;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import com.coursistant.lms.module.chat.entity.Query;

/**
 * Announcement 公告前端操作接口
 * Announcement frontend operation API
 **/
@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    @Resource
    private UserService userService;

    /**
     * 新增公告
     * Add a new announcement
     */
    @RequiresPermission("announcement:manage")
    @PostMapping("/add")
    public Result add(@RequestBody Announcement announcement) {
        announcementService.addAnnouncement(announcement);
        return Result.success();
    }

    /**
     * 根据 ID 删除公告
     * Delete an announcement by ID
     */
    @RequiresPermission("announcement:manage")
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        announcementService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除公告
     * Batch delete announcements
     */
    @RequiresPermission("announcement:manage")
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        announcementService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 更新公告
     * Update an announcement
     */
    @RequiresPermission("announcement:manage")
    @PutMapping("/update")
    public Result updateById(@RequestBody Announcement announcement){
        announcementService.updateById(announcement);
        return Result.success();
    }

    /**
     * 根据 ID 查询公告
     * Query an announcement by ID
     */
    @RequiresPermission("announcement:view")
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id,
                             @RequestHeader(value = "X-Timezone", required = false)
            String timezone) {
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        Announcement announcement = announcementService.selectById(id,zone);
        return Result.success(announcement);
    }

    /**
     * 查询所有公告
     * Query all announcements
     */
    @RequiresPermission("announcement:view")
    @GetMapping("/selectAll")
    public Result selectAll(@RequestHeader(value = "X-Timezone", required = false)
                                        String timezone) {
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Announcement> list = announcementService.selectAll(zone);
        return Result.success(list);
    }

    /**
     * 查询某个用户的所有公告
     * Query all announcements of a specific user
     */
    @RequiresPermission("announcement:view")
    @GetMapping("/selectByUserId/{userId}")
    public Result selectByUserId(@PathVariable Integer userId,
                                 @RequestHeader(value = "X-Timezone", required = false)
            String timezone) {
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Announcement> list = announcementService.selectByUserId(userId,zone);
        return Result.success(list);
    }

    // Mark the reading of an announcement by a student

    @PostMapping("/readAnnouncement")
    public Result readAnnouncement(@RequestParam("userId") Integer userId, @RequestParam("announcementId") Integer announcementId,
    @RequestParam("courseId") Integer courseId)
    {
        Integer isRead = announcementService.isAnnouncementRead(userId, announcementId, courseId);

        if(ObjectUtils.isEmpty(isRead))
        {
            User user = userService.selectById(userId);

            String role = user.getLevel();

            if(role.equals("STUDENT"))
            {
                announcementService.readAnnouncement(userId, announcementId, courseId);
            }

        }

        return Result.success();
    }

    /**
     * 查询某课程的所有公告
     * Query all announcements of a specific course
     */
    @RequiresPermission("announcement:view")
    @GetMapping("/selectByCourseId/{courseId}")
    public Result selectByCourseId(@PathVariable Integer courseId,
                                   @RequestHeader(value = "X-Timezone", required = false)
                                           String timezone) {
        ZoneId zone = TimeZoneUtils.resolveZoneId(timezone);
        List<Announcement> list = announcementService.selectByCourseId(courseId, zone);
        return Result.success(list);
    }



}
