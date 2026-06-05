package com.kidsgame.mathapp.admin;

import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.issue.QuestionIssueReportResponse;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.suggestion.TaskSuggestionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminMonitoringResponse(
        Instant generatedAt,
        LocalDate fromDate,
        LocalDate toDate,
        Instant periodStart,
        Instant periodEnd,
        long totalUsers,
        long childUsers,
        long totalAttempts,
        long startedToday,
        long activeAttempts,
        long staleActiveAttempts,
        long completedAttempts,
        long completedToday,
        long activeUsersToday,
        BigDecimal averageGradeToday,
        long averageDurationTodaySeconds,
        long fastestDurationTodaySeconds,
        long slowestDurationTodaySeconds,
        BigDecimal allTimeAverageGrade,
        long allTimeAverageDurationSeconds,
        long openIssueReports,
        long openSuggestions,
        List<UserMonitorRow> users,
        List<AttemptMonitorRow> active,
        List<AttemptMonitorRow> recentCompleted,
        List<AttemptMonitorRow> todayCompleted,
        List<CategoryMonitorRow> categories,
        List<CategoryMonitorRow> allTimeCategories,
        List<ModeMonitorRow> todayModes,
        List<UsageBucketRow> todayUsage,
        List<QuestionIssueReportResponse> recentReports,
        List<TaskSuggestionResponse> recentSuggestions
) {
    public record UserMonitorRow(
            Long userId,
            String username,
            String displayName,
            long activeAttempts,
            long completedAttempts,
            long totalAttempts,
            BigDecimal averageGrade,
            Instant lastActivityAt
    ) {
    }

    public record AttemptMonitorRow(
            UUID attemptId,
            Long userId,
            String displayName,
            QuizCategory category,
            QuizMode mode,
            int difficulty,
            AttemptStatus status,
            boolean leaderboardEligible,
            Integer score,
            int totalQuestions,
            BigDecimal grade,
            Instant startedAt,
            Instant completedAt,
            long durationSeconds,
            int answeredCount
    ) {
    }

    public record CategoryMonitorRow(
            QuizCategory category,
            long completedAttempts,
            BigDecimal averageGrade,
            int averageScorePercent
    ) {
    }

    public record ModeMonitorRow(
            QuizCategory category,
            QuizMode mode,
            long completedAttempts,
            BigDecimal averageGrade,
            long averageDurationSeconds,
            long fastestDurationSeconds,
            long slowestDurationSeconds,
            int averageScorePercent,
            List<AttemptMonitorRow> attempts
    ) {
    }

    public record UsageBucketRow(
            String label,
            long started,
            long completed
    ) {
    }
}
