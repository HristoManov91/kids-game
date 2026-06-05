package com.kidsgame.mathapp.reward;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardAlbumPictureRepository extends JpaRepository<RewardAlbumPicture, UUID> {
    List<RewardAlbumPicture> findByUser_IdOrderByUpdatedAtDesc(Long userId);

    Optional<RewardAlbumPicture> findByIdAndUser_Id(UUID id, Long userId);
}
