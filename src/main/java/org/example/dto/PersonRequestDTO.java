package org.example.dto;

import java.time.LocalDate;

public record PersonRequestDTO (
        String firstName,
        String subName,
        String middleName,
        LocalDate birthDate,
        String number,
        String email
) {}
