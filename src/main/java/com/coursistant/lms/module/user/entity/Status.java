package com.coursistant.lms.module.user.entity;

import java.io.Serializable;

/**
 * teach
*/
public class Status implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;


    private String type;

    private String due;




    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDue() {
        return due;
    }

    public void setDue(String due) {
        this.due = due;
    }
}