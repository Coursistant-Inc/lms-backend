package com.coursistant.lms.module.auth.oauth.service;

import java.util.*;

import jakarta.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.shared.enums.ResultCodeEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.module.user.entity.Account;
import com.coursistant.lms.module.auth.oauth.entity.LinkedInUserInfo;
import com.coursistant.lms.module.user.entity.User;
import com.coursistant.lms.module.auth.oauth.dto.LinkedInDTO;
import com.coursistant.lms.shared.exception.CustomException;
import com.coursistant.lms.module.user.repository.UserMapper;
import com.coursistant.lms.module.user.service.UserService;
import com.coursistant.lms.shared.security.TokenUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cn.hutool.core.util.ObjectUtil;

@Service
public class OAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String googleTokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.redirect-uri}")
    private String facebookRedirectUri;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-id}")
    private String microsoftClientId;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-secret}")
    private String microsoftClientSecret;

    @Value("${spring.security.oauth2.client.provider.microsoft.token-uri}")
    private String microsoftTokenUri;

    @Value("${spring.security.oauth2.client.registration.microsoft.redirect-uri}")
    private String microsoftRedirectUri;

    @Value("${spring.security.oauth2.client.registration.microsoft.scope}")
    private String microsoftScope;

    @Value("${spring.linkedin.client-id}")
    private String linkdeInClientId;

    @Value("${spring.linkedin.client-secret}")
    private String linkedInClientSecret;

    @Value("${spring.linkedin.redirect-uri}")
    private String linkedInRedirectUri;

    @Value("${spring.linkedin.token-url}")
    private String linkedInTokenUrl;

    @Value("${spring.linkedin.auth-url}")
    private String linkedInAuthUrl;

    @Value("${spring.linkedin.scope}")
    private String linkedInScope;


    @Resource
    private UserMapper userMapper;
    
    @Resource
    private UserService userService;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 注册 register
     */
    public void register(Account account) {

        User user = new User();
        BeanUtils.copyProperties(account, user);

        //check
        String invitation = user.getInvitation();
        if ("PZMWXN4UUO".equals(invitation)) {
            user.setInvitation("Local Student"); // 本土学生
        } else if ("YK0AU47BZ1".equals(invitation)) {
            user.setInvitation("International Student"); // 留学生
        } else if ("OPH31E5TOK".equals(invitation)) {
            user.setInvitation("Developer"); // 开发人员
        } else if ("Z4G2MZ1XO1".equals(invitation)) {
            user.setInvitation("Teaching Class"); // 教学班级
        } else {
            throw new CustomException(ResultCodeEnum.INVITATION_NOT_EXIST_ERROR);
        }

        userService.add(user);

    }

    public Result getEmailFromAuthCodeGoogle(String authorizationCode) {
        // Step 1: Exchange the authorization code for an access token
        String tokenResponse = exchangeAuthCodeForToken(authorizationCode);
        String accessToken = extractAccessToken(tokenResponse);

        // Step 2: Use the access token to retrieve the user's info (email)
        return getUserInfoGoogle(accessToken);
    }

    private String exchangeAuthCodeForToken(String authorizationCode) {
        String url = UriComponentsBuilder.fromHttpUrl(googleTokenUri)
                .queryParam("code", authorizationCode)
                .queryParam("client_id", googleClientId)
                .queryParam("client_secret", googleClientSecret)
                .queryParam("redirect_uri", googleRedirectUri) // Make sure this matches the one you set in Google Developer Console
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        return restTemplate.postForObject(url, null, String.class);
    }

    private String extractAccessToken(String tokenResponse) {
        JsonObject jsonObject = JsonParser.parseString(tokenResponse).getAsJsonObject();
        return jsonObject.get("access_token").getAsString();
    }

    private Result getUserInfoGoogle(String accessToken) {
        String userInfoResponse = restTemplate.getForObject(googleUserInfoUri + "?access_token=" + accessToken, String.class);

        JsonObject jsonObject = JsonParser.parseString(userInfoResponse).getAsJsonObject();
        String email = jsonObject.get("email").getAsString();
        String name = jsonObject.get("name").getAsString();

        return getUserInfo(email, name);
    }

    public Result getEmailFromAuthCodeFacebook(String code){
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v18.0/oauth/access_token")
            .queryParam("client_id", facebookClientId)
            .queryParam("redirect_uri", facebookRedirectUri)
            .queryParam("client_secret", facebookClientSecret)
            .queryParam("code", code)
            .toUriString();

        String tokenResponse = restTemplate.postForObject(url, null, String.class);
        String accessToken = extractAccessToken(tokenResponse);

        return getFacebookUserInfo(accessToken);

    }


    private Result getFacebookUserInfo(String accessToken){
        String userInfoUrl = "https://graph.facebook.com/me?fields=id,name,email&access_token="+accessToken;

        String userInfoResponse = restTemplate.getForObject(userInfoUrl, String.class);

        JsonObject jsonObject = JsonParser.parseString(userInfoResponse).getAsJsonObject();
        System.out.println(userInfoResponse);
        String email = jsonObject.get("email").getAsString();
        String name = jsonObject.get("name").getAsString();


        return getUserInfo(email,name);


    }

    public Result getEmailFromAuthCodeMicrosoft(String authorizationCode) {
        // Step 1: Exchange the authorization code for an access token
        String tokenUrl = microsoftTokenUri; // Replace {tenant} with 'common', a specific tenant ID, or 'organizations'

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", microsoftClientId);
        body.add("client_secret", microsoftClientSecret);
        body.add("code", authorizationCode);
        body.add("redirect_uri", microsoftRedirectUri);  // Must match Azure registered URI
        body.add("grant_type", "authorization_code");
        body.add("scope", "openid profile email");  // Required scope for token

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, requestEntity, Map.class
            );
        
        String idToken = response.getBody().get("id_token").toString();
        //System.out.println(idToken); 

        DecodedJWT jwt = JWT.decode(idToken); 

        String name = jwt.getClaim("name").asString(); 
        String email = jwt.getClaim("email").asString(); 

    
        // Step 2: Use the access token to retrieve the user's info (email)
        return getUserInfo(email, name);
        //return new Result();
    }

    /**
     * 生成授权链接，引导用户登录
     */
    public String returnLinkedInUrl(){
        String authorizationUrl = linkedInAuthUrl + "?response_type=code" +
                "&client_id=" + linkdeInClientId +
                "&redirect_uri=" + linkedInRedirectUri +
                "&scope=" + linkedInScope +
                "&state=" + UUID.randomUUID().toString();
        return authorizationUrl;
    }

    /**
     * 通过授权码获取 Access Token
     * @param authorizationCode LinkedIn 返回的授权码
     * @return access_token
     */
    public String getLinkedInAccessToken(String authorizationCode) {
        // 1. 创建请求体，使用 LinkedMultiValueMap
        LinkedMultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", authorizationCode);
        requestBody.add("redirect_uri", linkedInRedirectUri);
        requestBody.add("client_id", linkdeInClientId);
        requestBody.add("client_secret", linkedInClientSecret);

        // 2. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 3. 创建 HttpEntity
        HttpEntity<LinkedMultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        // 4. 确保 RestTemplate 具有合适的转换器
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        // 5. 发送 POST 请求
        ResponseEntity<Map> response = restTemplate.postForEntity(linkedInTokenUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("获取 access_token 失败：" + response.getStatusCode());
        }
    }


    /**
     * 使用 access_token 获取 LinkedIn 用户信息
     */
    public LinkedInUserInfo getLinkedInUserInfo(String accessToken) {
        String userInfoUrl = "https://api.linkedin.com/v2/userinfo";

        // 设置 Authorization 头部
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 发送 GET 请求
        ResponseEntity<LinkedInUserInfo> response = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, entity, LinkedInUserInfo.class);

        // 返回解析后的用户信息
        return response.getBody();
    }

    public Result getEmailFromAuthCodeLinkedIn(String authorizationCode) {

        String linkedIn_token=getLinkedInAccessToken(authorizationCode);
        LinkedInUserInfo userInfo=getLinkedInUserInfo(linkedIn_token);
        String name = userInfo.getName();
        String email = userInfo.getEmail();

    
        // Step 2: Use the access token to retrieve the user's info (email)
        return getUserInfo(email, name);

    }

    private Result getUserInfo(String email, String name) {
        String cacheKey = "user:email:" + email; // 用户缓存键 // User cache key

        Account dbUser;

        Account cachedAccount = (Account) generalRedisTemplate.opsForValue().get(cacheKey);
        if (cachedAccount != null) {
            dbUser = cachedAccount;
        } else {
            dbUser = userMapper.selectByEmail(email);
        }

        //ObjectMapper mapper = new ObjectMapper();
        Result result = null;

        if (ObjectUtil.isNull(dbUser)) {
            //jsonString = mapper.writeValueAsString(Result.error(ResultCodeEnum.USER_NOT_EXIST_ERROR, email));
            dbUser = new Account();
            dbUser.setEmail(email);
            dbUser.setName(name);
            result = Result.error(ResultCodeEnum.USER_NOT_EXIST_ERROR, dbUser);
        } else {
            dbUser.setAccessToken(TokenUtils.createAccessToken(dbUser.getId(), RoleEnum.USER.name()));

            result = Result.success(dbUser);
        }

        return result;
    }
}