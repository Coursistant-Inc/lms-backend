package com.coursistant.individual.common.enums;

public enum LevelEnum {
    TEACHER("TEACHER"),
    STUDENT("STUDENT"),

    ;

    public String level;

    LevelEnum(String level) {
        this.level=level;
    }
}
