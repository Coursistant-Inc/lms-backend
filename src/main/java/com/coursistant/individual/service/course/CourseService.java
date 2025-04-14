package com.coursistant.individual.service.course;


import com.coursistant.individual.common.enums.ResultCodeEnum;
import com.coursistant.individual.entity.Course;
import com.coursistant.individual.exception.CustomException;
import com.coursistant.individual.mapper.course.CourseMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Service
public class CourseService {

    @Resource
    private CourseMapper courseMapper;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource(name = "courseAllRedisTemplate")
    private RedisTemplate<String, Object> courseAllRedisTemplate;

    @Resource(name = "coursePageRedisTemplate")
    private RedisTemplate<String, Object> coursePageRedisTemplate;

    // 缓存过期时间（秒） / Cache expiration time (seconds)
    private static final long CACHE_EXPIRE_TIME = 300;

    /**
     * 清空 courseAll 数据库
     * Clear the courseAll database
     */
    public void clearCourseAllCache() {
        Objects.requireNonNull(courseAllRedisTemplate.getConnectionFactory())
                .getConnection()
                .flushDb();
        System.out.println("Cleared all data from courseAll database.");
    }

    /**
     * 清空 coursePage 数据库
     * Clear the coursePage database
     */
    public void clearCoursePageCache() {
        Objects.requireNonNull(coursePageRedisTemplate.getConnectionFactory())
                .getConnection()
                .flushDb();
        System.out.println("Cleared all data from coursePage database.");
    }

    /**
     * 新增
     * Add a new course
     */
    public void add(Course course) {
        courseMapper.insert(course);
        // 清理相关缓存 / Clear related cache
        clearCourseAllCache();
        clearCoursePageCache();
    }

    /**
     * 删除
     * Delete a course by ID
     */
    public void deleteById(Integer id) {
        courseMapper.deleteById(id);
        // 清理相关缓存 / Clear related cache
        clearCourseAllCache();
        clearCoursePageCache();
        generalRedisTemplate.delete("course:" + id);
    }

    /**
     * 批量删除
     * Delete multiple courses by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            courseMapper.deleteById(id);
            generalRedisTemplate.delete("course:" + id);
        }
        clearCourseAllCache();
        clearCoursePageCache();
    }

    /**
     * 修改
     * Update a course by ID
     */
    public void updateById(Course course) {
        courseMapper.updateById(course);
        // 清理相关缓存 / Clear related cache
        clearCourseAllCache();
        clearCoursePageCache();
        generalRedisTemplate.delete("course:" + course.getId());
    }

    /**
     * 根据ID查询
     * Query a course by ID
     */
    public Course selectById(Integer id) {
        String cacheKey = "course:" + id;

        // 从 Redis 获取缓存 / Get cache from Redis
        Course course = (Course) generalRedisTemplate.opsForValue().get(cacheKey);
        if (course != null) {
            System.out.println("from cache: " + cacheKey);
            return course;
        }

        // 如果缓存不存在，从数据库查询 / If cache does not exist, query from database
        course = courseMapper.selectById(id);
        if (course == null) {
            throw new CustomException(ResultCodeEnum.COURSE_NOT_EXIST_ERROR);
        }

        // 将结果存入 Redis，并设置过期时间 / Store result in Redis and set expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, course, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return course;
    }

    /**
     * 查询所有
     * Query all courses
     */
    public List<Course> selectAll(Course course) {
        String cacheKey = "course:all";
        if (course != null) {
            cacheKey += course.toString();
        }

        // 从 Redis 获取缓存 / Get cache from Redis
        List<Course> courses = (List<Course>) courseAllRedisTemplate.opsForValue().get(cacheKey);
        if (courses != null) {
            System.out.println("from cache: " + cacheKey);
            return courses;
        }

        // 如果缓存不存在，从数据库查询 / If cache does not exist, query from database
        courses = courseMapper.selectAll(course);
        if (courses != null && !courses.isEmpty()) {
            courseAllRedisTemplate.opsForValue().set(cacheKey, courses, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return courses;
    }

    /**
     * 分页查询
     * Paginate query for courses
     */
    public PageInfo<Course> selectPage(Course course, Integer pageNum, Integer pageSize) {
        String cacheKey = "course:page:" + pageNum + ":" + pageSize;
        if (course != null) {
            cacheKey += ":" + course.toString();
        }

        // 从 Redis 获取缓存 / Get cache from Redis
        PageInfo<Course> pageInfo = (PageInfo<Course>) coursePageRedisTemplate.opsForValue().get(cacheKey);
        if (pageInfo != null) {
            System.out.println("from cache: " + cacheKey);
            return pageInfo;
        }

        // 如果缓存不存在，从数据库查询 / If cache does not exist, query from database
        PageHelper.startPage(pageNum, pageSize);
        List<Course> list = courseMapper.selectAll(course);
        pageInfo = PageInfo.of(list);

        if (!list.isEmpty()) {
            coursePageRedisTemplate.opsForValue().set(cacheKey, pageInfo, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return pageInfo;
    }
}
