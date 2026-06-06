package com.kidsgame.mathapp.admin;

import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccessTest {
    private final AdminAccess adminAccess = new AdminAccess("христо");

    @Test
    void allowsConfiguredParentAdminIgnoringCaseAndWhitespace() {
        var principal = principal(" Христо ", Role.PARENT);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                principal.getAuthorities()
        );

        assertThat(adminAccess.isAdmin(authentication)).isTrue();
    }

    @Test
    void blocksConfiguredUsernameWhenRoleIsChild() {
        var principal = principal("христо", Role.CHILD);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                principal.getAuthorities()
        );

        assertThat(adminAccess.isAdmin(authentication)).isFalse();
    }

    @Test
    void recognizesReservedAdminUsernamesForRegistrationGuards() {
        assertThat(adminAccess.isConfiguredAdminUsername(" Христо ")).isTrue();
        assertThat(adminAccess.isConfiguredAdminUsername("hristo")).isFalse();
    }

    private static UserPrincipal principal(String username, Role role) {
        UserEntity user = new UserEntity(username, username.trim(), "hash", role);
        ReflectionTestUtils.setField(user, "id", 42L);
        return new UserPrincipal(user);
    }
}
