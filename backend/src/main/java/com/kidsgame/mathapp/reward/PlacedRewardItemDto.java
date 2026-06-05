package com.kidsgame.mathapp.reward;

import java.time.Instant;

public record PlacedRewardItemDto(
        String id,
        String catalogItemId,
        String name,
        String image,
        double x,
        double y,
        double scale,
        double rotation,
        Boolean mirrored,
        int zIndex,
        Instant createdAt
) {
}
