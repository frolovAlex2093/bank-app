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
import ru.yandex.practicum.cashservice.dto.NotificationRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // разрешаем лишние стабы, чтобы избежать UnnecessaryStubbingException
class CashServiceTest {

    @Mock
    private RestClient restClient;

    @InjectMocks
    private CashService cashService;

    @Captor
    private ArgumentCaptor<NotificationRequest> notificationCaptor;

    // Моки для цепочек вызовов
    private RestClient.RequestBodyUriSpec patchUriSpec;
    private RestClient.RequestBodySpec patchBodySpec;
    private RestClient.ResponseSpec patchResponseSpec;
    private RestClient.RequestBodyUriSpec postUriSpec;
    private RestClient.RequestBodySpec postBodySpec;
    private RestClient.ResponseSpec postResponseSpec;

    @BeforeEach
    void setUp() {
        patchUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        patchBodySpec = mock(RestClient.RequestBodySpec.class);
        patchResponseSpec = mock(RestClient.ResponseSpec.class);
        postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        postBodySpec = mock(RestClient.RequestBodySpec.class);
        postResponseSpec = mock(RestClient.ResponseSpec.class);

        // Настройка PATCH
        when(restClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), any(), any())).thenReturn(patchBodySpec);
        when(patchBodySpec.retrieve()).thenReturn(patchResponseSpec);
        when(patchResponseSpec.onStatus(any(), any())).thenReturn(patchResponseSpec);
        when(patchResponseSpec.toBodilessEntity()).thenReturn(mock(ResponseEntity.class));

        // Настройка POST (для уведомлений)
        when(restClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.body(any(NotificationRequest.class))).thenReturn(postBodySpec);
        when(postBodySpec.retrieve()).thenReturn(postResponseSpec);
        when(postResponseSpec.toBodilessEntity()).thenReturn(mock(ResponseEntity.class));
    }

    @Test
    @DisplayName("PUT — пополнение: отправляет положительную сумму в Accounts")
    void processCash_put_sendsPositiveAmount() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(500), "PUT");

        verify(patchUriSpec).uri(eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("ivan_ivanov"), eq(BigDecimal.valueOf(500)));
        verify(patchResponseSpec).onStatus(any(), any());
        verify(patchResponseSpec).toBodilessEntity();
        verify(postUriSpec).uri("http://notifications-service/api/notifications");
        verify(postBodySpec).body(any(NotificationRequest.class));
    }

    @Test
    @DisplayName("GET — снятие: отправляет отрицательную сумму в Accounts")
    void processCash_get_sendsNegativeAmount() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(300), "GET");

        verify(patchUriSpec).uri(eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("ivan_ivanov"), eq(BigDecimal.valueOf(-300)));
        verify(patchResponseSpec).toBodilessEntity();
        verify(postBodySpec).body(any(NotificationRequest.class));
    }

    @Test
    @DisplayName("PUT — после успешной операции отправляет уведомление")
    void processCash_put_sendsNotification() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(500), "PUT");

        verify(postBodySpec).body(notificationCaptor.capture());
        NotificationRequest sent = notificationCaptor.getValue();
        assertThat(sent.login()).isEqualTo("ivan_ivanov");
        assertThat(sent.message()).contains("Пополнение");
        verify(postResponseSpec).toBodilessEntity();
    }

    @Test
    @DisplayName("GET — при ошибке Accounts (4xx) бросает исключение, уведомление не отправляется")
    void processCash_accountsError_throwsAndNoNotification() {
        // Переопределяем только поведение toBodilessEntity для PATCH
        when(patchResponseSpec.toBodilessEntity()).thenThrow(new IllegalStateException("Недостаточно средств"));

        assertThatThrownBy(() ->
                cashService.processCash("ivan_ivanov", BigDecimal.valueOf(9999), "GET")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Недостаточно средств");

        verify(patchResponseSpec).onStatus(any(), any());
        verify(patchResponseSpec).toBodilessEntity();
        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("PUT — проверка, что уведомление содержит корректное сообщение для пополнения")
    void processCash_notificationMessageForPut() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(150), "PUT");

        verify(postBodySpec).body(notificationCaptor.capture());
        NotificationRequest sent = notificationCaptor.getValue();
        assertThat(sent.message()).isEqualTo("Пополнение счёта на сумму 150 руб.");
    }

    @Test
    @DisplayName("GET — проверка, что уведомление содержит корректное сообщение для снятия")
    void processCash_notificationMessageForGet() {
        cashService.processCash("ivan_ivanov", BigDecimal.valueOf(70), "GET");

        verify(postBodySpec).body(notificationCaptor.capture());
        NotificationRequest sent = notificationCaptor.getValue();
        assertThat(sent.message()).isEqualTo("Снятие со счёта на сумму 70 руб.");
    }
}