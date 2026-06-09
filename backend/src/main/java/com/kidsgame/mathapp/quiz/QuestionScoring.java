package com.kidsgame.mathapp.quiz;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class QuestionScoring {
    private QuestionScoring() {
    }

    public static int weight(GeneratedQuestion question) {
        return 1;
    }

    public static int score(GeneratedQuestion question, AnswerRecord answer) {
        if (answer == null) {
            return 0;
        }
        return answer.correct() ? 1 : 0;
    }

    public static String publicCorrectAnswer(GeneratedQuestion question) {
        if (question.kind() == QuestionKind.MEMORY_PAIRS) {
            int pairs = memoryPairsFromQuestion(question);
            return pairs > 0 ? "Всички " + pairs + " двойки" : "Всички двойки";
        }
        if (question.kind() == QuestionKind.PATTERN_SEQUENCE) {
            return question.answerSlots().stream()
                    .filter(slot -> slot != null && slot.startsWith("S|"))
                    .map(slot -> {
                        String[] parts = slot.split("\\|", -1);
                        return parts.length >= 5 ? parts[4] : "";
                    })
                    .filter(part -> !part.isBlank())
                    .collect(Collectors.joining(" → "));
        }
        if (question.kind() == QuestionKind.SUDOKU) {
            int size = sudokuSizeFromQuestion(question);
            String answer = question.answer() == null ? "" : question.answer();
            if (size <= 0 || answer.length() < size * size) {
                return answer;
            }
            return java.util.stream.IntStream.range(0, size)
                    .mapToObj(row -> answer.substring(row * size, row * size + size))
                    .collect(Collectors.joining(" / "));
        }
        return question.answer();
    }

    private static int sudokuSizeFromQuestion(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .filter(slot -> slot != null && slot.startsWith("G|"))
                .findFirst()
                .map(slot -> {
                    String[] parts = slot.split("\\|", -1);
                    return parts.length >= 2 ? parseInt(parts[1], 0) : 0;
                })
                .orElse(0);
    }

    private static Set<String> listAnswer(String answer) {
        return Arrays.stream((answer == null ? "" : answer).split(";"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private static int memoryPairsScore(GeneratedQuestion question, String answer) {
        String normalized = answer == null ? "" : answer.trim().toUpperCase();
        if (!normalized.startsWith("SOLVED")) {
            return 0;
        }
        int pairs = intField(answer, "pairs", 0);
        int attempts = intField(answer, "attempts", pairs);
        if (pairs <= 0 || attempts <= 0) {
            return 0;
        }
        int perfectMistakes = memoryPerfectMistakesFromQuestion(question);
        int extraAttempts = Math.max(0, attempts - pairs - perfectMistakes);
        double ratio = extraAttempts / (double) pairs;
        if (ratio == 0.0) {
            return 10;
        }
        if (ratio <= 0.25) {
            return 9;
        }
        if (ratio <= 0.5) {
            return 8;
        }
        if (ratio <= 1.0) {
            return 7;
        }
        if (ratio <= 1.5) {
            return 6;
        }
        if (ratio <= 2.0) {
            return 5;
        }
        if (ratio <= 3.0) {
            return 4;
        }
        return 3;
    }

    private static int memoryPairsFromQuestion(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .filter(slot -> slot != null && slot.startsWith("M|"))
                .findFirst()
                .map(slot -> {
                    String[] parts = slot.split("\\|", -1);
                    return parts.length >= 3 ? parseInt(parts[2], 0) : 0;
                })
                .orElse(0);
    }

    private static int memoryPerfectMistakesFromQuestion(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .filter(slot -> slot != null && slot.startsWith("M|"))
                .findFirst()
                .map(slot -> {
                    String[] parts = slot.split("\\|", -1);
                    return parts.length >= 4 ? parseInt(parts[3], 0) : 0;
                })
                .orElse(0);
    }

    private static int intField(String answer, String field, int fallback) {
        if (answer == null || answer.isBlank()) {
            return fallback;
        }
        String prefix = field + "=";
        return Arrays.stream(answer.split("\\|"))
                .map(String::trim)
                .filter(part -> part.startsWith(prefix))
                .findFirst()
                .map(part -> parseInt(part.substring(prefix.length()), fallback))
                .orElse(fallback);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
