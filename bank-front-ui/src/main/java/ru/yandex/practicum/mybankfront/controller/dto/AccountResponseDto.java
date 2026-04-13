package ru.yandex.practicum.mybankfront.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponseDto(
        String login,
        String firstName,
        String lastName,
        LocalDate birthDate,
        BigDecimal balance
) {
}
