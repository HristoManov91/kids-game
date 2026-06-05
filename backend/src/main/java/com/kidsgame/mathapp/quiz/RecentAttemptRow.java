package com.kidsgame.mathapp.quiz;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecentAttemptRow(
        UUID attemptId,
        QuizCategory category,
        QuizMode mode,
        int difficulty,
        int score,
        int totalQuestions,
        BigDecimal grade,
        int crystals,
        long durationSeconds,
        Instant completedAt
) {
}
