package com.kidsgame.mathapp.reward;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlbumPictureSummaryResponse(
        UUID id,
        String name,
        String themeId,
        String themeName,
        String backgroundImage,
        int itemCount,
        List<PlacedRewardItemDto> previewItems,
        Instant createdAt,
        Instant updatedAt
) {
}
