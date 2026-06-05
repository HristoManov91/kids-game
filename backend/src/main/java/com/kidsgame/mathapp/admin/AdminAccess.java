package com.kidsgame.mathapp.admin;

import com.kidsgame.mathapp.auth.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component("adminAccess")
public class AdminAccess {
    private static final Locale BG = Locale.forLanguageTag("bg-BG");

    private final Set<String> adminUsernames;

    public AdminAccess(@Value("${app.admin.usernames:христо}") String configuredUsernames) {
        this.adminUsernames = Arrays.stream(configuredUsernames.split(","))
                .map(AdminAccess::normalize)
                .filter(username -> !username.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return adminUsernames.contains(normalize(principal.getUsername()));
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(BG);
    }
}
