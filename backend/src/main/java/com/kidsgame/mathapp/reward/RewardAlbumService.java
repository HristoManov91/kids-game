package com.kidsgame.mathapp.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RewardAlbumService {
    private static final TypeReference<List<PlacedRewardItemDto>> PLACED_ITEM_LIST = new TypeReference<>() {
    };

    private final RewardAlbumPictureRepository pictureRepository;
    private final UserRepository userRepository;
    private final RewardCatalog catalog;
    private final CrystalBalanceService balanceService;
    private final ObjectMapper objectMapper;

    public RewardAlbumService(
            RewardAlbumPictureRepository pictureRepository,
            UserRepository userRepository,
            RewardCatalog catalog,
            CrystalBalanceService balanceService,
            ObjectMapper objectMapper
    ) {
        this.pictureRepository = pictureRepository;
        this.userRepository = userRepository;
        this.catalog = catalog;
        this.balanceService = balanceService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RewardCatalogResponse catalog() {
        return catalog.catalog();
    }

    @Transactional(readOnly = true)
    public RewardBalanceResponse balance(UserPrincipal principal) {
        return balanceService.balance(principal.id());
    }

    @Transactional(readOnly = true)
    public List<AlbumPictureSummaryResponse> pictures(UserPrincipal principal) {
        return pictureRepository.findByUser_IdOrderByUpdatedAtDesc(principal.id())
                .stream()
                .filter(picture -> catalog.isThemeActive(picture.getThemeId()))
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public AlbumPictureDetailResponse create(CreatePictureRequest request, UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Потребителят не е намерен."));
        RewardThemeResponse theme = catalog.activeTheme(request.themeId());
        String name = normalizedName(request.name(), theme);
        RewardAlbumPicture picture = new RewardAlbumPicture(user, name, theme.id(), writePlacedItems(List.of()));
        return toDetail(pictureRepository.save(picture));
    }

    @Transactional(readOnly = true)
    public AlbumPictureDetailResponse get(UUID pictureId, UserPrincipal principal) {
        return toDetail(loadOwnedPicture(pictureId, principal));
    }

    @Transactional
    public AlbumPictureDetailResponse update(UUID pictureId, UpdatePictureRequest request, UserPrincipal principal) {
        RewardAlbumPicture picture = loadOwnedPicture(pictureId, principal);
        RewardThemeResponse theme = catalog.theme(picture.getThemeId());
        picture.rename(normalizedName(request.name(), theme));
        List<PlacedRewardItemDto> currentItems = readPlacedItems(picture);
        List<PlacedRewardItemDto> updatedItems = sanitizeUpdateItems(currentItems, request.placedItems() == null ? List.of() : request.placedItems());
        List<PlacedRewardItemDto> removedItems = removedItems(currentItems, updatedItems);
        balanceService.refundRemovedItems(picture.getUser(), picture.getId(), removedItems);
        picture.replacePlacedItems(writePlacedItems(updatedItems));
        return toDetail(picture);
    }

    @Transactional
    public PurchaseRewardItemResponse purchase(UUID pictureId, PurchaseRewardItemRequest request, UserPrincipal principal) {
        RewardAlbumPicture picture = loadOwnedPicture(pictureId, principal);
        RewardCatalogItemResponse item = catalog.item(request.itemId());
        if (!item.belongsToTheme(picture.getThemeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Този предмет е за друг свят.");
        }

        RewardBalanceResponse currentBalance = balanceService.balance(principal.id());
        if (currentBalance.available() < item.price()) {
            int missing = item.price() - currentBalance.available();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Остават ти още " + missing + " кристала.");
        }

        List<PlacedRewardItemDto> placedItems = new ArrayList<>(readPlacedItems(picture));
        PlacedRewardItemDto placedItem = newPlacedItem(item, placedItems.size());
        placedItems.add(placedItem);
        picture.replacePlacedItems(writePlacedItems(placedItems));
        balanceService.spend(picture.getUser(), picture.getId(), item.id(), placedItem.id(), item.name(), item.price());
        RewardBalanceResponse newBalance = balanceService.balance(principal.id());
        return new PurchaseRewardItemResponse(
                newBalance,
                toDetail(picture),
                placedItem,
                "Супер! Добави " + item.name() + "!"
        );
    }

    @Transactional
    public RemoveRewardItemResponse removeItem(UUID pictureId, String placedItemId, UserPrincipal principal) {
        RewardAlbumPicture picture = loadOwnedPicture(pictureId, principal);
        List<PlacedRewardItemDto> currentItems = readPlacedItems(picture);
        PlacedRewardItemDto removed = currentItems.stream()
                .filter(item -> item.id().equals(placedItemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Предметът не е намерен в тази картина."));

        List<PlacedRewardItemDto> updatedItems = currentItems.stream()
                .filter(item -> !item.id().equals(placedItemId))
                .toList();
        int refundedCrystals = balanceService.refundRemovedItems(picture.getUser(), picture.getId(), List.of(removed));
        picture.replacePlacedItems(writePlacedItems(updatedItems));
        RewardBalanceResponse newBalance = balanceService.balance(principal.id());
        String crystalsLabel = refundedCrystals == 1 ? "кристал" : "кристала";
        String message = refundedCrystals > 0
                ? "Махна " + removed.name() + ". Върнахме " + refundedCrystals + " " + crystalsLabel + "."
                : "Махна " + removed.name() + ".";
        return new RemoveRewardItemResponse(newBalance, toDetail(picture), refundedCrystals, message);
    }

    @Transactional
    public void delete(UUID pictureId, UserPrincipal principal) {
        RewardAlbumPicture picture = loadOwnedPicture(pictureId, principal);
        pictureRepository.delete(picture);
    }

    private RewardAlbumPicture loadOwnedPicture(UUID pictureId, UserPrincipal principal) {
        RewardAlbumPicture picture = pictureRepository.findByIdAndUser_Id(pictureId, principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Картината не е намерена."));
        if (!catalog.isThemeActive(picture.getThemeId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Картината не е намерена.");
        }
        return picture;
    }

    private AlbumPictureSummaryResponse toSummary(RewardAlbumPicture picture) {
        RewardThemeResponse theme = catalog.theme(picture.getThemeId());
        List<PlacedRewardItemDto> items = readPlacedItems(picture).stream()
                .sorted(Comparator.comparingInt(PlacedRewardItemDto::zIndex))
                .toList();
        List<PlacedRewardItemDto> previewItems = items.stream()
                .limit(8)
                .toList();
        return new AlbumPictureSummaryResponse(
                picture.getId(),
                picture.getName(),
                theme.id(),
                theme.name(),
                theme.backgroundImage(),
                items.size(),
                previewItems,
                picture.getCreatedAt(),
                picture.getUpdatedAt()
        );
    }

    private AlbumPictureDetailResponse toDetail(RewardAlbumPicture picture) {
        RewardThemeResponse theme = catalog.theme(picture.getThemeId());
        return new AlbumPictureDetailResponse(
                picture.getId(),
                picture.getName(),
                theme.id(),
                theme.name(),
                theme.backgroundImage(),
                readPlacedItems(picture).stream()
                        .sorted(Comparator.comparingInt(PlacedRewardItemDto::zIndex))
                        .toList(),
                picture.getCreatedAt(),
                picture.getUpdatedAt()
        );
    }

    private List<PlacedRewardItemDto> sanitizeUpdateItems(List<PlacedRewardItemDto> currentItems, List<PlacedRewardItemDto> requestedItems) {
        Map<String, PlacedRewardItemDto> currentById = currentItems.stream()
                .collect(Collectors.toMap(PlacedRewardItemDto::id, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        List<PlacedRewardItemDto> sanitized = new ArrayList<>();
        int fallbackZ = 1;
        for (PlacedRewardItemDto requested : requestedItems) {
            PlacedRewardItemDto existing = currentById.get(requested.id());
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Първо добави предмета от каталога.");
            }
            RewardCatalogItemResponse catalogItem = catalog.item(existing.catalogItemId());
            sanitized.add(new PlacedRewardItemDto(
                    existing.id(),
                    existing.catalogItemId(),
                    catalogItem.name(),
                    catalogItem.image(),
                    clamp(requested.x(), 0, 100),
                    clamp(requested.y(), 0, 100),
                    clamp(requested.scale(), catalogItem.minScale(), catalogItem.maxScale()),
                    clamp(requested.rotation(), -180, 180),
                    requested.mirrored() == null ? Boolean.TRUE.equals(existing.mirrored()) : requested.mirrored(),
                    requested.zIndex() <= 0 ? fallbackZ : requested.zIndex(),
                    existing.createdAt() == null ? Instant.now() : existing.createdAt()
            ));
            fallbackZ++;
        }
        return sanitized;
    }

    private PlacedRewardItemDto newPlacedItem(RewardCatalogItemResponse item, int currentCount) {
        double offset = (currentCount % 7) * 3.5;
        int zIndex = currentCount + 1;
        return new PlacedRewardItemDto(
                UUID.randomUUID().toString(),
                item.id(),
                item.name(),
                item.image(),
                clamp(48 + offset, 10, 90),
                clamp(52 + offset / 2, 12, 88),
                item.defaultScale(),
                0,
                false,
                zIndex,
                Instant.now()
        );
    }

    private String normalizedName(String requestedName, RewardThemeResponse theme) {
        String normalized = requestedName == null ? "" : requestedName.trim();
        if (normalized.isBlank()) {
            return theme.defaultPictureName();
        }
        return normalized.length() <= 40 ? normalized : normalized.substring(0, 40);
    }

    private List<PlacedRewardItemDto> readPlacedItems(RewardAlbumPicture picture) {
        try {
            return objectMapper.readValue(picture.getPlacedItemsJson(), PLACED_ITEM_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read reward picture items", ex);
        }
    }

    private String writePlacedItems(List<PlacedRewardItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write reward picture items", ex);
        }
    }

    private List<PlacedRewardItemDto> removedItems(List<PlacedRewardItemDto> currentItems, List<PlacedRewardItemDto> updatedItems) {
        Map<String, PlacedRewardItemDto> keptById = updatedItems.stream()
                .collect(Collectors.toMap(PlacedRewardItemDto::id, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        return currentItems.stream()
                .filter(item -> !keptById.containsKey(item.id()))
                .toList();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
