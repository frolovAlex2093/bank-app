package ru.yandex.practicum.transferservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.transferservice.dto.NotificationRequest;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final RestClient restClient;

    public void transfer(String fromLogin, String toLogin, int amount) {
        BigDecimal amountBD = BigDecimal.valueOf(amount);

        var debitResponse = restClient.patch()
                .uri("http://accounts-service/api/accounts/{login}/balance?amount={amount}",
                        fromLogin, amountBD.negate())
                .retrieve()
                .toBodilessEntity();

        if (debitResponse.getStatusCode().isError()) {
            throw new IllegalStateException("Недостаточно средств для перевода");
        }

        restClient.patch()
                .uri("http://accounts-service/api/accounts/{login}/balance?amount={amount}",
                        toLogin, amountBD)
                .retrieve()
                .toBodilessEntity();

        restClient.post()
                .uri("http://notifications-service/api/notifications")
                .body(new NotificationRequest(fromLogin,
                        "Перевод " + amount + " руб. пользователю " + toLogin))
                .retrieve()
                .toBodilessEntity();

        restClient.post()
                .uri("http://notifications-service/api/notifications")
                .body(new NotificationRequest(toLogin,
                        "Получен перевод " + amount + " руб. от " + fromLogin))
                .retrieve()
                .toBodilessEntity();
    }
}