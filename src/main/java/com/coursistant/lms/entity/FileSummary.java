package com.coursistant.lms.entity;

import java.io.Serializable;

/**
 * teach
*/
public class FileSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    private String summary;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}