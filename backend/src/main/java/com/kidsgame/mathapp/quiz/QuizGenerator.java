package com.kidsgame.mathapp.quiz;

import com.kidsgame.mathapp.catalog.WordCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuizGenerator {
    public static final int QUESTIONS_PER_TEST = 20;
    public static final int BULGARIAN_QUESTIONS_PER_TEST = 10;
    public static final int LOGIC_QUESTIONS_PER_TEST = 5;
    private static final Locale BG = Locale.forLanguageTag("bg-BG");

    public static final List<QuizMode> MATH_LINEAR_MODES = List.of(
            QuizMode.ADDITION,
            QuizMode.SUBTRACTION,
            QuizMode.MULTIPLICATION,
            QuizMode.DIVISION,
            QuizMode.UNKNOWN_ADDITION,
            QuizMode.UNKNOWN_SUBTRACTION,
            QuizMode.COMPARE
    );
    public static final List<QuizMode> MATH_PRIMITIVE_MODES = List.of(
            QuizMode.ADDITION,
            QuizMode.SUBTRACTION,
            QuizMode.MULTIPLICATION,
            QuizMode.DIVISION,
            QuizMode.UNKNOWN_ADDITION,
            QuizMode.UNKNOWN_SUBTRACTION,
            QuizMode.COMPARE
    );
    public static final List<QuizMode> BULGARIAN_PRIMITIVE_MODES = List.of(
            QuizMode.WORD_LETTERS,
            QuizMode.WORD_SYLLABLES,
            QuizMode.WORD_TYPING,
            QuizMode.WORD_PICTURE,
            QuizMode.WORD_MISSING_LETTER,
            QuizMode.WORD_FIRST_LETTER_GROUP,
            QuizMode.WORD_WRONG_LETTER
    );
    public static final List<QuizMode> LOGIC_PRIMITIVE_MODES = List.of(
            QuizMode.FIND_OBJECT,
            QuizMode.SPOT_DIFFERENCES,
            QuizMode.MEMORY_PAIRS,
            QuizMode.PATTERN_SEQUENCE
    );
    public static final List<QuizMode> PRIMITIVE_MODES = primitiveModes();
    private static final List<String> BULGARIAN_DISTRACTOR_LETTERS = List.of(
            "А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "И", "Й", "К", "Л", "М", "Н",
            "О", "П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ь",
            "Ю", "Я"
    );
    private static final List<String> BULGARIAN_DISTRACTOR_SYLLABLES = List.of(
            "БА", "БО", "ВА", "ВЕ", "ГА", "ГО", "ДА", "ДЕ", "ЖА", "ЗА", "ЗЕ",
            "КА", "КО", "ЛА", "ЛЕ", "МА", "МО", "НА", "НЕ", "ПА", "ПЕ", "РА",
            "РО", "СА", "СЕ", "ТА", "ТЕ", "ФА", "ХА", "ЦА", "ЧЕ", "ША", "Ю", "Я"
    );

    private final WordCatalogService wordCatalogService;

    public QuizGenerator() {
        this.wordCatalogService = null;
    }

    @Autowired
    public QuizGenerator(WordCatalogService wordCatalogService) {
        this.wordCatalogService = wordCatalogService;
    }

    public List<GeneratedQuestion> generate(QuizMode mode, int difficulty) {
        return generate(QuizCategory.MATH, mode, difficulty);
    }

    public List<GeneratedQuestion> generate(QuizCategory category, QuizMode mode, int difficulty) {
        if (category == QuizCategory.BULGARIAN) {
            if (mode == QuizMode.ALL_GROUP) {
                return generateBulgarian(BULGARIAN_PRIMITIVE_MODES, difficulty);
            }
            if (mode == QuizMode.CUSTOM_GROUP) {
                throw new IllegalArgumentException("Тестовете по избор трябва да имат избрани категории.");
            }
            return generateBulgarian(List.of(mode), difficulty);
        }
        if (category == QuizCategory.LOGIC) {
            if (mode == QuizMode.ALL_GROUP) {
                return generateLogic(LOGIC_PRIMITIVE_MODES, difficulty);
            }
            if (mode == QuizMode.CUSTOM_GROUP) {
                throw new IllegalArgumentException("Тестовете по избор трябва да имат избрани категории.");
            }
            return generateLogic(List.of(mode), difficulty);
        }

        if (mode == QuizMode.ALL_GROUP) {
            return generate(MATH_LINEAR_MODES, difficulty);
        }
        if (mode == QuizMode.CUSTOM_GROUP) {
            throw new IllegalArgumentException("Тестовете по избор трябва да имат избрани категории.");
        }
        if (mode == QuizMode.MIXED) {
            return generate(List.of(QuizMode.ADDITION, QuizMode.SUBTRACTION), difficulty);
        }
        if (mode == QuizMode.UNKNOWN_MIXED) {
            return generate(List.of(QuizMode.UNKNOWN_ADDITION, QuizMode.UNKNOWN_SUBTRACTION), difficulty);
        }
        if (mode == QuizMode.MULTIPLICATION_DIVISION) {
            return generate(List.of(QuizMode.MULTIPLICATION, QuizMode.DIVISION), difficulty);
        }
        return generate(List.of(mode), difficulty);
    }

    public List<GeneratedQuestion> generate(Collection<QuizMode> modes, int difficulty) {
        return generate(QuizCategory.MATH, modes, difficulty);
    }

    public List<GeneratedQuestion> generate(QuizCategory category, Collection<QuizMode> modes, int difficulty) {
        if (category == QuizCategory.BULGARIAN) {
            return generateBulgarian(modes, difficulty);
        }
        if (category == QuizCategory.LOGIC) {
            return generateLogic(modes, difficulty);
        }

        int max = difficulty * 10;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<QuizMode> availableModes = modes.stream()
                .filter(MATH_LINEAR_MODES::contains)
                .distinct()
                .toList();
        if (availableModes.isEmpty()) {
            throw new IllegalArgumentException("At least one question category is required.");
        }
        List<GeneratedQuestion> questions = new ArrayList<>();
        Set<String> usedPrompts = new LinkedHashSet<>();

        int attempts = 0;
        while (questions.size() < QUESTIONS_PER_TEST && attempts < QUESTIONS_PER_TEST * 80) {
            GeneratedQuestion candidate = nextQuestion(questions.size() + 1, pickMode(availableModes, random), max, random);
            attempts++;
            if (usedPrompts.add(candidate.sourceMode() + ":" + candidate.prompt())) {
                questions.add(candidate);
            }
        }

        while (questions.size() < QUESTIONS_PER_TEST) {
            questions.add(nextQuestion(questions.size() + 1, pickMode(availableModes, random), max, random));
        }

        return questions;
    }

    private QuizMode pickMode(List<QuizMode> modes, ThreadLocalRandom random) {
        return modes.get(random.nextInt(modes.size()));
    }

    private GeneratedQuestion nextQuestion(int id, QuizMode mode, int max, ThreadLocalRandom random) {
        return switch (mode) {
            case ADDITION -> addition(id, max, random);
            case SUBTRACTION -> subtraction(id, max, random);
            case MULTIPLICATION -> multiplication(id, max, random);
            case DIVISION -> division(id, max, random);
            case UNKNOWN_ADDITION -> unknownAddition(id, max, random);
            case UNKNOWN_SUBTRACTION -> unknownSubtraction(id, max, random);
            case COMPARE -> compare(id, max, random);
            case MIXED, UNKNOWN_MIXED, MULTIPLICATION_DIVISION, CUSTOM_GROUP, ALL_GROUP, WORD_LETTERS, WORD_SYLLABLES, WORD_TYPING, WORD_PICTURE, WORD_MISSING_LETTER, WORD_FIRST_LETTER_GROUP, WORD_WRONG_LETTER, FIND_OBJECT, SPOT_DIFFERENCES, MEMORY_PAIRS, PATTERN_SEQUENCE -> throw new IllegalStateException("Grouped modes must be resolved before question generation.");
        };
    }

    private GeneratedQuestion addition(int id, int max, ThreadLocalRandom random) {
        int left = random.nextInt(0, max + 1);
        int right = random.nextInt(0, max + 1);
        return numeric(id, left + " + " + right + " = ?", left + right, QuizMode.ADDITION);
    }

    private GeneratedQuestion subtraction(int id, int max, ThreadLocalRandom random) {
        int left = random.nextInt(0, max + 1);
        int right = random.nextInt(0, left + 1);
        return numeric(id, left + " - " + right + " = ?", left - right, QuizMode.SUBTRACTION);
    }

    private GeneratedQuestion multiplication(int id, int max, ThreadLocalRandom random) {
        int left = random.nextInt(0, max + 1);
        int right = random.nextInt(0, max + 1);
        return numeric(id, left + " x " + right + " = ?", left * right, QuizMode.MULTIPLICATION);
    }

    private GeneratedQuestion division(int id, int max, ThreadLocalRandom random) {
        int divisor = random.nextInt(1, max + 1);
        int quotient = random.nextInt(0, (max / divisor) + 1);
        int dividend = divisor * quotient;
        return numeric(id, dividend + " : " + divisor + " = ?", quotient, QuizMode.DIVISION);
    }

    private GeneratedQuestion unknownAddition(int id, int max, ThreadLocalRandom random) {
        int left = random.nextInt(0, max + 1);
        int right = random.nextInt(0, max + 1);
        boolean hideLeft = random.nextBoolean();
        String prompt = hideLeft
                ? "? + " + right + " = " + (left + right)
                : left + " + ? = " + (left + right);
        return numeric(id, prompt, hideLeft ? left : right, QuizMode.UNKNOWN_ADDITION);
    }

    private GeneratedQuestion unknownSubtraction(int id, int max, ThreadLocalRandom random) {
        int left = random.nextInt(0, max + 1);
        int right = random.nextInt(0, left + 1);
        boolean hideLeft = random.nextBoolean();
        String prompt = hideLeft
                ? "? - " + right + " = " + (left - right)
                : left + " - ? = " + (left - right);
        return numeric(id, prompt, hideLeft ? left : right, QuizMode.UNKNOWN_SUBTRACTION);
    }

    private GeneratedQuestion compare(int id, int max, ThreadLocalRandom random) {
        int left = random.nextInt(0, max + 1);
        int right = random.nextInt(0, max + 1);
        String answer = left > right ? ">" : left < right ? "<" : "=";
        return new GeneratedQuestion(id, QuestionKind.CHOICE, left + " ? " + right, null, null, List.of(), List.of("<", "=", ">"), answer, QuizMode.COMPARE);
    }

    private GeneratedQuestion numeric(int id, String prompt, int answer, QuizMode sourceMode) {
        return new GeneratedQuestion(id, QuestionKind.NUMERIC, prompt, null, null, List.of(), List.of(), String.valueOf(answer), sourceMode);
    }

    private List<GeneratedQuestion> generateBulgarian(Collection<QuizMode> modes, int difficulty) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<QuizMode> availableModes = modes.stream()
                .filter(BULGARIAN_PRIMITIVE_MODES::contains)
                .distinct()
                .toList();
        if (availableModes.isEmpty()) {
            throw new IllegalArgumentException("At least one Bulgarian question category is required.");
        }

        List<GeneratedQuestion> questions = new ArrayList<>();
        Set<String> usedWords = new LinkedHashSet<>();

        int attempts = 0;
        while (questions.size() < BULGARIAN_QUESTIONS_PER_TEST && attempts < BULGARIAN_QUESTIONS_PER_TEST * 80) {
            QuizMode mode = pickMode(availableModes, random);
            List<BulgarianWordCatalog.BulgarianWordEntry> words = wordPool(mode, difficulty, random);
            BulgarianWordCatalog.BulgarianWordEntry word = words.get(random.nextInt(words.size()));
            attempts++;
            if (usedWords.add(word.normalizedWord())) {
                questions.add(wordQuestion(questions.size() + 1, mode, word, difficulty, random));
            }
        }

        while (questions.size() < BULGARIAN_QUESTIONS_PER_TEST) {
            QuizMode mode = pickMode(availableModes, random);
            List<BulgarianWordCatalog.BulgarianWordEntry> words = wordPool(mode, difficulty, random);
            BulgarianWordCatalog.BulgarianWordEntry word = words.get(random.nextInt(words.size()));
            questions.add(wordQuestion(questions.size() + 1, mode, word, difficulty, random));
        }

        return questions;
    }

    private List<BulgarianWordCatalog.BulgarianWordEntry> wordPool(QuizMode mode, int difficulty, ThreadLocalRandom random) {
        List<BulgarianWordCatalog.BulgarianWordEntry> words = activeBulgarianWords();
        List<BulgarianWordCatalog.BulgarianWordEntry> eligibleWords = words.stream()
                .filter(word -> mode != QuizMode.WORD_SYLLABLES || word.hasEnoughSyllablesForSyllableGame())
                .filter(word -> mode != QuizMode.WORD_WRONG_LETTER || word.letterCount() >= 3)
                .toList();
        List<BulgarianWordCatalog.BulgarianWordEntry> exact = eligibleWords.stream()
                .filter(word -> word.level() == difficulty)
                .toList();
        if (exact.size() >= BULGARIAN_QUESTIONS_PER_TEST) {
            return shuffled(exact, random);
        }

        int tolerance = difficulty >= 8 ? 3 : difficulty >= 5 ? 2 : 1;
        List<BulgarianWordCatalog.BulgarianWordEntry> close = eligibleWords.stream()
                .filter(word -> Math.abs(word.level() - difficulty) <= tolerance)
                .toList();
        if (close.size() >= BULGARIAN_QUESTIONS_PER_TEST) {
            return shuffled(close, random);
        }

        return shuffled(eligibleWords, random);
    }

    private List<BulgarianWordCatalog.BulgarianWordEntry> activeBulgarianWords() {
        return wordCatalogService == null ? BulgarianWordCatalog.words() : wordCatalogService.activeBulgarianWords();
    }

    private List<BulgarianWordCatalog.BulgarianWordEntry> shuffled(List<BulgarianWordCatalog.BulgarianWordEntry> words, ThreadLocalRandom random) {
        List<BulgarianWordCatalog.BulgarianWordEntry> copy = new ArrayList<>(words);
        Collections.shuffle(copy, random);
        return copy;
    }

    private GeneratedQuestion wordQuestion(
            int id,
            QuizMode mode,
            BulgarianWordCatalog.BulgarianWordEntry word,
            int difficulty,
            ThreadLocalRandom random
    ) {
        String answer = word.word().toUpperCase(BG);
        return switch (mode) {
            case WORD_LETTERS -> new GeneratedQuestion(
                    id,
                    QuestionKind.LETTER_ORDER,
                    "Подреди буквите",
                    word.image(),
                    answer,
                    letterSlots(word.word()),
                    shuffledTokens(tokensWithDistractors(word.letters(), BULGARIAN_DISTRACTOR_LETTERS, difficulty, random), random),
                    answer,
                    QuizMode.WORD_LETTERS
            );
            case WORD_SYLLABLES -> new GeneratedQuestion(
                    id,
                    QuestionKind.SYLLABLE_ORDER,
                    "Подреди сричките",
                    word.image(),
                    answer,
                    syllableSlots(word.word(), word.syllables()),
                    shuffledTokens(tokensWithDistractors(word.syllables(), BULGARIAN_DISTRACTOR_SYLLABLES, difficulty, random), random),
                    answer,
                    QuizMode.WORD_SYLLABLES
            );
            case WORD_TYPING -> new GeneratedQuestion(
                    id,
                    QuestionKind.WORD_TYPING,
                    "Напиши думата",
                    word.image(),
                    answer,
                    List.of(),
                    List.of(),
                    answer,
                    QuizMode.WORD_TYPING
            );
            case WORD_PICTURE -> new GeneratedQuestion(
                    id,
                    QuestionKind.CHOICE,
                    answer,
                    null,
                    answer,
                    List.of(),
                    pictureChoices(word, difficulty, random),
                    word.image(),
                    QuizMode.WORD_PICTURE
            );
            case WORD_MISSING_LETTER -> missingLetterQuestion(id, word, difficulty, random);
            case WORD_FIRST_LETTER_GROUP -> firstLetterGroupQuestion(id, difficulty, random);
            case WORD_WRONG_LETTER -> wrongLetterQuestion(id, word, random);
            default -> throw new IllegalStateException("Unsupported Bulgarian question mode.");
        };
    }

    private GeneratedQuestion firstLetterGroupQuestion(int id, int difficulty, ThreadLocalRandom random) {
        List<BulgarianWordCatalog.BulgarianWordEntry> pool = wordPool(QuizMode.WORD_FIRST_LETTER_GROUP, difficulty, random);
        Map<String, List<BulgarianWordCatalog.BulgarianWordEntry>> groups = firstLetterGroups(pool);
        if (groups.size() < 3) {
            groups = firstLetterGroups(activeBulgarianWords());
        }
        List<String> letters = groups.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(letters, random);
        if (letters.size() < 3) {
            throw new IllegalStateException("Not enough Bulgarian word groups for first-letter grouping.");
        }

        GroupingSelection selection = groupingSelection(groups, letters, random);
        List<GroupingItem> items = selection.items();
        Collections.shuffle(items, random);
        List<String> baskets = selection.letters().stream().sorted().toList();

        return new GeneratedQuestion(
                id,
                QuestionKind.GROUPING,
                "Групирай по първа буква",
                null,
                null,
                baskets,
                items.stream().map(GroupingItem::choiceValue).toList(),
                groupingAnswer(items),
                QuizMode.WORD_FIRST_LETTER_GROUP
        );
    }

    private GroupingSelection groupingSelection(
            Map<String, List<BulgarianWordCatalog.BulgarianWordEntry>> groups,
            List<String> letters,
            ThreadLocalRandom random
    ) {
        List<List<Integer>> distributions = new ArrayList<>(List.of(
                List.of(3, 3, 0),
                List.of(3, 2, 1),
                List.of(2, 3, 1),
                List.of(1, 2, 3),
                List.of(2, 2, 2)
        ));
        Collections.shuffle(distributions, random);
        for (List<Integer> distribution : distributions) {
            GroupingSelection selection = tryGroupingSelection(groups, letters, distribution, random);
            if (selection != null) {
                return selection;
            }
        }

        List<String> fallbackLetters = letters.stream()
                .filter(letter -> groups.get(letter).size() >= 2)
                .limit(3)
                .toList();
        if (fallbackLetters.size() < 3) {
            throw new IllegalStateException("Not enough Bulgarian word groups for first-letter grouping.");
        }
        List<GroupingItem> items = new ArrayList<>();
        for (String letter : fallbackLetters) {
            List<BulgarianWordCatalog.BulgarianWordEntry> words = new ArrayList<>(groups.get(letter));
            Collections.shuffle(words, random);
            words.stream()
                    .limit(2)
                    .map(word -> new GroupingItem(word.image(), word.word().toUpperCase(BG), letter))
                    .forEach(items::add);
        }
        return new GroupingSelection(fallbackLetters, items);
    }

    private GroupingSelection tryGroupingSelection(
            Map<String, List<BulgarianWordCatalog.BulgarianWordEntry>> groups,
            List<String> letters,
            List<Integer> distribution,
            ThreadLocalRandom random
    ) {
        List<String> candidates = new ArrayList<>(letters);
        Collections.shuffle(candidates, random);
        List<String> selectedLetters = new ArrayList<>();
        List<GroupingItem> items = new ArrayList<>();
        for (int count : distribution) {
            String selectedLetter = candidates.stream()
                    .filter(letter -> !selectedLetters.contains(letter))
                    .filter(letter -> count == 0 || groups.get(letter).size() >= count)
                    .findFirst()
                    .orElse(null);
            if (selectedLetter == null) {
                return null;
            }
            selectedLetters.add(selectedLetter);
            if (count == 0) {
                continue;
            }
            List<BulgarianWordCatalog.BulgarianWordEntry> words = new ArrayList<>(groups.get(selectedLetter));
            Collections.shuffle(words, random);
            words.stream()
                    .limit(count)
                    .map(word -> new GroupingItem(word.image(), word.word().toUpperCase(BG), selectedLetter))
                    .forEach(items::add);
        }
        return items.size() == 6 ? new GroupingSelection(selectedLetters, items) : null;
    }

    private Map<String, List<BulgarianWordCatalog.BulgarianWordEntry>> firstLetterGroups(List<BulgarianWordCatalog.BulgarianWordEntry> words) {
        return words.stream()
                .filter(word -> !word.letters().isEmpty())
                .filter(word -> word.image() != null && !word.image().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        word -> word.letters().get(0),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }

    private String groupingAnswer(List<GroupingItem> items) {
        return items.stream()
                .map(item -> item.word() + "=" + item.letter())
                .sorted()
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private record GroupingItem(String image, String word, String letter) {
        String choiceValue() {
            return image + "|" + word;
        }
    }

    private record GroupingSelection(List<String> letters, List<GroupingItem> items) {
    }

    private GeneratedQuestion missingLetterQuestion(
            int id,
            BulgarianWordCatalog.BulgarianWordEntry word,
            int difficulty,
            ThreadLocalRandom random
    ) {
        String answer = word.word().toUpperCase(BG);
        List<String> letters = word.letters();
        List<Integer> missingIndexes = missingLetterIndexes(letters.size(), difficulty);
        Set<Integer> missingIndexSet = new LinkedHashSet<>(missingIndexes);
        List<String> missingLetters = missingIndexes.stream()
                .map(letters::get)
                .toList();
        String prompt = missingWordPrompt(answer, missingIndexSet);

        return new GeneratedQuestion(
                id,
                QuestionKind.LETTER_ORDER,
                prompt,
                word.image(),
                answer,
                missingLetters,
                missingLetterChoices(missingLetters, random),
                String.join("", missingLetters),
                QuizMode.WORD_MISSING_LETTER
        );
    }

    private List<Integer> missingLetterIndexes(int letterCount, int difficulty) {
        if (letterCount <= 1) {
            return List.of(0);
        }
        if (difficulty <= 1) {
            return List.of(0);
        }
        if (difficulty == 2) {
            return List.of(letterCount - 1);
        }
        if (difficulty == 3) {
            return List.of(Math.max(1, Math.min(letterCount - 2, letterCount / 2)));
        }
        if (letterCount >= 4) {
            return List.of(1, letterCount - 2);
        }
        return List.of(0, letterCount - 1);
    }

    private String missingWordPrompt(String word, Set<Integer> missingIndexes) {
        StringBuilder prompt = new StringBuilder();
        int letterIndex = 0;
        int[] codePoints = word.codePoints().toArray();
        for (int codePoint : codePoints) {
            if (Character.isWhitespace(codePoint) || codePoint == '-') {
                prompt.appendCodePoint(codePoint);
                continue;
            }
            prompt.append(missingIndexes.contains(letterIndex) ? "_" : new String(Character.toChars(codePoint)));
            letterIndex++;
        }
        return prompt.toString();
    }

    private List<String> missingLetterChoices(List<String> missingLetters, ThreadLocalRandom random) {
        Set<String> missingSet = missingLetters.stream()
                .map(letter -> letter.toUpperCase(BG))
                .collect(java.util.stream.Collectors.toSet());
        List<String> distractors = BULGARIAN_DISTRACTOR_LETTERS.stream()
                .filter(letter -> !missingSet.contains(letter.toUpperCase(BG)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors, random);

        List<String> choices = new ArrayList<>(missingLetters);
        choices.addAll(distractors.stream().limit(Math.max(1, 3 - missingLetters.size())).toList());
        while (choices.size() < 3 && !distractors.isEmpty()) {
            choices.add(distractors.get(random.nextInt(distractors.size())));
        }
        return shuffledTokens(choices, random);
    }

    private List<String> pictureChoices(
            BulgarianWordCatalog.BulgarianWordEntry word,
            int difficulty,
            ThreadLocalRandom random
    ) {
        List<BulgarianWordCatalog.BulgarianWordEntry> candidates = new ArrayList<>(wordPool(QuizMode.WORD_PICTURE, difficulty, random));
        candidates.addAll(activeBulgarianWords());
        Collections.shuffle(candidates, random);

        Set<String> choices = new LinkedHashSet<>();
        choices.add(word.image());
        for (BulgarianWordCatalog.BulgarianWordEntry candidate : candidates) {
            if (choices.size() >= 3) {
                break;
            }
            if (candidate.normalizedWord().equals(word.normalizedWord()) || candidate.image().equals(word.image())) {
                continue;
            }
            choices.add(candidate.image());
        }

        if (choices.size() < 3) {
            throw new IllegalStateException("Not enough unique pictures for Bulgarian picture choices.");
        }
        return shuffledTokens(new ArrayList<>(choices), random);
    }

    private GeneratedQuestion wrongLetterQuestion(
            int id,
            BulgarianWordCatalog.BulgarianWordEntry word,
            ThreadLocalRandom random
    ) {
        String answer = word.word().toUpperCase(BG);
        List<String> letters = word.letters();
        int wrongIndex = random.nextInt(letters.size());
        String expectedLetter = letters.get(wrongIndex).toUpperCase(BG);
        String wrongLetter = wrongLetterFor(expectedLetter, letters, random);
        String prompt = wordWithWrongLetter(answer, wrongIndex, wrongLetter);

        return new GeneratedQuestion(
                id,
                QuestionKind.CHOICE,
                prompt,
                word.image(),
                answer,
                List.of(),
                replacementLetterChoices(expectedLetter, wrongLetter, random),
                wrongLetter + "=" + expectedLetter,
                QuizMode.WORD_WRONG_LETTER
        );
    }

    private String wrongLetterFor(String expectedLetter, List<String> answerLetters, ThreadLocalRandom random) {
        Set<String> answerSet = answerLetters.stream()
                .map(letter -> letter.toUpperCase(BG))
                .collect(java.util.stream.Collectors.toSet());
        List<String> candidates = BULGARIAN_DISTRACTOR_LETTERS.stream()
                .filter(letter -> !letter.equals(expectedLetter))
                .filter(letter -> !answerSet.contains(letter))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (candidates.isEmpty()) {
            candidates = BULGARIAN_DISTRACTOR_LETTERS.stream()
                    .filter(letter -> !letter.equals(expectedLetter))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private String wordWithWrongLetter(String word, int wrongLetterIndex, String wrongLetter) {
        StringBuilder prompt = new StringBuilder();
        int letterIndex = 0;
        int[] codePoints = word.codePoints().toArray();
        for (int codePoint : codePoints) {
            if (Character.isWhitespace(codePoint) || codePoint == '-') {
                prompt.appendCodePoint(codePoint);
                continue;
            }
            prompt.append(letterIndex == wrongLetterIndex ? wrongLetter : new String(Character.toChars(codePoint)));
            letterIndex++;
        }
        return prompt.toString();
    }

    private List<String> replacementLetterChoices(String expectedLetter, String wrongLetter, ThreadLocalRandom random) {
        List<String> distractors = BULGARIAN_DISTRACTOR_LETTERS.stream()
                .filter(letter -> !letter.equals(expectedLetter))
                .filter(letter -> !letter.equals(wrongLetter))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(distractors, random);
        List<String> choices = new ArrayList<>();
        choices.add(expectedLetter);
        choices.addAll(distractors.stream().limit(2).toList());
        return shuffledTokens(choices, random);
    }

    private List<String> letterSlots(String word) {
        List<String> slots = new ArrayList<>();
        word.toUpperCase(BG).codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint) || codePoint == '-') {
                addGap(slots);
                return;
            }
            slots.add(new String(Character.toChars(codePoint)));
        });
        return slots;
    }

    private List<String> syllableSlots(String word, List<String> syllables) {
        String[] parts = word.toUpperCase(BG).split("[\\s-]+");
        if (parts.length <= 1) {
            return syllables;
        }

        List<String> slots = new ArrayList<>();
        int syllableIndex = 0;
        for (int partIndex = 0; partIndex < parts.length; partIndex++) {
            String part = parts[partIndex];
            if (part.isBlank()) {
                continue;
            }
            StringBuilder joined = new StringBuilder();
            while (syllableIndex < syllables.size() && joined.length() < part.length()) {
                String syllable = syllables.get(syllableIndex++);
                slots.add(syllable);
                joined.append(syllable);
            }
            if (!joined.toString().equals(part)) {
                return syllables;
            }
            if (partIndex < parts.length - 1) {
                addGap(slots);
            }
        }
        return slots;
    }

    private void addGap(List<String> slots) {
        if (!slots.isEmpty() && !" ".equals(slots.get(slots.size() - 1))) {
            slots.add(" ");
        }
    }

    private List<String> tokensWithDistractors(
            List<String> answerTokens,
            List<String> distractorPool,
            int difficulty,
            ThreadLocalRandom random
    ) {
        int distractorCount = distractorCount(difficulty);
        if (distractorCount == 0) {
            return answerTokens;
        }

        Set<String> answerSet = answerTokens.stream()
                .map(token -> token.toUpperCase(BG))
                .collect(java.util.stream.Collectors.toSet());
        List<String> availableDistractors = distractorPool.stream()
                .filter(token -> !answerSet.contains(token.toUpperCase(BG)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(availableDistractors, random);

        List<String> choices = new ArrayList<>(answerTokens);
        choices.addAll(availableDistractors.stream().limit(distractorCount).toList());
        return choices;
    }

    private int distractorCount(int difficulty) {
        if (difficulty >= 8) {
            return 3;
        }
        if (difficulty >= 6) {
            return 2;
        }
        if (difficulty >= 4) {
            return 1;
        }
        return 0;
    }

    private List<String> shuffledTokens(List<String> tokens, ThreadLocalRandom random) {
        List<String> shuffled = new ArrayList<>(tokens);
        Collections.shuffle(shuffled, random);
        if (shuffled.size() > 1 && shuffled.equals(tokens)) {
            Collections.rotate(shuffled, 1);
        }
        return shuffled;
    }

    private List<GeneratedQuestion> generateLogic(Collection<QuizMode> modes, int difficulty) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<QuizMode> availableModes = modes.stream()
                .filter(LOGIC_PRIMITIVE_MODES::contains)
                .distinct()
                .toList();
        if (availableModes.isEmpty()) {
            throw new IllegalArgumentException("At least one logic question category is required.");
        }
        if (availableModes.size() == 1 && availableModes.contains(QuizMode.MEMORY_PAIRS)) {
            return List.of(memoryPairsQuestion(1, difficulty, random));
        }

        List<GeneratedQuestion> questions = new ArrayList<>();
        while (questions.size() < LOGIC_QUESTIONS_PER_TEST) {
            QuizMode mode = pickMode(availableModes, random);
            if (mode == QuizMode.SPOT_DIFFERENCES) {
                questions.add(spotDifferencesQuestion(questions.size() + 1, difficulty, random));
            } else if (mode == QuizMode.FIND_OBJECT) {
                questions.add(findObjectQuestion(questions.size() + 1, difficulty, random));
            } else if (mode == QuizMode.MEMORY_PAIRS) {
                questions.add(memoryPairsQuestion(questions.size() + 1, difficulty, random));
            } else if (mode == QuizMode.PATTERN_SEQUENCE) {
                questions.add(patternSequenceQuestion(questions.size() + 1, difficulty, random));
            }
        }
        return questions;
    }

    private GeneratedQuestion patternSequenceQuestion(int id, int difficulty, ThreadLocalRandom random) {
        PatternLevel level = patternLevel(difficulty);
        int sequenceLength = level.sequenceLength();
        int previewSeconds = level.previewSeconds();
        List<PatternToken> tokenPool = patternTokenPool(level);
        List<PatternToken> sequence = patternSequence(tokenPool, level, random);

        Set<String> usedIds = sequence.stream()
                .map(PatternToken::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<PatternToken> choices = new ArrayList<>(tokenPool.stream()
                .filter(token -> usedIds.contains(token.id()))
                .toList());
        Collections.shuffle(choices, random);

        List<String> slots = new ArrayList<>();
        slots.add("P|" + previewSeconds + "|" + sequenceLength);
        sequence.forEach(token -> slots.add(patternDescriptor("S", token)));
        List<String> choiceDescriptors = choices.stream()
                .map(token -> patternDescriptor("T", token))
                .toList();

        String answer = sequence.stream()
                .map(PatternToken::id)
                .collect(java.util.stream.Collectors.joining(","));
        return new GeneratedQuestion(
                id,
                QuestionKind.PATTERN_SEQUENCE,
                "Подреди модела",
                null,
                "Запомни поредицата и я подреди в същия ред.",
                slots,
                choiceDescriptors,
                answer,
                QuizMode.PATTERN_SEQUENCE
        );
    }

    private String patternDescriptor(String prefix, PatternToken token) {
        return String.join(
                "|",
                prefix,
                token.id(),
                token.shape(),
                token.color(),
                token.label()
        );
    }

    private PatternLevel patternLevel(int difficulty) {
        return switch (Math.max(1, Math.min(10, difficulty))) {
            case 1 -> new PatternLevel(3, 5, 3, 1, false, 0);
            case 2 -> new PatternLevel(4, 8, 4, 1, true, 1);
            case 3 -> new PatternLevel(5, 10, 5, 1, true, 1);
            case 4 -> new PatternLevel(6, 12, 6, 1, true, 2);
            case 5 -> new PatternLevel(6, 15, 6, 2, false, 0);
            case 6 -> new PatternLevel(6, 15, 6, 2, true, Integer.MAX_VALUE);
            case 7 -> new PatternLevel(7, 18, 7, 2, false, 0);
            case 8 -> new PatternLevel(7, 20, 7, 2, true, Integer.MAX_VALUE);
            case 9 -> new PatternLevel(8, 25, 8, 3, false, 0);
            default -> new PatternLevel(8, 25, 8, 3, true, Integer.MAX_VALUE);
        };
    }

    private List<PatternToken> patternSequence(List<PatternToken> tokenPool, PatternLevel level, ThreadLocalRandom random) {
        if (!level.allowRepeats()) {
            return uniquePatternSequence(tokenPool, level, random);
        }

        if (level.maxRepeatedTokens() != Integer.MAX_VALUE) {
            List<PatternToken> shuffled = new ArrayList<>(tokenPool);
            Collections.shuffle(shuffled, random);
            int maxDuplicates = Math.min(level.maxRepeatedTokens(), level.sequenceLength() / 2);
            int duplicateCount = maxDuplicates == 0 ? 0 : random.nextInt(maxDuplicates + 1);
            int uniqueCount = level.sequenceLength() - duplicateCount;
            List<PatternToken> sequence = new ArrayList<>(shuffled.subList(0, uniqueCount));
            List<PatternToken> repeatCandidates = new ArrayList<>(sequence);
            Collections.shuffle(repeatCandidates, random);
            sequence.addAll(repeatCandidates.stream().limit(duplicateCount).toList());
            Collections.shuffle(sequence, random);
            return sequence;
        }

        List<PatternToken> sequence = new ArrayList<>();
        if (level.shapeCount() > 1) {
            sequence.addAll(oneTokenPerShape(tokenPool, random));
        }
        while (sequence.size() < level.sequenceLength()) {
            PatternToken candidate = tokenPool.get(random.nextInt(tokenPool.size()));
            if (sequence.size() >= 2
                    && sequence.get(sequence.size() - 1).id().equals(candidate.id())
                    && sequence.get(sequence.size() - 2).id().equals(candidate.id())) {
                continue;
            }
            sequence.add(candidate);
        }
        return sequence;
    }

    private List<PatternToken> uniquePatternSequence(List<PatternToken> tokenPool, PatternLevel level, ThreadLocalRandom random) {
        List<PatternToken> sequence = new ArrayList<>();
        if (level.shapeCount() > 1) {
            sequence.addAll(oneTokenPerShape(tokenPool, random));
        }
        List<PatternToken> remaining = new ArrayList<>(tokenPool.stream()
                .filter(token -> sequence.stream().noneMatch(selected -> selected.id().equals(token.id())))
                .toList());
        Collections.shuffle(remaining, random);
        sequence.addAll(remaining.stream()
                .limit(Math.max(0, level.sequenceLength() - sequence.size()))
                .toList());
        Collections.shuffle(sequence, random);
        return sequence;
    }

    private List<PatternToken> oneTokenPerShape(List<PatternToken> tokenPool, ThreadLocalRandom random) {
        return tokenPool.stream()
                .map(PatternToken::shape)
                .distinct()
                .map(shape -> {
                    List<PatternToken> tokensForShape = tokenPool.stream()
                            .filter(token -> token.shape().equals(shape))
                            .toList();
                    return tokensForShape.get(random.nextInt(tokensForShape.size()));
                })
                .toList();
    }

    private List<PatternToken> patternTokenPool(PatternLevel level) {
        List<PatternColor> colors = List.of(
                new PatternColor("red", "#ef4444", "червен"),
                new PatternColor("yellow", "#f5c542", "жълт"),
                new PatternColor("green", "#22a06b", "зелен"),
                new PatternColor("blue", "#3b82f6", "син"),
                new PatternColor("purple", "#8b5cf6", "лилав"),
                new PatternColor("orange", "#f97316", "оранжев"),
                new PatternColor("pink", "#ec4899", "розов"),
                new PatternColor("cyan", "#06b6d4", "тюркоазен")
        );
        List<PatternShape> shapes = List.of(
                new PatternShape("circle", "кръг"),
                new PatternShape("square", "квадрат"),
                new PatternShape("triangle", "триъгълник")
        );
        List<PatternToken> tokens = new ArrayList<>();
        for (PatternShape shape : shapes.stream().limit(level.shapeCount()).toList()) {
            for (PatternColor color : colors.stream().limit(level.colorCount()).toList()) {
                tokens.add(new PatternToken(
                        color.id() + "-" + shape.id(),
                        shape.id(),
                        color.hex(),
                        color.label() + " " + shape.label()
                ));
            }
        }
        return tokens;
    }

    private GeneratedQuestion memoryPairsQuestion(int id, int difficulty, ThreadLocalRandom random) {
        int pairCount = memoryPairCount(difficulty);
        List<MemoryPairItem> pool = new ArrayList<>(memoryPairItems());
        Collections.shuffle(pool, random);
        List<MemoryPairItem> selectedPairs = pool.stream().limit(pairCount).toList();

        List<String> descriptors = new ArrayList<>();
        descriptors.add("M|" + memoryPreviewSeconds(difficulty) + "|" + pairCount + "|" + memoryPerfectMistakes(difficulty));
        List<MemoryCard> cards = new ArrayList<>();
        for (int pairIndex = 0; pairIndex < selectedPairs.size(); pairIndex++) {
            MemoryPairItem item = selectedPairs.get(pairIndex);
            String pairId = "P" + (pairIndex + 1);
            cards.add(new MemoryCard(pairId + "A", pairId, item.emoji(), item.label()));
            cards.add(new MemoryCard(pairId + "B", pairId, item.emoji(), item.label()));
        }
        Collections.shuffle(cards, random);
        cards.forEach(card -> descriptors.add(String.join(
                "|",
                "C",
                card.cardId(),
                card.pairId(),
                card.emoji(),
                card.label()
        )));

        return new GeneratedQuestion(
                id,
                QuestionKind.MEMORY_PAIRS,
                "Намери двойките",
                null,
                "Запомни картите и отвори еднаквите двойки.",
                descriptors,
                List.of(),
                "SOLVED",
                QuizMode.MEMORY_PAIRS
        );
    }

    private int memoryPairCount(int difficulty) {
        return Math.min(12, Math.max(3, difficulty + 2));
    }

    private int memoryPreviewSeconds(int difficulty) {
        if (difficulty >= 9) {
            return 15;
        }
        if (difficulty >= 7) {
            return 12;
        }
        if (difficulty >= 5) {
            return 10;
        }
        return 7;
    }

    private int memoryPerfectMistakes(int difficulty) {
        if (difficulty >= 9) {
            return 5;
        }
        if (difficulty >= 7) {
            return 4;
        }
        if (difficulty >= 5) {
            return 3;
        }
        if (difficulty >= 3) {
            return 2;
        }
        return 1;
    }

    private List<MemoryPairItem> memoryPairItems() {
        return List.of(
                new MemoryPairItem("cat", "🐱", "котка"),
                new MemoryPairItem("dog", "🐶", "куче"),
                new MemoryPairItem("mouse", "🐭", "мишка"),
                new MemoryPairItem("rabbit", "🐰", "зайче"),
                new MemoryPairItem("bear", "🐻", "мече"),
                new MemoryPairItem("fox", "🦊", "лисица"),
                new MemoryPairItem("lion", "🦁", "лъв"),
                new MemoryPairItem("panda", "🐼", "панда"),
                new MemoryPairItem("monkey", "🐵", "маймунка"),
                new MemoryPairItem("frog", "🐸", "жаба"),
                new MemoryPairItem("duckling", "🐤", "пате"),
                new MemoryPairItem("fish", "🐟", "риба"),
                new MemoryPairItem("whale", "🐳", "кит"),
                new MemoryPairItem("butterfly", "🦋", "пеперуда"),
                new MemoryPairItem("ladybug", "🐞", "калинка"),
                new MemoryPairItem("bee", "🐝", "пчела"),
                new MemoryPairItem("apple", "🍎", "ябълка"),
                new MemoryPairItem("banana", "🍌", "банан"),
                new MemoryPairItem("strawberry", "🍓", "ягода"),
                new MemoryPairItem("grapes", "🍇", "грозде"),
                new MemoryPairItem("watermelon", "🍉", "диня"),
                new MemoryPairItem("carrot", "🥕", "морков"),
                new MemoryPairItem("car", "🚗", "кола"),
                new MemoryPairItem("bus", "🚌", "автобус"),
                new MemoryPairItem("train", "🚂", "влак"),
                new MemoryPairItem("ball", "⚽", "топка"),
                new MemoryPairItem("star", "⭐", "звезда"),
                new MemoryPairItem("heart", "💙", "сърце"),
                new MemoryPairItem("gift", "🎁", "подарък"),
                new MemoryPairItem("book", "📘", "книга"),
                new MemoryPairItem("puzzle", "🧩", "пъзел"),
                new MemoryPairItem("key", "🔑", "ключ")
        );
    }

    private GeneratedQuestion findObjectQuestion(int id, int difficulty, ThreadLocalRandom random) {
        FindObjectScene scene = findObjectScene();
        List<FindObjectItem> available = scene.objects().stream()
                .filter(object -> object.minDifficulty() <= difficulty)
                .toList();
        List<FindObjectItem> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);
        FindObjectItem target = shuffled.getFirst();
        List<FindObjectItem> visible = findObjectVisibleItems(target, available, difficulty, random);

        List<String> descriptors = new ArrayList<>();
        descriptors.add("F|" + scene.name() + "|" + scene.theme());
        visible.forEach(object -> descriptors.add(findObjectDescriptor(object)));

        return new GeneratedQuestion(
                id,
                QuestionKind.CHOICE,
                "Намери предмета.",
                null,
                target.label(),
                descriptors,
                List.of(),
                target.id(),
                QuizMode.FIND_OBJECT
        );
    }

    private int findObjectCount(int difficulty) {
        if (difficulty >= 9) {
            return 60;
        }
        if (difficulty >= 7) {
            return 48;
        }
        if (difficulty >= 5) {
            return 38;
        }
        if (difficulty >= 3) {
            return 30;
        }
        return 22;
    }

    private List<FindObjectItem> findObjectVisibleItems(
            FindObjectItem target,
            List<FindObjectItem> available,
            int difficulty,
            ThreadLocalRandom random
    ) {
        int visibleCount = findObjectCount(difficulty);
        List<FindObjectPlacement> placements = findObjectPlacements(random);
        int sizeLimit = findObjectSizeLimit(difficulty);
        List<FindObjectItem> distractors = available.stream()
                .filter(object -> !object.id().equals(target.id()))
                .toList();
        List<FindObjectItem> shuffledDistractors = new ArrayList<>(distractors);
        Collections.shuffle(shuffledDistractors, random);

        List<FindObjectItem> visible = new ArrayList<>();
        Set<String> usedIds = new LinkedHashSet<>();
        FindObjectPlacement targetPlacement = placements.remove(random.nextInt(Math.min(placements.size(), visibleCount)));
        visible.add(findObjectAt(target, target.id(), targetPlacement, Math.min(target.size(), sizeLimit)));
        usedIds.add(target.id());

        int distractorIndex = 0;
        while (visible.size() < visibleCount && !placements.isEmpty()) {
            FindObjectItem base = shuffledDistractors.get(distractorIndex % shuffledDistractors.size());
            int copyNumber = distractorIndex / shuffledDistractors.size();
            String visibleId = copyNumber == 0 ? base.id() : base.id() + "-copy-" + copyNumber;
            while (usedIds.contains(visibleId)) {
                copyNumber++;
                visibleId = base.id() + "-copy-" + copyNumber;
            }
            FindObjectPlacement placement = placements.removeFirst();
            int size = Math.max(24, Math.min(base.size(), sizeLimit) - random.nextInt(0, difficulty >= 7 ? 6 : 3));
            visible.add(findObjectAt(base, visibleId, placement, size));
            usedIds.add(visibleId);
            distractorIndex++;
        }

        visible.sort((left, right) -> {
            int byY = Integer.compare(left.y(), right.y());
            return byY != 0 ? byY : Integer.compare(left.x(), right.x());
        });
        return visible;
    }

    private int findObjectSizeLimit(int difficulty) {
        if (difficulty >= 9) {
            return 33;
        }
        if (difficulty >= 7) {
            return 35;
        }
        if (difficulty >= 5) {
            return 38;
        }
        if (difficulty >= 3) {
            return 40;
        }
        return 44;
    }

    private List<FindObjectPlacement> findObjectPlacements(ThreadLocalRandom random) {
        int[] xs = {7, 13, 19, 25, 31, 37, 43, 49, 55, 61, 67, 73, 79, 85, 91};
        int[] ys = {18, 27, 36, 45, 54, 63, 72, 80, 88, 94};
        List<FindObjectPlacement> placements = new ArrayList<>();
        for (int y : ys) {
            for (int x : xs) {
                if (y == 18 && x > 37 && x < 61) {
                    continue;
                }
                int jitterX = random.nextInt(-2, 3);
                int jitterY = random.nextInt(-2, 3);
                placements.add(new FindObjectPlacement(
                        Math.max(5, Math.min(95, x + jitterX)),
                        Math.max(12, Math.min(95, y + jitterY))
                ));
            }
        }
        Collections.shuffle(placements, random);
        return placements;
    }

    private FindObjectItem findObjectAt(FindObjectItem item, String id, FindObjectPlacement placement, int size) {
        return new FindObjectItem(
                id,
                item.emoji(),
                item.label(),
                item.targetText(),
                placement.x(),
                placement.y(),
                size,
                item.minDifficulty()
        );
    }

    private String findObjectDescriptor(FindObjectItem object) {
        return String.join(
                "|",
                "I",
                object.id(),
                String.valueOf(object.x()),
                String.valueOf(object.y()),
                object.emoji(),
                String.valueOf(object.size()),
                object.label(),
                object.targetText()
        );
    }

    private GeneratedQuestion spotDifferencesQuestion(int id, int difficulty, ThreadLocalRandom random) {
        List<SpotSceneTemplate> scenes = spotScenes();
        SpotSceneTemplate scene = scenes.get(Math.floorMod(difficulty - 1, scenes.size()));
        int differenceCount = spotDifferenceCount(difficulty);
        List<SpotObject> shuffledObjects = new ArrayList<>(scene.objects());
        Collections.shuffle(shuffledObjects, random);
        Set<String> differenceObjectIds = shuffledObjects.stream()
                .limit(differenceCount)
                .map(SpotObject::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<String> descriptors = new ArrayList<>();
        descriptors.add("S|" + scene.name() + "|" + scene.theme());
        List<String> answers = new ArrayList<>();
        int differenceIndex = 1;
        for (SpotObject object : scene.objects()) {
            if (!differenceObjectIds.contains(object.id())) {
                descriptors.add(spotDescriptor("L", object, "", object.emoji(), object.x(), object.y(), object.size()));
                descriptors.add(spotDescriptor("R", object, "", object.emoji(), object.x(), object.y(), object.size()));
                continue;
            }

            String differenceId = "D" + String.format("%02d", differenceIndex++);
            answers.add(differenceId);
            addSpotDifference(descriptors, object, differenceId, difficulty, random);
        }

        Collections.sort(answers);
        return new GeneratedQuestion(
                id,
                QuestionKind.SPOT_DIFFERENCES,
                "Открий " + differenceCount + " разлики",
                null,
                null,
                descriptors,
                List.of(),
                String.join(";", answers),
                QuizMode.SPOT_DIFFERENCES
        );
    }

    private int spotDifferenceCount(int difficulty) {
        if (difficulty >= 9) {
            return 7;
        }
        if (difficulty >= 7) {
            return 6;
        }
        if (difficulty >= 4) {
            return 5;
        }
        if (difficulty >= 2) {
            return 4;
        }
        return 3;
    }

    private void addSpotDifference(
            List<String> descriptors,
            SpotObject object,
            String differenceId,
            int difficulty,
            ThreadLocalRandom random
    ) {
        SpotDifferenceType type = pickSpotDifferenceType(difficulty, random);
        if (type == SpotDifferenceType.COUNT && !isCountableSpotObject(object)) {
            type = SpotDifferenceType.DETAIL;
        }
        switch (type) {
            case DETAIL -> {
                descriptors.add(spotDescriptor("L", object, differenceId, object.emoji(), object.x(), object.y(), object.size()));
                descriptors.add(spotDescriptor("R", object, differenceId, alternateSpotEmoji(object.emoji()), object.x(), object.y(), object.size()));
            }
            case POSITION -> {
                descriptors.add(spotDescriptor("L", object, differenceId, object.emoji(), object.x(), object.y(), object.size()));
                int movedX = clampPercent(object.x() + randomOffset(random, difficulty));
                int movedY = clampPercent(object.y() + randomOffset(random, difficulty));
                descriptors.add(spotDescriptor("R", object, differenceId, object.emoji(), movedX, movedY, object.size()));
            }
            case SIZE -> {
                int changedSize = Math.max(26, object.size() + (random.nextBoolean() ? 1 : -1) * sizeOffset(difficulty));
                descriptors.add(spotDescriptor("L", object, differenceId, object.emoji(), object.x(), object.y(), object.size()));
                descriptors.add(spotDescriptor("R", object, differenceId, object.emoji(), object.x(), object.y(), changedSize));
            }
            case COUNT -> addCountDifference(descriptors, object, differenceId, random);
            case MISSING -> {
                if (random.nextBoolean()) {
                    descriptors.add(spotDescriptor("L", object, differenceId, object.emoji(), object.x(), object.y(), object.size()));
                } else {
                    descriptors.add(spotDescriptor("R", object, differenceId, object.emoji(), object.x(), object.y(), object.size()));
                }
            }
        }
    }

    private SpotDifferenceType pickSpotDifferenceType(int difficulty, ThreadLocalRandom random) {
        List<SpotDifferenceType> types;
        if (difficulty <= 1) {
            types = List.of(SpotDifferenceType.MISSING, SpotDifferenceType.DETAIL, SpotDifferenceType.COUNT);
        } else if (difficulty <= 3) {
            types = List.of(SpotDifferenceType.MISSING, SpotDifferenceType.DETAIL, SpotDifferenceType.COUNT, SpotDifferenceType.POSITION);
        } else if (difficulty <= 6) {
            types = List.of(SpotDifferenceType.DETAIL, SpotDifferenceType.COUNT, SpotDifferenceType.SIZE, SpotDifferenceType.POSITION);
        } else {
            types = List.of(SpotDifferenceType.DETAIL, SpotDifferenceType.COUNT, SpotDifferenceType.SIZE, SpotDifferenceType.POSITION);
        }
        return types.get(random.nextInt(types.size()));
    }

    private boolean isCountableSpotObject(SpotObject object) {
        String id = object.id();
        return !Set.of(
                "sun",
                "house",
                "lamp",
                "clock",
                "window",
                "umbrella",
                "boat",
                "tractor",
                "palm",
                "bench",
                "slide",
                "sandcastle",
                "bed",
                "chair"
        ).contains(id);
    }

    private void addCountDifference(
            List<String> descriptors,
            SpotObject object,
            String differenceId,
            ThreadLocalRandom random
    ) {
        int smallerCount = random.nextBoolean() ? 2 : 3;
        int largerCount = smallerCount + 1;
        boolean leftHasMore = random.nextBoolean();
        int leftCount = leftHasMore ? largerCount : smallerCount;
        int rightCount = leftHasMore ? smallerCount : largerCount;
        addSpotCluster(descriptors, "L", object, differenceId, leftCount);
        addSpotCluster(descriptors, "R", object, differenceId, rightCount);
    }

    private void addSpotCluster(
            List<String> descriptors,
            String side,
            SpotObject object,
            String differenceId,
            int count
    ) {
        List<int[]> offsets = List.of(
                new int[]{-6, -2},
                new int[]{6, -2},
                new int[]{-3, 6},
                new int[]{7, 6}
        );
        int size = Math.max(28, object.size() - 8);
        for (int index = 0; index < count; index++) {
            int[] offset = offsets.get(index % offsets.size());
            descriptors.add(spotDescriptor(
                    side,
                    object.id() + "-count-" + index,
                    differenceId,
                    object.emoji(),
                    clampPercent(object.x() + offset[0]),
                    clampPercent(object.y() + offset[1]),
                    size
            ));
        }
    }

    private int randomOffset(ThreadLocalRandom random, int difficulty) {
        int min = difficulty >= 7 ? 6 : difficulty >= 4 ? 8 : 12;
        int max = difficulty >= 7 ? 11 : difficulty >= 4 ? 14 : 20;
        int offset = random.nextInt(min, max);
        return random.nextBoolean() ? offset : -offset;
    }

    private int sizeOffset(int difficulty) {
        if (difficulty >= 7) {
            return 8;
        }
        if (difficulty >= 4) {
            return 11;
        }
        return 14;
    }

    private int clampPercent(int value) {
        return Math.max(10, Math.min(90, value));
    }

    private String spotDescriptor(String side, SpotObject object, String differenceId, String emoji, int x, int y, int size) {
        return spotDescriptor(side, object.id(), differenceId, emoji, x, y, size);
    }

    private String spotDescriptor(String side, String objectId, String differenceId, String emoji, int x, int y, int size) {
        return String.join(
                "|",
                "O",
                side,
                objectId,
                String.valueOf(x),
                String.valueOf(y),
                emoji,
                String.valueOf(size),
                differenceId
        );
    }

    private String alternateSpotEmoji(String emoji) {
        return switch (emoji) {
            case "☀️" -> "⭐";
            case "☁️" -> "🌧️";
            case "🌳" -> "🌲";
            case "🌷" -> "🌻";
            case "🌼" -> "🌺";
            case "⚽" -> "🏀";
            case "🦋" -> "🐝";
            case "🏠" -> "🏡";
            case "🐦" -> "🐤";
            case "🪁" -> "🎈";
            case "🐕" -> "🐈";
            case "🍄" -> "🌰";
            case "🪑" -> "🛋️";
            case "🛝" -> "🎠";
            case "🍂" -> "🍃";
            case "⛵" -> "🚤";
            case "🐟" -> "🐠";
            case "🐠" -> "🐡";
            case "🦀" -> "⭐";
            case "🐚" -> "🪨";
            case "🪣" -> "🧺";
            case "🏖️" -> "☂️";
            case "🟨" -> "🟦";
            case "🌴" -> "🌳";
            case "🏰" -> "⛱️";
            case "🥤" -> "🧃";
            case "🕶️" -> "👓";
            case "🏐" -> "⚽";
            case "🍦" -> "🍧";
            case "💡" -> "🕯️";
            case "⏰" -> "🕰️";
            case "🪟" -> "🖼️";
            case "📘" -> "📕";
            case "✏️" -> "🖍️";
            case "🧸" -> "🤖";
            case "🪴" -> "🌵";
            case "🛏️" -> "🛋️";
            case "🎲" -> "🧸";
            case "🧩" -> "🎲";
            case "🖍️" -> "✏️";
            case "🚗" -> "🚕";
            case "🎈" -> "🎀";
            case "🟦" -> "🟩";
            case "🍎" -> "🍐";
            case "🐄" -> "🐑";
            case "🐔" -> "🐥";
            case "🐥" -> "🐤";
            case "🚜" -> "🚚";
            case "🥕" -> "🌽";
            case "🌽" -> "🥕";
            case "🐑" -> "🐐";
            case "🐖" -> "🐄";
            case "🐝" -> "🦋";
            case "🧺" -> "🪣";
            default -> "⭐";
        };
    }

    private List<SpotSceneTemplate> spotScenes() {
        return List.of(
                new SpotSceneTemplate(
                        "Парк",
                        "park",
                        List.of(
                                new SpotObject("sun", "☀️", 84, 16, 52),
                                new SpotObject("cloud", "☁️", 22, 18, 52),
                                new SpotObject("small-cloud", "☁️", 48, 14, 34),
                                new SpotObject("tree", "🌳", 15, 62, 58),
                                new SpotObject("tree-two", "🌲", 88, 60, 50),
                                new SpotObject("flower", "🌷", 35, 72, 42),
                                new SpotObject("flower-two", "🌼", 46, 70, 34),
                                new SpotObject("ball", "⚽", 62, 75, 42),
                                new SpotObject("butterfly", "🦋", 52, 32, 38),
                                new SpotObject("house", "🏠", 78, 62, 54),
                                new SpotObject("bird", "🐦", 35, 38, 34),
                                new SpotObject("apple", "🍎", 24, 82, 34),
                                new SpotObject("kite", "🪁", 68, 28, 40),
                                new SpotObject("dog", "🐕", 50, 82, 40),
                                new SpotObject("mushroom", "🍄", 12, 84, 34),
                                new SpotObject("bench", "🪑", 74, 82, 38),
                                new SpotObject("slide", "🛝", 26, 54, 44),
                                new SpotObject("leaf", "🍂", 58, 88, 30)
                        )
                ),
                new SpotSceneTemplate(
                        "Плаж",
                        "beach",
                        List.of(
                                new SpotObject("sun", "☀️", 82, 16, 52),
                                new SpotObject("cloud", "☁️", 28, 16, 50),
                                new SpotObject("small-cloud", "☁️", 50, 21, 34),
                                new SpotObject("umbrella", "🏖️", 24, 62, 58),
                                new SpotObject("towel", "🟨", 18, 82, 38),
                                new SpotObject("boat", "⛵", 64, 52, 52),
                                new SpotObject("fish", "🐟", 47, 76, 40),
                                new SpotObject("fish-two", "🐠", 57, 82, 34),
                                new SpotObject("crab", "🦀", 78, 78, 42),
                                new SpotObject("shell", "🐚", 36, 84, 34),
                                new SpotObject("bucket", "🪣", 14, 82, 38),
                                new SpotObject("starfish", "⭐", 72, 86, 34),
                                new SpotObject("palm", "🌴", 88, 64, 54),
                                new SpotObject("sandcastle", "🏰", 50, 88, 40),
                                new SpotObject("drink", "🥤", 28, 84, 34),
                                new SpotObject("glasses", "🕶️", 40, 66, 34),
                                new SpotObject("beach-ball", "🏐", 68, 72, 38),
                                new SpotObject("icecream", "🍦", 86, 84, 34)
                        )
                ),
                new SpotSceneTemplate(
                        "Стая",
                        "room",
                        List.of(
                                new SpotObject("lamp", "💡", 17, 22, 42),
                                new SpotObject("clock", "⏰", 77, 22, 44),
                                new SpotObject("window", "🪟", 48, 24, 48),
                                new SpotObject("book", "📘", 34, 74, 42),
                                new SpotObject("pencil", "✏️", 44, 82, 34),
                                new SpotObject("teddy", "🧸", 56, 66, 58),
                                new SpotObject("plant", "🪴", 84, 72, 48),
                                new SpotObject("car", "🚗", 18, 80, 42),
                                new SpotObject("balloon", "🎈", 44, 32, 46),
                                new SpotObject("apple", "🍎", 70, 82, 36),
                                new SpotObject("star", "⭐", 64, 38, 34),
                                new SpotObject("pillow", "🟦", 82, 86, 34),
                                new SpotObject("chair", "🪑", 24, 64, 38),
                                new SpotObject("bed", "🛏️", 74, 66, 48),
                                new SpotObject("dice", "🎲", 44, 68, 34),
                                new SpotObject("puzzle", "🧩", 26, 84, 34),
                                new SpotObject("crayon", "🖍️", 62, 86, 34)
                        )
                ),
                new SpotSceneTemplate(
                        "Двор",
                        "garden",
                        List.of(
                                new SpotObject("sun", "☀️", 80, 16, 52),
                                new SpotObject("cloud", "☁️", 24, 18, 50),
                                new SpotObject("small-cloud", "☁️", 46, 16, 34),
                                new SpotObject("tree", "🌳", 18, 62, 58),
                                new SpotObject("tree-two", "🌲", 90, 62, 48),
                                new SpotObject("cow", "🐄", 58, 68, 58),
                                new SpotObject("chicken", "🐔", 76, 78, 42),
                                new SpotObject("chick", "🐥", 68, 84, 32),
                                new SpotObject("tractor", "🚜", 36, 78, 52),
                                new SpotObject("flower", "🌷", 88, 58, 36),
                                new SpotObject("carrot", "🥕", 48, 88, 34),
                                new SpotObject("corn", "🌽", 28, 88, 34),
                                new SpotObject("apple", "🍎", 12, 82, 34),
                                new SpotObject("sheep", "🐑", 72, 64, 46),
                                new SpotObject("pig", "🐖", 44, 68, 40),
                                new SpotObject("bee", "🐝", 52, 34, 32),
                                new SpotObject("basket", "🧺", 64, 88, 34),
                                new SpotObject("mushroom", "🍄", 20, 84, 32)
                        )
                )
        );
    }

    private FindObjectScene findObjectScene() {
        return new FindObjectScene(
                "Стая",
                "room",
                List.of(
                        new FindObjectItem("bed", "🛏️", "легло", "леглото", 76, 68, 54, 1),
                        new FindObjectItem("teddy", "🧸", "мече", "мечето", 66, 58, 52, 1),
                        new FindObjectItem("red-car", "🚗", "червена кола", "червената кола", 14, 84, 40, 1),
                        new FindObjectItem("books", "📚", "книги", "книгите", 42, 80, 40, 1),
                        new FindObjectItem("ball", "⚽", "топка", "топката", 68, 82, 40, 1),
                        new FindObjectItem("lamp", "💡", "лампа", "лампата", 17, 22, 42, 1),
                        new FindObjectItem("clock", "⏰", "часовник", "часовника", 76, 22, 44, 1),
                        new FindObjectItem("cup", "🥛", "чаша", "чашата", 9, 74, 34, 1),
                        new FindObjectItem("shoe", "👟", "обувка", "обувката", 23, 86, 34, 1),
                        new FindObjectItem("pillow", "🟦", "синя възглавница", "синята възглавница", 88, 84, 34, 1),
                        new FindObjectItem("plant", "🪴", "растение", "растението", 88, 72, 42, 2),
                        new FindObjectItem("pencil", "✏️", "молив", "молива", 56, 83, 34, 2),
                        new FindObjectItem("puzzle", "🧩", "пъзел", "пъзела", 28, 84, 34, 2),
                        new FindObjectItem("dice", "🎲", "зарче", "зарчето", 50, 66, 34, 2),
                        new FindObjectItem("hat", "🧢", "шапка", "шапката", 30, 44, 36, 2),
                        new FindObjectItem("spoon", "🥄", "лъжица", "лъжицата", 62, 64, 34, 2),
                        new FindObjectItem("backpack", "🎒", "раница", "раницата", 14, 60, 40, 3),
                        new FindObjectItem("robot", "🤖", "робот", "робота", 55, 54, 42, 3),
                        new FindObjectItem("train", "🚂", "влак", "влака", 36, 58, 42, 3),
                        new FindObjectItem("balloon", "🎈", "балон", "балона", 43, 32, 46, 3),
                        new FindObjectItem("gift", "🎁", "подарък", "подаръка", 91, 57, 36, 3),
                        new FindObjectItem("brush", "🖌️", "четка", "четката", 19, 72, 34, 3),
                        new FindObjectItem("blue-book", "📘", "синя книга", "синята книга", 34, 74, 36, 4),
                        new FindObjectItem("red-book", "📕", "червена книга", "червената книга", 50, 78, 36, 4),
                        new FindObjectItem("crayon", "🖍️", "пастел", "пастела", 61, 88, 32, 4),
                        new FindObjectItem("scissors", "✂️", "ножица", "ножицата", 7, 88, 32, 4),
                        new FindObjectItem("basket", "🧺", "кошница", "кошницата", 94, 82, 36, 4),
                        new FindObjectItem("orange", "🍊", "портокал", "портокала", 76, 86, 32, 5),
                        new FindObjectItem("apple", "🍎", "ябълка", "ябълката", 72, 78, 32, 5),
                        new FindObjectItem("chair", "🪑", "стол", "стола", 24, 66, 38, 5),
                        new FindObjectItem("mirror", "🪞", "огледало", "огледалото", 68, 40, 36, 5),
                        new FindObjectItem("socks", "🧦", "чорапи", "чорапите", 40, 88, 32, 5),
                        new FindObjectItem("window", "🪟", "прозорец", "прозореца", 49, 23, 50, 6),
                        new FindObjectItem("star", "⭐", "звезда", "звездата", 64, 38, 34, 6),
                        new FindObjectItem("door", "🚪", "врата", "вратата", 93, 38, 46, 6),
                        new FindObjectItem("soap", "🧼", "сапун", "сапуна", 15, 50, 32, 6),
                        new FindObjectItem("drum", "🥁", "барабан", "барабана", 63, 76, 36, 7),
                        new FindObjectItem("guitar", "🎸", "китара", "китарата", 24, 48, 46, 7),
                        new FindObjectItem("violin", "🎻", "цигулка", "цигулката", 72, 58, 36, 7),
                        new FindObjectItem("microphone", "🎤", "микрофон", "микрофона", 43, 48, 34, 7),
                        new FindObjectItem("cube", "🧊", "кубче", "кубчето", 82, 52, 36, 8),
                        new FindObjectItem("key", "🔑", "ключ", "ключа", 15, 43, 34, 8),
                        new FindObjectItem("candle", "🕯️", "свещ", "свещта", 82, 66, 34, 8),
                        new FindObjectItem("newspaper", "📰", "вестник", "вестника", 45, 70, 34, 8),
                        new FindObjectItem("magnifier", "🔎", "лупа", "лупата", 84, 45, 36, 9),
                        new FindObjectItem("bell", "🔔", "звънче", "звънчето", 58, 34, 36, 9),
                        new FindObjectItem("map", "🗺️", "карта", "картата", 38, 34, 38, 9),
                        new FindObjectItem("toolbox", "🧰", "кутия с инструменти", "кутията с инструменти", 59, 72, 36, 9)
                )
        );
    }

    private enum SpotDifferenceType {
        MISSING,
        DETAIL,
        POSITION,
        SIZE,
        COUNT
    }

    private record SpotObject(String id, String emoji, int x, int y, int size) {
    }

    private record SpotSceneTemplate(String name, String theme, List<SpotObject> objects) {
    }

    private record FindObjectItem(
            String id,
            String emoji,
            String label,
            String targetText,
            int x,
            int y,
            int size,
            int minDifficulty
    ) {
    }

    private record FindObjectPlacement(int x, int y) {
    }

    private record FindObjectScene(String name, String theme, List<FindObjectItem> objects) {
    }

    private record MemoryPairItem(String id, String emoji, String label) {
    }

    private record MemoryCard(String cardId, String pairId, String emoji, String label) {
    }

    private record PatternColor(String id, String hex, String label) {
    }

    private record PatternShape(String id, String label) {
    }

    private record PatternLevel(
            int sequenceLength,
            int previewSeconds,
            int colorCount,
            int shapeCount,
            boolean allowRepeats,
            int maxRepeatedTokens
    ) {
    }

    private record PatternToken(String id, String shape, String color, String label) {
    }

    private static List<QuizMode> primitiveModes() {
        List<QuizMode> modes = new ArrayList<>();
        modes.addAll(MATH_PRIMITIVE_MODES);
        modes.addAll(BULGARIAN_PRIMITIVE_MODES);
        modes.addAll(LOGIC_PRIMITIVE_MODES);
        return List.copyOf(modes);
    }
}
