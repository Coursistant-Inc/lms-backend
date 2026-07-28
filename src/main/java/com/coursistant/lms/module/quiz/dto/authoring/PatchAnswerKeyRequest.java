package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.util.List;

@Data
public class PatchAnswerKeyRequest {
    private List<OptionKeyInput> options;
    private String reason;
    private Integer expectedVersion;
}
