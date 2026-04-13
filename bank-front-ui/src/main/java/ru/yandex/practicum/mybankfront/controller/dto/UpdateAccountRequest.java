package ru.yandex.practicum.mybankfront.controller.dto;

import java.time.LocalDate;

public record UpdateAccountRequest(String firstName, String lastName, LocalDate birthDate) {
}
