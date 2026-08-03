package com.coursistant.lms.shared.enums;

public enum LevelEnum {
    INSTRUCTOR("INSTRUCTOR"),
    STUDENT("STUDENT"),
    NOT_APPLICABLE("NOT_APPLICABLE");

    public final String level;

    LevelEnum(String level) {
        this.level = level;
    }
}
