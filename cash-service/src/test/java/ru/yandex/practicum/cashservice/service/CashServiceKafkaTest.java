package ru.yandex.practicum.cashservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.cashservice.dto.NotificationMessage;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
class CashServiceKafkaTest {

    @Autowired
    private CashService cashService;

    @MockitoBean
    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    @Test
    void shouldSendDepositNotification() {
        String login = "alice";
        BigDecimal amount = BigDecimal.valueOf(1000);

        cashService.processCash(login, amount, "PUT");

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(login), captor.capture());

        NotificationMessage sentMessage = captor.getValue();
        assertEquals(login, sentMessage.getAccountId());
        assertEquals("DEPOSIT", sentMessage.getAction());
        assertEquals(amount, sentMessage.getAmount());
    }
}