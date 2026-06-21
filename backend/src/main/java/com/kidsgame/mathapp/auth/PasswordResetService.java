package com.kidsgame.mathapp.auth;

import com.kidsgame.mathapp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class PasswordResetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier notifier;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long tokenTtlMinutes;
    private final String frontendBaseUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetNotifier notifier,
            @Value("${app.security.password-reset.token-ttl-minutes}") long tokenTtlMinutes,
            @Value("${app.security.password-reset.frontend-base-url}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notifier = notifier;
        this.tokenTtlMinutes = tokenTtlMinutes;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
            String token = createToken();
            tokenRepository.save(new PasswordResetToken(
                    hash(token), user, Instant.now().plusSeconds(tokenTtlMinutes * 60)
            ));
            try {
                notifier.send(user.getEmail(), frontendBaseUrl + "/reset-password?token=" + token);
            } catch (RuntimeException ex) {
                // The public response remains identical so this endpoint cannot be used to discover accounts.
                LOGGER.error("Could not deliver password reset instructions", ex);
            }
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(rawToken.trim()))
                .orElseThrow(this::invalidToken);
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw invalidToken();
        }
        token.getUser().updatePassword(passwordEncoder.encode(newPassword));
        token.markUsed();
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Линкът е невалиден или е изтекъл. Поискай нов.");
    }
}
