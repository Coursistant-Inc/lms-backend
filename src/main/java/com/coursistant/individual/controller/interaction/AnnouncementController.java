package com.coursistant.individual.controller.interaction;

import com.coursistant.individual.common.Result;
import com.coursistant.individual.entity.Announcement;
import com.coursistant.individual.service.interaction.AnnouncementService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Announcement 公告前端操作接口
 * Announcement frontend operation API
 **/
@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    /**
     * 新增公告
     * Add a new announcement
     */
    @PostMapping("/add")
    public Result add(@RequestBody Announcement announcement) {
        announcementService.addAnnouncement(announcement);
        return Result.success();
    }

    /**
     * 根据 ID 删除公告
     * Delete an announcement by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        announcementService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除公告
     * Batch delete announcements
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        announcementService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 更新公告
     * Update an announcement
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Announcement announcement) {
        announcementService.updateById(announcement);
        return Result.success();
    }

    /**
     * 根据 ID 查询公告
     * Query an announcement by ID
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Announcement announcement = announcementService.selectById(id);
        return Result.success(announcement);
    }

    /**
     * 查询所有公告
     * Query all announcements
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        List<Announcement> list = announcementService.selectAll();
        return Result.success(list);
    }

    /**
     * 查询某个用户的所有公告
     * Query all announcements of a specific user
     */
    @GetMapping("/selectByUserId/{userId}")
    public Result selectByUserId(@PathVariable Integer userId) {
        List<Announcement> list = announcementService.selectByUserId(userId);
        return Result.success(list);
    }
}
