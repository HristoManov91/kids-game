package com.kidsgame.mathapp.issue;

import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;

import java.time.Instant;
import java.util.UUID;

public record QuestionIssueReportResponse(
        Long id,
        UUID attemptId,
        Long userId,
        String displayName,
        QuizCategory category,
        QuizMode mode,
        int difficulty,
        int questionId,
        String questionPrompt,
        String message,
        IssueReportStatus status,
        String adminNote,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
}
