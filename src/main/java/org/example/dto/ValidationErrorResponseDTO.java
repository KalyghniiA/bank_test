package org.example.dto;

import java.util.List;

public record ValidationErrorResponseDTO(String message, List<FieldErrorDTO> errors) {}