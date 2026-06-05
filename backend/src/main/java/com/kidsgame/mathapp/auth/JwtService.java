package com.kidsgame.mathapp.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final byte[] secret;
    private final long tokenTtlHours;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.security.jwt-secret}") String jwtSecret,
            @Value("${app.security.token-ttl-hours}") long tokenTtlHours,
            ObjectMapper objectMapper
    ) {
        this.secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtlHours = tokenTtlHours;
        this.objectMapper = objectMapper;
    }

    public String createToken(UserPrincipal principal) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", principal.getUsername());
            payload.put("uid", principal.id());
            payload.put("name", principal.displayName());
            payload.put("role", principal.role().name());
            payload.put("exp", Instant.now().plusSeconds(tokenTtlHours * 3600).getEpochSecond());

            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String signaturePart = sign(headerPart + "." + payloadPart);
            return headerPart + "." + payloadPart + "." + signaturePart;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create token", ex);
        }
    }

    public Optional<String> validateAndReadUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, MAP_TYPE);
            Object exp = payload.get("exp");
            if (!(exp instanceof Number expiration) || expiration.longValue() < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            Object subject = payload.get("sub");
            return subject instanceof String username ? Optional.of(username) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}

