package com.coursistant.lms.service.user;


import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Status;
import com.coursistant.lms.mapper.user.StatusMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Service
public class StatusService {

    @Resource
    private StatusMapper statusMapper;


    public void add(Status status) {
        statusMapper.insert(status);
    }

    /**
     * 删除
     * Delete a status by ID
     */
    public void deleteById(Integer id) {
        statusMapper.deleteById(id);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 批量删除
     * Delete multiple statuss by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            statusMapper.deleteById(id);
        }
    }

    /**
     * 修改
     * Update a status by ID
     */
    public void updateById(Status status) {
        statusMapper.updateById(status);
        // 清理相关缓存
        // Clear related cache
    }

    /**
     * 根据ID查询
     * Query a status by ID
     */
    public Status selectById(Integer id) {
        String cacheKey = "status:" + id;

        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        Status status = statusMapper.selectById(id);
        if (status == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }

        return status;
    }

    /**
     * 查询所有
     * Query all statuss
     */
    public List<Status> selectAll(Status status) {
        // 如果缓存不存在，从数据库查询
        // If cache does not exist, query from database
        List<Status> statuses = statusMapper.selectAll(status);

        return statuses;
    }

}
