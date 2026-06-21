package com.kidsgame.mathapp.auth;

import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordResetNotifier notifier;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        notifier = mock(PasswordResetNotifier.class);
        service = new PasswordResetService(
                userRepository, tokenRepository, passwordEncoder, notifier, 30, "https://kids.example/"
        );
    }

    @Test
    void createsOneTimeResetForKnownEmail() {
        UserEntity user = user("mila@example.com");
        when(userRepository.findByEmailIgnoreCase("mila@example.com")).thenReturn(Optional.of(user));

        service.requestReset(" MILA@example.com ");

        verify(tokenRepository).deleteByUser(user);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(notifier).send(
                org.mockito.ArgumentMatchers.eq("mila@example.com"),
                org.mockito.ArgumentMatchers.matches("https://kids\\.example/reset-password\\?token=[A-Za-z0-9_-]{43}")
        );
    }

    @Test
    void returnsSilentlyForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        service.requestReset("missing@example.com");

        verify(tokenRepository, never()).save(any());
        verify(notifier, never()).send(any(), any());
    }

    @Test
    void changesPasswordAndConsumesValidToken() {
        UserEntity user = user("mila@example.com");
        PasswordResetToken token = new PasswordResetToken("hash", user, Instant.now().plusSeconds(60));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.resetPassword("raw-token", "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void rejectsExpiredTokenWithoutChangingPassword() {
        UserEntity user = user("mila@example.com");
        PasswordResetToken token = new PasswordResetToken("hash", user, Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "new-password"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Линкът е невалиден");
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    private UserEntity user(String email) {
        UserEntity user = new UserEntity("mila", "Мила", "old-hash", Role.CHILD);
        user.updateEmail(email);
        return user;
    }
}
