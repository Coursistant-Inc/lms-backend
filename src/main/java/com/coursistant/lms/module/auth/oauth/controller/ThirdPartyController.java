package com.coursistant.lms.module.auth.oauth.controller;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.auth.oauth.service.OAuthService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.coursistant.lms.shared.web.Result;
import com.coursistant.lms.shared.idempotency.Idempotent;
import com.fasterxml.jackson.core.JsonProcessingException;


/**
 * Third-party OAuth frontend operation API
 **/
@RestController
@RequestMapping("/thirdParty")
public class ThirdPartyController {

    @Resource
    private OAuthService oAuthService;

    @Value("${server.port}")
    private String serverPort;
    @Value("${api.base.url}")
    private String url;


    private static final Logger logger = Logger.getLogger(ThirdPartyController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    private String getBaseUrl(){
        String baseUrl = url + ":";
        if (serverPort.charAt(0) == '1'){
            baseUrl = baseUrl + serverPort.substring(1);
        }else {
            baseUrl = baseUrl + serverPort;
        }
        return baseUrl;
    }

    /**
     *
     * continue
     */
    @Idempotent
    @PostMapping("/register")
    public Result registerWithThirdParty(@RequestBody Account account) {
        logRequest("registerWithThirdParty", null);
        oAuthService.register(account);
        logResponse("registerWithThirdParty", "Success");
        return Result.success();
    }

    /**
     * login url
     */
    @GetMapping("/linkedin")
    public RedirectView linkedInLogin() {
        logRequest("linkedIn", null);
        String url= oAuthService.returnLinkedInUrl();
        logResponse("linkedIn", "Success");
         return new RedirectView(url);
    }

    /**
     *
     * continue
     */
    @PostMapping("/linkedin/continue")
    public Result continueWithLinkedIn(@RequestParam String authorizationCode) {
        System.out.println("LinkedIn Login Request: " + authorizationCode);
        logRequest("continueWithLinkedIn", authorizationCode);
        Result userInfo = oAuthService.getEmailFromAuthCodeLinkedIn(authorizationCode);
        logResponse("continueWithLinkedIn", "Success");
        return userInfo;
    }

    @GetMapping("/google")
    public RedirectView googleLogin(HttpServletRequest request) {
        // Custom logic for initiating Google login, can be extended as needed
        String baseUrl = getBaseUrl();
        return new RedirectView(baseUrl + "/api/oauth2/authorization/google"); // This redirects to the default Google OAuth2 flow
    }

    @PostMapping("/google/continue")
    public Result continueWithGoogle(@RequestParam String authorizationCode) throws JsonProcessingException {
        System.out.println("Google Login Request: " + authorizationCode);
        logRequest("continueWithGoogle", authorizationCode);
        Result userInfo = oAuthService.getEmailFromAuthCodeGoogle(authorizationCode);
        logResponse("continueWithGoogle", "Success");
        return userInfo;
    }

    @GetMapping("/facebook")
    public void facebookLogin(HttpServletResponse response) throws IOException
    {
        String baseUrl = getBaseUrl();
        response.sendRedirect(baseUrl + "/api/oauth2/authorization/facebook");
    }

    @PostMapping("/facebook/continue")
    public Result facebookRedirect(@RequestParam String authorizationCode)
    {
        // logRequest("facebookRedirect",code);
        Result userInfo = oAuthService.getEmailFromAuthCodeFacebook(authorizationCode);
        return userInfo;  
    }

    @GetMapping("/microsoft")
    public RedirectView microsoftLogin() {
        // Custom logic for initiating Google login, can be extended as needed
        String baseUrl = getBaseUrl();
        return new RedirectView(baseUrl + "/api/oauth2/authorization/microsoft"); // This redirects to the default Google OAuth2 flow
    }

    @PostMapping("/microsoft/continue")
    public Result continueWithMicrosoft(@RequestParam String authorizationCode) throws JsonProcessingException {
        logRequest("continueWithMicrosoft", authorizationCode);
        Result userInfo = oAuthService.getEmailFromAuthCodeMicrosoft(authorizationCode);
        logResponse("continueWithMicrosoft", "Success");
        return userInfo;
    }



}
