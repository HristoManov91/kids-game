package com.kidsgame.mathapp.suggestion;

import com.kidsgame.mathapp.issue.IssueReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskSuggestionRequest(
        @NotNull IssueReportStatus status,
        @Size(max = 1000) String adminNote
) {
}
