package com.kidsgame.mathapp.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.issue.QuestionIssueReportResponse;
import com.kidsgame.mathapp.issue.QuestionIssueReportService;
import com.kidsgame.mathapp.quiz.AnswerRecord;
import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.GeneratedQuestion;
import com.kidsgame.mathapp.quiz.Medal;
import com.kidsgame.mathapp.quiz.QuestionKind;
import com.kidsgame.mathapp.quiz.QuestionScoring;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.quiz.QuizMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminAttemptService {
    private static final Locale BG = Locale.forLanguageTag("bg-BG");
    private static final TypeReference<List<GeneratedQuestion>> QUESTION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<AnswerRecord>> ANSWER_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<QuizMode>> MODE_LIST = new TypeReference<>() {
    };

    private final QuizAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;
    private final QuestionIssueReportService reportService;

    public AdminAttemptService(
            QuizAttemptRepository attemptRepository,
            ObjectMapper objectMapper,
            QuestionIssueReportService reportService
    ) {
        this.attemptRepository = attemptRepository;
        this.objectMapper = objectMapper;
        this.reportService = reportService;
    }

    @Transactional(readOnly = true)
    public AdminAttemptDetailResponse detail(UUID attemptId) {
        QuizAttempt attempt = loadAttempt(attemptId);
        return detail(attempt);
    }

    @Transactional
    public AdminAttemptDetailResponse updateAnswer(UUID attemptId, int questionId, AdminAnswerUpdateRequest request) {
        QuizAttempt attempt = loadAttempt(attemptId);
        List<GeneratedQuestion> questions = readQuestions(attempt);
        GeneratedQuestion question = questions.stream()
                .filter(candidate -> candidate.id() == questionId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Задачата не е намерена."));

        List<AnswerRecord> answers = new ArrayList<>(readAnswers(attempt));
        answers.removeIf(answer -> answer.questionId() == questionId);
        String answerText = request.answer() == null ? "" : request.answer().trim();
        answers.add(new AnswerRecord(
                questionId,
                answerText,
                normalize(answerText, question).equals(normalize(question.answer(), question)),
                QuestionScoring.publicCorrectAnswer(question),
                Instant.now()
        ));
        answers.sort(java.util.Comparator.comparingInt(AnswerRecord::questionId));

        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            replaceCompletedScore(attempt, questions, answers);
        } else {
            attempt.replaceAnswers(writeJson(answers));
        }

        return detail(attempt);
    }

    private AdminAttemptDetailResponse detail(QuizAttempt attempt) {
        List<GeneratedQuestion> questions = readQuestions(attempt);
        Map<Integer, AnswerRecord> answersByQuestion = readAnswers(attempt).stream()
                .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<Integer, List<QuestionIssueReportResponse>> reportsByQuestion = reportService.reportsForAttempt(attempt.getId())
                .stream()
                .collect(Collectors.groupingBy(QuestionIssueReportResponse::questionId));

        return new AdminAttemptDetailResponse(
                attemptRow(attempt, answersByQuestion.size()),
                questions.stream()
                        .map(question -> questionRow(question, answersByQuestion.get(question.id()), reportsByQuestion.getOrDefault(question.id(), List.of())))
                        .toList()
        );
    }

    private AdminAttemptDetailResponse.QuestionReviewRow questionRow(
            GeneratedQuestion question,
            AnswerRecord answer,
            List<QuestionIssueReportResponse> reports
    ) {
        return new AdminAttemptDetailResponse.QuestionReviewRow(
                question.id(),
                question.kind(),
                question.sourceMode(),
                question.prompt(),
                question.image(),
                question.speechText(),
                question.answerSlots(),
                question.choices(),
                question.answer(),
                QuestionScoring.publicCorrectAnswer(question),
                answer == null ? "" : answer.answer(),
                answer != null && answer.correct(),
                answer == null ? null : answer.answeredAt(),
                reports
        );
    }

    private AdminMonitoringResponse.AttemptMonitorRow attemptRow(QuizAttempt attempt, int answeredCount) {
        return new AdminMonitoringResponse.AttemptMonitorRow(
                attempt.getId(),
                attempt.getUser().getId(),
                attempt.getUser().getDisplayName(),
                attempt.getCategory(),
                attempt.getMode(),
                attempt.getDifficulty(),
                attempt.getStatus(),
                attempt.isLeaderboardEligible(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getGrade(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                Math.max(0, attempt.getActiveDurationSeconds()),
                answeredCount
        );
    }

    private void replaceCompletedScore(QuizAttempt attempt, List<GeneratedQuestion> questions, List<AnswerRecord> answers) {
        Map<Integer, GeneratedQuestion> questionsById = questions.stream()
                .collect(Collectors.toMap(GeneratedQuestion::id, Function.identity(), (first, ignored) -> first));
        Map<Integer, AnswerRecord> answersByQuestion = answers.stream()
                .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Instant now = Instant.now();
        List<AnswerRecord> finalAnswers = questions.stream()
                .map(question -> answersByQuestion.getOrDefault(
                        question.id(),
                        new AnswerRecord(question.id(), "", false, QuestionScoring.publicCorrectAnswer(question), now)
                ))
                .toList();
        int total = questions.stream().mapToInt(QuestionScoring::weight).sum();
        int score = finalAnswers.stream()
                .mapToInt(answer -> QuestionScoring.score(questionsById.get(answer.questionId()), answer))
                .sum();
        attempt.replaceGradedAnswers(score, calculateGrade(score, total), medalFor(score, total), writeJson(finalAnswers));
    }

    private QuizAttempt loadAttempt(UUID attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тестът не е намерен."));
    }

    private String normalize(String answer, GeneratedQuestion question) {
        String normalized = answer == null ? "" : answer.trim();
        if (question.sourceMode() == QuizMode.WORD_WRONG_LETTER) {
            return normalizeWrongLetterAnswer(normalized, question);
        }
        if (question.kind() == QuestionKind.GROUPING) {
            return normalizeGroupingAnswer(normalized);
        }
        if (question.kind() == QuestionKind.SPOT_DIFFERENCES) {
            return normalizeListAnswer(normalized);
        }
        if (question.kind() == QuestionKind.MEMORY_PAIRS) {
            return normalized.toUpperCase(BG).startsWith("SOLVED") ? "SOLVED" : normalized.toUpperCase(BG);
        }
        if (question.kind() == QuestionKind.CHOICE) {
            return normalized;
        }
        return normalized.toUpperCase(BG).replaceAll("\\s+", "");
    }

    private String normalizeWrongLetterAnswer(String answer, GeneratedQuestion question) {
        String normalized = answer.toUpperCase(BG).replaceAll("\\s+", "");
        if (normalized.contains("=")) {
            String[] parts = normalized.split("=", 2);
            return parts[0].trim() + "=" + parts[1].trim();
        }
        String storedAnswer = question.answer() == null ? "" : question.answer().trim();
        if (storedAnswer.contains("=")) {
            return normalized;
        }
        String expectedCorrection = expectedWrongLetterCorrection(question);
        String[] expectedParts = expectedCorrection.split("=", 2);
        if (expectedParts.length == 2 && normalized.equals(expectedParts[0])) {
            return expectedCorrection;
        }
        return normalized;
    }

    private String expectedWrongLetterCorrection(GeneratedQuestion question) {
        String storedAnswer = question.answer() == null
                ? ""
                : question.answer().trim().toUpperCase(BG).replaceAll("\\s+", "");
        if (storedAnswer.contains("=")) {
            String[] parts = storedAnswer.split("=", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                return parts[0].trim() + "=" + parts[1].trim();
            }
        }

        List<String> promptLetters = wordLetters(question.prompt());
        List<String> answerLetters = wordLetters(question.speechText());
        int size = Math.min(promptLetters.size(), answerLetters.size());
        for (int index = 0; index < size; index++) {
            if (!promptLetters.get(index).equals(answerLetters.get(index))) {
                return promptLetters.get(index) + "=" + answerLetters.get(index);
            }
        }
        return storedAnswer;
    }

    private List<String> wordLetters(String text) {
        List<String> letters = new ArrayList<>();
        if (text == null) {
            return letters;
        }
        text.toUpperCase(BG).codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint) || codePoint == '-') {
                return;
            }
            letters.add(new String(Character.toChars(codePoint)));
        });
        return letters;
    }

    private String normalizeGroupingAnswer(String answer) {
        return java.util.Arrays.stream(answer.split(";"))
                .map(String::trim)
                .filter(part -> !part.isBlank() && part.contains("="))
                .map(part -> {
                    String[] pieces = part.split("=", 2);
                    return pieces[0].trim().toUpperCase(BG).replaceAll("\\s+", "")
                            + "="
                            + pieces[1].trim().toUpperCase(BG).replaceAll("\\s+", "");
                })
                .sorted()
                .collect(Collectors.joining(";"));
    }

    private String normalizeListAnswer(String answer) {
        return java.util.Arrays.stream(answer.split(";"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .map(part -> part.toUpperCase(BG).replaceAll("\\s+", ""))
                .sorted()
                .collect(Collectors.joining(";"));
    }

    private BigDecimal calculateGrade(int score, int total) {
        double half = total / 2.0;
        if (score < half) {
            return BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP);
        }
        double grade = 3.0 + ((score - half) / half) * 3.0;
        return BigDecimal.valueOf(Math.min(6.0, grade)).setScale(2, RoundingMode.HALF_UP);
    }

    private Medal medalFor(int score, int total) {
        double ratio = score / (double) total;
        if (ratio == 1.0) {
            return Medal.DIAMOND_CUP;
        }
        if (ratio >= 0.9) {
            return Medal.GOLD_CUP;
        }
        if (ratio >= 0.75) {
            return Medal.SILVER_MEDAL;
        }
        if (ratio >= 0.5) {
            return Medal.BRONZE_STAR;
        }
        return Medal.KEEP_GOING;
    }

    private List<GeneratedQuestion> readQuestions(QuizAttempt attempt) {
        return readJson(attempt.getQuestionsJson(), QUESTION_LIST);
    }

    private List<AnswerRecord> readAnswers(QuizAttempt attempt) {
        return readJson(attempt.getAnswersJson(), ANSWER_LIST);
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read stored quiz data", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write quiz data", ex);
        }
    }
}
