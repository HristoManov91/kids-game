package com.kidsgame.mathapp.reward;

import java.time.Instant;
import java.util.List;

public record AdminRewardCatalogItemResponse(
        String id,
        String themeId,
        List<String> themeIds,
        String category,
        String name,
        int price,
        String image,
        double defaultScale,
        double minScale,
        double maxScale,
        boolean active,
        long usedCount,
        Instant createdAt,
        Instant updatedAt
) {
}
