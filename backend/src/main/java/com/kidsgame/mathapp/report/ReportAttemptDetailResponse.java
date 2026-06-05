package com.kidsgame.mathapp.report;

import java.util.List;

public record ReportAttemptDetailResponse(
        ReportAttemptRow attempt,
        List<ReportQuestionRow> questions
) {
}
