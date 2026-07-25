package com.coursistant.lms.module.course.enrollment.dto;

import java.util.ArrayList;
import java.util.List;

public class PromoteTaResponse {

    private MemberResponse member;
    private List<String> warnings = new ArrayList<>();

    public MemberResponse getMember() {
        return member;
    }

    public void setMember(MemberResponse member) {
        this.member = member;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
