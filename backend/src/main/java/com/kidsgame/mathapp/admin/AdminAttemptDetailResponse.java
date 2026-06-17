package com.kidsgame.mathapp.admin;

import com.kidsgame.mathapp.issue.QuestionIssueReportResponse;
import com.kidsgame.mathapp.quiz.QuestionKind;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.auth.UserResponse;

import java.time.Instant;
import java.util.List;

public record AdminAttemptDetailResponse(
        AdminMonitoringResponse.AttemptMonitorRow attempt,
        List<UserResponse> children,
        List<QuestionReviewRow> questions
) {
    public record QuestionReviewRow(
            int questionId,
            QuestionKind kind,
            QuizMode sourceMode,
            String prompt,
            String image,
            String speechText,
            List<String> answerSlots,
            List<String> choices,
            String expectedAnswer,
            String publicCorrectAnswer,
            String answer,
            boolean correct,
            Instant answeredAt,
            List<QuestionIssueReportResponse> reports
    ) {
    }
}
