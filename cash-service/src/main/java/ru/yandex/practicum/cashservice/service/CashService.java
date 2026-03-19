package ru.yandex.practicum.cashservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cashservice.dto.NotificationRequest;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final RestClient restClient;
    private static final String EXTERNAL_SERVICE = "externalService";

    @CircuitBreaker(name = EXTERNAL_SERVICE, fallbackMethod = "processCashFallback")
    @Retry(name = EXTERNAL_SERVICE)
    public void processCash(String login, BigDecimal amount, String action) {
        BigDecimal delta = action.equals("PUT") ? amount : amount.negate();

        log.info("Обработка наличных для {}: {} {}", login, action, amount);

        restClient.patch()
                .uri("http://accounts-service/api/accounts/{login}/balance?amount={amount}", login, delta)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new IllegalStateException("Недостаточно средств или неверный запрос");
                })
                .toBodilessEntity();

        String message = action.equals("PUT")
                ? "Пополнение счёта на сумму " + amount + " руб."
                : "Снятие со счёта на сумму " + amount + " руб.";

        restClient.post()
                .uri("http://notifications-service/api/notifications")
                .body(new NotificationRequest(login, message))
                .retrieve()
                .toBodilessEntity();
    }

    public void processCashFallback(String login, BigDecimal amount, String action, Throwable t) {
        log.error("Fallback для CashService. Причина: {}", t.getMessage());
        throw new RuntimeException("Операция с наличными временно недоступна. " + t.getMessage());
    }
}