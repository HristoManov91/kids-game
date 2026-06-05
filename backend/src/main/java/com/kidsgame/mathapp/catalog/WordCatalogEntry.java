package com.kidsgame.mathapp.catalog;

import com.kidsgame.mathapp.quiz.QuizCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "word_catalog_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_word_catalog_entries_category_word",
                columnNames = {"category", "word"}
        )
)
public class WordCatalogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuizCategory category;

    @Column(nullable = false, length = 120)
    private String word;

    @Column(nullable = false, length = 40)
    private String image;

    @Column(nullable = false, columnDefinition = "text")
    private String syllablesJson;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected WordCatalogEntry() {
    }

    public WordCatalogEntry(QuizCategory category, String word, String image, String syllablesJson, int difficulty) {
        this.category = category;
        this.word = word;
        this.image = image;
        this.syllablesJson = syllablesJson;
        this.difficulty = difficulty;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public QuizCategory getCategory() {
        return category;
    }

    public String getWord() {
        return word;
    }

    public String getImage() {
        return image;
    }

    public String getSyllablesJson() {
        return syllablesJson;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(QuizCategory category, String word, String image, String syllablesJson, int difficulty, boolean active) {
        this.category = category;
        this.word = word;
        this.image = image;
        this.syllablesJson = syllablesJson;
        this.difficulty = difficulty;
        this.active = active;
        this.updatedAt = Instant.now();
    }
}
