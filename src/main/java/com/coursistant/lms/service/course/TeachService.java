package com.coursistant.lms.service.course;


import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Teach;
import com.coursistant.lms.mapper.course.TeachMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 教师信息业务处理 // Teach service processing
 */
@Service
public class TeachService {

    @Resource
    private TeachMapper teachMapper;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource(name = "teachAllRedisTemplate")
    private RedisTemplate<String, Object> teachAllRedisTemplate;

    @Resource(name = "teachPageRedisTemplate")
    private RedisTemplate<String, Object> teachPageRedisTemplate;

    // 缓存过期时间（秒） // Cache expiration time (seconds)
    private static final long CACHE_EXPIRE_TIME = 300;

    /**
     * 清空 teachAll 数据库 // Clear teachAll database
     */
    public void clearTeachAllCache() {
        Objects.requireNonNull(teachAllRedisTemplate.getConnectionFactory())
                .getConnection()
                .flushDb();
        System.out.println("Cleared all data from teachAll database.");
    }

    /**
     * 清空 teachPage 数据库 // Clear teachPage database
     */
    public void clearTeachPageCache() {
        Objects.requireNonNull(teachPageRedisTemplate.getConnectionFactory())
                .getConnection()
                .flushDb();
        System.out.println("Cleared all data from teachPage database.");
    }

    /**
     * 新增 // Add new record
     */
    public void add(Teach teach) {
        teachMapper.insert(teach);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();
        clearTeachPageCache();
    }

    /**
     * 删除 // Delete by ID
     */
    public void deleteById(Integer id) {
        teachMapper.deleteById(id);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();
        clearTeachPageCache();
        generalRedisTemplate.delete("teach:" + id);
    }

    /**
     * 批量删除 // Batch delete
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            teachMapper.deleteById(id);
            generalRedisTemplate.delete("teach:" + id);
        }
        clearTeachAllCache();
        clearTeachPageCache();
    }

    /**
     * 修改 // Update by ID
     */
    public void updateById(Teach teach) {
        teachMapper.updateById(teach);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();
        clearTeachPageCache();
        generalRedisTemplate.delete("teach:" + teach.getId());
    }

    /**
     * 根据ID查询 // Select by ID
     */
    public Teach selectById(Integer id) {
        String cacheKey = "teach:" + id;

        // 从 Redis 获取缓存 // Get from Redis cache
        Teach teach = (Teach) generalRedisTemplate.opsForValue().get(cacheKey);
        if (teach != null) {
            System.out.println("from cache: " + cacheKey);
            return teach;
        }

        // 如果缓存不存在，从数据库查询 // If cache is missing, query from database
        teach = teachMapper.selectById(id);
        if (teach == null) {
            throw new CustomException(ResultCodeEnum.TEACH_NOT_EXIST_ERROR);
        }

        // 将结果存入 Redis，并设置过期时间 // Store result in Redis with expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, teach, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return teach;
    }

    /**
     * 查询所有 // Select all records
     */
    public List<Teach> selectAll(Teach teach) {
        String cacheKey = "teach:all";
        if (teach != null) {
            cacheKey += teach.toString();
        }

        // 从 Redis 获取缓存 // Get from Redis cache
        List<Teach> teaches = (List<Teach>) teachAllRedisTemplate.opsForValue().get(cacheKey);
        if (teaches != null) {
            System.out.println("from cache: " + cacheKey);
            return teaches;
        }

        // 如果缓存不存在，从数据库查询 // If cache is missing, query from database
        teaches = teachMapper.selectAll(teach);
        if (teaches != null && !teaches.isEmpty()) {
            teachAllRedisTemplate.opsForValue().set(cacheKey, teaches, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return teaches;
    }

    /**
     * 分页查询 // Paginated query
     */
    public PageInfo<Teach> selectPage(Teach teach, Integer pageNum, Integer pageSize) {
        String cacheKey = "teach:page:" + pageNum + ":" + pageSize;
        if (teach != null) {
            cacheKey += ":" + teach.toString();
        }

        // 从 Redis 获取缓存 // Get from Redis cache
        PageInfo<Teach> pageInfo = (PageInfo<Teach>) teachPageRedisTemplate.opsForValue().get(cacheKey);
        if (pageInfo != null) {
            System.out.println("from cache: " + cacheKey);
            return pageInfo;
        }

        // 如果缓存不存在，从数据库查询 // If cache is missing, query from database
        PageHelper.startPage(pageNum, pageSize);
        List<Teach> list = teachMapper.selectAll(teach);
        pageInfo = PageInfo.of(list);

        if (!list.isEmpty()) {
            teachPageRedisTemplate.opsForValue().set(cacheKey, pageInfo, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return pageInfo;
    }
}
