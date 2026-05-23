package com.ctn.offerwall.user.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String confirmPassword
) {
}
