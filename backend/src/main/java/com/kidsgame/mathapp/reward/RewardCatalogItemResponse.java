package com.kidsgame.mathapp.reward;

import java.util.LinkedHashSet;
import java.util.List;

public record RewardCatalogItemResponse(
        String id,
        String themeId,
        List<String> themeIds,
        String category,
        String name,
        int price,
        String image,
        double defaultScale,
        double minScale,
        double maxScale
) {
    public RewardCatalogItemResponse(
            String id,
            String themeId,
            String category,
            String name,
            int price,
            String image,
            double defaultScale,
            double minScale,
            double maxScale
    ) {
        this(id, themeId, List.of(themeId), category, name, price, image, defaultScale, minScale, maxScale);
    }

    public RewardCatalogItemResponse {
        LinkedHashSet<String> normalizedThemeIds = new LinkedHashSet<>();
        if (themeIds != null) {
            themeIds.stream()
                    .map(value -> value == null ? "" : value.trim())
                    .filter(value -> !value.isBlank())
                    .forEach(normalizedThemeIds::add);
        }
        if (normalizedThemeIds.isEmpty() && themeId != null && !themeId.isBlank()) {
            normalizedThemeIds.add(themeId.trim());
        }
        if (normalizedThemeIds.isEmpty()) {
            throw new IllegalArgumentException("Reward catalog item must have at least one theme.");
        }
        themeIds = List.copyOf(normalizedThemeIds);
        themeId = themeId == null || themeId.isBlank() ? themeIds.getFirst() : themeId.trim();
    }

    public boolean belongsToTheme(String themeId) {
        return themeIds.contains(themeId);
    }
}
