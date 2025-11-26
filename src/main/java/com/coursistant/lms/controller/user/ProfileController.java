package com.coursistant.lms.controller.user;

import com.coursistant.lms.controller.course.CourseController;
import com.coursistant.lms.entity.Profile;
import com.coursistant.lms.service.user.ProfileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.MediaType;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * 个人资料前端操作接口
 * Profile frontend operation API
 */
@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profileService;

    private static final Logger logger = Logger.getLogger(CourseController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 根据用户 ID 查询个人资料
     * Get profile by user ID
     */
    @GetMapping("/user/{userId}")
    public Profile getProfileByUserId(@PathVariable Integer userId) {
        return profileService.getProfileByUserId(userId);
    }

    /**
     * 查询所有个人资料
     * Get all profiles
     */
    @GetMapping("/selectAll")
    public List<Profile> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    /**
     * 新增个人资料
     * Add a new profile
     */
    @PostMapping("/add")
    public String createProfile(@RequestBody Profile profile) {
        logRequest("add", profile.toString());
        profileService.createProfile(profile);
        logResponse("add", profile.toString());
        return "Profile created successfully!";
    }

    /**
     * 更新个人资料
     * Update profile
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateProfile(@RequestParam("userId") Integer userId,
                                @RequestPart("profile") String profileJson,
                                @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        ObjectMapper objectMapper = new ObjectMapper();
        Profile profile;
        try {
            // 将 JSON 字符串转换为 Profile 对象
            // Convert JSON string to Profile object
            profile = objectMapper.readValue(profileJson, Profile.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid profile JSON format", e);
        }
        logRequest("update", profile.toString());
        profile.setUserId(userId);
        // System.out.println("TEST: Avatar ???" + avatar);

        if (avatar != null && !avatar.isEmpty()) {
            try {
                // 1. 创建按日期存储的目录
                // 1. Create a directory for today's date
                String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
                String baseDir = "/home/ubuntu/SpringBoot/saved_images/avatars/"; // 更改头像存储路径 / Change path for avatars
                // String baseDir = System.getProperty("user.home") + "/SpringBoot/saved_images/avatars";
                String uploadDir = baseDir + datePath + "/";
                File dir = new File(uploadDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("Failed to create directory: " + uploadDir);
                }

                // 2. 生成唯一的文件名
                // 2. Generate a unique filename
                String fileName = System.currentTimeMillis() + "_" + avatar.getOriginalFilename();
                File destFile = new File(uploadDir + fileName);

                // 3. 保存文件
                // 3. Save the file
                avatar.transferTo(destFile);
                String absoluteFilePath = destFile.getAbsolutePath();

                // 4. 设置头像文件路径
                // 4. Set the avatar file path in the profile
                profile.setAvatar(absoluteFilePath);
                logger.info("Avatar saved at: " + absoluteFilePath);
                System.out.println("TEST: Avatar saved at: " + absoluteFilePath);
            } catch (Exception e) {
                logger.severe("Error saving avatar: " + e.getMessage());
            }
        }
        profileService.updateProfile(profile);
        logResponse("update", profile.toString());
        return "Profile updated (or created) successfully!";
    }

    /**
     * 根据 ID 删除个人资料
     * Delete profile by ID
     */
    @DeleteMapping("/delete/{id}")
    public String deleteProfile(@PathVariable Integer id) {
        profileService.deleteProfile(id);
        return "Profile deleted successfully!";
    }
}
