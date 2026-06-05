package com.kidsgame.mathapp.reward;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardCatalogItemRepository extends JpaRepository<RewardCatalogItemEntity, String> {
    List<RewardCatalogItemEntity> findByActiveTrueOrderByThemeIdAscCategoryAscNameAsc();

    List<RewardCatalogItemEntity> findAllByOrderByThemeIdAscCategoryAscNameAsc();
}
