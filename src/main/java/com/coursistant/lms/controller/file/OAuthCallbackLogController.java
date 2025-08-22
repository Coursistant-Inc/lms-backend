package com.coursistant.lms.controller.file;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@RestController
public class OAuthCallbackLogController {

    private static final Logger log = Logger.getLogger(OAuthCallbackLogController.class.getName());

    // 你的应用有 contextPath=/api，这里用相对路径即可，运行时会变成 /api/login/oauth2/code/google-drive
    @GetMapping("/login/oauth2/code/google-drive")
    public void onDriveCallback(
            @RegisteredOAuth2AuthorizedClient("google-drive") OAuth2AuthorizedClient client,
            HttpServletResponse resp) throws IOException {

        if (client != null && client.getAccessToken() != null) {
            var token = client.getAccessToken();
            String masked = mask(token.getTokenValue());
            log.info(() -> String.format(
                    "Google Drive token acquired. exp=%s, scopes=%s, masked=%s",
                    token.getExpiresAt(), token.getScopes(), masked
            ));
            // 如果只在本地想看明文（不推荐），临时打印：log.info("raw="+token.getTokenValue());
        } else {
            log.warning("OAuth callback reached but token not available.");
        }

        // 回调后把用户带去你的前端成功页/下一步页面
        resp.sendRedirect("/app/drive-linked-success");
    }

    private String mask(String t) {
        if (t == null || t.length() <= 10) return "******";
        return t.substring(0, 6) + "..." + t.substring(t.length() - 4);
    }
}
