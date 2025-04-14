package com.coursistant.individual.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 表示 LinkedIn 用户信息的实体类
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkedInUserInfo {

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    @JsonProperty("name")
    private String name;

    @JsonProperty("locale")
    private LocaleInfo locale;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("picture")
    private String picture;

    // Getter 和 Setter 方法

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocaleInfo getLocale() {
        return locale;
    }

    public void setLocale(LocaleInfo locale) {
        this.locale = locale;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    @Override
    public String toString() {
        return "LinkedInUser{" +
                "sub='" + sub + '\'' +
                ", emailVerified=" + emailVerified +
                ", name='" + name + '\'' +
                ", locale=" + locale +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", email='" + email + '\'' +
                ", picture='" + picture + '\'' +
                '}';
    }

    /**
     * 内部类，用于封装 locale 信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocaleInfo {
        @JsonProperty("country")
        private String country;

        @JsonProperty("language")
        private String language;

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @Override
        public String toString() {
            return "LocaleInfo{" +
                    "country='" + country + '\'' +
                    ", language='" + language + '\'' +
                    '}';
        }
    }
}
