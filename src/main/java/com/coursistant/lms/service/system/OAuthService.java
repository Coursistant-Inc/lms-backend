package com.coursistant.lms.service.system;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.common.Result;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.common.enums.RoleEnum;
import com.coursistant.lms.entity.Account;
import com.coursistant.lms.mapper.user.UserMapper;
import com.coursistant.lms.utils.TokenUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cn.hutool.core.util.ObjectUtil;

@Service
public class OAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String userInfoUri;

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

    @Resource
    private UserMapper userMapper;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    public Result getEmailFromAuthCodeGoogle(String authorizationCode) {
        // Step 1: Exchange the authorization code for an access token
        String tokenResponse = exchangeAuthCodeForToken(authorizationCode);
        String accessToken = extractAccessToken(tokenResponse);

        // Step 2: Use the access token to retrieve the user's info (email)
        return getUserInfoGoogle(accessToken);
    }

    private String exchangeAuthCodeForToken(String authorizationCode) {
        RestTemplate restTemplate = new RestTemplate();
        String url = UriComponentsBuilder.fromHttpUrl(tokenUri)
                .queryParam("code", authorizationCode)
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", redirectUri) // Make sure this matches the one you set in Google Developer Console
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        return restTemplate.postForObject(url, null, String.class);
    }

    private String extractAccessToken(String tokenResponse) {
        JsonObject jsonObject = JsonParser.parseString(tokenResponse).getAsJsonObject();
        return jsonObject.get("access_token").getAsString();
    }

    private Result getUserInfoGoogle(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String userInfoResponse = restTemplate.getForObject(userInfoUri + "?access_token=" + accessToken, String.class);

        JsonObject jsonObject = JsonParser.parseString(userInfoResponse).getAsJsonObject();
        String email = jsonObject.get("email").getAsString();
        String name = jsonObject.get("name").getAsString();

        return getUserInfo(email, name);
    }

    public Result getFacebookUserAccessToken(String code){
        RestTemplate restTemplate = new RestTemplate();
        String url = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v18.0/oauth/access_token")
            .queryParam("client_id", facebookClientId)
            .queryParam("redirect_uri", facebookRedirectUri)
            .queryParam("client_secret", facebookClientSecret) // Make sure this matches the one you set in Google Developer Console
            .queryParam("code", code)
            .toUriString();

        String tokenResponse = restTemplate.postForObject(url, null, String.class);
        String accessToken = extractAccessToken(tokenResponse);

        return getFacebookUserInfo(accessToken);

    }


    private Result getFacebookUserInfo(String accessToken){
        String userInfoUrl = "https://graph.facebook.com/me?fields=id,name,email&access_token="+accessToken;

        RestTemplate restTemplate = new RestTemplate();
        String userInfoResponse = restTemplate.getForObject(userInfoUrl, String.class);

        JsonObject jsonObject = JsonParser.parseString(userInfoResponse).getAsJsonObject();
        System.out.println(userInfoResponse);
        String email = jsonObject.get("email").getAsString();
        String name = jsonObject.get("name").getAsString();


        return getUserInfo(email,name);


    }

    public Result getEmailFromAuthCodeMicrosoft(String authorizationCode) {
        // Step 1: Exchange the authorization code for an access token
        RestTemplate restTemplate = new RestTemplate();
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
            String tokenData = dbUser.getId() + "-" + RoleEnum.USER.name();
            String token = TokenUtils.createAccessToken(tokenData);
            dbUser.setAccessToken(token);

            result = Result.success(dbUser);
        }

        return result;
    }
}