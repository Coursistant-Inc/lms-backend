package com.coursistant.lms.module.user.profile.controller;

import com.coursistant.lms.module.user.profile.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/users")
@Tag(name = "User Avatars", description = "Public avatar binary download")
public class UserAvatarController {

    @Resource
    private ProfileService profileService;

    @GetMapping("/{userId}/avatar")
    @Operation(
            operationId = "userGetAvatar",
            summary = "Get user avatar image",
            description = "Returns raw image bytes (not the JSON ApiResponse envelope). Content-Type is image/jpeg or image/png."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Avatar image bytes",
                    content = {
                            @Content(mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(mediaType = "image/jpg",
                                    schema = @Schema(type = "string", format = "binary"))
                    },
                    headers = {
                            @Header(
                                    name = "Cache-Control",
                                    description = "Caching directive set by the server",
                                    schema = @Schema(type = "string", example = "private, max-age=300")),
                            @Header(
                                    name = "Content-Length",
                                    description = "Byte length when provided by the container",
                                    schema = @Schema(type = "integer", format = "int64"))
                    }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Avatar not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    public ResponseEntity<InputStreamResource> getAvatar(@PathVariable Integer userId) {
        return profileService.streamAvatar(userId);
    }
}
