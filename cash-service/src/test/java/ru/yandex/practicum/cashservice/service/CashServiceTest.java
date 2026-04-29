package ru.yandex.practicum.cashservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cashservice.dto.NotificationMessage;
import ru.yandex.practicum.cashservice.kafka.NotificationProducer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private CashService cashService;

    @Captor
    private ArgumentCaptor<NotificationMessage> notificationCaptor;

    private RestClient.RequestBodyUriSpec patchUriSpec;
    private RestClient.RequestBodySpec patchBodySpec;
    private RestClient.ResponseSpec patchResponseSpec;

    @BeforeEach
    void setUp() {
        patchUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        patchBodySpec = mock(RestClient.RequestBodySpec.class);
        patchResponseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), any(), any())).thenReturn(patchBodySpec);
        when(patchBodySpec.retrieve()).thenReturn(patchResponseSpec);
        when(patchResponseSpec.onStatus(any(), any())).thenReturn(patchResponseSpec);
        when(patchResponseSpec.toBodilessEntity()).thenReturn(mock(ResponseEntity.class));
    }

    @Test
    @DisplayName("PUT — пополнение: отправляет положительную сумму в Accounts и шлет уведомление")
    void processCash_put_sendsPositiveAmount() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(500), "PUT");

        verify(patchUriSpec).uri(eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("ivan_ivanov"), eq(BigDecimal.valueOf(500)));
        verify(patchResponseSpec).toBodilessEntity();

        verify(notificationProducer).sendNotification(any(NotificationMessage.class));
    }

    @Test
    @DisplayName("GET — снятие: отправляет отрицательную сумму в Accounts")
    void processCash_get_sendsNegativeAmount() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(300), "GET");

        verify(patchUriSpec).uri(eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("ivan_ivanov"), eq(BigDecimal.valueOf(-300)));
        verify(patchResponseSpec).toBodilessEntity();

        verify(notificationProducer).sendNotification(any(NotificationMessage.class));
    }

    @Test
    @DisplayName("PUT — после успешной операции отправляет корректное уведомление")
    void processCash_put_sendsNotification() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(500), "PUT");

        verify(notificationProducer).sendNotification(notificationCaptor.capture());
        NotificationMessage sent = notificationCaptor.getValue();

        assertThat(sent.getAccountId()).isEqualTo("ivan_ivanov");
        assertThat(sent.getAction()).isEqualTo("DEPOSIT");
        assertThat(sent.getMessage()).contains("Пополнение счёта на сумму 500");
    }

    @Test
    @DisplayName("GET — при ошибке Accounts (4xx) бросает исключение, уведомление не отправляется")
    void processCash_accountsError_throwsAndNoNotification() {
        when(patchResponseSpec.toBodilessEntity()).thenThrow(new IllegalStateException("Недостаточно средств"));

        assertThatThrownBy(() ->
                cashService.processCash("ivan_ivanov", BigDecimal.valueOf(9999), "GET")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Недостаточно средств");

        verify(notificationProducer, never()).sendNotification(any());
    }
}