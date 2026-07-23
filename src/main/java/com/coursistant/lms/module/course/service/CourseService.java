package com.coursistant.lms.module.course.service;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.course.dto.CourseDetailsDTO;
import com.coursistant.lms.module.course.entity.CourseSchedule;
import com.coursistant.lms.module.course.dto.CourseDTO;
import com.coursistant.lms.module.course.entity.Teach;
import com.coursistant.lms.module.user.entity.User;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.course.repository.CourseMapper;
import com.coursistant.lms.module.course.repository.CourseScheduleMapper;
import com.coursistant.lms.module.course.repository.LearnMapper;
import com.coursistant.lms.module.user.service.UserService;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.module.chat.entity.Query;


@Service
public class CourseService {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private LearnMapper learnMapper;

    @Resource
    private UserService userService;


    @Resource
    private TeachService teachService;

    @Resource
    private CourseScheduleMapper courseScheduleMapper;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource(name = "courseAllRedisTemplate")
    private RedisTemplate<String, Object> courseAllRedisTemplate;



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
     * 新增
     * Add a new course
     */
    public Integer add(Course course) {
        courseMapper.insert(course);
        Teach teach=new Teach();
        teach.setCourseId(course.getId());
        teach.setUserId(course.getTeacherId());
        teachService.add(teach);
        // 清理相关缓存 / Clear related cache
        clearCourseAllCache();
        return course.getId();
    }

    /**
     * 删除
     * Delete a course by ID
     */
    public void deleteById(Integer id) {
        courseMapper.deleteById(id);
        // 清理相关缓存 / Clear related cache
        clearCourseAllCache();
        Teach teach=new Teach();
        teach.setCourseId(id);
        List<Teach> teaches=teachService.selectAll(teach);
        for (Teach teach1:teaches){
            teachService.deleteByCourseId(teach1.getCourseId());
        }

        generalRedisTemplate.delete("course:" + id);
    }

    /**
     * 批量删除
     * Delete multiple courses by IDs
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
            generalRedisTemplate.delete("course:" + id);
        }
        clearCourseAllCache();

    }

    /**
     * 修改
     * Update a course by ID
     */
    public void updateById(Course course) {
        courseMapper.updateById(course);
        // 清理相关缓存 / Clear related cache
        clearCourseAllCache();

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
     * 根据User ID查询
     * Query a course by user ID
     */



    public List<Course>selectCoursesByUserId(Integer id) {
        User user=userService.selectById(id);
        List<Course> courses=new ArrayList();
        if (ObjectUtil.isNull(user)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }else{
            if ("TEACHER".equals(user.getLevel())){
                courses=courseMapper.selectByUserIdFromTeach(user.getId());
            }
            if ("STUDENT".equals(user.getLevel())){
                courses=courseMapper.selectByUserIdFromLearn(user.getId());
            }
        }
        return courses;
    }
    
    public List<CourseDTO>selectByUserId(Integer id) {
        User user=userService.selectById(id);
        List<Course> courses=new ArrayList();
        if (ObjectUtil.isNull(user)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }else{
            if ("TEACHER".equals(user.getLevel())){
                courses=courseMapper.selectByUserIdFromTeach(user.getId());
            }
            if ("STUDENT".equals(user.getLevel())){
                courses=courseMapper.selectByUserIdFromLearn(user.getId());
            }
        }

        // 构造返回的 CourseDTO 列表
        List<CourseDTO> result = new ArrayList<>();
        for (Course course : courses) {
            CourseDTO dto = new CourseDTO();
            // 继承 Course，所以直接 copy 属性
            BeanUtil.copyProperties(course, dto);

            // 查询 CourseSchedule
            List<CourseSchedule> schedules = courseScheduleMapper.selectByCourseId(course.getId());
            // 你 CourseDTO 只有一个 CourseSchedule 字段，可以决定放第一个或者改成 List<CourseSchedule>
            if (CollUtil.isNotEmpty(schedules)) {
                dto.setCourseSchedules(schedules);
            }

            result.add(dto);
        }

        return result;
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

    public List<CourseDetailsDTO> getCourseDetailsByUserId(Integer userId, List<Course> courseList)
    {
        List<CourseDetailsDTO> courseDetails = courseMapper.selectCourseDetailsByUserId(userId,courseList);
        return courseDetails;
    }

    public void updateLastSelectedCourse(Integer userId, Integer courseId)
    {
        courseMapper.updateLastSelectedCourse(userId,courseId);
    }

    public Integer countStudentByCourseId(Integer courseId){
        int num= learnMapper.countByCourseId(courseId);


        return  num;
    }


}
