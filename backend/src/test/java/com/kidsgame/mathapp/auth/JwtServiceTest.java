package com.kidsgame.mathapp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void validatesUntamperedTokenAndReadsSubject() {
        JwtService service = new JwtService("test-secret-with-enough-entropy", 12, objectMapper);

        String token = service.createToken(principal("mila"));

        assertThat(service.validateAndReadUsername(token)).contains("mila");
    }

    @Test
    void rejectsTokenWhenPayloadIsTampered() {
        JwtService service = new JwtService("test-secret-with-enough-entropy", 12, objectMapper);
        String token = service.createToken(principal("mila"));
        String[] parts = token.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));

        assertThat(service.validateAndReadUsername(parts[0] + "." + tamperedPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService("test-secret-with-enough-entropy", -1, objectMapper);

        String token = service.createToken(principal("mila"));

        assertThat(service.validateAndReadUsername(token)).isEmpty();
    }

    @Test
    void rejectsMalformedToken() {
        JwtService service = new JwtService("test-secret-with-enough-entropy", 12, objectMapper);

        assertThat(service.validateAndReadUsername("not-a-token")).isEmpty();
        assertThat(service.validateAndReadUsername("one.two.three.four")).isEmpty();
    }

    private UserPrincipal principal(String username) {
        UserEntity user = new UserEntity(username, "Мила", "hash", Role.CHILD);
        ReflectionTestUtils.setField(user, "id", 42L);
        return new UserPrincipal(user);
    }
}
