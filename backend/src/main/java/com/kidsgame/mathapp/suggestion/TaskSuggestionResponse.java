package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.issue.IssueReportStatus;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;

import java.time.Instant;

public record TaskSuggestionResponse(
        Long id,
        Long userId,
        String displayName,
        QuizCategory category,
        QuizMode mode,
        Integer difficulty,
        String message,
        IssueReportStatus status,
        String adminNote,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
}
