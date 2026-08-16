package com.coursistant.lms.module.user.profile;

import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectMetadata;
import com.coursistant.lms.module.file.storage.S3ObjectNotFoundException;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceStorageTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private S3ObjectStorage s3ObjectStorage;
    @Spy
    private S3ObjectKeyResolver s3ObjectKeyResolver = new S3ObjectKeyResolver();
    @Mock
    private AvatarUrlBuilder avatarUrlBuilder;
    @Mock
    private RedisTemplate<String, Object> generalRedisTemplate;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void uploadAvatar_emptyFile_is400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> profileService.uploadAvatar(1, new MockMultipartFile("file", new byte[0])));
        assertEquals(ErrorType.INVALID_AVATAR_FILE, ex.getErrorType());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getErrorType().getHttpStatus());
        verify(s3ObjectStorage, never()).putObject(anyString(), any());
    }

    @Test
    void uploadAvatar_putsAvatarPrefixedKey() {
        User user = user(1, null);
        when(userMapper.selectById(1)).thenReturn(user);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        profileService.uploadAvatar(1, file);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(key.capture(), any());
        assertEquals(true, key.getValue().startsWith("avatar/1/"));
        assertEquals(true, key.getValue().endsWith(".png"));
    }

    @Test
    void uploadAvatar_storageFailure_is503NotInvalidFile() {
        when(userMapper.selectById(1)).thenReturn(user(1, null));
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});
        doThrow(new S3StorageException("timeout")).when(s3ObjectStorage).putObject(anyString(), any());

        ApiException ex = assertThrows(ApiException.class, () -> profileService.uploadAvatar(1, file));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void streamAvatar_missingObject_is404() {
        when(userMapper.selectById(1)).thenReturn(user(1, "1/old.jpg"));
        when(s3ObjectStorage.getObject("avatar/1/old.jpg")).thenThrow(new S3ObjectNotFoundException("missing"));

        ApiException ex = assertThrows(ApiException.class, () -> profileService.streamAvatar(1));
        assertEquals(ErrorType.NOT_FOUND, ex.getErrorType());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorType().getHttpStatus());
    }

    @Test
    void streamAvatar_forbidden_is503Not404() {
        when(userMapper.selectById(1)).thenReturn(user(1, "1/old.jpg"));
        when(s3ObjectStorage.getObject("avatar/1/old.jpg")).thenThrow(new S3StorageException("403"));

        ApiException ex = assertThrows(ApiException.class, () -> profileService.streamAvatar(1));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void streamAvatar_setsContentLength() throws Exception {
        byte[] png = new byte[]{1, 2, 3, 4};
        when(userMapper.selectById(1)).thenReturn(user(1, "1/a.png"));
        when(s3ObjectStorage.getObject("avatar/1/a.png")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(png), new S3ObjectMetadata("image/png", 4L, "e")));

        ResponseEntity<InputStreamResource> response = profileService.streamAvatar(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4L, response.getHeaders().getContentLength());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
    }

    @Test
    void deleteAvatar_doesNotFailWhenObjectMissing() {
        when(userMapper.selectById(1)).thenReturn(user(1, "1/old.jpg"));
        doThrow(new S3ObjectNotFoundException("gone")).when(s3ObjectStorage).deleteObject("avatar/1/old.jpg");
        profileService.deleteAvatar(1);
        verify(s3ObjectStorage).deleteObject("avatar/1/old.jpg");
        verify(s3ObjectStorage, never()).putObject(startsWith("avatar/"), any());
    }

    private static User user(Integer id, String avatar) {
        User user = new User();
        user.setId(id);
        user.setAvatar(avatar);
        user.setEmail("a@example.com");
        user.setName("A");
        user.setRole("USER");
        user.setLevel("STUDENT");
        return user;
    }
}
