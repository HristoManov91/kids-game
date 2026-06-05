package com.kidsgame.mathapp.quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record QuestionResponse(
        int id,
        QuestionKind kind,
        String prompt,
        String image,
        String speechText,
        List<String> answerSlots,
        List<String> choices,
        QuizMode sourceMode
) {
    private static final Locale BG = Locale.forLanguageTag("bg-BG");

    public static QuestionResponse from(GeneratedQuestion question) {
        return new QuestionResponse(
                question.id(),
                question.kind(),
                question.prompt(),
                question.image(),
                question.speechText(),
                answerSlots(question),
                question.choices(),
                question.sourceMode()
        );
    }

    private static List<String> answerSlots(GeneratedQuestion question) {
        if (question.answerSlots() != null && !question.answerSlots().isEmpty()) {
            return question.answerSlots();
        }
        if (question.kind() == QuestionKind.LETTER_ORDER) {
            return letterSlots(question.answer());
        }
        if (question.kind() == QuestionKind.SYLLABLE_ORDER) {
            return syllableSlots(question.answer(), question.choices());
        }
        return List.of();
    }

    private static List<String> letterSlots(String answer) {
        List<String> slots = new ArrayList<>();
        answer.toUpperCase(BG).codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint) || codePoint == '-') {
                addGap(slots);
                return;
            }
            slots.add(new String(Character.toChars(codePoint)));
        });
        return slots;
    }

    private static List<String> syllableSlots(String answer, List<String> choices) {
        List<String> slots = new ArrayList<>();
        String[] parts = answer.toUpperCase(BG).split("[\\s-]+");
        for (int index = 0; index < parts.length; index++) {
            if (parts[index].isBlank()) {
                continue;
            }
            List<String> partSlots = segmentWithChoices(parts[index], choices);
            if (partSlots.isEmpty()) {
                return List.of();
            }
            slots.addAll(partSlots);
            if (index < parts.length - 1) {
                addGap(slots);
            }
        }
        return slots;
    }

    private static List<String> segmentWithChoices(String answerPart, List<String> choices) {
        List<String> candidates = choices.stream()
                .map(choice -> choice.toUpperCase(BG))
                .filter(choice -> !choice.isBlank())
                .distinct()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList();
        Map<String, Integer> counts = new HashMap<>();
        choices.stream()
                .map(choice -> choice.toUpperCase(BG))
                .forEach(choice -> counts.merge(choice, 1, Integer::sum));
        List<String> segmented = segment(answerPart.toUpperCase(BG), candidates, counts);
        return segmented == null ? List.of() : segmented;
    }

    private static List<String> segment(String remaining, List<String> candidates, Map<String, Integer> counts) {
        if (remaining.isEmpty()) {
            return new ArrayList<>();
        }
        for (String candidate : candidates) {
            int available = counts.getOrDefault(candidate, 0);
            if (available == 0 || !remaining.startsWith(candidate)) {
                continue;
            }
            counts.put(candidate, available - 1);
            List<String> tail = segment(remaining.substring(candidate.length()), candidates, counts);
            counts.put(candidate, available);
            if (tail != null) {
                List<String> result = new ArrayList<>();
                result.add(candidate);
                result.addAll(tail);
                return result;
            }
        }
        return null;
    }

    private static void addGap(List<String> slots) {
        if (!slots.isEmpty() && !" ".equals(slots.get(slots.size() - 1))) {
            slots.add(" ");
        }
    }
}
