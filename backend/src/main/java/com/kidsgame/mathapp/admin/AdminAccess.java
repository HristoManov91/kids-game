package com.kidsgame.mathapp.admin;

import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("adminAccess")
public class AdminAccess {
    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.role() == Role.ADMIN;
    }
}
