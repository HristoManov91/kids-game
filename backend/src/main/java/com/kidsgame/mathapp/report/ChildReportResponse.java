package com.kidsgame.mathapp.report;

import com.kidsgame.mathapp.auth.UserResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ChildReportResponse(
        UserResponse child,
        LocalDate from,
        LocalDate to,
        int totalAttempts,
        int completedAttempts,
        int totalCorrect,
        int totalWrong,
        long totalDurationSeconds,
        BigDecimal averageGrade,
        int averageAccuracyPercent,
        List<ReportFocusArea> focusAreas,
        List<ReportAttemptRow> attempts
) {
}
