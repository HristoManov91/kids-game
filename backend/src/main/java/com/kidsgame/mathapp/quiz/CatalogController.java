package com.kidsgame.mathapp.quiz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    @GetMapping
    public CatalogResponse catalog() {
        return new CatalogResponse(
                List.of(QuizCategory.MATH, QuizCategory.BULGARIAN, QuizCategory.LOGIC),
                Arrays.asList(QuizMode.values()),
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        );
    }

    public record CatalogResponse(
            List<QuizCategory> categories,
            List<QuizMode> modes,
            List<Integer> difficulties
    ) {
    }
}
