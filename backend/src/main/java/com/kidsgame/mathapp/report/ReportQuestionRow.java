package com.kidsgame.mathapp.report;

import com.kidsgame.mathapp.quiz.QuestionKind;
import com.kidsgame.mathapp.quiz.QuizMode;

public record ReportQuestionRow(
        int questionId,
        QuestionKind kind,
        QuizMode sourceMode,
        String prompt,
        String answer,
        String correctAnswer,
        boolean correct
) {
}
