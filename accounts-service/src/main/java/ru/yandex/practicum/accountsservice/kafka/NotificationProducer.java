package ru.yandex.practicum.accountsservice.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accountsservice.dto.NotificationMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private static final String TOPIC = "bank-notifications";

    public void sendNotification(NotificationMessage notification) {
        log.debug("Попытка отправки уведомления в Kafka для: {}", notification.getAccountId());
        kafkaTemplate.send(TOPIC, notification.getAccountId(), notification)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Сообщение успешно отправлено в Kafka топик {}: {}", TOPIC, notification);
                    } else {
                        log.error("Ошибка отправки уведомления в Kafka для {}: {}", notification.getAccountId(), ex.getMessage());
                        meterRegistry.counter("bank.notification.failed", "login", notification.getAccountId()).increment();
                    }
                });
    }
}