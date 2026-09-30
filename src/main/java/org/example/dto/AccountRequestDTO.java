package org.example.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountRequestDTO(
        UUID personId,
        BigDecimal balance,
        String type,
        Integer maxWithdrawalLimit,
        String overdraftLimit) {
}
