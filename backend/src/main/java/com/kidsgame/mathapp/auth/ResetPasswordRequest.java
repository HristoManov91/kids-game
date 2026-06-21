package com.kidsgame.mathapp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 4, max = 120) String password,
        @NotBlank @Size(min = 4, max = 120) String repeatPassword
) {
}
