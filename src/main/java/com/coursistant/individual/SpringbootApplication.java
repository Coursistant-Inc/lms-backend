package com.coursistant.individual;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@MapperScan("com.coursistant.individual.mapper")
public class SpringbootApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
        System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
        System.setProperty("MICROSOFT_CLIENT_ID", dotenv.get("MICROSOFT_CLIENT_ID"));
        System.setProperty("MICROSOFT_CLIENT_SECRET", dotenv.get("MICROSOFT_CLIENT_SECRET"));
        System.setProperty("FACEBOOK_CLIENT_ID", dotenv.get("FACEBOOK_CLIENT_ID"));
        System.setProperty("FACEBOOK_CLIENT_SECRET", dotenv.get("FACEBOOK_CLIENT_SECRET"));
        System.setProperty("LINKEDIN_CLIENT_ID", dotenv.get("LINKEDIN_CLIENT_ID"));
        System.setProperty("LINKEDIN_CLIENT_SECRET", dotenv.get("LINKEDIN_CLIENT_SECRET"));
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
        System.setProperty("MAIL_USERNAME", dotenv.get("MAIL_USERNAME"));
        System.setProperty("MAIL_PASSWORD", dotenv.get("MAIL_PASSWORD"));
        System.setProperty("REDIS_DEFAULT_USERNAME", dotenv.get("REDIS_DEFAULT_USERNAME"));
        System.setProperty("REDIS_DEFAULT_PASSWORD", dotenv.get("REDIS_DEFAULT_PASSWORD"));
        SpringApplication.run(SpringbootApplication.class, args);
    }

}
