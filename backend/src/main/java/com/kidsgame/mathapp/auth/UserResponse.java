package com.kidsgame.mathapp.auth;

import com.kidsgame.mathapp.user.Role;

public record UserResponse(Long id, String username, String displayName, Role role) {
    public static UserResponse from(UserPrincipal principal) {
        return new UserResponse(principal.id(), principal.getUsername(), principal.displayName(), principal.role());
    }
}

