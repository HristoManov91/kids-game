package com.kidsgame.mathapp.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class RewardAssetSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RewardAssetSeeder.class);
    private static final TypeReference<List<PlacedRewardItemDto>> PLACED_ITEM_LIST = new TypeReference<>() {
    };

    private final RewardCatalogItemRepository itemRepository;
    private final RewardAlbumPictureRepository pictureRepository;
    private final RewardAssetService assetService;
    private final ObjectMapper objectMapper;

    public RewardAssetSeeder(
            RewardCatalogItemRepository itemRepository,
            RewardAlbumPictureRepository pictureRepository,
            RewardAssetService assetService,
            ObjectMapper objectMapper
    ) {
        this.itemRepository = itemRepository;
        this.pictureRepository = pictureRepository;
        this.assetService = assetService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<RewardCatalogItemEntity> items = itemRepository.findAllByOrderByThemeIdAscCategoryAscNameAsc();
        Map<String, RewardCatalogItemEntity> changedItems = new HashMap<>();
        for (RewardCatalogItemEntity item : items) {
            String image = item.getStoredImage();
            if (!assetService.isStaticRewardAssetPath(image)) {
                continue;
            }
            if (seedCatalogItemAsset(item, image)) {
                changedItems.put(item.getId(), item);
            }
        }
        seedStandaloneStaticAsset("/reward-assets/space/space-background.svg");
        seedStandaloneStaticAsset("/reward-assets/sea/sea-background.svg");
        seedStandaloneStaticAsset("/reward-assets/farm/farm-background.svg");
        seedStandaloneStaticAsset("/reward-assets/jungle/jungle-background.svg");
        if (!changedItems.isEmpty()) {
            syncPlacedItems(changedItems);
        }
    }

    private boolean seedCatalogItemAsset(RewardCatalogItemEntity item, String staticPath) {
        return readStaticAsset(staticPath)
                .map(data -> {
                    RewardAssetEntity asset = assetService.upsertStaticAsset(staticPath, data);
                    item.attachImageAsset(asset.getId(), assetService.publicUrl(asset.getId()));
                    return true;
                })
                .orElse(false);
    }

    private void syncPlacedItems(Map<String, RewardCatalogItemEntity> catalogItems) {
        for (RewardAlbumPicture picture : pictureRepository.findAll()) {
            List<PlacedRewardItemDto> placedItems = readItems(picture);
            boolean changed = false;
            List<PlacedRewardItemDto> updated = new ArrayList<>();
            for (PlacedRewardItemDto placed : placedItems) {
                RewardCatalogItemEntity catalogItem = catalogItems.get(placed.catalogItemId());
                if (catalogItem == null) {
                    updated.add(placed);
                    continue;
                }
                changed = true;
                updated.add(new PlacedRewardItemDto(
                        placed.id(),
                        placed.catalogItemId(),
                        catalogItem.getName(),
                        catalogItem.getImage(),
                        placed.x(),
                        placed.y(),
                        placed.scale(),
                        placed.rotation(),
                        placed.mirrored(),
                        placed.zIndex(),
                        placed.createdAt() == null ? Instant.now() : placed.createdAt()
                ));
            }
            if (changed) {
                picture.replacePlacedItems(writeItems(updated));
            }
        }
    }

    private void seedStandaloneStaticAsset(String staticPath) {
        readStaticAsset(staticPath).ifPresent(data -> assetService.upsertStaticAsset(staticPath, data));
    }

    private java.util.Optional<byte[]> readStaticAsset(String staticPath) {
        for (Path root : frontendPublicRoots()) {
            Path file = root.resolve(staticPath.substring(1)).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                continue;
            }
            try {
                return java.util.Optional.of(Files.readAllBytes(file));
            } catch (IOException ex) {
                LOGGER.warn("Could not read reward asset {}", file, ex);
            }
        }
        LOGGER.warn("Reward asset {} was not found. Keeping the current image path as fallback.", staticPath);
        return java.util.Optional.empty();
    }

    private List<Path> frontendPublicRoots() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path parent = current.getParent();
        if (parent == null) {
            return List.of(current.resolve("frontend/public").normalize());
        }
        return List.of(
                current.resolve("frontend/public").normalize(),
                parent.resolve("frontend/public").normalize()
        );
    }

    private List<PlacedRewardItemDto> readItems(RewardAlbumPicture picture) {
        try {
            return objectMapper.readValue(picture.getPlacedItemsJson(), PLACED_ITEM_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read reward picture items", ex);
        }
    }

    private String writeItems(List<PlacedRewardItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write reward picture items", ex);
        }
    }
}
