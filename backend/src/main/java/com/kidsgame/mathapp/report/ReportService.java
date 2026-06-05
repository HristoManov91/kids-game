package com.kidsgame.mathapp.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.auth.UserResponse;
import com.kidsgame.mathapp.quiz.AnswerRecord;
import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.CrystalReward;
import com.kidsgame.mathapp.quiz.GeneratedQuestion;
import com.kidsgame.mathapp.quiz.QuestionScoring;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizGenerator;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private static final ZoneId REPORT_ZONE = ZoneId.of("Europe/Sofia");
    private static final TypeReference<List<GeneratedQuestion>> QUESTION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<AnswerRecord>> ANSWER_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<QuizMode>> MODE_LIST = new TypeReference<>() {
    };

    private final UserRepository userRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;

    public ReportService(UserRepository userRepository, QuizAttemptRepository attemptRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> children() {
        return userRepository.findByRoleOrderByDisplayNameAsc(Role.CHILD)
                .stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChildReportResponse childReport(
            Long childId,
            LocalDate from,
            LocalDate to,
            QuizCategory category,
            QuizMode mode,
            List<QuizMode> modes,
            Integer difficulty
    ) {
        UserEntity child = loadChild(childId);
        LocalDate startDate = from == null ? LocalDate.now(REPORT_ZONE) : from;
        LocalDate endDate = to == null ? startDate : to;
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Крайната дата трябва да е след началната.");
        }

        Instant start = startDate.atStartOfDay(REPORT_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
        List<QuizMode> selectedModes = selectedModes(mode, modes);
        List<QuizAttempt> attempts = attemptRepository.findByUser_IdAndStartedAtBetweenOrderByStartedAtDesc(childId, start, end)
                .stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .filter(attempt -> category == null || attempt.getCategory() == category)
                .filter(attempt -> matchesModes(attempt, selectedModes))
                .filter(attempt -> difficulty == null || attempt.getDifficulty() == difficulty)
                .toList();
        List<ReportAttemptRow> rows = attempts.stream().map(this::toRow).toList();

        int completedAttempts = attempts.size();
        int totalCorrect = rows.stream().mapToInt(ReportAttemptRow::correct).sum();
        int totalWrong = rows.stream().mapToInt(ReportAttemptRow::wrong).sum();
        int answeredTotal = totalCorrect + totalWrong;
        int accuracy = answeredTotal == 0 ? 0 : (int) Math.round((totalCorrect * 100.0) / answeredTotal);
        BigDecimal averageGrade = averageGrade(attempts);
        long totalDuration = attempts.stream()
                .mapToLong(this::durationSeconds)
                .sum();

        return new ChildReportResponse(
                new UserResponse(child.getId(), child.getUsername(), child.getDisplayName(), child.getRole()),
                startDate,
                endDate,
                attempts.size(),
                completedAttempts,
                totalCorrect,
                totalWrong,
                totalDuration,
                averageGrade,
                accuracy,
                focusAreas(attempts, category, focusModes(selectedModes)),
                rows
        );
    }

    private List<QuizMode> selectedModes(QuizMode mode, List<QuizMode> modes) {
        List<QuizMode> selectedModes = new ArrayList<>();
        if (mode != null) {
            selectedModes.add(mode);
        }
        if (modes != null) {
            selectedModes.addAll(modes);
        }
        return selectedModes;
    }

    private boolean matchesModes(QuizAttempt attempt, List<QuizMode> selectedModes) {
        if (selectedModes.isEmpty()) {
            return true;
        }

        List<QuizMode> includedModes = readIncludedModes(attempt);
        return selectedModes.stream()
                .anyMatch(selectedMode -> attempt.getMode() == selectedMode || includedModes.contains(selectedMode));
    }

    private List<QuizMode> focusModes(List<QuizMode> selectedModes) {
        if (selectedModes.isEmpty()) {
            return QuizGenerator.PRIMITIVE_MODES;
        }

        List<QuizMode> result = new ArrayList<>();
        for (QuizMode selectedMode : selectedModes) {
            for (QuizMode primitiveMode : primitiveModesFor(selectedMode)) {
                if (!result.contains(primitiveMode)) {
                    result.add(primitiveMode);
                }
            }
        }
        return result.isEmpty() ? QuizGenerator.PRIMITIVE_MODES : result;
    }

    private List<QuizMode> primitiveModesFor(QuizMode mode) {
        return switch (mode) {
            case ADDITION -> List.of(QuizMode.ADDITION);
            case SUBTRACTION -> List.of(QuizMode.SUBTRACTION);
            case MIXED -> List.of(QuizMode.ADDITION, QuizMode.SUBTRACTION);
            case UNKNOWN_ADDITION -> List.of(QuizMode.UNKNOWN_ADDITION);
            case UNKNOWN_SUBTRACTION -> List.of(QuizMode.UNKNOWN_SUBTRACTION);
            case UNKNOWN_MIXED -> List.of(QuizMode.UNKNOWN_ADDITION, QuizMode.UNKNOWN_SUBTRACTION);
            case MULTIPLICATION -> List.of(QuizMode.MULTIPLICATION);
            case DIVISION -> List.of(QuizMode.DIVISION);
            case MULTIPLICATION_DIVISION -> List.of(QuizMode.MULTIPLICATION, QuizMode.DIVISION);
            case COMPARE -> List.of(QuizMode.COMPARE);
            case WORD_LETTERS -> List.of(QuizMode.WORD_LETTERS);
            case WORD_SYLLABLES -> List.of(QuizMode.WORD_SYLLABLES);
            case WORD_TYPING -> List.of(QuizMode.WORD_TYPING);
            case WORD_PICTURE -> List.of(QuizMode.WORD_PICTURE);
            case WORD_MISSING_LETTER -> List.of(QuizMode.WORD_MISSING_LETTER);
            case WORD_FIRST_LETTER_GROUP -> List.of(QuizMode.WORD_FIRST_LETTER_GROUP);
            case WORD_WRONG_LETTER -> List.of(QuizMode.WORD_WRONG_LETTER);
            case FIND_OBJECT -> List.of(QuizMode.FIND_OBJECT);
            case SPOT_DIFFERENCES -> List.of(QuizMode.SPOT_DIFFERENCES);
            case MEMORY_PAIRS -> List.of(QuizMode.MEMORY_PAIRS);
            case PATTERN_SEQUENCE -> List.of(QuizMode.PATTERN_SEQUENCE);
            case ALL_GROUP, CUSTOM_GROUP -> QuizGenerator.PRIMITIVE_MODES;
        };
    }

    @Transactional(readOnly = true)
    public ReportAttemptDetailResponse attemptDetail(Long childId, UUID attemptId) {
        loadChild(childId);
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тестът не е намерен."));
        if (!attempt.getUser().getId().equals(childId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Този тест е на друг профил.");
        }
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Тестът още не е завършен и не участва в справките.");
        }

        Map<Integer, AnswerRecord> answersByQuestion = readAnswers(attempt).stream()
                .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first));
        List<ReportQuestionRow> questions = readQuestions(attempt).stream()
                .map(question -> {
                    AnswerRecord answer = answersByQuestion.get(question.id());
                    return new ReportQuestionRow(
                            question.id(),
                            question.kind(),
                            question.sourceMode(),
                            question.prompt(),
                            answer == null ? "" : answer.answer(),
                            answer == null ? question.answer() : answer.correctAnswer(),
                            answer != null && answer.correct()
                    );
                })
                .toList();

        return new ReportAttemptDetailResponse(toRow(attempt), questions);
    }

    private UserEntity loadChild(Long childId) {
        UserEntity child = userRepository.findById(childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Детският профил не е намерен."));
        if (child.getRole() != Role.CHILD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Изберете детски профил.");
        }
        return child;
    }

    private ReportAttemptRow toRow(QuizAttempt attempt) {
        List<AnswerRecord> answers = readAnswers(attempt);
        int correct;
        int wrong;
        int unanswered;
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            correct = attempt.getScore() == null ? 0 : attempt.getScore();
            wrong = Math.max(0, attempt.getTotalQuestions() - correct);
            unanswered = (int) answers.stream().filter(answer -> answer.answer().isBlank()).count();
        } else {
            correct = (int) answers.stream().filter(AnswerRecord::correct).count();
            wrong = (int) answers.stream().filter(answer -> !answer.correct() && !answer.answer().isBlank()).count();
            unanswered = Math.max(0, attempt.getTotalQuestions() - answers.size());
        }

        return new ReportAttemptRow(
                attempt.getId(),
                attempt.getCategory(),
                attempt.getMode(),
                readIncludedModes(attempt),
                attempt.getDifficulty(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                correct,
                wrong,
                unanswered,
                attempt.getGrade(),
                attempt.getMedal(),
                CrystalReward.calculate(attempt.getDifficulty(), attempt.getScore(), attempt.getTotalQuestions()),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                durationSeconds(attempt)
        );
    }

    private BigDecimal averageGrade(List<QuizAttempt> attempts) {
        List<BigDecimal> grades = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .map(QuizAttempt::getGrade)
                .filter(grade -> grade != null)
                .toList();
        if (grades.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = grades.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP);
    }

    private List<ReportFocusArea> focusAreas(List<QuizAttempt> attempts, QuizCategory selectedCategory, List<QuizMode> focusModes) {
        List<QuizCategory> categories = selectedCategory == null
                ? attempts.stream()
                        .map(QuizAttempt::getCategory)
                        .distinct()
                        .sorted(Comparator.comparing(Enum::ordinal))
                        .toList()
                : List.of(selectedCategory);
        Map<FocusKey, int[]> totals = new LinkedHashMap<>();
        for (QuizCategory category : categories) {
            for (QuizMode mode : focusModes) {
                if (primitiveModesForCategory(category).contains(mode)) {
                    totals.putIfAbsent(new FocusKey(category, mode), new int[]{0, 0});
                }
            }
        }
        if (totals.isEmpty()) {
            for (QuizAttempt attempt : attempts) {
                for (QuizMode mode : primitiveModesForCategory(attempt.getCategory())) {
                    totals.putIfAbsent(new FocusKey(attempt.getCategory(), mode), new int[]{0, 0});
                }
            }
        }

        for (QuizAttempt attempt : attempts) {
            if (attempt.getStatus() != AttemptStatus.COMPLETED) {
                continue;
            }
            Map<Integer, AnswerRecord> answersByQuestion = readAnswers(attempt).stream()
                    .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first));
            for (GeneratedQuestion question : readQuestions(attempt)) {
                FocusKey key = new FocusKey(attempt.getCategory(), question.sourceMode());
                if (question.sourceMode() == null || !totals.containsKey(key)) {
                    continue;
                }
                int[] count = totals.get(key);
                count[1] += QuestionScoring.weight(question);
                AnswerRecord answer = answersByQuestion.get(question.id());
                if (answer != null) {
                    count[0] += QuestionScoring.score(question, answer);
                }
            }
        }

        List<ReportFocusArea> areas = new ArrayList<>();
        for (Map.Entry<FocusKey, int[]> entry : totals.entrySet()) {
            int correct = entry.getValue()[0];
            int total = entry.getValue()[1];
            if (total == 0) {
                continue;
            }
            int wrong = total - correct;
            int accuracy = (int) Math.round((correct * 100.0) / total);
            areas.add(new ReportFocusArea(entry.getKey().category(), entry.getKey().mode(), correct, total, wrong, accuracy, gradeForScore(correct, total)));
        }
        areas.sort(Comparator
                .comparing(ReportFocusArea::category)
                .thenComparing(ReportFocusArea::averageGrade)
                .thenComparing(ReportFocusArea::wrong, Comparator.reverseOrder())
                .thenComparing(area -> area.mode().name()));
        return areas;
    }

    private List<QuizMode> primitiveModesForCategory(QuizCategory category) {
        return switch (category) {
            case MATH -> QuizGenerator.MATH_PRIMITIVE_MODES;
            case BULGARIAN -> QuizGenerator.BULGARIAN_PRIMITIVE_MODES;
            case LOGIC -> QuizGenerator.LOGIC_PRIMITIVE_MODES;
        };
    }

    private BigDecimal gradeForScore(int score, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double half = total / 2.0;
        if (score < half) {
            return BigDecimal.valueOf(2).setScale(2, RoundingMode.HALF_UP);
        }
        double grade = 3.0 + ((score - half) / half) * 3.0;
        return BigDecimal.valueOf(Math.min(6.0, grade)).setScale(2, RoundingMode.HALF_UP);
    }

    private long durationSeconds(QuizAttempt attempt) {
        return Math.max(0, attempt.getActiveDurationSeconds());
    }

    private List<GeneratedQuestion> readQuestions(QuizAttempt attempt) {
        return readJson(attempt.getQuestionsJson(), QUESTION_LIST);
    }

    private List<AnswerRecord> readAnswers(QuizAttempt attempt) {
        return readJson(attempt.getAnswersJson(), ANSWER_LIST);
    }

    private List<QuizMode> readIncludedModes(QuizAttempt attempt) {
        List<QuizMode> modes = readJson(attempt.getIncludedModesJson(), MODE_LIST);
        if (!modes.isEmpty()) {
            return modes;
        }
        return switch (attempt.getMode()) {
            case ADDITION, SUBTRACTION, UNKNOWN_ADDITION, UNKNOWN_SUBTRACTION, MULTIPLICATION, DIVISION, COMPARE -> List.of(attempt.getMode());
            case MIXED -> List.of(QuizMode.ADDITION, QuizMode.SUBTRACTION);
            case UNKNOWN_MIXED -> List.of(QuizMode.UNKNOWN_ADDITION, QuizMode.UNKNOWN_SUBTRACTION);
            case MULTIPLICATION_DIVISION -> List.of(QuizMode.MULTIPLICATION, QuizMode.DIVISION);
            case WORD_LETTERS, WORD_SYLLABLES, WORD_TYPING, WORD_PICTURE, WORD_MISSING_LETTER, WORD_FIRST_LETTER_GROUP, WORD_WRONG_LETTER -> List.of(attempt.getMode());
            case FIND_OBJECT, SPOT_DIFFERENCES, MEMORY_PAIRS, PATTERN_SEQUENCE -> List.of(attempt.getMode());
            case ALL_GROUP -> attempt.getCategory() == QuizCategory.BULGARIAN
                    ? QuizGenerator.BULGARIAN_PRIMITIVE_MODES
                    : attempt.getCategory() == QuizCategory.LOGIC
                    ? QuizGenerator.LOGIC_PRIMITIVE_MODES
                    : QuizGenerator.MATH_LINEAR_MODES;
            case CUSTOM_GROUP -> List.of();
        };
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read stored report data", ex);
        }
    }

    private record FocusKey(QuizCategory category, QuizMode mode) {
    }
}
