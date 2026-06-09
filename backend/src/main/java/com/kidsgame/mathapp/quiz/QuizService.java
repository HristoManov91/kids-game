package com.kidsgame.mathapp.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.leaderboard.CrystalLeaderboardRow;
import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.leaderboard.LeaderboardRow;
import com.kidsgame.mathapp.reward.CrystalBalanceService;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private static final long MAX_HEARTBEAT_GAP_SECONDS = 15;
    private static final Locale BG = Locale.forLanguageTag("bg-BG");
    private static final TypeReference<List<GeneratedQuestion>> QUESTION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<AnswerRecord>> ANSWER_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<QuizMode>> MODE_LIST = new TypeReference<>() {
    };

    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final QuizGenerator quizGenerator;
    private final ObjectMapper objectMapper;
    private final CrystalBalanceService crystalBalanceService;

    public QuizService(
            QuizAttemptRepository attemptRepository,
            UserRepository userRepository,
            QuizGenerator quizGenerator,
            ObjectMapper objectMapper,
            CrystalBalanceService crystalBalanceService
    ) {
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.quizGenerator = quizGenerator;
        this.objectMapper = objectMapper;
        this.crystalBalanceService = crystalBalanceService;
    }

    @Transactional
    public QuizAttemptResponse start(StartQuizRequest request, UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Потребителят не е намерен."));
        List<QuizMode> includedModes = includedModesFor(request);
        boolean leaderboardEligible = request.mode() != QuizMode.CUSTOM_GROUP;
        List<GeneratedQuestion> questions = request.mode() == QuizMode.CUSTOM_GROUP
                ? quizGenerator.generate(request.category(), includedModes, request.difficulty())
                : quizGenerator.generate(request.category(), request.mode(), request.difficulty());
        QuizAttempt attempt = new QuizAttempt(
                user,
                request.category(),
                request.mode(),
                request.difficulty(),
                writeJson(questions),
                writeJson(includedModes),
                leaderboardEligible,
                totalQuestionCount(questions)
        );
        return toAttemptResponse(attemptRepository.save(attempt));
    }

    @Transactional
    public QuestionResultResponse answer(UUID attemptId, AnswerQuestionRequest request, UserPrincipal principal) {
        QuizAttempt attempt = loadOwnedAttempt(attemptId, principal);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Тестът вече е завършен.");
        }

        List<GeneratedQuestion> questions = readQuestions(attempt);
        GeneratedQuestion question = questions.stream()
                .filter(candidate -> candidate.id() == request.questionId())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Задачата не е намерена."));

        List<AnswerRecord> answers = new ArrayList<>(readAnswers(attempt));
        for (AnswerRecord answer : answers) {
            if (answer.questionId() == request.questionId()) {
                return QuestionResultResponse.from(answer);
            }
        }

        String normalizedAnswer = normalize(request.answer(), question);
        boolean correct = normalizedAnswer.equals(normalize(question.answer(), question));
        AnswerRecord answer = new AnswerRecord(
                question.id(),
                request.answer().trim(),
                correct,
                QuestionScoring.publicCorrectAnswer(question),
                Instant.now()
        );
        answers.add(answer);
        attempt.replaceAnswers(writeJson(answers));
        return QuestionResultResponse.from(answer);
    }

    @Transactional
    public QuizSummaryResponse finish(UUID attemptId, UserPrincipal principal) {
        QuizAttempt attempt = loadOwnedAttempt(attemptId, principal);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            return toSummaryResponse(attempt);
        }
        attempt.recordActivity(Instant.now(), MAX_HEARTBEAT_GAP_SECONDS);

        List<GeneratedQuestion> questions = readQuestions(attempt);
        Map<Integer, AnswerRecord> answersByQuestion = readAnswers(attempt).stream()
                .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));

        Instant now = Instant.now();
        for (GeneratedQuestion question : questions) {
            answersByQuestion.putIfAbsent(
                    question.id(),
                    new AnswerRecord(question.id(), "", false, QuestionScoring.publicCorrectAnswer(question), now)
            );
        }

        List<AnswerRecord> finalAnswers = questions.stream()
                .map(question -> answersByQuestion.get(question.id()))
                .toList();
        Map<Integer, GeneratedQuestion> questionsById = questions.stream()
                .collect(Collectors.toMap(GeneratedQuestion::id, Function.identity(), (first, ignored) -> first));
        int total = totalQuestionCount(questions);
        int score = finalAnswers.stream()
                .mapToInt(answer -> QuestionScoring.score(questionsById.get(answer.questionId()), answer))
                .sum();
        BigDecimal grade = calculateGrade(score, total);
        Medal medal = medalFor(score, total);

        attempt.complete(score, grade, medal, writeJson(finalAnswers));
        return toSummaryResponse(attempt);
    }

    @Transactional(readOnly = true)
    public QuizAttemptResponse getAttempt(UUID attemptId, UserPrincipal principal) {
        return toAttemptResponse(loadOwnedAttempt(attemptId, principal));
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> history(UserPrincipal principal) {
        return attemptRepository.findTop20ByUser_IdOrderByStartedAtDesc(principal.id())
                .stream()
                .map(this::toAttemptResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> activeAttempts(UserPrincipal principal) {
        return attemptRepository.findByUser_IdAndStatusOrderByStartedAtDesc(principal.id(), AttemptStatus.ACTIVE)
                .stream()
                .map(this::toAttemptResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentAttemptRow> recentForSelection(UserPrincipal principal, QuizCategory category, QuizMode mode, int difficulty) {
        return attemptRepository
                .findTop10ByUser_IdAndCategoryAndModeAndDifficultyAndStatusOrderByCompletedAtDesc(
                        principal.id(),
                        category,
                        mode,
                        difficulty,
                        AttemptStatus.COMPLETED
                )
                .stream()
                .map(this::toRecentAttemptRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public CrystalTotalResponse crystalTotal(UserPrincipal principal) {
        return new CrystalTotalResponse(crystalBalanceService.availableCrystals(principal.id()));
    }

    @Transactional
    public QuizTimeResponse heartbeat(UUID attemptId, UserPrincipal principal) {
        QuizAttempt attempt = loadOwnedAttempt(attemptId, principal);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            return new QuizTimeResponse(durationSeconds(attempt));
        }
        attempt.recordActivity(Instant.now(), MAX_HEARTBEAT_GAP_SECONDS);
        return new QuizTimeResponse(durationSeconds(attempt));
    }

    @Transactional
    public void cancel(UUID attemptId, UserPrincipal principal) {
        QuizAttempt attempt = loadOwnedAttempt(attemptId, principal);
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Завършен тест не може да бъде отказан.");
        }
        attemptRepository.delete(attempt);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> leaderboard(QuizCategory category, QuizMode mode, int difficulty) {
        if (mode == QuizMode.CUSTOM_GROUP) {
            return List.of();
        }

        List<QuizAttempt> attempts = attemptRepository
                .findByCategoryAndModeAndDifficultyAndStatusAndLeaderboardEligibleTrue(
                        category,
                        mode,
                        difficulty,
                        AttemptStatus.COMPLETED
                );

        Map<Long, Integer> entriesByUser = new LinkedHashMap<>();
        List<QuizAttempt> rankedAttempts = new ArrayList<>();
        attempts.stream()
                .sorted(Comparator
                        .comparing(QuizAttempt::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(this::durationSeconds)
                        .thenComparing(QuizAttempt::getCompletedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(attempt -> {
                    long userId = attempt.getUser().getId();
                    int userEntries = entriesByUser.getOrDefault(userId, 0);
                    if (userEntries < 5 && rankedAttempts.size() < 10) {
                        entriesByUser.put(userId, userEntries + 1);
                        rankedAttempts.add(attempt);
                    }
                });

        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 1;
        for (QuizAttempt attempt : rankedAttempts) {
            rows.add(new LeaderboardRow(
                    rank++,
                    attempt.getUser().getId(),
                    attempt.getUser().getDisplayName(),
                    attempt.getId(),
                    attempt.getScore(),
                    attempt.getTotalQuestions(),
                    attempt.getGrade(),
                    attempt.getMedal(),
                    CrystalReward.calculate(attempt.getDifficulty(), attempt.getScore(), attempt.getTotalQuestions()),
                    durationSeconds(attempt),
                    attempt.getCompletedAt()
            ));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<CrystalLeaderboardRow> crystalLeaderboard() {
        Map<Long, CrystalStats> statsByUser = new LinkedHashMap<>();
        attemptRepository.findByStatusAndLeaderboardEligibleTrue(AttemptStatus.COMPLETED)
                .forEach(attempt -> statsByUser
                        .computeIfAbsent(
                                attempt.getUser().getId(),
                                ignored -> new CrystalStats(attempt.getUser().getId(), attempt.getUser().getDisplayName())
                        )
                        .add(crystalsFor(attempt)));

        List<CrystalStats> stats = statsByUser.values()
                .stream()
                .sorted(Comparator
                        .comparingInt(CrystalStats::crystals).reversed()
                        .thenComparing(Comparator.comparingInt(CrystalStats::completedAttempts).reversed())
                        .thenComparing(CrystalStats::displayName))
                .limit(10)
                .toList();

        List<CrystalLeaderboardRow> rows = new ArrayList<>();
        int rank = 1;
        for (CrystalStats stat : stats) {
            rows.add(new CrystalLeaderboardRow(
                    rank++,
                    stat.userId(),
                    stat.displayName(),
                    stat.crystals(),
                    stat.completedAttempts()
            ));
        }
        return rows;
    }

    private QuizAttempt loadOwnedAttempt(UUID attemptId, UserPrincipal principal) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тестът не е намерен."));
        if (!attempt.getUser().getId().equals(principal.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нямате достъп до този тест.");
        }
        return attempt;
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt) {
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getCategory(),
                attempt.getMode(),
                readIncludedModes(attempt),
                attempt.getDifficulty(),
                attempt.isLeaderboardEligible(),
                attempt.getStatus(),
                readQuestions(attempt).stream().map(QuestionResponse::from).toList(),
                readAnswers(attempt).stream().map(QuestionResultResponse::from).toList(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getGrade(),
                attempt.getMedal(),
                CrystalReward.calculate(attempt.getDifficulty(), attempt.getScore(), attempt.getTotalQuestions()),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                durationSeconds(attempt),
                scoreBreakdown(readQuestions(attempt), readAnswers(attempt))
        );
    }

    private RecentAttemptRow toRecentAttemptRow(QuizAttempt attempt) {
        return new RecentAttemptRow(
                attempt.getId(),
                attempt.getCategory(),
                attempt.getMode(),
                attempt.getDifficulty(),
                attempt.getScore() == null ? 0 : attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getGrade(),
                crystalsFor(attempt),
                durationSeconds(attempt),
                attempt.getCompletedAt()
        );
    }

    private int crystalsFor(QuizAttempt attempt) {
        return CrystalReward.calculate(
                attempt.getDifficulty(),
                attempt.getScore() == null ? 0 : attempt.getScore(),
                attempt.getTotalQuestions()
        );
    }

    private QuizSummaryResponse toSummaryResponse(QuizAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Тестът още не е завършен.");
        }
        return new QuizSummaryResponse(
                attempt.getId(),
                attempt.getCategory(),
                attempt.getMode(),
                readIncludedModes(attempt),
                attempt.getDifficulty(),
                attempt.isLeaderboardEligible(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getGrade(),
                attempt.getMedal(),
                CrystalReward.calculate(attempt.getDifficulty(), attempt.getScore() == null ? 0 : attempt.getScore(), attempt.getTotalQuestions()),
                attempt.getCompletedAt(),
                durationSeconds(attempt),
                readAnswers(attempt).stream().map(QuestionResultResponse::from).toList(),
                scoreBreakdown(readQuestions(attempt), readAnswers(attempt))
        );
    }

    private List<GeneratedQuestion> readQuestions(QuizAttempt attempt) {
        return readJson(attempt.getQuestionsJson(), QUESTION_LIST);
    }

    private List<AnswerRecord> readAnswers(QuizAttempt attempt) {
        return readJson(attempt.getAnswersJson(), ANSWER_LIST);
    }

    private List<QuizMode> readIncludedModes(QuizAttempt attempt) {
        List<QuizMode> modes = readJson(attempt.getIncludedModesJson(), MODE_LIST);
        return modes.isEmpty() ? includedModesFor(attempt.getCategory(), attempt.getMode(), List.of()) : modes;
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
        if (question.kind() == QuestionKind.SUDOKU) {
            return normalized.replaceAll("[^0-9]", "");
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

    private List<QuizMode> includedModesFor(StartQuizRequest request) {
        List<QuizMode> includedModes = includedModesFor(
                request.category(),
                request.mode(),
                request.includedModes() == null ? List.of() : request.includedModes()
        );
        if (!allowedModesFor(request.category()).containsAll(includedModes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Избраният тип задачи не е за този предмет.");
        }
        return includedModes;
    }

    private List<QuizMode> includedModesFor(QuizCategory category, QuizMode mode, List<QuizMode> requestedModes) {
        return switch (mode) {
            case ADDITION, SUBTRACTION, UNKNOWN_ADDITION, UNKNOWN_SUBTRACTION, MULTIPLICATION, DIVISION, COMPARE -> List.of(mode);
            case MIXED -> List.of(QuizMode.ADDITION, QuizMode.SUBTRACTION);
            case UNKNOWN_MIXED -> List.of(QuizMode.UNKNOWN_ADDITION, QuizMode.UNKNOWN_SUBTRACTION);
            case MULTIPLICATION_DIVISION -> List.of(QuizMode.MULTIPLICATION, QuizMode.DIVISION);
            case WORD_LETTERS, WORD_SYLLABLES, WORD_TYPING, WORD_PICTURE, WORD_MISSING_LETTER, WORD_FIRST_LETTER_GROUP, WORD_WRONG_LETTER -> List.of(mode);
            case FIND_OBJECT, SPOT_DIFFERENCES, MEMORY_PAIRS, PATTERN_SEQUENCE, SUDOKU -> List.of(mode);
            case ALL_GROUP -> primitiveModesFor(category);
            case CUSTOM_GROUP -> {
                List<QuizMode> allowedModes = primitiveModesFor(category);
                List<QuizMode> selected = requestedModes.stream()
                        .filter(allowedModes::contains)
                        .distinct()
                        .toList();
                if (selected.size() < 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Изберете поне две категории за тест по избор.");
                }
                yield selected;
            }
        };
    }

    private List<QuizMode> primitiveModesFor(QuizCategory category) {
        return switch (category) {
            case MATH -> QuizGenerator.MATH_LINEAR_MODES;
            case BULGARIAN -> QuizGenerator.BULGARIAN_PRIMITIVE_MODES;
            case LOGIC -> QuizGenerator.LOGIC_PRIMITIVE_MODES;
        };
    }

    private List<QuizMode> allowedModesFor(QuizCategory category) {
        return switch (category) {
            case MATH -> QuizGenerator.MATH_PRIMITIVE_MODES;
            case BULGARIAN -> QuizGenerator.BULGARIAN_PRIMITIVE_MODES;
            case LOGIC -> QuizGenerator.LOGIC_PRIMITIVE_MODES;
        };
    }

    private long durationSeconds(QuizAttempt attempt) {
        return Math.max(0, attempt.getActiveDurationSeconds());
    }

    private int totalQuestionCount(List<GeneratedQuestion> questions) {
        return questions.stream().mapToInt(QuestionScoring::weight).sum();
    }

    private List<ScoreBreakdownResponse> scoreBreakdown(List<GeneratedQuestion> questions, List<AnswerRecord> answers) {
        Map<Integer, AnswerRecord> answersByQuestion = answers.stream()
                .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first));
        Map<QuizMode, int[]> counts = new LinkedHashMap<>();
        for (QuizMode mode : QuizGenerator.PRIMITIVE_MODES) {
            counts.put(mode, new int[]{0, 0});
        }
        for (GeneratedQuestion question : questions) {
            QuizMode mode = question.sourceMode();
            if (mode == null || !counts.containsKey(mode)) {
                continue;
            }
            int[] count = counts.get(mode);
            count[1] += QuestionScoring.weight(question);
            AnswerRecord answer = answersByQuestion.get(question.id());
            if (answer != null) {
                count[0] += QuestionScoring.score(question, answer);
            }
        }

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue()[1] > 0)
                .map(entry -> new ScoreBreakdownResponse(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private static final class CrystalStats {
        private final Long userId;
        private final String displayName;
        private int crystals;
        private int completedAttempts;

        private CrystalStats(Long userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }

        private void add(int crystals) {
            this.crystals += crystals;
            this.completedAttempts++;
        }

        private Long userId() {
            return userId;
        }

        private String displayName() {
            return displayName;
        }

        private int crystals() {
            return crystals;
        }

        private int completedAttempts() {
            return completedAttempts;
        }
    }
}
