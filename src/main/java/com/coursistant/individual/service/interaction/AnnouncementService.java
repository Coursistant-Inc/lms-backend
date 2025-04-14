package com.coursistant.individual.service.interaction;

import com.coursistant.individual.entity.Announcement;
import com.coursistant.individual.mapper.interaction.AnnouncementMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AnnouncementService {

    @Resource
    private AnnouncementMapper announcementMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 新增 Announcement
     * Add a new Announcement
     */
    public void addAnnouncement(Announcement announcement) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        announcement.setDate(LocalDateTime.now().format(formatter));  // 这里进行格式化 / Formatting here
        announcementMapper.insert(announcement);
    }

    /**
     * 根据 ID 删除 Announcement
     * Delete an Announcement by ID
     */
    public void deleteById(Integer id) {
        announcementMapper.deleteById(id);
    }

    /**
     * 批量删除 Announcement
     * Delete multiple Announcements by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            announcementMapper.deleteById(id);
        }
    }

    /**
     * 更新 Announcement
     * Update an Announcement by ID
     */
    public void updateById(Announcement announcement) {
        announcementMapper.updateById(announcement);
    }

    /**
     * 根据 ID 查询 Announcement
     * Query an Announcement by ID
     */
    public Announcement selectById(Integer id) {
        return announcementMapper.selectById(id);
    }

    /**
     * 查询所有 Announcement
     * Query all Announcements
     */
    public List<Announcement> selectAll() {
        return announcementMapper.selectAll();
    }

    /**
     * 查询某个用户的所有 Announcement
     * Query all Announcements of a specific user
     */
    public List<Announcement> selectByUserId(Integer userId) {
        return announcementMapper.selectByUserId(userId);
    }
}
