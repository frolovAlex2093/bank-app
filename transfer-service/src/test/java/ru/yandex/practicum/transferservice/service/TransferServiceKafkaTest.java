package ru.yandex.practicum.transferservice.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.transferservice.dto.NotificationMessage;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class TransferServiceKafkaTest {

    @Autowired
    private TransferService transferService;

    @MockitoBean
    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    @Test
    void shouldSendTwoNotificationsOnTransfer() {
        String fromLogin = "bob";
        String toLogin = "alice";
        int amount = 500;

        transferService.transfer(fromLogin, toLogin, amount);

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(kafkaTemplate, times(2)).send(eq("bank-notifications"), anyString(), captor.capture());

        List<NotificationMessage> messages = captor.getAllValues();
        assertEquals(2, messages.size());

        NotificationMessage senderMsg = messages.get(0);
        assertEquals(fromLogin, senderMsg.getAccountId());
        assertEquals("TRANSFER_OUT", senderMsg.getAction());
        assertEquals(BigDecimal.valueOf(amount), senderMsg.getAmount());

        NotificationMessage receiverMsg = messages.get(1);
        assertEquals(toLogin, receiverMsg.getAccountId());
        assertEquals("TRANSFER_IN", receiverMsg.getAction());
        assertEquals(BigDecimal.valueOf(amount), receiverMsg.getAmount());
    }
}