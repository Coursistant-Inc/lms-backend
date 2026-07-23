package com.coursistant.lms.module.course.dto;

public class CourseDetailsDTO {

    private String name;
    private String avatar;
    private String title;
    private Integer units;
    private Float progress;


    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void setUnits(Integer units)
    {
        this.units = units;
    }

    public Integer getUnits()
    {
        return units;
    }

    public void setProgress(Float progress)
    {
        this.progress = progress;
    }

    public Float getProgress()
    {
        return progress;
    }


}
