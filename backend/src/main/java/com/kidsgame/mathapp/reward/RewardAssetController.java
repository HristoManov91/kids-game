package com.kidsgame.mathapp.reward;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/reward-assets")
public class RewardAssetController {
    private final RewardAssetService service;

    public RewardAssetController(RewardAssetService service) {
        this.service = service;
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<byte[]> image(@PathVariable String assetId) {
        RewardAssetEntity asset = service.get(assetId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(asset.getData());
    }
}
