package com.kidsgame.mathapp.issue;

import jakarta.validation.constraints.Size;

public record QuestionIssueReportRequest(
        @Size(max = 1000) String message
) {
}
