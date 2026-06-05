package com.kidsgame.mathapp.report;

import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.quiz.QuizCategory;

import java.math.BigDecimal;

public record ReportFocusArea(
        QuizCategory category,
        QuizMode mode,
        int correct,
        int total,
        int wrong,
        int accuracyPercent,
        BigDecimal averageGrade
) {
}
