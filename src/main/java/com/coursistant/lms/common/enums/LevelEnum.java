package com.coursistant.lms.common.enums;

public enum LevelEnum {
    TEACHER("TEACHER"),
    STUDENT("STUDENT"),
    SELF("SELF")

    ;

    public String level;

    LevelEnum(String level) {
        this.level=level;
    }
}
