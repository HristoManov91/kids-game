package com.kidsgame.mathapp.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminRewardCatalogService {
    private static final TypeReference<List<PlacedRewardItemDto>> PLACED_ITEM_LIST = new TypeReference<>() {
    };
    private static final Locale BG = Locale.forLanguageTag("bg-BG");

    private final RewardCatalogItemRepository itemRepository;
    private final RewardAlbumPictureRepository pictureRepository;
    private final RewardCrystalSpendRepository spendRepository;
    private final RewardAssetService assetService;
    private final RewardCatalog catalog;
    private final ObjectMapper objectMapper;

    public AdminRewardCatalogService(
            RewardCatalogItemRepository itemRepository,
            RewardAlbumPictureRepository pictureRepository,
            RewardCrystalSpendRepository spendRepository,
            RewardAssetService assetService,
            RewardCatalog catalog,
            ObjectMapper objectMapper
    ) {
        this.itemRepository = itemRepository;
        this.pictureRepository = pictureRepository;
        this.spendRepository = spendRepository;
        this.assetService = assetService;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminRewardCatalogItemResponse> list() {
        Map<String, Long> usage = usageCounts();
        return itemRepository.findAllByOrderByThemeIdAscCategoryAscNameAsc()
                .stream()
                .map(item -> toAdminResponse(item, usage.getOrDefault(item.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminRewardThemeResponse> themes() {
        return catalog.adminThemes();
    }

    @Transactional
    public AdminRewardThemeResponse updateThemeStatus(String themeId, boolean active) {
        return catalog.updateThemeStatus(themeId, active);
    }

    @Transactional
    public AdminRewardCatalogItemResponse create(
            String id,
            List<String> themeIds,
            String themeId,
            String category,
            String name,
            String price,
            Double defaultScale,
            Double minScale,
            Double maxScale,
            MultipartFile file
    ) {
        List<String> normalizedThemeIds = validatedThemes(themeIds, themeId);
        String normalizedThemeId = normalizedThemeIds.getFirst();
        String normalizedName = required(name, "Въведи име.");
        ensureUniqueName(null, normalizedThemeIds, normalizedName);
        RewardAssetEntity imageAsset = assetService.storeUpload(file);
        RewardCatalogItemEntity item = new RewardCatalogItemEntity(new RewardCatalogItemResponse(
                normalizedId(id, name),
                normalizedThemeId,
                normalizedThemeIds,
                validatedCategory(normalizedThemeIds, category),
                normalizedName,
                positivePrice(price),
                assetService.publicUrl(imageAsset.getId()),
                scale(defaultScale, 1.0),
                scale(minScale, 0.35),
                scale(maxScale, 2.5)
        ));
        validateScales(item.getMinScale(), item.getDefaultScale(), item.getMaxScale());
        if (itemRepository.existsById(item.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вече има предмет с този идентификатор.");
        }
        item.update(
                item.getThemeId(),
                item.getThemeIds(),
                item.getCategory(),
                item.getName(),
                item.getPrice(),
                item.getImage(),
                imageAsset.getId(),
                item.getDefaultScale(),
                item.getMinScale(),
                item.getMaxScale(),
                true
        );
        RewardCatalogItemEntity saved = itemRepository.save(item);
        return toAdminResponse(saved, 0);
    }

    @Transactional
    public AdminRewardCatalogItemResponse update(
            String itemId,
            List<String> themeIds,
            String themeId,
            String category,
            String name,
            String price,
            Double defaultScale,
            Double minScale,
            Double maxScale,
            MultipartFile file
    ) {
        RewardCatalogItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Предметът не е намерен."));
        List<String> normalizedThemeIds = validatedThemes(themeIds, themeId);
        String normalizedThemeId = normalizedThemeIds.getFirst();
        String normalizedName = required(name, "Въведи име.");
        ensureUniqueName(itemId, normalizedThemeIds, normalizedName);
        RewardAssetEntity uploadedAsset = file == null || file.isEmpty() ? null : assetService.storeUpload(file);
        String image = uploadedAsset == null ? item.getImage() : assetService.publicUrl(uploadedAsset.getId());
        String imageAssetId = uploadedAsset == null ? item.getImageAssetId() : uploadedAsset.getId();
        double normalizedMin = scale(minScale, item.getMinScale());
        double normalizedDefault = scale(defaultScale, item.getDefaultScale());
        double normalizedMax = scale(maxScale, item.getMaxScale());
        validateScales(normalizedMin, normalizedDefault, normalizedMax);
        item.update(
                normalizedThemeId,
                normalizedThemeIds,
                validatedCategory(normalizedThemeIds, category),
                normalizedName,
                positivePrice(price),
                image,
                imageAssetId,
                normalizedDefault,
                normalizedMin,
                normalizedMax,
                item.isActive()
        );
        RewardCatalogItemEntity saved = itemRepository.save(item);
        syncPlacedItems(saved);
        return toAdminResponse(saved, usageCounts().getOrDefault(saved.getId(), 0L));
    }

    @Transactional
    public AdminRewardCatalogItemResponse updateItemStatus(String itemId, boolean active) {
        RewardCatalogItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Предметът не е намерен."));
        item.update(
                item.getThemeId(),
                item.getThemeIds(),
                item.getCategory(),
                item.getName(),
                item.getPrice(),
                item.getStoredImage(),
                item.getImageAssetId(),
                item.getDefaultScale(),
                item.getMinScale(),
                item.getMaxScale(),
                active
        );
        RewardCatalogItemEntity saved = itemRepository.save(item);
        return toAdminResponse(saved, usageCounts().getOrDefault(saved.getId(), 0L));
    }

    @Transactional
    public DeleteRewardCatalogItemResponse delete(String itemId) {
        RewardCatalogItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Предметът не е намерен."));
        int removed = removePlacedItems(itemId);
        List<RewardCrystalSpend> spends = spendRepository.findByItemId(itemId);
        long refundedCrystals = spends.stream().mapToLong(RewardCrystalSpend::getAmount).sum();
        long refundedPurchases = spends.size();
        spendRepository.deleteAll(spends);
        itemRepository.delete(item);
        return new DeleteRewardCatalogItemResponse(itemId, item.getName(), removed, refundedPurchases, refundedCrystals);
    }

    private void syncPlacedItems(RewardCatalogItemEntity catalogItem) {
        List<RewardAlbumPicture> pictures = pictureRepository.findAll();
        for (RewardAlbumPicture picture : pictures) {
            List<PlacedRewardItemDto> items = readItems(picture);
            boolean changed = false;
            List<PlacedRewardItemDto> updated = new ArrayList<>();
            for (PlacedRewardItemDto placed : items) {
                if (!placed.catalogItemId().equals(catalogItem.getId())) {
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
                        clamp(placed.scale(), catalogItem.getMinScale(), catalogItem.getMaxScale()),
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

    private int removePlacedItems(String itemId) {
        int removed = 0;
        List<RewardAlbumPicture> pictures = pictureRepository.findAll();
        for (RewardAlbumPicture picture : pictures) {
            List<PlacedRewardItemDto> items = readItems(picture);
            List<PlacedRewardItemDto> kept = items.stream()
                    .filter(item -> !item.catalogItemId().equals(itemId))
                    .toList();
            if (kept.size() != items.size()) {
                removed += items.size() - kept.size();
                picture.replacePlacedItems(writeItems(kept));
            }
        }
        return removed;
    }

    private Map<String, Long> usageCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (RewardAlbumPicture picture : pictureRepository.findAll()) {
            for (PlacedRewardItemDto item : readItems(picture)) {
                counts.merge(item.catalogItemId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private AdminRewardCatalogItemResponse toAdminResponse(RewardCatalogItemEntity item, long usedCount) {
        return new AdminRewardCatalogItemResponse(
                item.getId(),
                item.getThemeId(),
                item.getThemeIds(),
                item.getCategory(),
                item.getName(),
                item.getPrice(),
                item.getImage(),
                item.getDefaultScale(),
                item.getMinScale(),
                item.getMaxScale(),
                item.isActive(),
                usedCount,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String normalizedId(String requestedId, String name) {
        String id = requestedId == null ? "" : requestedId.trim();
        if (id.isBlank()) {
            id = "custom-" + slug(required(name, "Въведи име.")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (!id.matches("[a-zA-Z0-9_-]{3,80}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Идентификаторът може да съдържа само латински букви, цифри, - и _.");
        }
        return id;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value.toLowerCase(BG), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ъ', 'a')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "item" : normalized;
    }

    private List<String> validatedThemes(List<String> themeIds, String fallbackThemeId) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (themeIds != null) {
            themeIds.stream()
                    .flatMap(value -> Arrays.stream(value.split(",")))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty() && fallbackThemeId != null && !fallbackThemeId.isBlank()) {
            normalized.add(fallbackThemeId.trim());
        }
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Избери поне един свят.");
        }
        normalized.forEach(catalog::theme);
        return List.copyOf(normalized);
    }

    private String validatedCategory(List<String> themeIds, String category) {
        String normalized = required(category, "Избери категория.");
        boolean available = themeIds.stream()
                .map(catalog::theme)
                .anyMatch(theme -> theme.categories().contains(normalized));
        if (!available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Избери категория от списъка.");
        }
        return normalized;
    }

    private String required(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private int positivePrice(String value) {
        String normalized = required(value, "Въведи цена.");
        if (!normalized.matches("\\d+(,\\d{1,2})?")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Цената може да съдържа само цифри и десетична запетая.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(normalized.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Цената трябва да е число.");
        }
        if (amount.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Цената може да има до 2 знака след запетаята.");
        }
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Цената трябва да е по-голяма от 0.");
        }
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Цената е в цели кристали. Може например 10 или 10,00.");
        }
        try {
            return amount.intValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Цената е прекалено голяма.");
        }
    }

    private void ensureUniqueName(String currentItemId, List<String> themeIds, String name) {
        String normalizedName = name.toLowerCase(BG);
        boolean duplicated = itemRepository.findAllByOrderByThemeIdAscCategoryAscNameAsc()
                .stream()
                .anyMatch(item ->
                        !item.getId().equals(currentItemId) &&
                                item.getThemeIds().stream().anyMatch(themeIds::contains) &&
                                item.getName().trim().toLowerCase(BG).equals(normalizedName)
                );
        if (duplicated) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вече има предмет с това име в избрания свят.");
        }
    }

    private double scale(Double value, double fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private void validateScales(double minScale, double defaultScale, double maxScale) {
        if (minScale <= 0 || defaultScale <= 0 || maxScale <= 0 || minScale > defaultScale || defaultScale > maxScale) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Провери размерите: минимум <= основен <= максимум.");
        }
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

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
