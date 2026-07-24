package com.coursistant.lms.module.user.profile.controller;

import com.coursistant.lms.module.user.profile.ProfileService;
import jakarta.annotation.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/users")
public class UserAvatarController {

    @Resource
    private ProfileService profileService;

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<InputStreamResource> getAvatar(@PathVariable Integer userId) {
        return profileService.streamAvatar(userId);
    }
}
