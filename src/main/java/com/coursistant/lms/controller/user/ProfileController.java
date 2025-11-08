package com.coursistant.lms.controller.user;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.controller.course.CourseController;
import com.coursistant.lms.entity.Profile;
import com.coursistant.lms.service.user.ProfileService;
import com.coursistant.lms.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * 个人资料前端操作接口
 * Profile frontend operation API
 */
@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final UserService userService;

    private static final Logger logger = Logger.getLogger(CourseController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    public ProfileController(ProfileService profileService, UserService userService) {
        this.profileService = profileService;
        this.userService = userService;
        
    }

    /**
     * 根据用户 ID 查询个人资料
     * Get profile by user ID
     */
    @GetMapping("/user/{userId}")
    public Result getProfileByUserId(@PathVariable Integer userId,@RequestParam(value="self_user_id") Integer selfUserId) {
        String selfLevel = userService.getUserLevel(selfUserId);
        String userPrivacyLevel = profileService.selectUserPrivacy(userId);
        if(ObjectUtils.isEmpty(userPrivacyLevel)||userPrivacyLevel.equals(selfLevel))
        {
            logger.log(Level.INFO,"view profile of :"+userId+" by: "+selfUserId);
            Profile profile = profileService.getProfileByUserId(userId);
            // if(profile == null)
            if(ObjectUtils.isEmpty(profile))
            {
                // return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found!");
                return Result.error(ResultCodeEnum.PROFILE_NOT_FOUND);
            }
            logResponse("getProfileByUserId", profile.toString());
            return Result.success(profile);
        }

        // return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Viewing profile of this user is not allowed as the user's level is: "+userPrivacyLevel + " and your level is: " + selfLevel);
        return Result.error(ResultCodeEnum.PROFILE_VIEWING_NOT_ALLOWED);
    }

    /**
     * 查询所有个人资料
     * Get all profiles
     */
    @GetMapping("/selectAll")
    public Result getAllProfiles() {
        List<Profile> allProfiles = profileService.getAllProfiles();
        return Result.success(allProfiles);
    }

    /**
     * 新增个人资料
     * Add a new profile
     */
    // @PostMapping("/add")
    // public String createProfile(@RequestBody Profile profile) {
    //     logRequest("add", profile.toString());
    //     profileService.createProfile(profile);
    //     logResponse("add", profile.toString());
    //     return "Profile created successfully!";
    // }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result createProfile(@RequestParam("userId") Integer userId, @RequestPart("profile") String profileJson,
                                @RequestParam(value="avatar",required=false) MultipartFile avatar)

    {
        ObjectMapper objectMapper = new ObjectMapper();
        Profile profile;

        if(ObjectUtils.isEmpty(profileJson))
        {
            // return "Profile details not found!";
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        try
        {
            profile = objectMapper.readValue(profileJson, Profile.class);
        }

        catch(JsonProcessingException e)
        {
            throw new RuntimeException("Invalid profile JSON format: "+e);
        }

        Profile existingProfile = profileService.getProfileByUserId(userId);
        // if(existingProfile!=null)
        if(!ObjectUtils.isEmpty(existingProfile))
        {
            // return "A profile of this user already exists.";
            return Result.error(ResultCodeEnum.PROFILE_ALREADY_EXISTS);
        }

        logRequest("add", profile.toString());
        if (!ObjectUtils.isEmpty(avatar) && !avatar.isEmpty()) {
            try {


                String avatarFileName = avatar.getOriginalFilename();
                int i = avatarFileName.lastIndexOf(".");

                if(i == -1)
                {
                    // return "Invalid file";
                    return Result.error(ResultCodeEnum.FILE_READ_ERROR);
                }
                String extension = i > 0? avatarFileName.substring(i + 1):"";
                if(!extension.equals("jpg")&&!extension.equals("png"))
                {
                    // return "Invalid avatar file format.";
                    // return Result.error("4030","Invalid file");
                    return Result.error(ResultCodeEnum.INVALID_AVATAR_FILE);

                }

                long avatarFileSizeInMB = avatar.getSize()/1048576;
                if(avatarFileSizeInMB > 50)
                {
                    // return "File size limit exceeded";
                    return Result.error(ResultCodeEnum.FILE_LIMIT_EXCEEDED);
                }

 

                // 1. 创建按日期存储的目录
                // 1. Create a directory for today's date
                String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
                String baseDir = "/home/admir/SpringBoot/saved_images/avatars/"; // 更改头像存储路径 / Change path for avatars
                // String baseDir = "C:/Users/Shreyansh Bardia/LMS-pictures/";
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
                System.out.println("AVATAR FILE PATH:"+absoluteFilePath);
                logger.log(Level.INFO, "Avatar saved at: {0}", absoluteFilePath);
                System.out.println("TEST: Avatar saved at: " + absoluteFilePath);
            } catch (Exception e) {
                logger.severe("Error saving avatar: " + e.getMessage());
            }

        }


        profileService.createProfile(profile);
        logRequest("add",profile.toString());
        // return "Profile created successfully!";
        return Result.success("Profile created successfully!");
    }

    /**
     * 更新个人资料
     * Update profile
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result updateProfile(@RequestParam("userId") Integer userId,
                                @RequestParam(value = "profile", required = false) String profileJson,
                                @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        ObjectMapper objectMapper = new ObjectMapper();
        Profile profile;
        try {

            Profile profileExists = profileService.getProfileByUserId(userId);
            if(ObjectUtils.isEmpty(profileExists))
            {
                return Result.error(ResultCodeEnum.PROFILE_NOT_FOUND);
            }

            // 将 JSON 字符串转换为 Profile 对象
            // Convert JSON string to Profile object
            // if(profileJson!=null)
            if(ObjectUtils.isEmpty(profileJson))
            {
                profile = objectMapper.readValue(profileJson, Profile.class);

            }

            else
            {
                profile = null;
            }

            // System.out.println("Avatar file size in MB: "+avatarFileSizeInMB);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid profile JSON format", e);
        }
        if(profile!=null)
        {
            logRequest("update", profile.toString());
            profile.setUserId(userId);

        }
        // System.out.println("TEST: Avatar ???" + avatar);


    

        if (ObjectUtils.isEmpty(avatar) && avatar!=null) {
            try {

               String avatarFileName = avatar.getOriginalFilename();
               System.out.println("AVATAR FILE NAME: "+avatarFileName);
                Integer i = avatarFileName.lastIndexOf(".");
                if(i == -1 || ObjectUtils.isEmpty(i))
                {
                    // return "Invalid file";
                    return Result.error(ResultCodeEnum.FILE_READ_ERROR);
                }
                String extension = i > 0? avatarFileName.substring(i + 1):"";
                if(!extension.equals("jpg")&&!extension.equals("png"))
                {
                    // return "Invalid avatar file format.";
                    return Result.error(ResultCodeEnum.INVALID_AVATAR_FILE);
                }


                long avatarFileSizeInMB = avatar.getSize()/1048576;
                if(avatarFileSizeInMB > 50)
                {
                    // return "File size limit exceeded";
                    return Result.error(ResultCodeEnum.FILE_LIMIT_EXCEEDED);
                }

                String oldAvatarPath = profileService.selectAvatarPathById(userId);
                if(ObjectUtils.isEmpty(oldAvatarPath)&& !oldAvatarPath.isEmpty())
                {
                    File oldAvatar = new File(oldAvatarPath);
                    oldAvatar.delete();
                    // System.out.println("OLD AVATAR DELETED");
                }

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
                // if(profile!=null)
                // {
                //     profile.setAvatar(absoluteFilePath);
                // }

                profileService.updateAvatarPathById(userId, absoluteFilePath);

                logger.info("Avatar saved at: " + absoluteFilePath);
                System.out.println("TEST: Avatar saved at: " + absoluteFilePath);
            } catch (Exception e) {
                logger.severe("Error saving avatar: " + e.getMessage());
            }

        }
        // if(profile!=null)
        if(!ObjectUtils.isEmpty(profile))
        {
            profileService.updateProfile(profile);
            logResponse("update", profile.toString());
            // return response;
        }
        // return "Profile updated successfully!";
        return Result.success("profile updated successfully!");


    }

    @GetMapping("/avatar/{id}")
    public Result selectAvatar(@PathVariable Integer id)
    {
        logger.log(Level.INFO,"selectAvatar of: "+id);
        // return profileService.selectAvatarPathById(id);
        String avatarPath = profileService.selectAvatarPathById(id);
        return Result.success(avatarPath);
    }

    @DeleteMapping("/delete/avatar/{id}")
    public Result deleteAvatar(@PathVariable Integer id)
    {
        String oldAvatarPath = profileService.selectAvatarPathById(id);
        if(!ObjectUtils.isEmpty(oldAvatarPath) &&!oldAvatarPath.isEmpty())
        {
            logger.log(Level.INFO,"deleteAvatar of: "+id);
            File oldAvatar = new File(oldAvatarPath);
            oldAvatar.delete();

            profileService.deleteAvatatById(id);
        }

        // return "Avatar deleted successfully!";
        return Result.success("Avatar deleted successfully!");

    }

    /**
     * 根据 ID 删除个人资料
     * Delete profile by ID
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteProfile(@PathVariable Integer id) {

        String oldAvatarPath = profileService.selectAvatarPathById(id);
        if(ObjectUtils.isEmpty(oldAvatarPath)&& !oldAvatarPath.isEmpty())
        {
            logger.log(Level.INFO,"Deleted profile of user id: "+id);
            File oldAvatar = new File(oldAvatarPath);
            oldAvatar.delete();
        }
        profileService.deleteProfile(id);
        // return "Profile deleted successfully!";
        return Result.success("Profile deleted successfully!");
    }

    @PostMapping("/update/privacy")
    public Result updatePrivacy(@RequestParam(value="privacy") String privacy, @RequestParam(value="user_id") Integer userId) {
        //TODO: process POST request
        
        // logger.log(Level.INFO,"Deleted profile of user id: "+id);
        logger.log(Level.INFO,"Updated privacy setting of user id: "+userId);

        profileService.updateUserPrivacy(privacy, userId);

        // return "Privacy setting updated.";
        return Result.success("Privacy setting updated.");
    }

    // Student viewing their grades
    @PostMapping("/grades")
    public Result viewGrades(@RequestParam("userId") Integer userId)
    {
        List<Map<String,Object>> grades = profileService.selectGradesById(userId);
        // return ResponseEntity.ok(grades);
        return Result.success(grades);
    }

    // Viewing grades of students in a course by a professor
    @PostMapping("/course/grades")
    public Result viewCourseGrades(@RequestParam(value="userId") Integer userId, @RequestParam(value="courseId") Integer courseId)
    {
        List<Map<String,Object>> grades = profileService.selectCourseGradesById(userId, courseId);
        // return ResponseEntity.ok(grades);
        return Result.success(grades);
    }
    
}
