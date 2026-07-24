package com.coursistant.lms.module.user.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.module.user.entity.Profile;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.user.repository.ProfileMapper;
import com.coursistant.lms.module.user.entity.User;

/**
 * 用户个人资料业务处理 // User profile service processing
 */
@Service
public class ProfileService {
    private final ProfileMapper profileMapper;

    public ProfileService(ProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    /**
     * 根据用户 ID 获取个人资料 // Get profile by user ID
     */
    public Profile getProfileByUserId(Integer userId) {
        return profileMapper.selectByUserId(userId);
    }

    /**
     * 获取所有个人资料 // Get all profiles
     */
    public List<Profile> getAllProfiles() {
        return profileMapper.selectAll();
    }

    /**
     * 创建个人资料 // Create a new profile
     */
    public void createProfile(Profile profile) {
        profileMapper.insert(profile);
    }

    /**
     * 更新个人资料 // Update profile
     */
    public void updateProfile(Profile profile) {
        Profile existingProfile = profileMapper.selectByUserId(profile.getUserId());
        if (existingProfile != null) {
            // 个人资料存在，进行更新 // Profile exists, update it
            profile.setId(existingProfile.getId());  // 确保使用正确的 ID // Ensure correct ID is used
            profileMapper.updateById(profile);
        } else {
            // 个人资料不存在，创建新记录 // Profile does not exist, create a new one
            // profileMapper.insert(profile);
            // return "Profile not found!";
        }
    }

    /**
     * 根据 ID 删除个人资料 // Delete profile by ID
     */
    public void deleteProfile(Integer id) {
        profileMapper.deleteById(id);
    }

    // public void updateUserPrivacy(String privacy, Integer userId)
    // {
    //     if(privacy.equals(LevelEnum.SELF.level)||privacy.equals(LevelEnum.INSTRUCTOR.level)||privacy.equals(LevelEnum.STUDENT.level)||privacy.equals(LevelEnum.TA.level))
    //     {
    //         profileMapper.updateUserPrivacyById(privacy, userId);
    //     }

    //     else
    //     {
    //         throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    //     }
    // }

    // public String selectUserPrivacy(Integer userId)
    // {
    //     String privacy = profileMapper.selectUserPrivacyById(userId);
    //     return privacy;
    // }

    public List<Map<String,Object>> selectGradesById(Integer userId)
    {
        return profileMapper.selectGradesById(userId);
    }

    public List<Map<String,Object>> selectCourseGradesById(Integer userId, Integer courseId)
    {
        return profileMapper.selectCourseGradesById(userId, courseId);
    }

    public String selectAvatarPathById(Integer userId)
    {
        return profileMapper.selectAvatarPathById(userId);
    }

    public void updateAvatarPathById(Integer userId, String avatarPath)
    {
        profileMapper.updateAvatarPathById(userId, avatarPath);
    }

    public void deleteAvatatById(Integer userId)
    {
        profileMapper.deleteAvatarById(userId);
    }
}
