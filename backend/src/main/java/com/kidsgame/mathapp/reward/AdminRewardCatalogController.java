package com.kidsgame.mathapp.reward;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/reward-catalog")
public class AdminRewardCatalogController {
    private final AdminRewardCatalogService service;

    public AdminRewardCatalogController(AdminRewardCatalogService service) {
        this.service = service;
    }

    @GetMapping("/themes")
    public List<AdminRewardThemeResponse> themes() {
        return service.themes();
    }

    @PutMapping("/themes/{themeId}")
    public AdminRewardThemeResponse updateThemeStatus(
            @PathVariable String themeId,
            @Valid @RequestBody UpdateRewardThemeStatusRequest request
    ) {
        return service.updateThemeStatus(themeId, request.active());
    }

    @GetMapping
    public List<AdminRewardCatalogItemResponse> list() {
        return service.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminRewardCatalogItemResponse create(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) List<String> themeIds,
            @RequestParam(required = false) String themeId,
            @RequestParam String category,
            @RequestParam String name,
            @RequestParam String price,
            @RequestParam Double defaultScale,
            @RequestParam Double minScale,
            @RequestParam Double maxScale,
            @RequestParam(required = false) MultipartFile file
    ) {
        return service.create(id, themeIds, themeId, category, name, price, defaultScale, minScale, maxScale, file);
    }

    @PutMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminRewardCatalogItemResponse update(
            @PathVariable String itemId,
            @RequestParam(required = false) List<String> themeIds,
            @RequestParam(required = false) String themeId,
            @RequestParam String category,
            @RequestParam String name,
            @RequestParam String price,
            @RequestParam Double defaultScale,
            @RequestParam Double minScale,
            @RequestParam Double maxScale,
            @RequestParam(required = false) MultipartFile file
    ) {
        return service.update(itemId, themeIds, themeId, category, name, price, defaultScale, minScale, maxScale, file);
    }

    @PutMapping("/{itemId}/status")
    public AdminRewardCatalogItemResponse updateItemStatus(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateRewardCatalogItemStatusRequest request
    ) {
        return service.updateItemStatus(itemId, request.active());
    }

    @DeleteMapping("/{itemId}")
    public DeleteRewardCatalogItemResponse delete(@PathVariable String itemId) {
        return service.delete(itemId);
    }
}
