package ru.yandex.practicum.cashservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cashservice.dto.NotificationMessage;
import ru.yandex.practicum.cashservice.kafka.NotificationProducer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final RestClient restClient;
    private final NotificationProducer notificationProducer;
    private final MeterRegistry meterRegistry;

    private static final String EXTERNAL_SERVICE = "externalService";

    @Value("${services.accounts-url:http://accounts-service:8081}")
    private String accountsServiceUrl;

    @CircuitBreaker(name = EXTERNAL_SERVICE, fallbackMethod = "processCashFallback")
    @Retry(name = EXTERNAL_SERVICE)
    public void processCash(String login, BigDecimal amount, String action) {
        BigDecimal delta = action.equals("PUT") ? amount : amount.negate();
        log.info("Начало обработки наличных. Логин: {}, Действие: {}, Сумма: {}", login, action, amount);

        try {
            restClient.patch()
                    .uri(accountsServiceUrl + "/api/accounts/{login}/balance?amount={amount}", login, delta)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.warn("Отказ в операции для {}. Недостаточно средств или неверный запрос.", login);
                        throw new IllegalStateException("Недостаточно средств или неверный запрос");
                    })
                    .toBodilessEntity();

            log.debug("Успешное обновление баланса в accounts-service для {}", login);
        } catch (Exception e) {
            if (action.equals("GET")) {
                meterRegistry.counter("bank.cash.withdraw.failed", "login", login).increment();
            }
            log.error("Ошибка при обработке наличных для {}: {}", login, e.getMessage(), e);
            throw e;
        }

        String message = action.equals("PUT")
                ? "Пополнение счёта на сумму " + amount + " руб."
                : "Снятие со счёта на сумму " + amount + " руб.";

        String kafkaAction = action.equals("PUT") ? "DEPOSIT" : "WITHDRAW";

        NotificationMessage notification = NotificationMessage.builder()
                .accountId(login)
                .action(kafkaAction)
                .amount(amount)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        notificationProducer.sendNotification(notification);
    }

    public void processCashFallback(String login, BigDecimal amount, String action, Throwable t) {
        if (action.equals("GET")) {
            meterRegistry.counter("bank.cash.withdraw.failed", "login", login).increment();
        }
        log.error("Сработал Fallback для CashService. Логин: {}. Причина: {}", login, t.getMessage());
        throw new RuntimeException("Операция с наличными временно недоступна. " + t.getMessage());
    }
}