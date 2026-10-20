package org.example.dto;

import java.time.LocalDate;

public record PersonResponseDTO(
        String firstName,
        String subName,
        String middleName,
        LocalDate birthDate,
        String phoneNumber,
        String email
) {
}
