package com.coursistant.lms.module.course.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.course.entity.Learn;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.course.repository.LearnMapper;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.account.service.UserService;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import java.util.stream.Collectors;
import com.coursistant.lms.module.chat.entity.Query;

@Service
public class LearnService {

    @Resource
    private LearnMapper learnMapper;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource(name = "learnAllRedisTemplate")
    private RedisTemplate<String, Object> learnAllRedisTemplate;

    @Resource(name = "learnPageRedisTemplate")
    private RedisTemplate<String, Object> learnPageRedisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    // 缓存过期时间（秒） // Cache expiration time (seconds)
    private static final long CACHE_EXPIRE_TIME = 300;

    /**
     * Clear learnAll database (for selectAll queries)
     */
    public void clearLearnAllCache() {
        Objects.requireNonNull(learnAllRedisTemplate.getConnectionFactory())
                .getConnection()
                .flushDb();
        System.out.println("Cleared all data from learnAll database.");
    }

    /**
     * Clear specific course cache from generalRedisTemplate
     */
    private void clearCourseCache(Integer courseId) {
        if (courseId != null) {
            String cacheKey = "learn:course:" + courseId;
            generalRedisTemplate.delete(cacheKey);
            System.out.println("Cleared cache: " + cacheKey);
        }
    }

    /**
     * Clear all learn-related caches from both templates
     */
    private void clearAllLearnCaches(Integer courseId) {
        clearLearnAllCache();        // Clear learnAllRedisTemplate
        clearCourseCache(courseId);   // Clear generalRedisTemplate learn:* keys
    }



    /**
     * 新增 // Add new record
     */
    public void add(Learn learn) {
        Learn existingLearn = learnMapper.selectByUserIdAndCourseId(learn.getUserId(), learn.getCourseId());
        if (existingLearn != null) {
            throw new CustomException(ResultCodeEnum.DUPLICATED_LEARN_RELATION_ERROR); // You may need to create this enum
        }
        learnMapper.insert(learn);
        // 清理相关缓存 // Clear relevant caches
        clearAllLearnCaches(learn.getCourseId());

    }

    /**
     * 通过 Email 添加记录 // Add record by email
     */
    public void addByEmail(String email, Integer courseId, MultipartFile file) {
        Learn learn = new Learn();
        if (ObjectUtil.isNotNull(email)) {
            User student = userMapper.selectByEmail(email);
            if (!ObjectUtil.isNotNull(student)) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
            learn.setUserId(student.getId());
            learn.setCourseId(courseId);
            learnMapper.insert(learn);
        }
        if (ObjectUtil.isNotNull(file)) {
            Learn excelLearn = new Learn();
            List<String> emails = extractUsernamesFromExcel(file);
            for (String excelEmail : emails) {
                User student = userMapper.selectByEmail(excelEmail);
                if (!ObjectUtil.isNotNull(student)) {
                    throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
                }
                excelLearn.setUserId(student.getId());
                excelLearn.setCourseId(courseId);
                learnMapper.insert(excelLearn);
            }
        }
        // 清理相关缓存 // Clear relevant caches
        clearLearnAllCache();

    }

    /**
     * 根据 ID 删除 // Delete by ID
     */
    public void deleteById(Integer id) {
        learnMapper.deleteById(id);
        // 清理相关缓存 // Clear relevant caches
        clearLearnAllCache();

        generalRedisTemplate.delete("learn:" + id);
    }

    /**
     * 批量删除 // Batch delete
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            learnMapper.deleteById(id);
            generalRedisTemplate.delete("learn:" + id);
        }
        clearLearnAllCache();

    }

    /**
     * 修改 // Update record
     */
    public void updateById(Learn learn) {
        learnMapper.updateById(learn);
        // 清理相关缓存 // Clear relevant caches
        clearLearnAllCache();

        generalRedisTemplate.delete("learn:" + learn.getId());
    }

    /**
     * 根据 ID 查询 // Select by ID
     */
    public Learn selectById(Integer id) {
        String cacheKey = "learn:" + id;

        // 从 Redis 获取缓存 // Get cache from Redis
        Learn learn = (Learn) generalRedisTemplate.opsForValue().get(cacheKey);
        if (learn != null) {
            System.out.println("from cache: " + cacheKey);
            return learn;
        }

        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from database
        learn = learnMapper.selectById(id);
        if (learn == null) {
            throw new CustomException(ResultCodeEnum.LEARN_NOT_EXIST_ERROR);
        }

        // 将结果存入 Redis，并设置过期时间 // Store result in Redis with expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, learn, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return learn;
    }


