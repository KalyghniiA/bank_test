package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CredentialsRequestDTO(
        @NotBlank @Size(min = 3, max = 100) String login,
        @NotBlank @Size(min = 8, message = "Пароль должен быть не короче 8 символов") String password
) {}
