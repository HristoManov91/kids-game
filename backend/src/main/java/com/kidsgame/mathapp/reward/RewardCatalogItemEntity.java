package com.kidsgame.mathapp.reward;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Entity
@Table(name = "reward_catalog_items")
public class RewardCatalogItemEntity {
    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 80)
    private String themeId;

    @Column(nullable = false, length = 500)
    private String themeIds;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false, columnDefinition = "text")
    private String image;

    @Column(length = 120)
    private String imageAssetId;

    @Column(nullable = false)
    private double defaultScale;

    @Column(nullable = false)
    private double minScale;

    @Column(nullable = false)
    private double maxScale;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RewardCatalogItemEntity() {
    }

    public RewardCatalogItemEntity(RewardCatalogItemResponse item) {
        this.id = item.id();
        this.themeId = item.themeId();
        this.themeIds = encodeThemeIds(item.themeIds(), item.themeId());
        this.category = item.category();
        this.name = item.name();
        this.price = item.price();
        this.image = item.image();
        this.imageAssetId = null;
        this.defaultScale = item.defaultScale();
        this.minScale = item.minScale();
        this.maxScale = item.maxScale();
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        ensureThemeIds();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        ensureThemeIds();
        updatedAt = Instant.now();
    }

    public RewardCatalogItemResponse toResponse() {
        return new RewardCatalogItemResponse(id, themeId, getThemeIds(), category, name, price, getImage(), defaultScale, minScale, maxScale);
    }

    public void update(
            String themeId,
            List<String> themeIds,
            String category,
            String name,
            int price,
            String image,
            double defaultScale,
            double minScale,
            double maxScale,
            boolean active
    ) {
        update(themeId, themeIds, category, name, price, image, null, defaultScale, minScale, maxScale, active);
    }

    public void update(
            String themeId,
            List<String> themeIds,
            String category,
            String name,
            int price,
            String image,
            String imageAssetId,
            double defaultScale,
            double minScale,
            double maxScale,
            boolean active
    ) {
        this.themeId = themeId;
        this.themeIds = encodeThemeIds(themeIds, themeId);
        this.category = category;
        this.name = name;
        this.price = price;
        this.image = image;
        this.imageAssetId = imageAssetId;
        this.defaultScale = defaultScale;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.active = active;
    }

    public void attachImageAsset(String imageAssetId, String publicUrl) {
        this.imageAssetId = imageAssetId;
        this.image = publicUrl;
    }

    public String getId() {
        return id;
    }

    public String getThemeId() {
        return themeId;
    }

    public List<String> getThemeIds() {
        ensureThemeIds();
        return Arrays.stream(themeIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getImage() {
        if (imageAssetId != null && !imageAssetId.isBlank()) {
            return "/api/reward-assets/" + imageAssetId;
        }
        return image;
    }

    public String getStoredImage() {
        return image;
    }

    public String getImageAssetId() {
        return imageAssetId;
    }

    public double getDefaultScale() {
        return defaultScale;
    }

    public double getMinScale() {
        return minScale;
    }

    public double getMaxScale() {
        return maxScale;
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

    private void ensureThemeIds() {
        if (themeIds == null || themeIds.isBlank()) {
            themeIds = encodeThemeIds(List.of(themeId), themeId);
        }
    }

    private static String encodeThemeIds(List<String> themeIds, String fallbackThemeId) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (themeIds != null) {
            themeIds.stream()
                    .map(value -> value == null ? "" : value.trim())
                    .filter(value -> !value.isBlank())
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty() && fallbackThemeId != null && !fallbackThemeId.isBlank()) {
            normalized.add(fallbackThemeId.trim());
        }
        return String.join(",", normalized);
    }
}
