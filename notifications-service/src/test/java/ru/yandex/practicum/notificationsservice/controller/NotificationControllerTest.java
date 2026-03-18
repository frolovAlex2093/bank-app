package ru.yandex.practicum.notificationsservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.notificationsservice.dto.NotificationRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final NotificationController controller = new NotificationController();

    @Test
    @DisplayName("notify — возвращает 200")
    void notify_returns200() {
        var request = new NotificationRequest("ivan_ivanov", "Пополнение счёта на 500 руб.");

        ResponseEntity<Void> response = controller.notify(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("notify — не падает при любом сообщении")
    void notify_anyMessage_noException() {
        var requests = java.util.List.of(
                new NotificationRequest("user1", "Пополнение счёта на 100 руб."),
                new NotificationRequest("user2", "Снятие 50 руб."),
                new NotificationRequest("user3", "Перевод 200 руб. пользователю user4"),
                new NotificationRequest("user4", "Получен перевод 200 руб. от user3")
        );

        requests.forEach(req -> {
            ResponseEntity<Void> response = controller.notify(req);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        });
    }
}