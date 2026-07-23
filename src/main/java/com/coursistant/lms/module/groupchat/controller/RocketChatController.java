package com.coursistant.lms.module.groupchat.controller;

import com.coursistant.lms.module.user.entity.Account;
import com.coursistant.lms.shared.security.TokenUtils;
import com.coursistant.lms.module.groupchat.service.RocketChatAuthService;
import com.coursistant.lms.module.user.service.UserService;
import com.auth0.jwt.JWT;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import com.coursistant.lms.module.user.entity.User;

@RestController
@RequestMapping("/rocketchat")
public class RocketChatController {

    @Resource
    private RocketChatAuthService rocketChatAuthService;
    
    @Resource
    private UserService userService;

    @PostMapping("/iframe-auth")
    public ResponseEntity<Map<String, Object>> iframeAuth(
        @RequestHeader(value = "token", required = false) String token,
        HttpServletRequest request
    ) {
        try {
            System.out.println("🔐 iframe-auth request received");
            
            Account currentUser = null;
            
            // 1. 尝试从 token header 获取用户
            if (token != null && !token.isEmpty()) {
                try {
                    String userRole = JWT.decode(token).getAudience().get(0);
                    String userId = userRole.split("-")[0];
                    currentUser = userService.selectById(Integer.valueOf(userId));
                    System.out.println("✅ User from token header: " + currentUser.getEmail());
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to parse token: " + e.getMessage());
                }
            }
            
            // 2. 否则从 TokenUtils 获取
            if (currentUser == null) {
                currentUser = TokenUtils.getCurrentUser();
            }
            
            if (currentUser == null || currentUser.getEmail() == null) {
                System.err.println("❌ No user logged in");
                return ResponseEntity.status(401)
                    .body(Map.of("success", false, "error", "unauthorized"));
            }

            System.out.println("✅ Current LMS User: " + currentUser.getEmail());

            // ⭐ 实时创建 RocketChat token
            System.out.println("🔑 Creating RocketChat token for: " + currentUser.getEmail());
            
            Map<String, String> rcToken = rocketChatAuthService.createTokenForUser(currentUser.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", rcToken.get("authToken"));
            response.put("userId", rcToken.get("userId"));
            
            System.out.println("✅ RocketChat token created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error in iframe-auth: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "RocketChat Integration"));
    }
}