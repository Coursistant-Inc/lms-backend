package com.coursistant.lms.mapper.interaction;

import com.coursistant.lms.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;


/**
 * 操作 Announcement 相关数据接口
 * Data access interface for Announcement-related operations
 */
@Mapper
public interface AnnouncementMapper {

    /**
     * 新增 Announcement
     * Insert a new Announcement
     */
    int insert(Announcement announcement);

    /**
     * 根据 ID 删除 Announcement
     * Delete an Announcement by ID
     */
    int deleteById(Integer id);

    /**
     * 根据 ID 更新 Announcement
     * Update an Announcement by ID
     */
    int updateById(Announcement announcement);

    /**
     * 根据 ID 查询 Announcement
     * Query an Announcement by ID
     */
    Announcement selectById(Integer id);

    /**
     * 查询所有 Announcement（这里假设不带参数筛选）
     * Query all Announcements (assuming no parameter filtering)
     */
    List<Announcement> selectAll();

    /**
     * 根据 user_id 查询 Announcement
     * Query Announcements by user_id
     */
    @Select("SELECT * FROM Announcement WHERE user_id = #{userId}")
    List<Announcement> selectByUserId(Integer userId);

    /**
     * 根据 course_id 查询 Announcement
     * Query Announcements by course_id
     */
    @Select("SELECT * FROM Announcement WHERE course_id = #{courseId}")
    List<Announcement> selectByCourseId(Integer courseId);

}
