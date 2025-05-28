package com.coursistant.lms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                //.pathMapping("/api")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.coursistant.lms.controller")) // 修改为你的控制器包路径 / Modify to your controller package path
                .paths(PathSelectors.any())
                .build();
    }
}
