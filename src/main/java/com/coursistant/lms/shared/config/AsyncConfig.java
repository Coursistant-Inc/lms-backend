package com.coursistant.lms.shared.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 关键注解，启用异步
public class AsyncConfig {

    /**
     * 可选：自定义线程池 bean
     * 如果不需要自定义，可以不写这个，直接使用Spring默认线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(10);
        // 队列容量
        executor.setQueueCapacity(25);
        // 线程名前缀
        executor.setThreadNamePrefix("Async-Executor-");
        // 初始化
        executor.initialize();
        return executor;
    }
}
