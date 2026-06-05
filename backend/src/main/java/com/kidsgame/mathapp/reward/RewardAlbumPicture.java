package com.kidsgame.mathapp.reward;

import com.kidsgame.mathapp.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reward_album_pictures")
public class RewardAlbumPicture {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, length = 80)
    private String themeId;

    @Column(nullable = false, columnDefinition = "text")
    private String placedItemsJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RewardAlbumPicture() {
    }

    public RewardAlbumPicture(UserEntity user, String name, String themeId, String placedItemsJson) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = name;
        this.themeId = themeId;
        this.placedItemsJson = placedItemsJson;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getThemeId() {
        return themeId;
    }

    public String getPlacedItemsJson() {
        return placedItemsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String name) {
        this.name = name;
        touch();
    }

    public void replacePlacedItems(String placedItemsJson) {
        this.placedItemsJson = placedItemsJson;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
