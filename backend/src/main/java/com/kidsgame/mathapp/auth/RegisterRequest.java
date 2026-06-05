package com.kidsgame.mathapp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 60) String username,
        @NotBlank @Size(min = 4, max = 120) String password,
        @NotBlank @Size(min = 4, max = 120) String repeatPassword
) {
}
