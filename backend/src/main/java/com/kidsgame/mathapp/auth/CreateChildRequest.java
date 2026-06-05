package com.kidsgame.mathapp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChildRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Size(min = 4, max = 80) String password,
        @NotBlank @Size(min = 2, max = 120) String displayName
) {
}

