package com.coursistant.lms.controller.system;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import com.coursistant.lms.entity.Account;
import com.coursistant.lms.service.system.LinkedInAuthService;
import com.coursistant.lms.service.system.OAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.coursistant.lms.common.Result;
import com.coursistant.lms.entity.DTO.LinkedInDTO;
import com.fasterxml.jackson.core.JsonProcessingException;


/**
 * 部门信息表前端操作接口
 * Teach frontend operation API
 **/
@RestController
@RequestMapping("/thirdParty")
public class ThirdPartyController {

    @Resource
    private LinkedInAuthService linkedInAuthService;

    @Resource
    private OAuthService oAuthService;

    private static final Logger logger = Logger.getLogger(ThirdPartyController.class.getName());

    private void logRequest(String methodName, String requestBody) {
        logger.info(() -> String.format("Start %s: %s", methodName, requestBody));
    }

    private void logResponse(String methodName, String response) {
        logger.info(() -> String.format("End %s: %s", methodName, response));
    }

    /**
     * login url
     */
    @PostMapping("/linkedIn/loginUrl")
    public Result linkedInLoginUrl() {
        logRequest("linkedIn/loginUrl", null);
        String url= linkedInAuthService.returnUrl();
        logResponse("linkedIn/loginUrl", "Success");
        return Result.success(url);
    }

    /**
     *
     * continue
     */
    @PostMapping("/linkedIn/continue/{authorizationCode}")
    public Result continueWithLinkedIn(@PathVariable String authorizationCode) {
        logRequest("continueWithLinkedIn", authorizationCode);
        LinkedInDTO dto= linkedInAuthService.continueWithLinkedIn(authorizationCode);
        logResponse("continueWithLinkedIn", "Success");
        return Result.success(dto);
    }

    /**
     *
     * continue
     */
    @PostMapping("/linkedIn/register")
    public Result registerWithLinkedIn(@RequestBody Account account) {
        logRequest("registerWithLinkedIn", null);
        linkedInAuthService.register(account);
        logResponse("registerWithLinkedIn", "Success");
        return Result.success();
    }

    @GetMapping("/google")
    public RedirectView googleLogin() {
        // Custom logic for initiating Google login, can be extended as needed
        return new RedirectView("/api/oauth2/authorization/google"); // This redirects to the default Google OAuth2 flow
    }

    @PostMapping("/google/continue")
    public Result continueWithGoogle(@RequestParam String authorizationCode) throws JsonProcessingException {
        logRequest("continueWithGoogle", authorizationCode);
        Result userInfo = oAuthService.getEmailFromAuthCodeGoogle(authorizationCode);
        logResponse("continueWithGoogle", "Success");
        return userInfo;
    }

    @GetMapping("/facebook")
    public void facebookLogin(HttpServletResponse response) throws IOException
    {
        response.sendRedirect("/api/oauth2/authorization/facebook");
    }

    @PostMapping("/facebook/continue")
    public Result facebookRedirect(@RequestParam String authorizationCode)
    {
        // logRequest("facebookRedirect",code);
        Result userInfo = oAuthService.getFacebookUserAccessToken(authorizationCode);
        return userInfo;  
    }

    @GetMapping("/microsoft")
    public RedirectView microsoftLogin() {
        // Custom logic for initiating Google login, can be extended as needed
        return new RedirectView("/api/oauth2/authorization/microsoft"); // This redirects to the default Google OAuth2 flow
    }

    @PostMapping("/microsoft/continue")
    public Result continueWithMicrosoft(@RequestParam String authorizationCode) throws JsonProcessingException {
        logRequest("continueWithMicrosoft", authorizationCode);
        Result userInfo = oAuthService.getEmailFromAuthCodeMicrosoft(authorizationCode);
        logResponse("continueWithMicrosoft", "Success");
        return userInfo;
    }



}
