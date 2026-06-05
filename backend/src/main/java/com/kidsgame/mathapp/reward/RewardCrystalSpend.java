package com.kidsgame.mathapp.reward;

import com.kidsgame.mathapp.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reward_crystal_spends")
public class RewardCrystalSpend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private UUID pictureId;

    @Column(nullable = false, length = 80)
    private String itemId;

    @Column(length = 80)
    private String placedItemId;

    @Column(nullable = false, length = 120)
    private String itemName;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private Instant createdAt;

    protected RewardCrystalSpend() {
    }

    public RewardCrystalSpend(UserEntity user, UUID pictureId, String itemId, String placedItemId, String itemName, int amount) {
        this.user = user;
        this.pictureId = pictureId;
        this.itemId = itemId;
        this.placedItemId = placedItemId;
        this.itemName = itemName;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public UUID getPictureId() {
        return pictureId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getPlacedItemId() {
        return placedItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
