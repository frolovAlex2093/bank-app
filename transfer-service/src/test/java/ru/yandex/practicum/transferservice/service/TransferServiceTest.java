package ru.yandex.practicum.transferservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

    @Mock
    private RestClient restClient;

    @InjectMocks
    private TransferService transferService;

    private RestClient.RequestBodyUriSpec requestSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        requestSpec = mock(RestClient.RequestBodyUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.patch()).thenReturn(requestSpec);
        when(restClient.post()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(requestSpec);
        when(requestSpec.uri(anyString(), any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.uri(anyString(), any(), any())).thenReturn(requestSpec);
        when(requestSpec.body(any())).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
    }

    @Test
    @DisplayName("transfer — при недостатке средств бросает IllegalStateException")
    void transfer_insufficientFunds_throws() {
        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            RestClient.ResponseSpec.ErrorHandler handler = invocation.getArgument(1);
            handler.handle(null, null);
            return responseSpec;
        });

        assertThatThrownBy(() ->
                transferService.transfer("ivan_ivanov", "petr_petrov", 9999)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("transfer — Saga: возврат средств при ошибке зачисления")
    void transfer_saga_rollback() {

        when(responseSpec.onStatus(any(), any()))
                .thenReturn(responseSpec)
                .thenAnswer(inv -> {
                    inv.getArgument(1, RestClient.ResponseSpec.ErrorHandler.class).handle(null, null);
                    return responseSpec;
                });

        assertThatThrownBy(() ->
                transferService.transfer("ivan_ivanov", "petr_petrov", 300)
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Перевод не удался");

        verify(requestSpec).uri(eq("http://accounts-service/api/accounts/{login}/balance?amount={amount}"),
                eq("ivan_ivanov"),
                eq(BigDecimal.valueOf(300)));
    }
}