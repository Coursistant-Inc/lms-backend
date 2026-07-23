package com.coursistant.lms.module.groupchat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.course.entity.Learn;
import com.coursistant.lms.module.user.entity.User;

@Service
public class RocketChatAuthService {

    @Value("${rocketchat.url}")
    private String rocketChatUrl;

    @Value("${rocketchat.admin.username}")
    private String adminUsername;

    @Value("${rocketchat.admin.password}")
    private String adminPassword;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminAuthToken;
    private String adminUserId;

    private static final Logger logger = LoggerFactory.getLogger(RocketChatAuthService.class);

    /**
     * 🎯 主入口：确保用户存在并自动加入课程频道
     * 在用户登录时调用这个方法即可！
     */
    public void ensureUserExistsAndJoinCourses(String email, String password, String name, Long userId) {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🚀 开始处理RocketChat用户同步");
            System.out.println("📧 邮箱: " + email);
            System.out.println("👤 用户ID: " + userId);
            System.out.println("=".repeat(60));
            
            // 1. 确保用户在RocketChat中存在
            ensureUserExists(email, password, name);
            
            // 2. 获取RocketChat用户ID
            String rocketChatUserId = getUserIdByEmail(email);
            if (rocketChatUserId == null) {
                throw new RuntimeException("Failed to get RocketChat user ID for: " + email);
            }
            
            // 3. 获取用户的课程列表
            List<CourseInfo> courses = getUserCourses(userId);
            System.out.println("📚 用户共选修 " + courses.size() + " 门课程");
            
            // 4. 自动加入课程频道
            if (!courses.isEmpty()) {
                autoJoinCourseChannels(rocketChatUserId, courses);
            }
            
            System.out.println("=".repeat(60));
            System.out.println("✅ RocketChat同步完成！");
            System.out.println("=".repeat(60) + "\n");
            
        } catch (Exception e) {
            System.err.println("❌ Error in ensureUserExistsAndJoinCourses: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process user and courses", e);
        }
    }

    /**
     * 📚 从数据库获取用户的课程列表
     */
    private List<CourseInfo> getUserCourses(Long userId) {
        String sql = "SELECT c.id, c.name " +
                     "FROM Learn l " +
                     "JOIN Course c ON l.course_id = c.id " +
                     "WHERE l.user_id = ?";
        
        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> {
            CourseInfo course = new CourseInfo();
            course.setCourseId(rs.getLong("id"));
            course.setCourseName(rs.getString("name"));
            return course;
        });
    }

    /**
     * 🎯 自动将用户加入所有课程频道
     */
    private void autoJoinCourseChannels(String userId, List<CourseInfo> courses) {
        System.out.println("\n📖 开始处理课程频道...");
        
        int successCount = 0;
        
        for (CourseInfo course : courses) {
            try {
                System.out.println("\n  处理课程: " + course.getCourseName());
                
                // 1. 检查频道是否存在
                String channelId = checkChannelExists(course.getCourseName());
                
                // 2. 如果不存在，创建频道
                if (channelId == null) {
                    System.out.println("    ⚠️ 频道不存在，正在创建...");
                    channelId = createChannel(course.getCourseName());
                } else {
                    System.out.println("    ✅ 频道已存在 (ID: " + channelId + ")");
                }
                
                // 3. 将用户加入频道
                boolean joined = addUserToChannel(channelId, userId);
                if (joined) {
                    successCount++;
                    System.out.println("    ✅ 用户已加入频道");
                }
                
            } catch (Exception e) {
                System.err.println("    ❌ 处理课程失败: " + e.getMessage());
                continue;
            }
        }
        
        System.out.println("\n✅ 完成！成功加入 " + successCount + "/" + courses.size() + " 个课程频道\n");
    }

    /**
     * 🔍 检查频道是否存在
     */
    private String checkChannelExists(String courseName) {
        try {
            ensureAdminLogin();
            
            String channelName = normalizeChannelName(courseName);
            
            String url = rocketChatUrl + "/api/v1/channels.info?roomName=" + 
                         java.net.URLEncoder.encode(channelName, "UTF-8");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (jsonResponse.path("success").asBoolean()) {
                return jsonResponse.path("channel").path("_id").asText();
            }
            
            return null;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ➕ 创建新频道
     */
    private String createChannel(String courseName) {
        try {
            ensureAdminLogin();
            
            String channelName = normalizeChannelName(courseName);
            String url = rocketChatUrl + "/api/v1/channels.create";
            
            ObjectNode channelData = objectMapper.createObjectNode();
            channelData.put("name", channelName);
            channelData.put("readOnly", false);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);
            HttpEntity<String> request = new HttpEntity<>(channelData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (!jsonResponse.path("success").asBoolean()) {
                throw new RuntimeException("Failed to create channel: " + 
                    jsonResponse.path("error").asText());
            }
            
            String channelId = jsonResponse.path("channel").path("_id").asText();
            System.out.println("    ✅ 频道创建成功: " + channelName + " (ID: " + channelId + ")");
            return channelId;
            
        } catch (Exception e) {
            System.err.println("❌ Error creating channel: " + e.getMessage());
            throw new RuntimeException("Failed to create channel", e);
        }
    }

    /**
     * 👥 将用户加入频道
     */
    private boolean addUserToChannel(String channelId, String userId) {
        try {
            ensureAdminLogin();
            
            String url = rocketChatUrl + "/api/v1/channels.invite";
            
            ObjectNode inviteData = objectMapper.createObjectNode();
            inviteData.put("roomId", channelId);
            inviteData.put("userId", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);
            HttpEntity<String> request = new HttpEntity<>(inviteData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            return jsonResponse.path("success").asBoolean();
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && 
                e.getMessage().contains("already in the channel")) {
                System.out.println("    ℹ️ 用户已在频道中");
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 🔧 规范化频道名称
     */
    private String normalizeChannelName(String courseName) {
        String normalized = courseName.replaceAll("\\s+", "-");
        normalized = normalized.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5._-]", "");
        normalized = normalized.toLowerCase();
        normalized = normalized.replaceAll("^-+|-+$", "");
        normalized = normalized.replaceAll("-+", "-");
        return normalized;
    }

    /**
     * 确保用户存在，如果不存在则创建，如果是 Pending 状态则激活
     */
    public void ensureUserExists(String email, String password, String name) {
        try {
            ensureAdminLogin();
            
            String userId = getUserIdByEmail(email);
            
            if (userId != null) {
                System.out.println("🔍 User exists in RocketChat: " + email);
                
                boolean isActive = checkUserActive(userId);
                if (!isActive) {
                    System.out.println("⚠️ User is Pending/Inactive, activating...");
                    activateUser(userId);
                    System.out.println("✅ User activated: " + email);
                } else {
                    System.out.println("✅ User is already active: " + email);
                }
            } else {
                System.out.println("📝 Creating new RocketChat user: " + email);
                createUser(email, password, name);
                System.out.println("✅ User created and activated: " + email);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error ensuring RocketChat user: " + e.getMessage());
            throw new RuntimeException("Failed to ensure RocketChat user exists", e);
        }
    }

    /**
     * 为用户创建 RocketChat 登录 Token
     */
    public Map<String, String> createTokenForUser(String email) {
        try {
            ensureAdminLogin();
            
            String userId = getUserIdByEmail(email);
            if (userId == null) {
                throw new RuntimeException("User not found in RocketChat: " + email);
            }

            String url = rocketChatUrl + "/api/v1/users.createToken";
            
            ObjectNode tokenData = objectMapper.createObjectNode();
            tokenData.put("userId", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);
            HttpEntity<String> request = new HttpEntity<>(tokenData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (!jsonResponse.path("success").asBoolean()) {
                throw new RuntimeException("Failed to create token for user: " + email);
            }

            String authToken = jsonResponse.path("data").path("authToken").asText();
            String returnedUserId = jsonResponse.path("data").path("userId").asText();
            
            System.out.println("✅ Token created for user: " + email);
            
            Map<String, String> result = new HashMap<>();
            result.put("authToken", authToken);
            result.put("userId", returnedUserId);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error creating token: " + e.getMessage());
            throw new RuntimeException("Failed to create token for user", e);
        }
    }

    private void ensureAdminLogin() {
        if (adminAuthToken != null && adminUserId != null) {
            return;
        }

        try {
            String url = rocketChatUrl + "/api/v1/login";
            
            ObjectNode loginData = objectMapper.createObjectNode();
            loginData.put("user", adminUsername);
            loginData.put("password", adminPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(loginData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            adminAuthToken = jsonResponse.path("data").path("authToken").asText();
            adminUserId = jsonResponse.path("data").path("userId").asText();

            System.out.println("✅ Admin logged in to RocketChat");
            
        } catch (Exception e) {
            System.err.println("❌ Admin login failed: " + e.getMessage());
            throw new RuntimeException("Failed to login as admin", e);
        }
    }

    private String getUserIdByEmail(String email) {
        try {
            System.out.println("🔍 [getUserIdByEmail] Searching for: " + email);
            
            int count = 100; // 每页100条
            int offset = 0;
            int maxPages = 20; // 最多查20页（2000个用户）
            
            for (int page = 0; page < maxPages; page++) {
                String queryJson = "{\"emails.address\":\"" + email + "\"}";
                String encodedQuery = java.net.URLEncoder.encode(queryJson, "UTF-8");
                String url = rocketChatUrl + "/api/v1/users.list?query=" + encodedQuery + 
                            "&count=" + count + "&offset=" + offset;
                
                System.out.println("🔍 [getUserIdByEmail] Page " + (page + 1) + ", offset: " + offset);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Auth-Token", adminAuthToken);
                headers.set("X-User-Id", adminUserId);
                HttpEntity<String> request = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                if (jsonResponse.path("success").asBoolean()) {
                    JsonNode users = jsonResponse.path("users");
                    int userCount = users.size();
                    
                    System.out.println("🔍 [getUserIdByEmail] Found " + userCount + " users on this page");
                    
                    if (userCount == 0) {
                        break; // 没有更多用户了
                    }
                    
                    // 遍历当前页的用户
                    for (int i = 0; i < userCount; i++) {
                        JsonNode user = users.get(i);
                        JsonNode emails = user.path("emails");
                        
                        for (int j = 0; j < emails.size(); j++) {
                            String userEmail = emails.get(j).path("address").asText();
                            
                            if (userEmail.equalsIgnoreCase(email)) {
                                String userId = user.path("_id").asText();
                                String username = user.path("username").asText();
                                System.out.println("✅ [getUserIdByEmail] Found! Username: " + username + ", UserID: " + userId);
                                return userId;
                            }
                        }
                    }
                    
                    // 如果这页用户数少于count，说明已经是最后一页
                    if (userCount < count) {
                        break;
                    }
                    
                    offset += count; // 下一页
                } else {
                    System.out.println("❌ [getUserIdByEmail] API call failed");
                    break;
                }
            }
            
            System.out.println("❌ [getUserIdByEmail] User not found after checking all pages: " + email);
            return null;
            
        } catch (Exception e) {
            System.err.println("❌ [getUserIdByEmail] Exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkUserActive(String userId) {
        try {
            String url = rocketChatUrl + "/api/v1/users.info?userId=" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (jsonResponse.path("success").asBoolean()) {
                return jsonResponse.path("user").path("active").asBoolean(false);
            }
            return false;
            
        } catch (Exception e) {
            System.err.println("❌ Error checking user active status: " + e.getMessage());
            return false;
        }
    }

    private void activateUser(String userId) {
        try {
            String url = rocketChatUrl + "/api/v1/users.setActiveStatus";
            
            ObjectNode activateData = objectMapper.createObjectNode();
            activateData.put("userId", userId);
            activateData.put("activeStatus", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);
            HttpEntity<String> request = new HttpEntity<>(activateData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (!jsonResponse.path("success").asBoolean()) {
                throw new RuntimeException("Failed to activate user: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error activating user: " + e.getMessage());
            throw new RuntimeException("Failed to activate user", e);
        }
    }

    private void createUser(String email, String password, String name) {
        try {
            String url = rocketChatUrl + "/api/v1/users.create";

            String username = email.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

            ObjectNode userData = objectMapper.createObjectNode();
            userData.put("email", email);
            userData.put("name", name != null ? name : username);
            userData.put("password", password);
            userData.put("username", username);
            userData.put("active", true);
            userData.put("joinDefaultChannels", true);
            userData.put("requirePasswordChange", false);
            userData.put("sendWelcomeEmail", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Auth-Token", adminAuthToken);
            headers.set("X-User-Id", adminUserId);

            logger.info("RocketChat users.create headers - X-User-Id={}, X-Auth-Token prefix={}",
                    adminUserId,
                    adminAuthToken != null && adminAuthToken.length() > 8
                            ? adminAuthToken.substring(0, 8) + "****"
                            : adminAuthToken);

            HttpEntity<String> request = new HttpEntity<>(userData.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            logger.info("RocketChat users.create status={}, body={}",
                    response.getStatusCode().value(), response.getBody());

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            if (!jsonResponse.path("success").asBoolean()) {
                String error = jsonResponse.path("error").asText();
                if (error != null && error.contains("already in use")) {
                    String userId = getUserIdByEmail(email);
                    if (userId != null) {
                        activateUser(userId);
                        return;
                    }
                }
                throw new RuntimeException("Failed to create user: " + error);
            }

            String userId = jsonResponse.path("user").path("_id").asText();
            if (userId != null && !userId.isEmpty()) {
                activateUser(userId);
            }

        } catch (Exception e) {
            logger.error("❌ Error creating user in RocketChat for email={}", email, e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * 课程信息实体类
     */
    public static class CourseInfo {
        private Long courseId;
        private String courseName;

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public String getCourseName() {
            return courseName;
        }

        public void setCourseName(String courseName) {
            this.courseName = courseName;
        }
    }
}