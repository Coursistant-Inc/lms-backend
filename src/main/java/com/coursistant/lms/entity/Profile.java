package com.coursistant.lms.entity;

import java.io.Serializable;

public class Profile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String preferredName;
    private String pronouns;
    private String email;
    private String introduction;
    private String avatar;
    private Integer id;
    private Integer userId;

    private String privacy;
    private String completedCourses;

    // Educational Background
    private String schoolName;
    private String graduationYear;
    private String major;
    private String interest;
    private String profile;



    @Override
    public String toString() {
        return "Profile{" +
                "preferredName='" + preferredName + '\'' +
                ", pronouns='" + pronouns + '\'' +
                ", email='" + email + '\'' +
                ", introduction='" + introduction + '\'' +
                ", avatar='" + avatar + '\'' +
                ", schoolName='" + schoolName + '\'' +
                ", graduationYear='" + graduationYear + '\'' +
                ", major='" + major + '\'' +
                ", interest='" + interest + '\'' +
                ", profile='" + profile + '\'' +
                '}';
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(String graduationYear) {
        this.graduationYear = graduationYear;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getCompletedCourses() {
        return completedCourses;
    }

    public void setCompletedCourses(String completedCourses) {
        this.completedCourses = completedCourses;
    }

    

    
}
