package com.kidsgame.mathapp.reward;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardAlbumServiceTest {
    private static final TypeReference<List<PlacedRewardItemDto>> PLACED_ITEMS = new TypeReference<>() {
    };

    @Mock
    private RewardAlbumPictureRepository pictureRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CrystalBalanceService balanceService;

    @Mock
    private UserPrincipal principal;

    private ObjectMapper objectMapper;
    private RewardCatalog catalog;
    private RewardAlbumService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        catalog = new RewardCatalog();
        service = new RewardAlbumService(pictureRepository, userRepository, catalog, balanceService, objectMapper);
        user = new UserEntity("mila", "Мила", "hash", Role.CHILD);
        when(principal.id()).thenReturn(1L);
    }

    @Test
    void createsPictureWithDefaultNameAndLoadsItFromPersistence() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pictureRepository.save(any(RewardAlbumPicture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlbumPictureDetailResponse created = service.create(new CreatePictureRequest("", "forest-meadow"), principal);

        assertThat(created.name()).isEqualTo("Моята горска полянка");
        assertThat(created.themeId()).isEqualTo("forest-meadow");
        assertThat(created.placedItems()).isEmpty();
    }

    @Test
    void purchasesItemWhenCrystalsAreEnoughAndCreatesOnePlacedInstance() {
        RewardAlbumPicture picture = pictureWithItems(List.of());
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));
        when(balanceService.balance(1L))
                .thenReturn(new RewardBalanceResponse(100, 0, 100))
                .thenReturn(new RewardBalanceResponse(100, 12, 88));

        PurchaseRewardItemResponse response = service.purchase(
                picture.getId(),
                new PurchaseRewardItemRequest("forest-red-flower"),
                principal
        );

        assertThat(response.balance().available()).isEqualTo(88);
        assertThat(response.placedItem().catalogItemId()).isEqualTo("forest-red-flower");
        assertThat(response.picture().placedItems()).hasSize(1);
        assertThat(readItems(picture)).hasSize(1);
        verify(balanceService).spend(user, picture.getId(), "forest-red-flower", response.placedItem().id(), "Червено цвете", 12);
    }

    @Test
    void rejectsPurchaseWhenCrystalsAreNotEnough() {
        RewardAlbumPicture picture = pictureWithItems(List.of());
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));
        when(balanceService.balance(1L)).thenReturn(new RewardBalanceResponse(5, 0, 5));

        assertThatThrownBy(() -> service.purchase(picture.getId(), new PurchaseRewardItemRequest("forest-red-flower"), principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Остават ти още 7 кристала");

        assertThat(readItems(picture)).isEmpty();
        verify(balanceService, never()).spend(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void rejectsPurchaseForRemovedCatalogItem() {
        RewardAlbumPicture picture = pictureWithItems(List.of());
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));

        assertThatThrownBy(() -> service.purchase(picture.getId(), new PurchaseRewardItemRequest("forest-flower"), principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Този предмет не е в каталога");

        assertThat(readItems(picture)).isEmpty();
        verify(balanceService, never()).spend(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void rejectsPurchaseForItemFromAnotherTheme() {
        RewardAlbumPicture picture = pictureWithItems(List.of());
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));

        assertThatThrownBy(() -> service.purchase(picture.getId(), new PurchaseRewardItemRequest("space-rocket"), principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Този предмет е за друг свят");

        assertThat(readItems(picture)).isEmpty();
        verify(balanceService, never()).spend(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void movesPlacedItemWithoutCreatingUnpaidItems() {
        PlacedRewardItemDto placed = placedItem("one", "forest-red-flower", 30, 30);
        RewardAlbumPicture picture = pictureWithItems(List.of(placed));
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));

        AlbumPictureDetailResponse updated = service.update(
                picture.getId(),
                new UpdatePictureRequest("Моя гора", List.of(new PlacedRewardItemDto(
                        placed.id(),
                        placed.catalogItemId(),
                        placed.name(),
                        placed.image(),
                        88,
                        12,
                        1.1,
                        10,
                        false,
                        3,
                        placed.createdAt()
                ))),
                principal
        );

        assertThat(updated.placedItems()).hasSize(1);
        assertThat(updated.placedItems().getFirst().x()).isEqualTo(88);
        assertThat(updated.placedItems().getFirst().y()).isEqualTo(12);
    }

    @Test
    void clampsUpdatedItemGeometryToCatalogLimits() {
        PlacedRewardItemDto placed = placedItem("one", "forest-red-flower", 30, 30);
        RewardAlbumPicture picture = pictureWithItems(List.of(placed));
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));

        AlbumPictureDetailResponse updated = service.update(
                picture.getId(),
                new UpdatePictureRequest("Моя гора", List.of(new PlacedRewardItemDto(
                        placed.id(),
                        placed.catalogItemId(),
                        "Подменено име",
                        "Подменена картинка",
                        -42,
                        142,
                        99,
                        500,
                        true,
                        -10,
                        placed.createdAt()
                ))),
                principal
        );

        PlacedRewardItemDto sanitized = updated.placedItems().getFirst();
        assertThat(sanitized.name()).isEqualTo("Червено цвете");
        assertThat(sanitized.image()).isEqualTo("/reward-assets/polished/forest/flower-red.png");
        assertThat(sanitized.x()).isEqualTo(0);
        assertThat(sanitized.y()).isEqualTo(100);
        assertThat(sanitized.scale()).isEqualTo(1.4);
        assertThat(sanitized.rotation()).isEqualTo(180);
        assertThat(sanitized.zIndex()).isEqualTo(1);
    }

    @Test
    void removesPlacedItemAndRefundsCrystals() {
        PlacedRewardItemDto placed = placedItem("one", "forest-red-flower", 30, 30);
        RewardAlbumPicture picture = pictureWithItems(List.of(placed));
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));

        AlbumPictureDetailResponse updated = service.update(
                picture.getId(),
                new UpdatePictureRequest("Моя гора", List.of()),
                principal
        );

        assertThat(updated.placedItems()).isEmpty();
        verify(balanceService, never()).spend(any(), any(), any(), any(), any(), anyInt());
        verify(balanceService).refundRemovedItems(user, picture.getId(), List.of(placed));
    }

    @Test
    void rejectsSavingUnknownPlacedItem() {
        RewardAlbumPicture picture = pictureWithItems(List.of());
        when(pictureRepository.findByIdAndUser_Id(picture.getId(), 1L)).thenReturn(Optional.of(picture));

        PlacedRewardItemDto forged = placedItem("forged", "forest-red-flower", 40, 40);
        assertThatThrownBy(() -> service.update(picture.getId(), new UpdatePictureRequest("Не", List.of(forged)), principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Първо добави предмета");
        verify(balanceService, never()).refundRemovedItems(any(), any(), anyList());
    }

    private RewardAlbumPicture pictureWithItems(List<PlacedRewardItemDto> items) {
        return new RewardAlbumPicture(user, "Моя гора", "forest-meadow", writeItems(items));
    }

    private PlacedRewardItemDto placedItem(String id, String itemId, double x, double y) {
        RewardCatalogItemResponse item = catalog.item(itemId);
        return new PlacedRewardItemDto(id, item.id(), item.name(), item.image(), x, y, item.defaultScale(), 0, false, 1, java.time.Instant.now());
    }

    private List<PlacedRewardItemDto> readItems(RewardAlbumPicture picture) {
        try {
            return objectMapper.readValue(picture.getPlacedItemsJson(), PLACED_ITEMS);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private String writeItems(List<PlacedRewardItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
