package com.kidsgame.mathapp.reward;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardCrystalSpendRepository extends JpaRepository<RewardCrystalSpend, Long> {
    @Query("select coalesce(sum(spend.amount), 0) from RewardCrystalSpend spend where spend.user.id = :userId")
    long sumAmountByUserId(@Param("userId") Long userId);

    List<RewardCrystalSpend> findByItemId(String itemId);

    Optional<RewardCrystalSpend> findTopByUser_IdAndPictureIdAndPlacedItemIdOrderByCreatedAtDesc(
            Long userId,
            UUID pictureId,
            String placedItemId
    );

    Optional<RewardCrystalSpend> findTopByUser_IdAndPictureIdAndItemIdOrderByCreatedAtDesc(
            Long userId,
            UUID pictureId,
            String itemId
    );
}
