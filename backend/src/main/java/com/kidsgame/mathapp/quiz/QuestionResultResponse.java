package com.kidsgame.mathapp.quiz;

import java.time.Instant;

public record QuestionResultResponse(
        int questionId,
        String answer,
        boolean correct,
        String correctAnswer,
        Instant answeredAt
) {
    public static QuestionResultResponse from(AnswerRecord answer) {
        return new QuestionResultResponse(
                answer.questionId(),
                answer.answer(),
                answer.correct(),
                answer.correctAnswer(),
                answer.answeredAt()
        );
    }
}

