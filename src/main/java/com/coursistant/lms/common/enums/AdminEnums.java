package com.coursistant.lms.common.enums;

public enum AdminEnums {
    APPROVED("Approved"),
    DENIED("Denied");

    public String decision;

    AdminEnums(String decision)
    {
        this.decision = decision;
    }

}
