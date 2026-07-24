package com.coursistant.lms.shared.enums;

public enum LevelEnum {
    INSTRUCTOR("INSTRUCTOR"),
    TA("TA"),
    STUDENT("STUDENT"),
    SELF("SELF") // privacy: only the user can see their profile
    ;

    public String level;

    LevelEnum(String level) {
        this.level = level;
    }
}
