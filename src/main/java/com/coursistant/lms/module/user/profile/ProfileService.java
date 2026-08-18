package com.coursistant.lms.module.user.profile;

import cn.hutool.core.util.StrUtil;
import com.coursistant.lms.module.file.storage.FileDownloadHeaders;
import com.coursistant.lms.module.file.storage.FileObjectSniff;
import com.coursistant.lms.module.file.storage.FileSignature;
import com.coursistant.lms.module.file.storage.S3DownloadBody;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectNotFoundException;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.module.file.storage.S3UploadRollback;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.profile.dto.ProfileResponse;
import com.coursistant.lms.module.user.profile.dto.UpdateProfileRequest;
import com.coursistant.lms.module.interaction.notification.service.NotificationDeliveryOpsService;
import com.coursistant.lms.module.interaction.notification.service.NotificationSupport;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ProfileService {

    private static final Logger logger = Logger.getLogger(ProfileService.class.getName());
    public static final String AVATAR_BUCKET = "avatar";
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    @Resource
    private UserMapper userMapper;

    @Resource
    private S3ObjectStorage s3ObjectStorage;

    @Resource
    private S3ObjectKeyResolver s3ObjectKeyResolver;

    @Resource
    private S3UploadRollback s3UploadRollback;

    @Resource
    private MinioOutboxService minioOutboxService;

    @Resource
    private AvatarUrlBuilder avatarUrlBuilder;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Resource
    private NotificationDeliveryOpsService notificationDeliveryOpsService;

    @Resource
    private NotificationSupport notificationSupport;

    public ProfileResponse getMyProfile(Integer userId) {
        User user = requireUser(userId);
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse updateMyProfile(Integer userId, UpdateProfileRequest request) {
        if (request == null
                || (request.getDisplayName() == null && request.getEmailNotifications() == null)) {
            throw new ApiException(ErrorType.PARAM_MISSING, "At least one field is required");
        }

        User user = requireUser(userId);
        User patch = new User();
        patch.setId(userId);

        if (request.getDisplayName() != null) {
            String name = request.getDisplayName().trim();
            if (name.isEmpty() || name.length() > 100) {
                throw new ApiException(ErrorType.BAD_REQUEST, "displayName must be 1-100 characters");
            }
            patch.setName(name);
            user.setName(name);
        }
        boolean disableEmail = false;
        if (request.getEmailNotifications() != null) {
            boolean previous = user.getEmailNotifications() == null || user.getEmailNotifications();
            patch.setEmailNotifications(request.getEmailNotifications());
            user.setEmailNotifications(request.getEmailNotifications());
            disableEmail = previous && !request.getEmailNotifications();
        }

        userMapper.updateById(patch);
        evictUserCache(user);
        if (disableEmail) {
            notificationSupport.afterCommit(() -> notificationDeliveryOpsService.cancelPendingEmailsFor(userId));
        }
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse uploadAvatar(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorType.INVALID_AVATAR_FILE, "Avatar file is required");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ApiException(ErrorType.INVALID_AVATAR_FILE, "Avatar must be at most 5MB");
        }

        FileSignature.Kind kind = FileSignature.detect(file);
        String extension;
        if (kind == FileSignature.Kind.JPEG) {
            extension = "jpg";
        } else if (kind == FileSignature.Kind.PNG) {
            extension = "png";
        } else {
            throw new ApiException(ErrorType.INVALID_AVATAR_FILE, "Avatar must be JPG or PNG");
        }
        String canonicalMime = FileSignature.canonicalMime(kind, extension);
        User user = requireUser(userId);
        String oldKey = user.getAvatar();
        String newKey = userId + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;

        S3UploadRollback.Scope rollback = s3UploadRollback.open(null, null);
        try {
            try {
                s3ObjectStorage.putObject(physicalKey(newKey), file, canonicalMime);
            } catch (S3StorageException e) {
                logger.log(Level.WARNING, "Avatar upload to S3 failed for user " + userId, e);
                throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to upload avatar");
            }
            rollback.remember(AVATAR_BUCKET, newKey);

            User patch = new User();
            patch.setId(userId);
            patch.setAvatar(newKey);
            userMapper.updateById(patch);
            user.setAvatar(newKey);
            evictUserCache(user);

            if (StrUtil.isNotBlank(oldKey) && !oldKey.equals(newKey)) {
                minioOutboxService.enqueueDelete(AVATAR_BUCKET, oldKey, null, null);
            }

            return toResponse(user);
        } catch (RuntimeException e) {
            rollback.abortIfNoTransaction();
            throw e;
        }
    }

    public ProfileResponse deleteAvatar(Integer userId) {
        User user = requireUser(userId);
        String oldKey = user.getAvatar();
        if (StrUtil.isBlank(oldKey)) {
            return toResponse(user);
        }

        userMapper.clearAvatar(userId);
        user.setAvatar(null);
        evictUserCache(user);

        deleteQuietly(oldKey);
        return toResponse(user);
    }

    public ResponseEntity<InputStreamResource> streamAvatar(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null || StrUtil.isBlank(user.getAvatar())) {
            throw new ApiException(ErrorType.NOT_FOUND, "Avatar not found");
        }
        String key = user.getAvatar();
        try {
            S3ObjectPayload payload = s3ObjectStorage.getObject(physicalKey(key));
            FileObjectSniff.Result sniffed;
            try {
                sniffed = FileObjectSniff.wrap(payload);
            } catch (RuntimeException e) {
                payload.close();
                throw e;
            }
            if (sniffed.kind() != FileSignature.Kind.JPEG && sniffed.kind() != FileSignature.Kind.PNG) {
                sniffed.abort();
                throw new ApiException(ErrorType.NOT_FOUND, "Avatar not found");
            }
            String mime = FileSignature.canonicalMime(sniffed.kind(), extensionOfKey(key));
            long length = S3DownloadBody.contentLength(sniffed.payload());
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                    .contentType(MediaType.parseMediaType(mime));
            FileDownloadHeaders.applySecurity(builder);
            if (length >= 0) {
                builder.contentLength(length);
            }
            return builder.body(S3DownloadBody.resource(sniffed.payload(), sniffed.stream()));
        } catch (ApiException e) {
            throw e;
        } catch (S3ObjectNotFoundException e) {
            throw new ApiException(ErrorType.NOT_FOUND, "Avatar not found");
        } catch (S3StorageException e) {
            logger.log(Level.WARNING, "Failed to download avatar for user " + userId, e);
            throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to load avatar");
        }
    }

    private String physicalKey(String objectKey) {
        return s3ObjectKeyResolver.resolve(AVATAR_BUCKET, objectKey);
    }

    private void deleteQuietly(String objectKey) {
        try {
            s3ObjectStorage.deleteObject(physicalKey(objectKey));
        } catch (S3StorageException e) {
            logger.log(Level.WARNING, "Failed to delete avatar object: " + objectKey, e);
        }
    }

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        return user;
    }

    private ProfileResponse toResponse(User user) {
        ProfileResponse response = new ProfileResponse();
        response.setUserId(user.getId());
        response.setDisplayName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setLevel(user.getLevel());
        response.setAvatarUrl(avatarUrlBuilder.build(user.getId(), user.getAvatar()));
        Boolean notifications = user.getEmailNotifications();
        response.setEmailNotifications(notifications == null || notifications);
        return response;
    }

    private void evictUserCache(User user) {
        if (user.getId() != null) {
            generalRedisTemplate.delete("user:" + user.getId());
        }
        if (StrUtil.isNotBlank(user.getEmail())) {
            generalRedisTemplate.delete("user:email:" + user.getEmail());
        }
    }

    private static String extensionOfKey(String key) {
        if (key == null) {
            return "";
        }
        String lower = key.toLowerCase(Locale.ROOT);
        int slash = lower.lastIndexOf('/');
        String name = slash >= 0 ? lower.substring(slash + 1) : lower;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }
}