    public List<Learn> selectByCourseId(Integer courseId) {
        String cacheKey = "learn:course:" + courseId;

        // 从 Redis 获取缓存 // Get cache from Redis
        List<Learn> learnList = (List<Learn>) generalRedisTemplate.opsForValue().get(cacheKey);
        if (learnList != null) {
            System.out.println("from cache: " + cacheKey);
            return learnList;
        }

        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from database
        learnList = learnMapper.selectByCourseId(courseId);
        if (learnList == null || learnList.isEmpty()) {
            return new ArrayList<>();
            // 或者抛出异常:
            // Or throw exception:
            // throw new CustomException(ResultCodeEnum.NO_STUDENTS_IN_COURSE_ERROR);
        }

        // 将结果存入 Redis，并设置过期时间
        // Store result in Redis with expiration time
        generalRedisTemplate.opsForValue().set(cacheKey, learnList, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        return learnList;
    }

    public List<User> getStudentsByCourseId(Integer courseId) {
        List<Learn> learnList = selectByCourseId(courseId);

        // 提取用户ID列表并查询用户信息
        // Extract user ID list and query user information
        List<Integer> userIds = learnList.stream()
                .map(Learn::getUserId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 假设你有 userService 来批量查询用户
        // Assuming you have userService to batch query users
        return userService.selectUsersByIds(userIds);
    }

    public List<Learn> selectByStudentId(Integer id) {

        List<Learn> learns = learnMapper.selectByUserId(id);
        if (learns == null) {
            throw new CustomException(ResultCodeEnum.LEARN_NOT_EXIST_ERROR);
        }


        return learns;
    }


    public Learn selectByUserIdAndCourseId(Integer userId, Integer courseId) {
        if (userId == null || courseId == null) {
            return null;
        }

        String cacheKey = "learn:user:" + userId + ":course:" + courseId;

        Learn learn = (Learn) learnAllRedisTemplate.opsForValue().get(cacheKey);
        if (learn != null) {
            System.out.println("from cache: " + cacheKey);
            return learn;
        }

        // Query from database
        learn = learnMapper.selectByUserIdAndCourseId(userId, courseId);

        // Cache the result (including null results to avoid repeated DB queries)
        if (learn != null) {
            learnAllRedisTemplate.opsForValue().set(cacheKey, learn, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        } else {
            // Cache null result for shorter time to avoid repeated queries for non-existent records
            learnAllRedisTemplate.opsForValue().set(cacheKey, "NULL", 60, TimeUnit.SECONDS);
        }

        return learn;
    }

    /**
     * 查询所有 // Select all records
     */
    public List<Learn> selectAll(Learn learn) {
        String cacheKey = "learn:all";
        if (learn != null) {
            cacheKey += learn.toString();
        }

        // 从 Redis 获取缓存 // Get cache from Redis
        List<Learn> learns = (List<Learn>) learnAllRedisTemplate.opsForValue().get(cacheKey);
        if (learns != null) {
            System.out.println("from cache: " + cacheKey);
            return learns;
        }

        // 如果缓存不存在，从数据库查询 // If cache does not exist, query from database
        learns = learnMapper.selectAll(learn);
        if (learns != null && !learns.isEmpty()) {
            learnAllRedisTemplate.opsForValue().set(cacheKey, learns, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return learns;
    }


    /**
     * 读取 Excel 文件，提取用户名列表 // Read Excel file and extract username list
     */
    private List<String> extractUsernamesFromExcel(MultipartFile file) {
        List<String> emails = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            System.out.println("上传的文件名：" + file.getOriginalFilename()); // Uploaded file name
            System.out.println("文件大小：" + file.getSize()); // File size

            Workbook workbook = WorkbookFactory.create(inputStream); // 自动解析 .xls / .xlsx // Auto-detect and parse .xls / .xlsx
            Sheet sheet = workbook.getSheetAt(0); // 读取第一个工作表 // Read the first sheet

            System.out.println("读取 Excel 文件成功！解析 Sheet 名称：" + sheet.getSheetName()); // Successfully read Excel file! Parsed sheet name:

            for (Row row : sheet) {
                Cell cell = row.getCell(0); // 假设用户名在第一列 // Assume username is in the first column
                if (cell != null) {
                    cell.setCellType(CellType.STRING); // 确保读取为字符串 // Ensure reading as a string
                    emails.add(cell.getStringCellValue().trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // 打印详细异常信息 // Print detailed exception information
            throw new CustomException(ResultCodeEnum.FILE_READ_ERROR);
        }
        return emails;
    }

    public void updateCourseStatus(Integer userId, Integer courseId, String courseStatus)
    {
        learnMapper.updateLearnStatusById(userId, courseId, courseStatus);
    }

    public String selectCourseStatus(Integer userId, Integer courseId)
    {
       String courseStatus = learnMapper.selectLearnStatusById(userId, courseId);

       return courseStatus;
    }

    public void updateCourseGrade(Integer userId, Integer courseId, String grade)
    {
        learnMapper.updateGradeById(userId, courseId, grade);
    }

    public String selectCourseGrade(Integer userId, Integer courseId)
    {
        String grade = learnMapper.selectGradeById(userId, courseId);
        return grade;
    }
}
