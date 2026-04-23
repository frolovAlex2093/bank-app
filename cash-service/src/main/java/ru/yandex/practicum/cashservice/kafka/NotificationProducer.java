package ru.yandex.practicum.cashservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cashservice.dto.NotificationMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;
    private static final String TOPIC = "bank-notifications";

    public void sendNotification(NotificationMessage notification) {
        kafkaTemplate.send(TOPIC, notification.getAccountId(), notification);
        log.info("Сообщение отправлено в Kafka топик {}: {}", TOPIC, notification);
    }
}