package com.quickcommerce.dto;

import com.quickcommerce.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 60) String firstName,
            @Size(max = 60) String middleName,
            @Size(max = 60) String lastName,
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,14}$", message = "must be 10-14 digits") String phone,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 6, max = 72) String password) {
    }

    public record LoginRequest(@NotNull Role role, @NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, Role role, Long id, String name, String email) {
    }
}
