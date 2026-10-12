package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PersonRequestDTO(
        @NotBlank String firstName,
        @NotBlank String subName,
        String middleName,
        @NotNull @Past LocalDate birthDate,
        String number,
        @Email String email
) {}
