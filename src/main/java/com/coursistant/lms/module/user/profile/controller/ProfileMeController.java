package com.coursistant.lms.module.user.profile.controller;

import com.coursistant.lms.module.user.profile.ProfileService;
import com.coursistant.lms.module.user.profile.dto.ProfileResponse;
import com.coursistant.lms.module.user.profile.dto.UpdateProfileRequest;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v2/me/profile")
public class ProfileMeController {

    @Resource
    private ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> getMyProfile(HttpServletRequest request) {
        return ApiResponse.success(profileService.getMyProfile(currentUserId(request)));
    }

    @Idempotent
    @PatchMapping
    public ApiResponse<ProfileResponse> updateMyProfile(HttpServletRequest request,
                                                        @RequestBody UpdateProfileRequest body) {
        return ApiResponse.success(profileService.updateMyProfile(currentUserId(request), body));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponse> uploadAvatar(HttpServletRequest request,
                                                     @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(profileService.uploadAvatar(currentUserId(request), file));
    }

    @Idempotent
    @DeleteMapping("/avatar")
    public ApiResponse<ProfileResponse> deleteAvatar(HttpServletRequest request) {
        return ApiResponse.success(profileService.deleteAvatar(currentUserId(request)));
    }

    private Integer currentUserId(HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        if (!(attr instanceof Integer userId)) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        return userId;
    }
}
