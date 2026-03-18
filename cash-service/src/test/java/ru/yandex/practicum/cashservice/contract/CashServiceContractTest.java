package ru.yandex.practicum.cashservice.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureStubRunner(
        ids = "ru.yandex.practicum:accounts-service:+:stubs:8081",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class CashServiceContractTest {

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
        when(accessToken.getTokenValue()).thenReturn("test-token");
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any())).thenReturn(authorizedClient);
    }

    @Test
    @DisplayName("Контракт: PATCH /balance с положительной суммой возвращает 200")
    void contract_deposit_returns200() {
        var response = restClient.patch()
                .uri("http://localhost:8081/api/accounts/ivan_ivanov/balance?amount=500")
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Контракт: PATCH /balance с суммой -9999 возвращает 400")
    void contract_overdraft_returns400() {
        assertThatThrownBy(() ->
                restClient.patch()
                        .uri("http://localhost:8081/api/accounts/ivan_ivanov/balance?amount=-9999")
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(Exception.class)
                .hasMessageContaining("400");
    }
}