package com.coursistant.lms.module.auth.oauth.dto;

import com.coursistant.lms.module.user.account.entity.Account;
import com.coursistant.lms.module.auth.oauth.entity.LinkedInUserInfo;

import java.io.Serializable;

/**
 * LinkedIn OAuth data transfer object
 */
public class LinkedInDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status;

    private LinkedInUserInfo userInfo;

    private Account account;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LinkedInUserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(LinkedInUserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
