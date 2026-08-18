package com.coursistant.lms.module.user.profile;

import com.coursistant.lms.module.file.storage.FileDownloadHeaders;
import com.coursistant.lms.module.file.storage.FileSignatureSamples;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectMetadata;
import com.coursistant.lms.module.file.storage.S3ObjectNotFoundException;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.module.file.storage.S3UploadRollback;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.ArgumentMatchers.isNull;
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
    @Mock
    private MinioOutboxService minioOutboxService;

    @InjectMocks
    private ProfileService profileService;

    @BeforeEach
    void injectRollback() {
        ReflectionTestUtils.setField(profileService, "s3UploadRollback", new S3UploadRollback(minioOutboxService));
    }

    @Test
    void uploadAvatar_emptyFile_is400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> profileService.uploadAvatar(1, new MockMultipartFile("file", new byte[0])));
        assertEquals(ErrorType.INVALID_AVATAR_FILE, ex.getErrorType());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getErrorType().getHttpStatus());
        verify(s3ObjectStorage, never()).putObject(anyString(), any(), any());
    }

    @Test
    void uploadAvatar_putsAvatarPrefixedKey() {
        User user = user(1, null);
        when(userMapper.selectById(1)).thenReturn(user);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "text/html", FileSignatureSamples.PNG);

        profileService.uploadAvatar(1, file);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(key.capture(), eq(file), eq("image/png"));
        assertEquals(true, key.getValue().startsWith("avatar/1/"));
        assertEquals(true, key.getValue().endsWith(".png"));
        verify(minioOutboxService, never()).enqueueDelete(any(), any(), any(), any());
        verify(s3ObjectStorage, never()).deleteObject(anyString());
    }

    @Test
    void orphanV3_putThenDbFailure_enqueuesIndependentAbort() {
        when(userMapper.selectById(1)).thenReturn(user(1, null));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", FileSignatureSamples.PNG);
        doThrow(new RuntimeException("db")).when(userMapper).updateById(any());

        assertThrows(RuntimeException.class, () -> profileService.uploadAvatar(1, file));

        ArgumentCaptor<String> physical = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(physical.capture(), eq(file), eq("image/png"));
        String logical = physical.getValue().substring("avatar/".length());
        verify(minioOutboxService).enqueueAbortStagingIndependent(eq("avatar"), eq(logical), isNull(), isNull());
    }

    @Test
    void orphanV7_replacingAvatar_enqueuesDeleteNotQuietDelete() {
        when(userMapper.selectById(1)).thenReturn(user(1, "1/old.jpg"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", FileSignatureSamples.PNG);

        profileService.uploadAvatar(1, file);

        verify(minioOutboxService).enqueueDelete("avatar", "1/old.jpg", null, null);
        verify(s3ObjectStorage, never()).deleteObject(anyString());
        verify(minioOutboxService, never()).enqueueAbortStagingIndependent(any(), any(), any(), any());
    }

    @Test
    void xssA1_htmlNamedPng_isInvalidAvatar() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", FileSignatureSamples.HTML);
        ApiException ex = assertThrows(ApiException.class, () -> profileService.uploadAvatar(1, file));
        assertEquals(ErrorType.INVALID_AVATAR_FILE, ex.getErrorType());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getErrorType().getHttpStatus());
        verify(s3ObjectStorage, never()).putObject(anyString(), any(), any());
    }

    @Test
    void uploadAvatar_storageFailure_is503NotInvalidFile() {
        when(userMapper.selectById(1)).thenReturn(user(1, null));
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", FileSignatureSamples.JPEG);
        doThrow(new S3StorageException("timeout")).when(s3ObjectStorage)
                .putObject(anyString(), any(), anyString());

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
        byte[] png = FileSignatureSamples.PNG;
        when(userMapper.selectById(1)).thenReturn(user(1, "1/a.png"));
        when(s3ObjectStorage.getObject("avatar/1/a.png")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(png), new S3ObjectMetadata("image/png", (long) png.length, "e")));

        ResponseEntity<InputStreamResource> response = profileService.streamAvatar(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(png.length, response.getHeaders().getContentLength());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertTrue(response.getHeaders().getFirst(FileDownloadHeaders.CONTENT_SECURITY_POLICY_HEADER).contains("sandbox"));
        assertEquals(FileDownloadHeaders.CONTENT_SECURITY_POLICY,
                response.getHeaders().getFirst(FileDownloadHeaders.CONTENT_SECURITY_POLICY_HEADER));
    }

    @Test
    void deleteAvatar_doesNotFailWhenObjectMissing() {
        when(userMapper.selectById(1)).thenReturn(user(1, "1/old.jpg"));
        doThrow(new S3ObjectNotFoundException("gone")).when(s3ObjectStorage).deleteObject("avatar/1/old.jpg");
        profileService.deleteAvatar(1);
        verify(s3ObjectStorage).deleteObject("avatar/1/old.jpg");
        verify(s3ObjectStorage, never()).putObject(startsWith("avatar/"), any(), any());
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
