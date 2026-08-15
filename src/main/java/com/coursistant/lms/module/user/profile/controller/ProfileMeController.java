package com.coursistant.lms.module.user.profile.controller;

import com.coursistant.lms.module.user.profile.ProfileService;
import com.coursistant.lms.module.user.profile.dto.ProfileResponse;
import com.coursistant.lms.module.user.profile.dto.UpdateProfileRequest;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiResponse;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Profile", description = "Current-user profile and avatar self-service")
public class ProfileMeController {

    @Resource
    private ProfileService profileService;

    @GetMapping
    @Operation(operationId = "meProfileGet", summary = "Get my profile")
    public ApiResponse<ProfileResponse> getMyProfile(HttpServletRequest request) {
        return ApiResponse.success(profileService.getMyProfile(currentUserId(request)));
    }

    @Idempotent
    @PatchMapping
    @Operation(operationId = "meProfileUpdate", summary = "Update my profile")
    public ApiResponse<ProfileResponse> updateMyProfile(HttpServletRequest request,
                                                        @RequestBody UpdateProfileRequest body) {
        return ApiResponse.success(profileService.updateMyProfile(currentUserId(request), body));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "meProfileUploadAvatar",
            summary = "Upload my avatar",
            description = "Multipart field name must be `file`. JPG/JPEG/PNG only, max 5MB.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "object", requiredProperties = {"file"})
                    )
            )
    )
    public ApiResponse<ProfileResponse> uploadAvatar(HttpServletRequest request,
                                                     @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(profileService.uploadAvatar(currentUserId(request), file));
    }

    @Idempotent
    @DeleteMapping("/avatar")
    @Operation(operationId = "meProfileDeleteAvatar", summary = "Delete my avatar")
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
