package ru.yandex.practicum.transferservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.transferservice.dto.NotificationRequest;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final RestClient restClient;
    private static final String ACCOUNTS_SERVICE = "accountsService";

    @CircuitBreaker(name = ACCOUNTS_SERVICE, fallbackMethod = "transferFallback")
    @Retry(name = ACCOUNTS_SERVICE)
    public void transfer(String fromLogin, String toLogin, int amount) {
        BigDecimal amountBD = BigDecimal.valueOf(amount);

        log.info("Начало перевода: {} -> {} (сумма: {})", fromLogin, toLogin, amount);

        restClient.patch()
                .uri("http://accounts-service:8081/api/accounts/{login}/balance?amount={amount}",
                        fromLogin, amountBD.negate())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new IllegalStateException("Недостаточно средств для перевода");
                })
                .toBodilessEntity();

        try {
            restClient.patch()
                    .uri("http://accounts-service:8081/api/accounts/{login}/balance?amount={amount}",
                            toLogin, amountBD)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new RuntimeException("Ошибка зачисления");
                    })
                    .toBodilessEntity();

            sendNotifications(fromLogin, toLogin, amount);

        } catch (Exception e) {
            log.error("Сбой. Запуск компенсации: {}", e.getMessage());
            compensateDebit(fromLogin, amountBD);
            throw new RuntimeException("Перевод не удался. Средства возвращены отправителю.", e);
        }
    }

    private void compensateDebit(String login, BigDecimal amount) {
        try {
            restClient.patch()
                    .uri("http://accounts-service:8081/api/accounts/{login}/balance?amount={amount}",
                            login, amount)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("ОШИБКА: Не удалось вернуть деньги пользователю {}!", login);
        }
    }

    private void sendNotifications(String fromLogin, String toLogin, int amount) {
        restClient.post()
                .uri("http://notifications-service:8084/api/notifications")
                .body(new NotificationRequest(fromLogin, "Перевод " + amount + " руб. пользователю " + toLogin))
                .retrieve()
                .toBodilessEntity();

        restClient.post()
                .uri("http://notifications-service:8084/api/notifications")
                .body(new NotificationRequest(toLogin, "Получен перевод " + amount + " руб. от " + fromLogin))
                .retrieve()
                .toBodilessEntity();
    }

    public void transferFallback(String fromLogin, String toLogin, int amount, Throwable t) {
        log.error("Fallback: Сервис аккаунтов недоступен. Причина: {}", t.getMessage());
        throw new RuntimeException("Сервис переводов временно недоступен.");
    }
}