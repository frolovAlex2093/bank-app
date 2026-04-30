package ru.yandex.practicum.transferservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.transferservice.dto.NotificationMessage;
import ru.yandex.practicum.transferservice.kafka.NotificationProducer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final RestClient restClient;
    private final NotificationProducer notificationProducer;
    private final MeterRegistry meterRegistry;

    private static final String ACCOUNTS_SERVICE = "accountsService";

    @Value("${services.accounts-url:http://accounts-service:8081}")
    private String accountsServiceUrl;

    @CircuitBreaker(name = ACCOUNTS_SERVICE, fallbackMethod = "transferFallback")
    @Retry(name = ACCOUNTS_SERVICE)
    public void transfer(String fromLogin, String toLogin, int amount) {
        BigDecimal amountBD = BigDecimal.valueOf(amount);
        log.info("Начало перевода: {} -> {} (сумма: {})", fromLogin, toLogin, amount);

        try {
            // Списание
            restClient.patch()
                    .uri(accountsServiceUrl + "/api/accounts/{login}/balance?amount={amount}",
                            fromLogin, amountBD.negate())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.warn("Недостаточно средств у {} для перевода", fromLogin);
                        throw new IllegalStateException("Недостаточно средств для перевода");
                    })
                    .toBodilessEntity();

            // Зачисление
            restClient.patch()
                    .uri(accountsServiceUrl + "/api/accounts/{login}/balance?amount={amount}",
                            toLogin, amountBD)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new RuntimeException("Ошибка зачисления");
                    })
                    .toBodilessEntity();

            sendNotifications(fromLogin, toLogin, amount);
            log.info("Перевод успешно завершен: {} -> {}", fromLogin, toLogin);

        } catch (Exception e) {
            log.error("Ошибка перевода: {} -> {}. Запуск компенсации. Причина: {}", fromLogin, toLogin, e.getMessage());
            meterRegistry.counter("bank.transfer.failed", "from", fromLogin, "to", toLogin).increment();
            if (!(e instanceof IllegalStateException)) {
                compensateDebit(fromLogin, amountBD);
            }
            throw new RuntimeException("Перевод не удался.", e);
        }
    }

    private void compensateDebit(String login, BigDecimal amount) {
        try {
            restClient.patch()
                    .uri(accountsServiceUrl + "/api/accounts/{login}/balance?amount={amount}",
                            login, amount)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("ОШИБКА: Не удалось вернуть деньги пользователю {}!", login);
        }
    }

    private void sendNotifications(String fromLogin, String toLogin, int amount) {
        BigDecimal amountBD = BigDecimal.valueOf(amount);

        NotificationMessage senderNotification = NotificationMessage.builder()
                .accountId(fromLogin)
                .action("TRANSFER_OUT")
                .amount(amountBD)
                .message("Перевод " + amount + " руб. пользователю " + toLogin)
                .timestamp(LocalDateTime.now())
                .build();
        notificationProducer.sendNotification(senderNotification);

        NotificationMessage receiverNotification = NotificationMessage.builder()
                .accountId(toLogin)
                .action("TRANSFER_IN")
                .amount(amountBD)
                .message("Получен перевод " + amount + " руб. от " + fromLogin)
                .timestamp(LocalDateTime.now())
                .build();
        notificationProducer.sendNotification(receiverNotification);
    }

    public void transferFallback(String fromLogin, String toLogin, int amount, Throwable t) {
        log.error("Fallback перевода: Сервис аккаунтов недоступен. Причина: {}", t.getMessage());
        meterRegistry.counter("bank.transfer.failed", "from", fromLogin, "to", toLogin).increment();
        throw new RuntimeException("Сервис переводов временно недоступен.");
    }
}