package ru.yandex.practicum.notificationsservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notificationsservice.dto.NotificationMessage;

@Slf4j
@Service
public class NotificationListener {

    @KafkaListener(topics = "bank-notifications", groupId = "notifications-group")
    public void handleNotification(NotificationMessage message) {
        log.info("======================================================");
        log.info("УВЕДОМЛЕНИЕ ОПЕРАЦИИ");
        log.info("Аккаунт: {}", message.getAccountId());
        log.info("Действие: {}", message.getAction());
        log.info("Сумма: {}", message.getAmount());
        log.info("Сообщение: {}", message.getMessage());
        log.info("Время: {}", message.getTimestamp());
        log.info("======================================================");

    }
}