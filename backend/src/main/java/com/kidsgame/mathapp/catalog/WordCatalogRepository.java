package com.kidsgame.mathapp.catalog;

import com.kidsgame.mathapp.quiz.QuizCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordCatalogRepository extends JpaRepository<WordCatalogEntry, Long> {
    List<WordCatalogEntry> findAllByOrderByCategoryAscDifficultyAscWordAsc();

    List<WordCatalogEntry> findByCategoryAndActiveTrueOrderByDifficultyAscWordAsc(QuizCategory category);

    Optional<WordCatalogEntry> findByCategoryAndWordIgnoreCase(QuizCategory category, String word);
}
