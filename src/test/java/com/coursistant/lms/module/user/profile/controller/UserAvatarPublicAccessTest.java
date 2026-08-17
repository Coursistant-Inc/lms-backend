package com.coursistant.lms.module.user.profile.controller;

import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectMetadata;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.interaction.notification.service.NotificationDeliveryOpsService;
import com.coursistant.lms.module.interaction.notification.service.NotificationSupport;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.module.user.profile.AvatarUrlBuilder;
import com.coursistant.lms.module.user.profile.ProfileService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ApiExceptionHandler;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.config.SecurityConfig;
import com.coursistant.lms.shared.config.WebConfig;
import com.coursistant.lms.shared.idempotency.IdempotencyFilter;
import com.coursistant.lms.shared.idempotency.IdempotencyInterceptor;
import com.coursistant.lms.shared.security.AccessTokenAuthService;
import com.coursistant.lms.shared.security.JwtAuthenticationFilter;
import com.coursistant.lms.shared.security.JwtInterceptor;
import com.coursistant.lms.shared.security.SecurityErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots a slim MVC + Security slice (not the full app) so Filter, SecurityConfig, and
 * JwtInterceptor from {@link WebConfig} all run. Storage and user lookup are mocked.
 */
@SpringBootTest(classes = UserAvatarPublicAccessTest.TestApp.class)
@AutoConfigureMockMvc
class UserAvatarPublicAccessTest {

    private static final byte[] PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final String CACHE_BUSTER = "fe3143012c4144ba9c986d4fb91ed8d1";

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            MailSenderAutoConfiguration.class
    })
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            SecurityErrorResponseWriter.class,
            JwtInterceptor.class,
            WebConfig.class,
            IdempotencyFilter.class,
            IdempotencyInterceptor.class,
            ProfileService.class,
            S3ObjectKeyResolver.class,
            UserAvatarController.class,
            ProfileMeController.class,
            ApiExceptionHandler.class
    })
    static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenAuthService accessTokenAuthService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private S3ObjectStorage s3ObjectStorage;

    @MockitoBean
    private AvatarUrlBuilder avatarUrlBuilder;

    @MockitoBean
    private NotificationDeliveryOpsService notificationDeliveryOpsService;

    @MockitoBean
    private NotificationSupport notificationSupport;

    @MockitoBean(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @MockitoBean(name = "idempotencyStringRedisTemplate")
    private StringRedisTemplate idempotencyStringRedisTemplate;

    @BeforeEach
    void stubAuthAndStorage() {
        when(accessTokenAuthService.authenticateBearer(any(), any()))
                .thenThrow(new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token"));

        User withAvatar = new User();
        withAvatar.setId(385);
        withAvatar.setAvatar("385/a.png");
        when(userMapper.selectById(385)).thenReturn(withAvatar);
        when(s3ObjectStorage.getObject("avatar/385/a.png")).thenAnswer(invocation ->
                new S3ObjectPayload(new ByteArrayInputStream(PNG),
                        new S3ObjectMetadata("image/png", (long) PNG.length, "e")));

        when(userMapper.selectById(999)).thenReturn(null);

        User noAvatar = new User();
        noAvatar.setId(1000);
        noAvatar.setAvatar(null);
        when(userMapper.selectById(1000)).thenReturn(noAvatar);
    }

    @Test
    void anonymousGetExistingAvatar_returnsImageAndPrivateCache() throws Exception {
        mockMvc.perform(get("/api/v2/users/385/avatar")
                        .contextPath("/api")
                        .queryParam("v", CACHE_BUSTER))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", "private, max-age=300"));
    }

    @Test
    void anonymousGetMissingUserOrAvatar_returns404Not401() throws Exception {
        mockMvc.perform(get("/api/v2/users/999/avatar").contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v2/users/1000/avatar").contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void anonymousNearbyAndMutatingAvatarPaths_remainProtected() throws Exception {
        mockMvc.perform(get("/api/v2/users/1").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v2/users/385/avatar/extra").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v2/users/abc/avatar").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v2/users/385/avatar").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/v2/me/profile/avatar")
                        .file(new MockMultipartFile("file", "a.png", "image/png", PNG))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .contextPath("/api"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v2/me/profile/avatar").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedGetAvatar_stillSucceeds() throws Exception {
        mockMvc.perform(get("/api/v2/users/385/avatar")
                        .contextPath("/api")
                        .header("Authorization", "Bearer valid-token")
                        .queryParam("v", CACHE_BUSTER))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
    }
}
