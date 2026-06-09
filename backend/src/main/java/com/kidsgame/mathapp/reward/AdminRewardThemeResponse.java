package com.kidsgame.mathapp.reward;

import java.time.Instant;
import java.util.List;

public record AdminRewardThemeResponse(
        String id,
        String name,
        String description,
        String backgroundImage,
        String thumbnailImage,
        List<String> categories,
        String defaultPictureName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
