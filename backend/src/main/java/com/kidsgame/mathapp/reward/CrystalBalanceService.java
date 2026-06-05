package com.kidsgame.mathapp.reward;

import com.kidsgame.mathapp.quiz.AttemptStatus;
import com.kidsgame.mathapp.quiz.CrystalReward;
import com.kidsgame.mathapp.quiz.QuizAttempt;
import com.kidsgame.mathapp.quiz.QuizAttemptRepository;
import com.kidsgame.mathapp.user.UserEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CrystalBalanceService {
    private final QuizAttemptRepository attemptRepository;
    private final RewardCrystalSpendRepository spendRepository;

    public CrystalBalanceService(QuizAttemptRepository attemptRepository, RewardCrystalSpendRepository spendRepository) {
        this.attemptRepository = attemptRepository;
        this.spendRepository = spendRepository;
    }

    public RewardBalanceResponse balance(Long userId) {
        int earned = earnedCrystals(userId);
        int spent = Math.toIntExact(Math.max(0, spendRepository.sumAmountByUserId(userId)));
        return new RewardBalanceResponse(earned, spent, Math.max(0, earned - spent));
    }

    public int availableCrystals(Long userId) {
        return balance(userId).available();
    }

    public RewardCrystalSpend spend(UserEntity user, UUID pictureId, String itemId, String placedItemId, String itemName, int amount) {
        return spendRepository.save(new RewardCrystalSpend(user, pictureId, itemId, placedItemId, itemName, amount));
    }

    public int refundRemovedItems(UserEntity user, UUID pictureId, List<PlacedRewardItemDto> removedItems) {
        if (removedItems == null || removedItems.isEmpty()) {
            return 0;
        }
        int refunded = 0;
        for (PlacedRewardItemDto removed : removedItems) {
            Optional<RewardCrystalSpend> exactMatch = spendRepository
                    .findTopByUser_IdAndPictureIdAndPlacedItemIdOrderByCreatedAtDesc(user.getId(), pictureId, removed.id());
            Optional<RewardCrystalSpend> spend = exactMatch.isPresent()
                    ? exactMatch
                    : spendRepository.findTopByUser_IdAndPictureIdAndItemIdOrderByCreatedAtDesc(
                            user.getId(),
                            pictureId,
                            removed.catalogItemId()
                    );
            if (spend.isEmpty()) {
                continue;
            }
            refunded += spend.get().getAmount();
            spendRepository.delete(spend.get());
        }
        return refunded;
    }

    private int earnedCrystals(Long userId) {
        return attemptRepository.findByUser_IdAndStatusOrderByStartedAtDesc(userId, AttemptStatus.COMPLETED)
                .stream()
                .mapToInt(this::crystalsFor)
                .sum();
    }

    private int crystalsFor(QuizAttempt attempt) {
        return CrystalReward.calculate(
                attempt.getDifficulty(),
                attempt.getScore() == null ? 0 : attempt.getScore(),
                attempt.getTotalQuestions()
        );
    }
}
