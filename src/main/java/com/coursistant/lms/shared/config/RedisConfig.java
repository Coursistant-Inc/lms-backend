package com.coursistant.lms.shared.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * Redis configuration class
 */
@Configuration
public class RedisConfig {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    //@Value("${REDIS_DEFAULT_USERNAME}")
    //private String redisUsername;

    @Value("${REDIS_DEFAULT_PASSWORD}")
    private String redisPassword;

    /**
     * 通用 RedisTemplate（数据库 0）
     * General RedisTemplate (db0)
     */
    @Bean(name = "generalRedisTemplate")
    public RedisTemplate<String, Object> generalRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(0));
    }

    /**
     * 管理员所有数据的 RedisTemplate（数据库 1）
     * Admin All RedisTemplate (db1)
     */
    @Bean(name = "adminAllRedisTemplate")
    public RedisTemplate<String, Object> adminAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(1));
    }


    /**
     * 课程所有数据的 RedisTemplate（数据库 3）
     * Course All RedisTemplate (db3)
     */
    @Bean(name = "courseAllRedisTemplate")
    public RedisTemplate<String, Object> courseAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(3));
    }


    /**
     * 幂等系统 StringRedisTemplate（数据库 2）
     * Idempotency StringRedisTemplate (db2)
     */
    @Bean(name = "idempotencyStringRedisTemplate")
    public StringRedisTemplate idempotencyStringRedisTemplate() {
        LettuceConnectionFactory factory = createLettuceConnectionFactory(2);
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "refreshTokenRedisTemplate")
    public RedisTemplate<String, Object> refreshTokenRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(6));
    }

    /**
     * 创建 LettuceConnectionFactory 并指定数据库
     * Create LettuceConnectionFactory and specify the database
     */
    private LettuceConnectionFactory createLettuceConnectionFactory(int database) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(REDIS_HOST);
        redisConfig.setPort(REDIS_PORT);
        redisConfig.setDatabase(database);
        //redisConfig.setUsername(redisUsername); // 设置用户名 / Set username
        redisConfig.setPassword(redisPassword); // 设置密码 / Set password

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 创建 RedisTemplate 并绑定指定的 LettuceConnectionFactory
     * Create RedisTemplate and bind it to the specified LettuceConnectionFactory
     */
    private RedisTemplate<String, Object> createRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置 Jackson 序列化器（保留 @class）
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        serializer.setObjectMapper(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }



}