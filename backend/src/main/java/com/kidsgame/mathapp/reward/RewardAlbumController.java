package com.kidsgame.mathapp.reward;

import com.kidsgame.mathapp.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/rewards")
public class RewardAlbumController {
    private final RewardAlbumService service;

    public RewardAlbumController(RewardAlbumService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public RewardCatalogResponse catalog() {
        return service.catalog();
    }

    @GetMapping("/balance")
    public RewardBalanceResponse balance(@AuthenticationPrincipal UserPrincipal principal) {
        return service.balance(principal);
    }

    @GetMapping("/pictures")
    public List<AlbumPictureSummaryResponse> pictures(@AuthenticationPrincipal UserPrincipal principal) {
        return service.pictures(principal);
    }

    @PostMapping("/pictures")
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumPictureDetailResponse create(
            @Valid @RequestBody CreatePictureRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return service.create(request, principal);
    }

    @GetMapping("/pictures/{pictureId}")
    public AlbumPictureDetailResponse get(@PathVariable UUID pictureId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.get(pictureId, principal);
    }

    @PutMapping("/pictures/{pictureId}")
    public AlbumPictureDetailResponse update(
            @PathVariable UUID pictureId,
            @Valid @RequestBody UpdatePictureRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return service.update(pictureId, request, principal);
    }

    @PostMapping("/pictures/{pictureId}/items")
    public PurchaseRewardItemResponse purchase(
            @PathVariable UUID pictureId,
            @Valid @RequestBody PurchaseRewardItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return service.purchase(pictureId, request, principal);
    }

    @DeleteMapping("/pictures/{pictureId}/items/{placedItemId}")
    public RemoveRewardItemResponse removeItem(
            @PathVariable UUID pictureId,
            @PathVariable String placedItemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return service.removeItem(pictureId, placedItemId, principal);
    }

    @DeleteMapping("/pictures/{pictureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID pictureId, @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(pictureId, principal);
    }
}
