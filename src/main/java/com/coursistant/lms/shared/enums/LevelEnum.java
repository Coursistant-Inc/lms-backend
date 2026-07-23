package com.coursistant.lms.shared.enums;

public enum LevelEnum {
    TEACHER("TEACHER"),
    STUDENT("STUDENT"),
    SELF("SELF") // Adding a new level here to configure privacy settings to "self" (no one else can see the user's profile)

    ;

    public String level;

    LevelEnum(String level) {
        this.level=level;
    }
}
