package com.kidsgame.mathapp.reward;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RewardAssetService {
    private static final long MAX_IMAGE_BYTES = 1_500_000;
    private static final Locale BG = Locale.forLanguageTag("bg-BG");
    private static final List<String> UPLOAD_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");

    private final RewardAssetRepository repository;

    public RewardAssetService(RewardAssetRepository repository) {
        this.repository = repository;
    }

    public RewardAssetEntity get(String assetId) {
        return repository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Картинката не е намерена."));
    }

    public RewardAssetEntity storeUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Качи картинка от компютър.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!UPLOAD_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Качи PNG, JPG или WEBP картинка. SVG файлове не се приемат от съображения за сигурност.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинката трябва да е до 1.5 MB.");
        }
        try {
            byte[] data = file.getBytes();
            String extension = extensionFor(contentType);
            String name = safeFileName(file.getOriginalFilename(), "reward-image" + extension);
            String id = "upload-" + UUID.randomUUID();
            return repository.save(new RewardAssetEntity(id, name, contentType, data, sha256(data), "ADMIN_UPLOAD"));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Картинката не може да бъде прочетена.");
        }
    }

    public RewardAssetEntity upsertStaticAsset(String staticPath, byte[] data) {
        String assetId = staticAssetId(staticPath);
        String fileName = fileName(staticPath);
        String contentType = contentTypeForPath(staticPath);
        String checksum = sha256(data);
        RewardAssetEntity asset = repository.findById(assetId)
                .orElseGet(() -> new RewardAssetEntity(assetId, fileName, contentType, data, checksum, "SEEDED"));
        if (!checksum.equals(asset.getChecksumSha256())) {
            asset.replace(fileName, contentType, data, checksum, "SEEDED");
        }
        return repository.save(asset);
    }

    public String publicUrl(String assetId) {
        return "/api/reward-assets/" + assetId;
    }

    public String publicUrlForStaticPath(String staticPath) {
        return publicUrl(staticAssetId(staticPath));
    }

    public boolean isStaticRewardAssetPath(String value) {
        return value != null && value.startsWith("/reward-assets/");
    }

    public String staticAssetId(String staticPath) {
        String normalized = staticPath == null ? "" : staticPath.trim();
        if (!normalized.startsWith("/reward-assets/")) {
            throw new IllegalArgumentException("Static reward asset path must start with /reward-assets/.");
        }
        String withoutPrefix = normalized.substring("/reward-assets/".length());
        int dot = withoutPrefix.lastIndexOf('.');
        if (dot > 0) {
            withoutPrefix = withoutPrefix.substring(0, dot);
        }
        String slug = withoutPrefix
                .replace('/', '-')
                .replaceAll("[^a-zA-Z0-9_-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.length() > 110) {
            slug = slug.substring(0, 110);
        }
        return "static-" + slug;
    }

    public String contentTypeForPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        throw new IllegalArgumentException("Unsupported reward asset type: " + path);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private String safeFileName(String requestedName, String fallback) {
        String normalized = requestedName == null ? "" : requestedName.trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        normalized = Normalizer.normalize(normalized.toLowerCase(BG), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('ъ', 'a')
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? fallback : normalized.substring(0, Math.min(180, normalized.length()));
    }

    private String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }
}
