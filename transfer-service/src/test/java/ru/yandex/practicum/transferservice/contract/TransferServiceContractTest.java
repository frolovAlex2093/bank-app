package ru.yandex.practicum.transferservice.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureStubRunner(
        ids = "ru.yandex.practicum:accounts-service:+:stubs:8081",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class TransferServiceContractTest {
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

    @Test
    @DisplayName("Контракт: списание (отрицательная сумма) — 400 при недостатке")
    void contract_debit_overdraft_returns400() {
        var response = restClient.patch()
                .uri("http://localhost:8081/api/accounts/ivan_ivanov/balance?amount=-9999")
                .header("Authorization", "Bearer test-token")
                .retrieve()
                .onStatus(
                        status -> status.value() == 400,
                        (req, resp) -> {
                        }
                )
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Контракт: зачисление (положительная сумма) — 200")
    void contract_credit_returns200() {
        var response = restClient.patch()
                .uri("http://localhost:8081/api/accounts/ivan_ivanov/balance?amount=500")
                .header("Authorization", "Bearer test-token")
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}