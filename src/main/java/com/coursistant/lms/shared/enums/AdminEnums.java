package com.coursistant.lms.shared.enums;

public enum AdminEnums {
    APPROVED("Approved"),
    DENIED("Denied");

    public String decision;

    AdminEnums(String decision)
    {
        this.decision = decision;
    }

}
