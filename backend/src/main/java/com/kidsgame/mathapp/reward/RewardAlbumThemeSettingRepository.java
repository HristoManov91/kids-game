package com.kidsgame.mathapp.reward;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardAlbumThemeSettingRepository extends JpaRepository<RewardAlbumThemeSetting, String> {
    List<RewardAlbumThemeSetting> findByActiveTrueOrderByThemeIdAsc();
}
