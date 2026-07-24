package com.coursistant.lms.module.interaction.service;

import java.time.ZoneId;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import com.coursistant.lms.module.interaction.entity.Announcement;
import com.coursistant.lms.module.interaction.repository.AnnouncementMapper;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import com.coursistant.lms.module.chat.entity.Query;

@Service
public class AnnouncementService {

    @Resource
    private AnnouncementMapper announcementMapper;


    /**
     * 新增 Announcement
     * Add a new Announcement
     */
    public void addAnnouncement(Announcement announcement) {

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
    public Announcement selectById(Integer id, ZoneId timezone) {
        Announcement announcement=announcementMapper.selectById(id);
        announcement.setCreatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getCreatedAt(),timezone));
        announcement.setUpdatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getUpdatedAt(),timezone));
        return announcement;
    }

    /**
     * 根据 courseId 查询 Announcement 列表，并转换时区
     * Query Announcements by courseId and convert timestamps to the given timezone
     */
    public List<Announcement> selectByCourseId(Integer courseId, ZoneId timezone) {
        List<Announcement> announcements = announcementMapper.selectByCourseId(courseId);
        for (Announcement announcement : announcements) {
            announcement.setCreatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getCreatedAt(), timezone));
            announcement.setUpdatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getUpdatedAt(), timezone));
        }
        return announcements;
    }


    /**
     * 查询所有 Announcement
     * Query all Announcements
     */
    public List<Announcement> selectAll(ZoneId timezone) {
        List<Announcement> announcements=announcementMapper.selectAll();
        for (Announcement announcement:announcements){
            announcement.setCreatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getCreatedAt(),timezone));
            announcement.setUpdatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getUpdatedAt(),timezone));
        }
        return announcements;
    }

    /**
     * 查询某个用户的所有 Announcement
     * Query all Announcements of a specific user
     */
    public List<Announcement> selectByUserId(Integer userId,ZoneId timezone) {
        List<Announcement> announcements=announcementMapper.selectByUserId(userId);
        for (Announcement announcement:announcements){
            announcement.setCreatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getCreatedAt(),timezone));
            announcement.setUpdatedAt(TimeZoneUtils.fromUtcLocalDateTime(announcement.getUpdatedAt(),timezone));
        }
        return announcements;

    }

    public void readAnnouncement(Integer userId, Integer announcementId, Integer courseId)
    {
        announcementMapper.readAnnouncement(userId, announcementId, courseId);
    }

    public Integer isAnnouncementRead(Integer userId, Integer announcementId, Integer courseId)
    {
        return announcementMapper.isAnnouncementRead(userId, announcementId, courseId);
    }

    public List<Announcement> selectLatestAnnouncementByCourseId(List<Integer> courseIds)
    {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        return announcementMapper.selectLatestAnnouncementByCourseId(courseIds);
    }

}
