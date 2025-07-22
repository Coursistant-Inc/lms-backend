package com.coursistant.lms.service.course;



import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.entity.Teach;
import com.coursistant.lms.mapper.course.TeachMapper;

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
     * 新增 // Add new record
     */
    public void add(Teach teach) {
        teachMapper.insert(teach);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();

    }

    /**
     * 删除 // Delete by ID
     */
    public void deleteById(Integer id) {
        teachMapper.deleteById(id);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();

        generalRedisTemplate.delete("teach:" + id);
    }
    /**
     * 根据课程 ID 删除记录 // Delete by Course ID
     */
    public void deleteByCourseId(Integer courseId) {
        teachMapper.deleteByCourseId(courseId);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();

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

    }

    /**
     * 修改 // Update by ID
     */
    public void updateById(Teach teach) {
        teachMapper.updateById(teach);
        // 清理相关缓存 // Clear related cache
        clearTeachAllCache();

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

    public List<Teach> selectByTeacherId(Integer id) {

        List<Teach> teachs = teachMapper.selectByUserId(id);
        if (teachs == null) {
            throw new CustomException(ResultCodeEnum.LEARN_NOT_EXIST_ERROR);
        }


        return teachs;
    }


}
