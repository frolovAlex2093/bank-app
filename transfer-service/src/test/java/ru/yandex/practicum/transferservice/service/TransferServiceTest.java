package ru.yandex.practicum.transferservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.transferservice.dto.NotificationRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // разрешаем неиспользуемые стабы
class TransferServiceTest {

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private TransferService transferService;

    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        // Настройка PATCH
        when(restClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        // Настройка POST
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(NotificationRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
    }

    @Test
    @DisplayName("transfer — списывает у отправителя отрицательную сумму")
    void transfer_debitsFromSender() {
        transferService.transfer("ivan_ivanov", "petr_petrov", 300);

        verify(requestBodyUriSpec, atLeastOnce()).uri(
                eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("ivan_ivanov"),
                eq(BigDecimal.valueOf(-300))
        );
    }

    @Test
    @DisplayName("transfer — зачисляет получателю положительную сумму")
    void transfer_creditsToReceiver() {
        transferService.transfer("ivan_ivanov", "petr_petrov", 300);

        verify(requestBodyUriSpec, atLeastOnce()).uri(
                eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("petr_petrov"),
                eq(BigDecimal.valueOf(300))
        );
    }

    @Test
    @DisplayName("transfer — порядок вызовов: списание, зачисление, затем два уведомления")
    void transfer_orderOfCalls() {
        transferService.transfer("ivan_ivanov", "petr_petrov", 300);

        // Захватываем аргументы всех вызовов requestBodyUriSpec.uri(...)
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> arg1Captor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg2Captor = ArgumentCaptor.forClass(Object.class);

        verify(requestBodyUriSpec, times(2)).uri(uriCaptor.capture(), arg1Captor.capture(), arg2Captor.capture());

        List<String> uris = uriCaptor.getAllValues();
        List<Object> args1 = arg1Captor.getAllValues();
        List<Object> args2 = arg2Captor.getAllValues();

        // Проверяем первый вызов (списание)
        assertThat(uris.get(0)).isEqualTo("http://accounts-service/api/accounts/{login}/balance?amount={amount}");
        assertThat(args1.get(0)).isEqualTo("ivan_ivanov");
        assertThat(args2.get(0)).isEqualTo(BigDecimal.valueOf(-300));

        // Проверяем второй вызов (зачисление)
        assertThat(uris.get(1)).isEqualTo("http://accounts-service/api/accounts/{login}/balance?amount={amount}");
        assertThat(args1.get(1)).isEqualTo("petr_petrov");
        assertThat(args2.get(1)).isEqualTo(BigDecimal.valueOf(300));

        // Проверяем, что было два уведомления (POST)
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("transfer — отправляет два уведомления: отправителю и получателю")
    void transfer_sendsTwoNotifications() {
        transferService.transfer("ivan_ivanov", "petr_petrov", 300);

        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("transfer — при недостатке средств бросает исключение")
    void transfer_insufficientFunds_throws() {
        // Переопределяем только поведение toBodilessEntity для первого вызова (списание)
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.badRequest().build());

        assertThatThrownBy(() ->
                transferService.transfer("ivan_ivanov", "petr_petrov", 9999)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Недостаточно средств");

        verify(restClient, times(1)).patch(); // только списание
        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("transfer — если списание упало, зачисление не происходит")
    void transfer_debitFails_noCreditHappens() {
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.badRequest().build());

        assertThatThrownBy(() ->
                transferService.transfer("ivan_ivanov", "petr_petrov", 9999)
        ).isInstanceOf(IllegalStateException.class);

        verify(restClient, times(1)).patch(); // только списание
        verify(restClient, never()).post();
    }
}