package com.coursistant.lms.service.system;

import cn.hutool.core.util.ObjectUtil;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.common.enums.RoleEnum;
import com.coursistant.lms.entity.Account;
import com.coursistant.lms.entity.DTO.LinkedInDTO;
import com.coursistant.lms.entity.LinkedInUserInfo;
import com.coursistant.lms.entity.User;
import com.coursistant.lms.exception.CustomException;
import com.coursistant.lms.mapper.user.UserMapper;
import com.coursistant.lms.service.user.UserService;
import com.coursistant.lms.utils.TokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

/**
 * 使用 LinkedIn 账号登录（Deprecated）
 */
@Service
public class LinkedInAuthService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Value("${spring.linkedin.client-id}")
    private String clientId;

    @Value("${spring.linkedin.client-secret}")
    private String clientSecret;

    @Value("${spring.linkedin.redirect-uri}")
    private String redirectUri;

    @Value("${spring.linkedin.token-url}")
    private String tokenUrl;

    @Value("${spring.linkedin.auth-url}")
    private String authUrl;

    @Value("${spring.linkedin.scope}")
    private String scope;


    private final RestTemplate restTemplate = new RestTemplate();


    /**
     * 生成授权链接，引导用户登录
     */
    public String returnUrl(){
        String authorizationUrl = authUrl + "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scope +
                "&state=" + UUID.randomUUID().toString();
        return authorizationUrl;
    }

    /**
     * 通过授权码获取 Access Token
     * @param authorizationCode LinkedIn 返回的授权码
     * @return access_token
     */
    public String getAccessToken(String authorizationCode) {
        // 1. 创建请求体，使用 LinkedMultiValueMap
        LinkedMultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", authorizationCode);
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);

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
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("获取 access_token 失败：" + response.getStatusCode());
        }
    }


    /**
     * 使用 access_token 获取 LinkedIn 用户信息
     */
    public LinkedInUserInfo getUserInfo(String accessToken) {
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


    /**
     * continueWithLinkedIn
     */
    public LinkedInDTO continueWithLinkedIn(String authorizationCode) {

        Account dbUser;
        LinkedInDTO dto=new LinkedInDTO();
        String linkedIn_token=getAccessToken(authorizationCode);
        LinkedInUserInfo userInfo=getUserInfo(linkedIn_token);
        dbUser = userMapper.selectByEmail(userInfo.getEmail());

        //if user not exist, register
        if (ObjectUtil.isNull(dbUser)) {
            //
            dto.setStatus("User Not Exist");
            dto.setUserInfo(userInfo);
        }
        //login
        else{
            String tokenData = dbUser.getId() + "-" + RoleEnum.USER.name();
            String token = TokenUtils.createAccessToken(tokenData);
            dbUser.setAccessToken(token);
            //
            dto.setStatus("Login success");
            dto.setAccount(dbUser);
        }


        return dto;

    }





}

