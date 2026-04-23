package ru.yandex.practicum.notificationsservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.yandex.practicum.notificationsservice.dto.NotificationMessage;
import ru.yandex.practicum.notificationsservice.kafka.NotificationListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    @MockitoBean
    private NotificationListener notificationListener;

    @Test
    void shouldReceiveAndProcessNotification() {
        NotificationMessage message = NotificationMessage.builder()
                .accountId("user123")
                .action("DEPOSIT")
                .amount(BigDecimal.valueOf(5000))
                .message("Тестовое пополнение")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send("bank-notifications", message.getAccountId(), message);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(notificationListener).handleNotification(any(NotificationMessage.class));
        });
    }
}

