package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import com.coursistant.lms.module.quiz.entity.QuizQuestionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizScoringServiceTest {

    private QuizScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new QuizScoringService();
    }

    @Test
    void singleChoice_correctFullPoints() {
        QuizQuestion q = question(QuizConstants.TYPE_SINGLE_CHOICE, "10");
        List<QuizQuestionOption> options = List.of(option(1, true), option(2, false));
        assertEquals(new BigDecimal("10"), scoringService.scoreObjective(q, List.of(1), options));
    }

    @Test
    void singleChoice_wrongZero() {
        QuizQuestion q = question(QuizConstants.TYPE_SINGLE_CHOICE, "10");
        List<QuizQuestionOption> options = List.of(option(1, true), option(2, false));
        assertEquals(BigDecimal.ZERO, scoringService.scoreObjective(q, List.of(2), options));
    }

    @Test
    void trueFalse_correct() {
        QuizQuestion q = question(QuizConstants.TYPE_TRUE_FALSE, "5");
        List<QuizQuestionOption> options = List.of(option(10, true), option(11, false));
        assertEquals(new BigDecimal("5"), scoringService.scoreObjective(q, List.of(10), options));
    }

    @Test
    void multipleSelect_allOrNothing() {
        QuizQuestion q = question(QuizConstants.TYPE_MULTIPLE_SELECT, "8");
        List<QuizQuestionOption> options = List.of(option(1, true), option(2, true), option(3, false));
        assertEquals(new BigDecimal("8"), scoringService.scoreObjective(q, List.of(1, 2), options));
        assertEquals(BigDecimal.ZERO, scoringService.scoreObjective(q, List.of(1), options));
        assertEquals(BigDecimal.ZERO, scoringService.scoreObjective(q, List.of(1, 2, 3), options));
    }

    private static QuizQuestion question(String type, String points) {
        QuizQuestion q = new QuizQuestion();
        q.setType(type);
        q.setPoints(new BigDecimal(points));
        return q;
    }

    private static QuizQuestionOption option(int id, boolean correct) {
        QuizQuestionOption o = new QuizQuestionOption();
        o.setId(id);
        o.setIsCorrect(correct);
        return o;
    }
}
