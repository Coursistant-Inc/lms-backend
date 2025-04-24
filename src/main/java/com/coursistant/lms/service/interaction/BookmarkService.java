package com.coursistant.lms.service.interaction;


import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.interaction.BookmarkMapper;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Bookmark;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Service
public class BookmarkService {

    @Resource
    private BookmarkMapper bookmarkMapper;


    public void add(Bookmark bookmark) {
        bookmarkMapper.insert(bookmark);
    }

    /**
     * 删除
     * Delete a bookmark by ID
     */
    public void deleteById(Integer id) {
        bookmarkMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple bookmarks by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            bookmarkMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a bookmark by ID
     */
    public void updateById(Bookmark bookmark) {
        bookmarkMapper.updateById(bookmark);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query a bookmark by ID
     */
    public Bookmark selectById(Integer id) {
        //String cacheKey = "bookmark:" + id;

        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        Bookmark bookmark = bookmarkMapper.selectById(id);
        if (bookmark == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }

        return bookmark;
    }

    /**
     * 查询所有
     * Query all bookmarks
     */
    public List<Bookmark> selectAll(Bookmark bookmark) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<Bookmark> bookmarkes = bookmarkMapper.selectAll(bookmark);

        return bookmarkes;
    }

}
