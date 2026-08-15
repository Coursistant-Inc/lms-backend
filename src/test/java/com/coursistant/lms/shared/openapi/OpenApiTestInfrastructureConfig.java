package com.coursistant.lms.shared.openapi;

import io.minio.MinioClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Stubs external infra for the openapi profile so contract export needs no Redis/MinIO/Mail.
 */
@TestConfiguration
@Profile("openapi")
public class OpenApiTestInfrastructureConfig {

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(template.opsForValue()).thenReturn(ops);
        return template;
    }

    @Bean(name = "generalRedisTemplate")
    public RedisTemplate<String, Object> generalRedisTemplate() {
        return mockRedisTemplate();
    }

    @Bean(name = "adminAllRedisTemplate")
    public RedisTemplate<String, Object> adminAllRedisTemplate() {
        return mockRedisTemplate();
    }

    @Bean(name = "courseAllRedisTemplate")
    public RedisTemplate<String, Object> courseAllRedisTemplate() {
        return mockRedisTemplate();
    }

    @Bean(name = "idempotencyStringRedisTemplate")
    public StringRedisTemplate idempotencyStringRedisTemplate() {
        return stringRedisTemplate();
    }

    @Bean(name = "refreshTokenRedisTemplate")
    public RedisTemplate<String, Object> refreshTokenRedisTemplate() {
        return mockRedisTemplate();
    }

    @Bean
    @Primary
    public MinioClient minioClient() {
        return Mockito.mock(MinioClient.class);
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> template = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, Object> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(template.opsForValue()).thenReturn(ops);
        return template;
    }
}
