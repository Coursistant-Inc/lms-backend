package com.coursistant.lms.module.file.util;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import org.springframework.stereotype.Component;

@Component
public class DriveBuilder {

    public Drive build(String accessTokenValue) throws Exception {
        var http = GoogleNetHttpTransport.newTrustedTransport();
        var json = GsonFactory.getDefaultInstance();

        // 用静态工厂方法创建 OAuth2Credentials
        OAuth2Credentials creds = OAuth2Credentials.create(
                new AccessToken(accessTokenValue, null)
        );

        return new Drive.Builder(http, json, new HttpCredentialsAdapter(creds))
                .setApplicationName("lms")
                .build();
    }
}
