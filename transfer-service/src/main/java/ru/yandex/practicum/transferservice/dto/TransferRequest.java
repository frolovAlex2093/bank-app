package ru.yandex.practicum.transferservice.dto;

public record TransferRequest(String fromLogin, String toLogin, int amount) {
}
