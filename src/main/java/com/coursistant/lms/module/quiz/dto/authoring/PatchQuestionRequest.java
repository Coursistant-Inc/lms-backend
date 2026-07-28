package com.coursistant.lms.module.quiz.dto.authoring;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PatchQuestionRequest {
    private String stem;
    private BigDecimal points;
    private List<OptionInput> options;
    private Integer expectedVersion;
}
