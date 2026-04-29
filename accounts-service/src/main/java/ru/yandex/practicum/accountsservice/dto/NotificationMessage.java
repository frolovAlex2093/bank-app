package ru.yandex.practicum.accountsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String accountId;
    private String action;
    private BigDecimal amount;
    private String message;
    private LocalDateTime timestamp;
}