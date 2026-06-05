package com.kidsgame.mathapp.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.quiz.AnswerRecord;
import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.BulgarianWordCatalog;
import com.kidsgame.mathapp.quiz.GeneratedQuestion;
import com.kidsgame.mathapp.quiz.QuizCategory;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.quiz.QuizGenerator;
import com.kidsgame.mathapp.quiz.QuizMode;
import com.kidsgame.mathapp.quiz.SuggestedBulgarianImageCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class WordCatalogService {
    private static final Locale BG = Locale.forLanguageTag("bg-BG");
    private static final Pattern BULGARIAN_WORD = Pattern.compile("^[А-Яа-яЁё\\s-]+$");
    private static final Set<Integer> BULGARIAN_VOWELS = Set.of(
            (int) 'А', (int) 'Ъ', (int) 'О', (int) 'У', (int) 'Е', (int) 'И', (int) 'Ю', (int) 'Я'
    );
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<GeneratedQuestion>> QUESTION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<AnswerRecord>> ANSWER_LIST = new TypeReference<>() {
    };

    private final WordCatalogRepository repository;
    private final QuizAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;

    public WordCatalogService(
            WordCatalogRepository repository,
            QuizAttemptRepository attemptRepository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.attemptRepository = attemptRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WordCatalogEntryResponse> list() {
        Map<String, CatalogUsageStats> statsByWord = usageStatsByWord();
        return repository.findAllByOrderByCategoryAscDifficultyAscWordAsc()
                .stream()
                .map(entry -> toResponse(entry, statsByWord.getOrDefault(normalizedKey(entry.getWord()), CatalogUsageStats.empty())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WordCatalogSuggestionResponse suggest(WordCatalogSuggestionRequest request) {
        requireSupportedCategory(request.category());
        String word = normalizeDisplayWord(request.word());
        return suggestion(request.category(), word);
    }

    @Transactional
    public WordCatalogEntryResponse create(WordCatalogUpsertRequest request) {
        ValidatedWord validated = validate(request, null);
        repository.findByCategoryAndWordIgnoreCase(validated.category(), validated.word())
                .ifPresent(ignored -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Думата вече е добавена.");
                });
        WordCatalogEntry entry = new WordCatalogEntry(
                validated.category(),
                validated.word(),
                validated.image(),
                writeJson(validated.syllables()),
                validated.difficulty()
        );
        entry.update(validated.category(), validated.word(), validated.image(), writeJson(validated.syllables()), validated.difficulty(), validated.active());
        return toResponse(repository.save(entry), CatalogUsageStats.empty());
    }

    @Transactional
    public WordCatalogEntryResponse update(Long id, WordCatalogUpsertRequest request) {
        WordCatalogEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Думата не е намерена."));
        ValidatedWord validated = validate(request, id);
        repository.findByCategoryAndWordIgnoreCase(validated.category(), validated.word())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(ignored -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Думата вече е добавена.");
                });
        entry.update(
                validated.category(),
                validated.word(),
                validated.image(),
                writeJson(validated.syllables()),
                validated.difficulty(),
                validated.active()
        );
        return toResponse(entry, CatalogUsageStats.empty());
    }

    @Transactional
    public void delete(Long id) {
        WordCatalogEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Думата не е намерена."));
        entry.update(
                entry.getCategory(),
                entry.getWord(),
                entry.getImage(),
                entry.getSyllablesJson(),
                entry.getDifficulty(),
                false
        );
    }

    @Transactional(readOnly = true)
    public List<BulgarianWordCatalog.BulgarianWordEntry> activeBulgarianWords() {
        List<WordCatalogEntry> entries = repository.findByCategoryAndActiveTrueOrderByDifficultyAscWordAsc(QuizCategory.BULGARIAN);
        if (entries.isEmpty()) {
            return BulgarianWordCatalog.words();
        }
        return entries.stream()
                .map(entry -> new BulgarianWordCatalog.BulgarianWordEntry(
                        entry.getWord(),
                        entry.getImage(),
                        readSyllables(entry.getSyllablesJson()),
                        entry.getDifficulty()
                ))
                .toList();
    }

    @Transactional
    public void seedDefaults() {
        for (BulgarianWordCatalog.BulgarianWordEntry word : BulgarianWordCatalog.words()) {
            seedWord(word.word(), word.image(), word.syllables(), word.level());
        }
        for (SuggestedBulgarianImageCatalog.SuggestedWord word : SuggestedBulgarianImageCatalog.words()) {
            String normalizedWord = normalizeDisplayWord(word.word());
            WordCatalogSuggestionResponse suggestion = suggestion(QuizCategory.BULGARIAN, normalizedWord);
            seedWord(normalizedWord, word.image(), suggestion.suggestedSyllables(), suggestion.suggestedDifficulty());
        }
    }

    private void seedWord(String word, String image, List<String> syllables, int difficulty) {
        repository.findByCategoryAndWordIgnoreCase(QuizCategory.BULGARIAN, word)
                .ifPresentOrElse(
                        entry -> {
                            if (entry.getImage().equals(image)
                                    && (entry.getCreatedAt().equals(entry.getUpdatedAt()) || hasBadSoftSignSyllable(entry))) {
                                entry.update(
                                        entry.getCategory(),
                                        entry.getWord(),
                                        entry.getImage(),
                                        writeJson(syllables),
                                        difficulty,
                                        entry.isActive()
                                );
                            }
                        },
                        () -> repository.save(new WordCatalogEntry(
                                QuizCategory.BULGARIAN,
                                word,
                                image,
                                writeJson(syllables),
                                difficulty
                        ))
                );
    }

    private boolean hasBadSoftSignSyllable(WordCatalogEntry entry) {
        return readSyllables(entry.getSyllablesJson()).stream()
                .anyMatch(syllable -> syllable.startsWith("Ь"));
    }

    private WordCatalogEntryResponse toResponse(WordCatalogEntry entry, CatalogUsageStats stats) {
        BulgarianWordCatalog.BulgarianWordEntry word = new BulgarianWordCatalog.BulgarianWordEntry(
                entry.getWord(),
                entry.getImage(),
                readSyllables(entry.getSyllablesJson()),
                entry.getDifficulty()
        );
        return new WordCatalogEntryResponse(
                entry.getId(),
                entry.getCategory(),
                entry.getWord(),
                entry.getImage(),
                word.letters(),
                word.syllables(),
                entry.getDifficulty(),
                entry.isActive(),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                stats.usedCount(),
                stats.correctCount(),
                stats.wrongCount()
        );
    }

    private Map<String, CatalogUsageStats> usageStatsByWord() {
        Map<String, CatalogUsageStats> statsByWord = new LinkedHashMap<>();
        List<QuizAttempt> attempts = attemptRepository.findByCategoryAndStatus(QuizCategory.BULGARIAN, AttemptStatus.COMPLETED);
        for (QuizAttempt attempt : attempts) {
            if (isAdminTestAttempt(attempt)) {
                continue;
            }
            Map<Integer, AnswerRecord> answersByQuestion = readAnswers(attempt).stream()
                    .collect(Collectors.toMap(AnswerRecord::questionId, Function.identity(), (first, ignored) -> first));
            for (GeneratedQuestion question : readQuestions(attempt)) {
                if (question.sourceMode() == null || !QuizGenerator.BULGARIAN_PRIMITIVE_MODES.contains(question.sourceMode())) {
                    continue;
                }
                AnswerRecord answer = answersByQuestion.get(question.id());
                boolean correct = answer != null && answer.correct();
                if (question.sourceMode() == QuizMode.WORD_FIRST_LETTER_GROUP) {
                    for (String choice : question.choices()) {
                        GroupingChoice groupingChoice = parseGroupingChoice(choice);
                        if (groupingChoice != null) {
                            statsByWord.merge(normalizedKey(groupingChoice.word()), CatalogUsageStats.fromAnswer(correct), CatalogUsageStats::plus);
                        }
                    }
                    continue;
                }
                String wordKey = normalizedKey(question.sourceMode() == QuizMode.WORD_PICTURE
                                || question.sourceMode() == QuizMode.WORD_MISSING_LETTER
                                || question.sourceMode() == QuizMode.WORD_WRONG_LETTER
                        ? (question.speechText() == null ? question.prompt() : question.speechText())
                        : question.answer());
                if (wordKey.isBlank()) {
                    continue;
                }
                statsByWord.merge(wordKey, CatalogUsageStats.fromAnswer(correct), CatalogUsageStats::plus);
            }
        }
        return statsByWord;
    }

    private GroupingChoice parseGroupingChoice(String choice) {
        int separatorIndex = choice.lastIndexOf('|');
        if (separatorIndex < 0 || separatorIndex == choice.length() - 1) {
            return null;
        }
        return new GroupingChoice(choice.substring(0, separatorIndex), choice.substring(separatorIndex + 1));
    }

    private boolean isAdminTestAttempt(QuizAttempt attempt) {
        return attempt.getUser().getUsername().toLowerCase(BG).equals("христо");
    }

    private List<GeneratedQuestion> readQuestions(QuizAttempt attempt) {
        return readJson(attempt.getQuestionsJson(), QUESTION_LIST);
    }

    private List<AnswerRecord> readAnswers(QuizAttempt attempt) {
        return readJson(attempt.getAnswersJson(), ANSWER_LIST);
    }

    private ValidatedWord validate(WordCatalogUpsertRequest request, Long currentId) {
        requireSupportedCategory(request.category());
        String word = normalizeDisplayWord(request.word());
        if (!BULGARIAN_WORD.matcher(word).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Думата може да съдържа само български букви, интервал или тире.");
        }
        String image = request.image().trim();
        validateSafeEmoji(image);

        WordCatalogSuggestionResponse suggestion = suggestion(request.category(), word);
        List<String> syllables = request.syllables() == null || request.syllables().isEmpty()
                ? suggestion.suggestedSyllables()
                : request.syllables().stream()
                        .map(this::normalizeSyllable)
                        .filter(syllable -> !syllable.isBlank())
                        .toList();
        validateSyllables(word, syllables);
        return new ValidatedWord(request.category(), word, image, syllables, request.difficulty(), request.active());
    }

    private WordCatalogSuggestionResponse suggestion(QuizCategory category, String word) {
        BulgarianWordCatalog.BulgarianWordEntry entry = new BulgarianWordCatalog.BulgarianWordEntry(
                word,
                "⭐",
                suggestSyllables(word),
                suggestDifficulty(word)
        );
        return new WordCatalogSuggestionResponse(
                category,
                word,
                entry.letters(),
                entry.syllables(),
                entry.level()
        );
    }

    private void requireSupportedCategory(QuizCategory category) {
        if (category != QuizCategory.BULGARIAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В момента картинният каталог е само за Български.");
        }
    }

    private String normalizeDisplayWord(String value) {
        String cleaned = value.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Думата е задължителна.");
        }
        return cleaned.toLowerCase(BG);
    }

    private String normalizedKey(String value) {
        return value.toUpperCase(BG).replace(" ", "").replace("-", "");
    }

    private String normalizeSyllable(String value) {
        return value.trim().replaceAll("\\s+", "").toUpperCase(BG);
    }

    private void validateSyllables(String word, List<String> syllables) {
        if (syllables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Трябва да има поне една сричка.");
        }
        String normalizedWord = word.toUpperCase(BG).replace(" ", "").replace("-", "");
        String joined = String.join("", syllables);
        if (!joined.equals(normalizedWord)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сричките трябва да образуват точно думата.");
        }
    }

    private void validateSafeEmoji(String image) {
        if (image.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинката е задължителна.");
        }
        if (image.startsWith("focus:")) {
            validateFocusedEmoji(image);
            return;
        }
        String lower = image.toLowerCase(Locale.ROOT);
        if (lower.contains("http") || lower.contains("/") || lower.contains("<") || lower.contains(">")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинката трябва да е emoji, не адрес, HTML или файл.");
        }
        long visibleSymbols = image.codePoints()
                .filter(codePoint -> Character.getType(codePoint) != Character.NON_SPACING_MARK)
                .filter(codePoint -> Character.getType(codePoint) != Character.FORMAT)
                .count();
        if (visibleSymbols < 1 || visibleSymbols > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Използвай от 1 до 6 emoji символа.");
        }
        boolean allSafeSymbols = image.codePoints().allMatch(codePoint -> {
            int type = Character.getType(codePoint);
            return type == Character.OTHER_SYMBOL
                    || type == Character.MODIFIER_SYMBOL
                    || type == Character.NON_SPACING_MARK
                    || type == Character.FORMAT
                    || codePoint == 0xFE0F;
        });
        if (!allSafeSymbols) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинката трябва да съдържа само emoji символи.");
        }
    }

    private void validateFocusedEmoji(String image) {
        String[] parts = image.substring("focus:".length()).split("\\|", -1);
        if (parts.length < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Фокусираната картинка трябва да има поне две emoji и номер за ограждане.");
        }
        int targetIndex;
        try {
            targetIndex = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Фокусираната картинка трябва да завършва с номер за ограждане.");
        }
        if (targetIndex < 0 || targetIndex >= parts.length - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Номерът за ограждане не сочи към emoji в картинката.");
        }
        for (int index = 0; index < parts.length - 1; index++) {
            validateSafeEmoji(parts[index]);
        }
    }

    private List<String> suggestSyllables(String word) {
        List<String> syllables = new ArrayList<>();
        for (String part : word.toUpperCase(BG).split("[\\s-]+")) {
            if (part.isBlank()) {
                continue;
            }
            syllables.addAll(syllablesForPart(part));
        }
        return syllables.isEmpty() ? List.of(word.toUpperCase(BG).replace(" ", "").replace("-", "")) : syllables;
    }

    private List<String> syllablesForPart(String wordPart) {
        List<Integer> vowels = new ArrayList<>();
        int[] codePoints = wordPart.codePoints().toArray();
        for (int index = 0; index < codePoints.length; index++) {
            if (BULGARIAN_VOWELS.contains(codePoints[index])) {
                vowels.add(index);
            }
        }
        if (vowels.size() <= 1) {
            return List.of(wordPart);
        }

        List<String> syllables = new ArrayList<>();
        int start = 0;
        for (int vowelIndex = 0; vowelIndex < vowels.size() - 1; vowelIndex++) {
            int currentVowel = vowels.get(vowelIndex);
            int nextVowel = vowels.get(vowelIndex + 1);
            int consonantsBetween = nextVowel - currentVowel - 1;
            boolean softSignBeforeNextVowel = nextVowel > 0 && codePoints[nextVowel - 1] == 'Ь';
            int endExclusive = softSignBeforeNextVowel || consonantsBetween <= 1 ? currentVowel + 1 : currentVowel + 2;
            syllables.add(slice(codePoints, start, endExclusive));
            start = endExclusive;
        }
        syllables.add(slice(codePoints, start, codePoints.length));
        return syllables;
    }

    private String slice(int[] codePoints, int startInclusive, int endExclusive) {
        return new String(codePoints, startInclusive, endExclusive - startInclusive);
    }

    private int suggestDifficulty(String word) {
        int letters = word.toUpperCase(BG).replace(" ", "").replace("-", "").length();
        int words = word.trim().split("[\\s-]+").length;
        int level = Math.max(1, letters - 2);
        if (words > 1) {
            level += 2;
        }
        return Math.max(1, Math.min(10, level));
    }

    private List<String> readSyllables(String json) {
        return readJson(json, STRING_LIST);
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read stored catalog data", ex);
        }
    }

    private String writeJson(List<String> syllables) {
        try {
            return objectMapper.writeValueAsString(syllables);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write catalog syllables", ex);
        }
    }

    private record ValidatedWord(
            QuizCategory category,
            String word,
            String image,
            List<String> syllables,
            int difficulty,
            boolean active
    ) {
    }

    private record GroupingChoice(String image, String word) {
    }

    private record CatalogUsageStats(long usedCount, long correctCount, long wrongCount) {
        static CatalogUsageStats empty() {
            return new CatalogUsageStats(0, 0, 0);
        }

        static CatalogUsageStats fromAnswer(boolean correct) {
            return new CatalogUsageStats(1, correct ? 1 : 0, correct ? 0 : 1);
        }

        CatalogUsageStats plus(CatalogUsageStats other) {
            return new CatalogUsageStats(
                    usedCount + other.usedCount,
                    correctCount + other.correctCount,
                    wrongCount + other.wrongCount
            );
        }
    }
}
