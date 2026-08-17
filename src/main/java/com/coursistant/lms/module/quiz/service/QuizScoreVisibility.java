package com.coursistant.lms.module.quiz.service;

/**
 * Student-facing score visibility. Same rules as the result API; not a Spring bean.
 */
final class QuizScoreVisibility {

    private QuizScoreVisibility() {
    }

    static boolean showAutoScore(boolean instant, boolean released) {
        return instant || released;
    }

    static boolean showManualAndTotal(boolean released, boolean manualPending) {
        return released && !manualPending;
    }

    static boolean showManualGradingStatus(boolean instant, boolean released) {
        return instant || released;
    }

    static boolean showQuestionScore(boolean instant, boolean released) {
        return instant || released;
    }
}
