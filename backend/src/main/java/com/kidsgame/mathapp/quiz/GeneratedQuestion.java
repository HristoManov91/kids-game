package com.kidsgame.mathapp.quiz;

import java.util.List;

public record GeneratedQuestion(
        int id,
        QuestionKind kind,
        String prompt,
        String image,
        String speechText,
        List<String> answerSlots,
        List<String> choices,
        String answer,
        QuizMode sourceMode
) {
    public GeneratedQuestion {
        answerSlots = answerSlots == null ? List.of() : List.copyOf(answerSlots);
        choices = choices == null ? List.of() : List.copyOf(choices);
    }
}
