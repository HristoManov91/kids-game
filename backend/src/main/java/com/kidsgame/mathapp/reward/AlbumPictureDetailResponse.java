package com.kidsgame.mathapp.reward;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlbumPictureDetailResponse(
        UUID id,
        String name,
        String themeId,
        String themeName,
        String backgroundImage,
        List<PlacedRewardItemDto> placedItems,
        Instant createdAt,
        Instant updatedAt
) {
}
