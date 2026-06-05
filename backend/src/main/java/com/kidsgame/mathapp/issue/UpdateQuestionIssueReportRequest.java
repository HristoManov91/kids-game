package com.kidsgame.mathapp.issue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateQuestionIssueReportRequest(
        @NotNull IssueReportStatus status,
        @Size(max = 1000) String adminNote
) {
}
