package com.coursistant.lms.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * Redis configuration class
 */
@Configuration
public class RedisConfig {

    private static final String REDIS_HOST = "labserver101.ddns.net";
    private static final int REDIS_PORT = 6381;
    private static final String REDIS_USERNAME = "default";
    private static final String REDIS_PASSWORD = "p@ssWord";

    /**
     * 通用 RedisTemplate（数据库 0）
     * General RedisTemplate (db0)
     */
    @Bean(name = "generalRedisTemplate")
    public RedisTemplate<String, Object> generalRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(2));
    }

    /**
     * 管理员所有数据的 RedisTemplate（数据库 1）
     * Admin All RedisTemplate (db1)
     */
    @Bean(name = "adminAllRedisTemplate")
    public RedisTemplate<String, Object> adminAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(3));
    }

    /**
     * 管理员页面数据的 RedisTemplate（数据库 2）
     * Admin Page RedisTemplate (db2)
     */
    @Bean(name = "adminPageRedisTemplate")
    public RedisTemplate<String, Object> adminPageRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(4));
    }

    /**
     * 用户所有数据的 RedisTemplate（数据库 3）
     * User All RedisTemplate (db3)
     */
    @Bean(name = "userAllRedisTemplate")
    public RedisTemplate<String, Object> userAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(5));
    }

    /**
     * 用户页面数据的 RedisTemplate（数据库 4）
     * User Page RedisTemplate (db4)
     */
    @Bean(name = "userPageRedisTemplate")
    public RedisTemplate<String, Object> userPageRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(6));
    }

    /**
     * 课程所有数据的 RedisTemplate（数据库 5）
     * Course All RedisTemplate (db5)
     */
    @Bean(name = "courseAllRedisTemplate")
    public RedisTemplate<String, Object> courseAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(7));
    }

    /**
     * 课程页面数据的 RedisTemplate（数据库 6）
     * Course Page RedisTemplate (db6)
     */
    @Bean(name = "coursePageRedisTemplate")
    public RedisTemplate<String, Object> coursePageRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(8));
    }

    /**
     * 教学所有数据的 RedisTemplate（数据库 7）
     * Teach All RedisTemplate (db7)
     */
    @Bean(name = "teachAllRedisTemplate")
    public RedisTemplate<String, Object> teachAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(9));
    }

    /**
     * 教学页面数据的 RedisTemplate（数据库 8）
     * Teach Page RedisTemplate (db8)
     */
    @Bean(name = "teachPageRedisTemplate")
    public RedisTemplate<String, Object> teachPageRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(10));
    }

    /**
     * 学习所有数据的 RedisTemplate（数据库 9）
     * Learn All RedisTemplate (db9)
     */
    @Bean(name = "learnAllRedisTemplate")
    public RedisTemplate<String, Object> learnAllRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(11));
    }

    /**
     * 学习页面数据的 RedisTemplate（数据库 10）
     * Learn Page RedisTemplate (db10)
     */
    @Bean(name = "learnPageRedisTemplate")
    public RedisTemplate<String, Object> learnPageRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(12));
    }

    @Bean(name = "refreshTokenRedisTemplate")
    public RedisTemplate<String, Object> refreshTokenRedisTemplate() {
        return createRedisTemplate(createLettuceConnectionFactory(13));
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
        redisConfig.setUsername(REDIS_USERNAME); // 设置用户名 / Set username
        redisConfig.setPassword(REDIS_PASSWORD); // 设置密码 / Set password

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
