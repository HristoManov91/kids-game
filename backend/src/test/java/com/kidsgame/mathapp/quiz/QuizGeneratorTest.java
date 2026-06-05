package com.kidsgame.mathapp.quiz;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGeneratorTest {
    private final QuizGenerator generator = new QuizGenerator();

    @Test
    void generatesTwentyQuestionsForEveryMode() {
        for (QuizMode mode : QuizGenerator.MATH_LINEAR_MODES) {
            List<GeneratedQuestion> questions = generator.generate(mode, 1);

            assertThat(questions).hasSize(20);
            assertThat(new HashSet<>(questions.stream().map(GeneratedQuestion::prompt).toList()))
                    .hasSizeGreaterThan(10);
        }
    }

    @Test
    void generatesTenBulgarianQuestionsForEveryMode() {
        for (QuizMode mode : QuizGenerator.BULGARIAN_PRIMITIVE_MODES) {
            List<GeneratedQuestion> questions = generator.generate(QuizCategory.BULGARIAN, mode, 1);

            assertThat(questions).hasSize(10);
            assertThat(questions)
                    .allSatisfy(question -> {
                        assertThat(question.answer()).isNotBlank();
                        if (mode == QuizMode.WORD_PICTURE) {
                            assertThat(question.image()).isNull();
                            assertThat(question.speechText()).isNotBlank();
                        } else if (mode == QuizMode.WORD_MISSING_LETTER) {
                            assertThat(question.image()).isNotBlank();
                            assertThat(question.speechText()).isNotBlank();
                            assertThat(question.prompt()).contains("_");
                        } else if (mode == QuizMode.WORD_FIRST_LETTER_GROUP) {
                            assertThat(question.image()).isNull();
                            assertThat(question.speechText()).isNull();
                        } else if (mode == QuizMode.WORD_WRONG_LETTER) {
                            assertThat(question.image()).isNotBlank();
                            assertThat(question.speechText()).isNotBlank();
                            assertThat(question.prompt()).isNotEqualTo(question.speechText());
                        } else {
                            assertThat(question.image()).isNotBlank();
                            assertThat(question.speechText()).isEqualTo(question.answer());
                        }
                        assertThat(question.sourceMode()).isEqualTo(mode);
                    });
        }
    }

    @Test
    void bulgarianPictureQuestionsShowOneWordAndThreePictures() {
        List<GeneratedQuestion> questions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_PICTURE, 3);

        assertThat(questions)
                .hasSize(QuizGenerator.BULGARIAN_QUESTIONS_PER_TEST)
                .allSatisfy(question -> {
                    assertThat(question.kind()).isEqualTo(QuestionKind.CHOICE);
                    assertThat(question.prompt()).isEqualTo(question.speechText());
                    assertThat(question.choices()).hasSize(3).doesNotHaveDuplicates().contains(question.answer());
                    assertThat(question.answerSlots()).isEmpty();
                });
    }

    @Test
    void bulgarianMissingLetterQuestionsFollowDifficultyPositions() {
        List<GeneratedQuestion> levelOne = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_MISSING_LETTER, 1);
        List<GeneratedQuestion> levelTwo = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_MISSING_LETTER, 2);
        List<GeneratedQuestion> levelThree = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_MISSING_LETTER, 3);
        List<GeneratedQuestion> levelFour = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_MISSING_LETTER, 4);

        assertThat(levelOne).allSatisfy(question -> {
            assertThat(question.prompt()).startsWith("_");
            assertMissingLettersQuestion(question, 1);
        });
        assertThat(levelTwo).allSatisfy(question -> {
            assertThat(question.prompt()).endsWith("_");
            assertMissingLettersQuestion(question, 1);
        });
        assertThat(levelThree).allSatisfy(question -> {
            assertThat(question.prompt().charAt(0)).isNotEqualTo('_');
            assertThat(question.prompt().charAt(question.prompt().length() - 1)).isNotEqualTo('_');
            assertMissingLettersQuestion(question, 1);
        });
        assertThat(levelFour).allSatisfy(question -> assertMissingLettersQuestion(question, 2));
    }

    @Test
    void bulgarianFirstLetterGroupingQuestionsUseBasketsAndPictureWords() {
        List<GeneratedQuestion> questions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_FIRST_LETTER_GROUP, 2);

        assertThat(questions)
                .hasSize(QuizGenerator.BULGARIAN_QUESTIONS_PER_TEST)
                .allSatisfy(question -> {
                    assertThat(question.kind()).isEqualTo(QuestionKind.GROUPING);
                    assertThat(question.answerSlots()).hasSize(3).doesNotHaveDuplicates();
                    assertThat(question.choices()).hasSize(6).doesNotHaveDuplicates();
                    assertThat(question.prompt()).isEqualTo("Групирай по първа буква");
                    assertThat(question.choices())
                            .allSatisfy(choice -> {
                                String[] parts = choice.split("\\|", 2);
                                assertThat(parts).hasSize(2);
                                String firstLetter = parts[1].substring(0, 1);
                                assertThat(question.answerSlots()).contains(firstLetter);
                                assertThat(question.answer()).contains(parts[1] + "=" + firstLetter);
                            });
                });
    }

    @Test
    void bulgarianWrongLetterQuestionsShowCorruptedWordAndCorrectLetterChoice() {
        List<GeneratedQuestion> questions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_WRONG_LETTER, 1);

        assertThat(questions)
                .hasSize(QuizGenerator.BULGARIAN_QUESTIONS_PER_TEST)
                .allSatisfy(question -> {
                    String[] answerParts = question.answer().split("=", 2);
                    assertThat(question.kind()).isEqualTo(QuestionKind.CHOICE);
                    assertThat(question.image()).isNotBlank();
                    assertThat(question.speechText()).isNotBlank();
                    assertThat(question.answerSlots()).isEmpty();
                    assertThat(answerParts).hasSize(2);
                    assertThat(question.choices()).hasSize(3).contains(answerParts[1]).doesNotContain(answerParts[0]);
                    assertThat(question.speechText()).isNotEqualTo(question.prompt());
                    assertThat(differentLetters(question.prompt(), question.speechText())).isEqualTo(1);
                });
    }

    @Test
    void logicSpotDifferencesQuestionsHaveTwoScenesAndDifficultyBasedDifferences() {
        Map<Integer, Integer> expectedDifferences = Map.of(
                1, 3,
                2, 4,
                4, 5,
                7, 6,
                9, 7
        );

        expectedDifferences.forEach((difficulty, expectedCount) -> {
            List<GeneratedQuestion> questions = generator.generate(QuizCategory.LOGIC, QuizMode.SPOT_DIFFERENCES, difficulty);

            assertThat(questions)
                    .hasSize(QuizGenerator.LOGIC_QUESTIONS_PER_TEST)
                    .allSatisfy(question -> {
                        List<String> differenceIds = spotDifferenceIds(question);

                        assertThat(question.kind()).isEqualTo(QuestionKind.SPOT_DIFFERENCES);
                        assertThat(question.sourceMode()).isEqualTo(QuizMode.SPOT_DIFFERENCES);
                        assertThat(question.prompt()).contains(String.valueOf(expectedCount));
                        assertThat(question.answerSlots()).anySatisfy(slot -> assertThat(slot).startsWith("S|"));
                        assertThat(question.answerSlots()).anySatisfy(slot -> assertThat(slot).startsWith("O|L|"));
                        assertThat(question.answerSlots()).anySatisfy(slot -> assertThat(slot).startsWith("O|R|"));
                        assertThat(spotObjectCount(question)).isGreaterThanOrEqualTo(30);
                        assertThat(differenceIds).hasSize(expectedCount);
                        assertThat(java.util.Arrays.asList(question.answer().split(";"))).containsExactlyElementsOf(differenceIds);
                        assertThat(QuestionScoring.weight(question)).isEqualTo(expectedCount);
                    });
        });
    }

    @Test
    void spotDifferencesScoringPenalizesWrongMarks() {
        GeneratedQuestion question = generator.generate(QuizCategory.LOGIC, QuizMode.SPOT_DIFFERENCES, 4).getFirst();
        List<String> differenceIds = spotDifferenceIds(question);
        AnswerRecord answer = new AnswerRecord(
                question.id(),
                differenceIds.get(0) + ";" + differenceIds.get(1) + ";N:L:not-a-difference",
                false,
                question.answer(),
                java.time.Instant.now()
        );

        assertThat(QuestionScoring.score(question, answer)).isEqualTo(1);
    }

    @Test
    void logicFindObjectQuestionHasRoomSceneAndTargetObject() {
        List<GeneratedQuestion> questions = generator.generate(QuizCategory.LOGIC, QuizMode.FIND_OBJECT, 5);
        GeneratedQuestion question = questions.getFirst();
        List<String> objectIds = findObjectIds(question);

        assertThat(questions).hasSize(QuizGenerator.LOGIC_QUESTIONS_PER_TEST);
        assertThat(question.kind()).isEqualTo(QuestionKind.CHOICE);
        assertThat(question.sourceMode()).isEqualTo(QuizMode.FIND_OBJECT);
        assertThat(question.prompt()).startsWith("Намери ");
        assertThat(question.speechText()).isNotBlank();
        assertThat(question.choices()).isEmpty();
        assertThat(question.answerSlots()).anySatisfy(slot -> assertThat(slot).startsWith("F|Стая|room"));
        assertThat(objectIds).hasSizeGreaterThanOrEqualTo(10);
        assertThat(objectIds).contains(question.answer());
        assertThat(QuestionScoring.weight(question)).isEqualTo(1);
    }

    @Test
    void logicMemoryPairsQuestionUsesDifficultyForPairsAndPreview() {
        GeneratedQuestion levelOne = generator.generate(QuizCategory.LOGIC, QuizMode.MEMORY_PAIRS, 1).getFirst();
        GeneratedQuestion levelTen = generator.generate(QuizCategory.LOGIC, QuizMode.MEMORY_PAIRS, 10).getFirst();

        assertThat(levelOne.kind()).isEqualTo(QuestionKind.MEMORY_PAIRS);
        assertThat(levelOne.sourceMode()).isEqualTo(QuizMode.MEMORY_PAIRS);
        assertThat(levelOne.answer()).isEqualTo("SOLVED");
        assertThat(levelOne.answerSlots().getFirst()).isEqualTo("M|7|3|1");
        assertThat(memoryPairIds(levelOne)).hasSize(3);
        assertThat(levelOne.answerSlots().stream().filter(slot -> slot.startsWith("C|"))).hasSize(6);
        assertThat(QuestionScoring.weight(levelOne)).isEqualTo(10);

        assertThat(levelTen.answerSlots().getFirst()).isEqualTo("M|15|12|5");
        assertThat(memoryPairIds(levelTen)).hasSize(12);
        assertThat(levelTen.answerSlots().stream().filter(slot -> slot.startsWith("C|"))).hasSize(24);
    }

    @Test
    void memoryPairsScoringUsesExtraAttempts() {
        GeneratedQuestion question = generator.generate(QuizCategory.LOGIC, QuizMode.MEMORY_PAIRS, 1).getFirst();

        assertThat(QuestionScoring.score(question, new AnswerRecord(question.id(), "SOLVED|attempts=3|pairs=3", true, "", java.time.Instant.now())))
                .isEqualTo(10);
        assertThat(QuestionScoring.score(question, new AnswerRecord(question.id(), "SOLVED|attempts=4|pairs=3", true, "", java.time.Instant.now())))
                .isEqualTo(10);
        assertThat(QuestionScoring.score(question, new AnswerRecord(question.id(), "SOLVED|attempts=9|pairs=3", true, "", java.time.Instant.now())))
                .isEqualTo(5);
        assertThat(QuestionScoring.score(question, new AnswerRecord(question.id(), "", false, "", java.time.Instant.now())))
                .isZero();
    }

    @Test
    void patternSequenceUsesDifficultyForLengthAndPreview() {
        Map<Integer, String> expectedHeaders = Map.of(
                1, "P|5|3",
                2, "P|8|4",
                3, "P|10|5",
                4, "P|12|6",
                5, "P|15|6",
                6, "P|15|6",
                7, "P|18|7",
                8, "P|20|7",
                9, "P|25|8",
                10, "P|25|8"
        );
        Map<Integer, Integer> expectedShapeCounts = Map.of(
                1, 1,
                2, 1,
                3, 1,
                4, 1,
                5, 2,
                6, 2,
                7, 2,
                8, 2,
                9, 3,
                10, 3
        );

        expectedHeaders.forEach((difficulty, header) -> {
            List<GeneratedQuestion> questions = generator.generate(QuizCategory.LOGIC, QuizMode.PATTERN_SEQUENCE, difficulty);
            GeneratedQuestion question = questions.getFirst();
            int expectedLength = Integer.parseInt(header.substring(header.lastIndexOf('|') + 1));
            List<String> answerTokens = List.of(question.answer().split(","));

            assertThat(questions).hasSize(QuizGenerator.LOGIC_QUESTIONS_PER_TEST);
            assertThat(question.kind()).isEqualTo(QuestionKind.PATTERN_SEQUENCE);
            assertThat(question.sourceMode()).isEqualTo(QuizMode.PATTERN_SEQUENCE);
            assertThat(question.answerSlots().getFirst()).isEqualTo(header);
            assertThat(question.answerSlots().stream().filter(slot -> slot.startsWith("S|"))).hasSize(expectedLength);
            assertThat(question.choices()).allSatisfy(choice -> assertThat(choice).startsWith("T|"));
            assertThat(answerTokens).hasSize(expectedLength);
            assertThat(QuestionScoring.weight(question)).isEqualTo(1);
            assertThat(answerTokens.stream()
                    .map(token -> token.substring(token.indexOf('-') + 1))
                    .collect(Collectors.toSet()))
                    .hasSize(expectedShapeCounts.get(difficulty));
            if (List.of(1, 5, 7, 9).contains(difficulty)) {
                assertThat(answerTokens).doesNotHaveDuplicates();
            }
        });
    }

    @Test
    void bulgarianCatalogHasConsistentWordsLettersAndSyllables() {
        List<BulgarianWordCatalog.BulgarianWordEntry> words = BulgarianWordCatalog.words();

        assertThat(words).hasSizeGreaterThanOrEqualTo(100);
        assertThat(words.stream().map(BulgarianWordCatalog.BulgarianWordEntry::image).distinct().count())
                .isGreaterThanOrEqualTo(100);
        List<BulgarianWordCatalog.BulgarianWordEntry> syllableReadyWords = words.stream()
                .filter(BulgarianWordCatalog.BulgarianWordEntry::hasEnoughSyllablesForSyllableGame)
                .toList();
        assertThat(syllableReadyWords).hasSizeGreaterThanOrEqualTo(100);
        assertThat(syllableReadyWords.stream().map(BulgarianWordCatalog.BulgarianWordEntry::image).distinct().count())
                .isGreaterThanOrEqualTo(100);
        assertThat(words.stream().map(BulgarianWordCatalog.BulgarianWordEntry::normalizedWord))
                .doesNotHaveDuplicates();
        assertThat(words)
                .allSatisfy(word -> {
                    assertThat(word.word()).isNotBlank();
                    assertThat(word.image()).isNotBlank();
                    assertThat(word.syllables()).isNotEmpty();
                    assertThat(String.join("", word.syllables()))
                            .isEqualTo(word.normalizedWord());
                    assertThat(word.letters()).hasSize(word.letterCount());
                });
    }

    @Test
    void suggestedImageCatalogAddsBroadPictureCoverage() {
        List<SuggestedBulgarianImageCatalog.SuggestedWord> suggestedWords = SuggestedBulgarianImageCatalog.words();

        assertThat(suggestedWords).hasSizeGreaterThanOrEqualTo(300);
        assertThat(suggestedWords.stream().map(SuggestedBulgarianImageCatalog.SuggestedWord::word))
                .doesNotHaveDuplicates();
        assertThat(suggestedWords.stream().map(SuggestedBulgarianImageCatalog.SuggestedWord::image))
                .doesNotHaveDuplicates();
        assertThat(suggestedWords)
                .allSatisfy(word -> assertThat(word.word()).matches("^[А-Яа-яЁё\\s-]+$"));

        long combinedImageCount = java.util.stream.Stream.concat(
                        BulgarianWordCatalog.words().stream().map(BulgarianWordCatalog.BulgarianWordEntry::image),
                        suggestedWords.stream().map(SuggestedBulgarianImageCatalog.SuggestedWord::image)
                )
                .distinct()
                .count();
        assertThat(combinedImageCount).isGreaterThanOrEqualTo(490);
    }

    @Test
    void bulgarianLetterQuestionsCanBeSolvedFromTheShownLetters() {
        for (int difficulty = 1; difficulty <= 10; difficulty++) {
            List<GeneratedQuestion> questions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_LETTERS, difficulty);

            assertThat(questions)
                    .allSatisfy(question -> assertContainsAtLeast(
                            counts(question.choices()),
                            counts(question.answer().replace(" ", "").codePoints()
                                    .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                                    .toList())
                    ));
        }
    }

    @Test
    void higherDifficultyBulgarianBuilderQuestionsIncludeExtraTokens() {
        List<GeneratedQuestion> letterQuestions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_LETTERS, 8);
        List<GeneratedQuestion> syllableQuestions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_SYLLABLES, 8);

        assertThat(letterQuestions)
                .allSatisfy(question -> {
                    long requiredSlots = question.answerSlots().stream().filter(slot -> !slot.isBlank()).count();
                    assertThat(requiredSlots).isEqualTo(question.answer().replace(" ", "").length());
                    assertThat(question.choices().size()).isGreaterThan((int) requiredSlots);
                });
        assertThat(syllableQuestions)
                .allSatisfy(question -> {
                    String answerFromSlots = String.join("", question.answerSlots()).replace(" ", "");
                    assertThat(answerFromSlots).isEqualTo(question.answer().replace(" ", ""));
                    assertThat(question.choices().size()).isGreaterThan(question.answerSlots().size());
                });
    }

    @Test
    void bulgarianSyllableQuestionsUseAtLeastTwoSyllables() {
        for (int difficulty = 1; difficulty <= 10; difficulty++) {
            List<GeneratedQuestion> questions = generator.generate(QuizCategory.BULGARIAN, QuizMode.WORD_SYLLABLES, difficulty);

            assertThat(questions)
                    .hasSize(QuizGenerator.BULGARIAN_QUESTIONS_PER_TEST)
                    .allSatisfy(question -> assertThat(question.choices()).hasSizeGreaterThanOrEqualTo(2));
        }
    }

    @Test
    void bulgarianCatalogAvoidsKnownAmbiguousPictureWordPairs() {
        assertThat(BulgarianWordCatalog.words().stream().map(BulgarianWordCatalog.BulgarianWordEntry::word))
                .doesNotContain("кот", "оса", "маса", "дъб", "хладилник", "възглавница", "аквариум", "малко мече", "балони");
    }

    @Test
    void ducklingUsesBabyBirdImage() {
        BulgarianWordCatalog.BulgarianWordEntry duckling = BulgarianWordCatalog.words().stream()
                .filter(word -> word.word().equals("пате"))
                .findFirst()
                .orElseThrow();

        assertThat(duckling.image()).isEqualTo("🐤");
    }

    @Test
    void waterUsesTapImageInsteadOfDrop() {
        BulgarianWordCatalog.BulgarianWordEntry water = BulgarianWordCatalog.words().stream()
                .filter(word -> word.word().equals("вода"))
                .findFirst()
                .orElseThrow();

        assertThat(water.image()).isEqualTo("🚰");
    }

    @Test
    void forestUsesManyTreesImage() {
        BulgarianWordCatalog.BulgarianWordEntry forest = BulgarianWordCatalog.words().stream()
                .filter(word -> word.word().equals("гора"))
                .findFirst()
                .orElseThrow();

        assertThat(forest.image()).isEqualTo("🌲🌳🌲");
    }

    @Test
    void balloonUsesSingularWordForSingleBalloonImage() {
        BulgarianWordCatalog.BulgarianWordEntry balloon = BulgarianWordCatalog.words().stream()
                .filter(word -> word.image().equals("🎈"))
                .findFirst()
                .orElseThrow();

        assertThat(balloon.word()).isEqualTo("балон");
        assertThat(balloon.syllables()).containsExactly("БА", "ЛОН");
    }

    @Test
    void allGroupMixesPrimitiveModes() {
        List<GeneratedQuestion> questions = generator.generate(QuizMode.ALL_GROUP, 2);

        assertThat(questions).hasSize(20);
        assertThat(questions.stream().map(GeneratedQuestion::sourceMode).distinct().count()).isGreaterThan(1);
    }

    @Test
    void groupedReadyModesMixTheirPairs() {
        List<GeneratedQuestion> unknownQuestions = generator.generate(QuizMode.UNKNOWN_MIXED, 2);
        List<GeneratedQuestion> multiplicationQuestions = generator.generate(QuizMode.MULTIPLICATION_DIVISION, 2);

        assertThat(unknownQuestions).hasSize(20);
        assertThat(unknownQuestions.stream().map(GeneratedQuestion::sourceMode).distinct())
                .containsOnly(QuizMode.UNKNOWN_ADDITION, QuizMode.UNKNOWN_SUBTRACTION);
        assertThat(multiplicationQuestions).hasSize(20);
        assertThat(multiplicationQuestions.stream().map(GeneratedQuestion::sourceMode).distinct())
                .containsOnly(QuizMode.MULTIPLICATION, QuizMode.DIVISION);
    }

    @Test
    void subtractionQuestionsNeverHaveNegativeAnswers() {
        List<GeneratedQuestion> questions = generator.generate(QuizMode.SUBTRACTION, 3);

        assertThat(questions)
                .allSatisfy(question -> assertThat(Integer.parseInt(question.answer())).isBetween(0, 30));
    }

    @Test
    void compareQuestionsUseOnlyComparisonChoices() {
        List<GeneratedQuestion> questions = generator.generate(QuizMode.COMPARE, 2);

        assertThat(questions)
                .allSatisfy(question -> {
                    assertThat(question.kind()).isEqualTo(QuestionKind.CHOICE);
                    assertThat(question.choices()).containsExactly("<", "=", ">");
                    assertThat(question.answer()).isIn("<", "=", ">");
                });
    }

    @Test
    void divisionQuestionsHaveWholeNumberAnswers() {
        List<GeneratedQuestion> questions = generator.generate(QuizMode.DIVISION, 3);

        assertThat(questions)
                .allSatisfy(question -> {
                    assertThat(question.prompt()).contains(" : ").endsWith(" = ?");
                    assertThat(Integer.parseInt(question.answer())).isBetween(0, 30);
                });
    }

    private Map<String, Long> counts(List<String> tokens) {
        return tokens.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private void assertContainsAtLeast(Map<String, Long> available, Map<String, Long> required) {
        required.forEach((token, count) ->
                assertThat(available.getOrDefault(token, 0L))
                        .as("token %s", token)
                        .isGreaterThanOrEqualTo(count)
        );
    }

    private void assertMissingLettersQuestion(GeneratedQuestion question, int missingLetters) {
        assertThat(question.kind()).isEqualTo(QuestionKind.LETTER_ORDER);
        assertThat(question.sourceMode()).isEqualTo(QuizMode.WORD_MISSING_LETTER);
        assertThat(question.answer()).hasSize(missingLetters);
        assertThat(question.answerSlots()).hasSize(missingLetters);
        assertThat(question.prompt().chars().filter(codePoint -> codePoint == '_').count()).isEqualTo(missingLetters);
        assertThat(question.choices()).hasSizeGreaterThanOrEqualTo(3);
        assertContainsAtLeast(counts(question.choices()), counts(question.answerSlots()));
    }

    private int differentLetters(String left, String right) {
        List<String> leftLetters = left.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint) && codePoint != '-')
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toList();
        List<String> rightLetters = right.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint) && codePoint != '-')
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toList();
        assertThat(leftLetters).hasSize(rightLetters.size());
        int differences = 0;
        for (int index = 0; index < leftLetters.size(); index++) {
            if (!leftLetters.get(index).equals(rightLetters.get(index))) {
                differences++;
            }
        }
        return differences;
    }

    private List<String> spotDifferenceIds(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .map(slot -> slot.split("\\|", -1))
                .filter(parts -> parts.length == 8 && parts[0].equals("O") && !parts[7].isBlank())
                .map(parts -> parts[7])
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> findObjectIds(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .map(slot -> slot.split("\\|", -1))
                .filter(parts -> parts.length == 8 && parts[0].equals("I") && !parts[1].isBlank())
                .map(parts -> parts[1])
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> memoryPairIds(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .map(slot -> slot.split("\\|", -1))
                .filter(parts -> parts.length == 5 && parts[0].equals("C") && !parts[2].isBlank())
                .map(parts -> parts[2])
                .distinct()
                .sorted()
                .toList();
    }

    private long spotObjectCount(GeneratedQuestion question) {
        return question.answerSlots().stream()
                .filter(slot -> slot.startsWith("O|"))
                .count();
    }

}
