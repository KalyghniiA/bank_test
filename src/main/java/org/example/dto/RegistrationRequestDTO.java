package org.example.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequestDTO(
        @NotNull @Valid PersonRequestDTO person,
        @NotNull @Valid CredentialsRequestDTO credentials
) {
}
