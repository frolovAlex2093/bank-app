package ru.yandex.practicum.mybankfront.controller.dto;

public record TransferRequest(String toLogin, int amount) {
}
