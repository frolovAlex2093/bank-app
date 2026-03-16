package ru.yandex.practicum.cashservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cashservice.dto.NotificationRequest;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashService {

    private final RestClient restClient;

    public void processCash(String login, BigDecimal amount, String action) {
        BigDecimal delta = action.equals("PUT") ? amount : amount.negate();

        var response = restClient.patch()
                .uri("http://accounts-service/api/accounts/{login}/balance?amount={amount}", login, delta)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new IllegalStateException("Недостаточно средств");
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
}