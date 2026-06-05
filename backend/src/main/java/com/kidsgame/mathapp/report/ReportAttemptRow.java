package com.kidsgame.mathapp.report;

import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.Medal;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportAttemptRow(
        UUID attemptId,
        QuizCategory category,
        QuizMode mode,
        List<QuizMode> includedModes,
        int difficulty,
        AttemptStatus status,
        Integer score,
        int totalQuestions,
        int correct,
        int wrong,
        int unanswered,
        BigDecimal grade,
        Medal medal,
        Integer crystals,
        Instant startedAt,
        Instant completedAt,
        long durationSeconds
) {
}
