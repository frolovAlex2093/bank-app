package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponseDto;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.controller.dto.UpdateAccountRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class MainControllerTest {

    @Mock
    private RestClient restClient;

    @InjectMocks
    private MainController controller;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private final AccountResponseDto ivanAccount = new AccountResponseDto(
            "ivan_ivanov", "Иван", "Иванов",
            LocalDate.of(1990, 1, 1), BigDecimal.valueOf(1000)
    );

    private final List<AccountDto> otherAccounts = List.of(
            new AccountDto("petr_petrov", "Петров Пётр")
    );

    @BeforeEach
    void setUp() {
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        lenient().when(authorizedClient.getAccessToken()).thenReturn(accessToken);
    }

    private void mockGetAccount() {
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(contains("/me"))).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.header(anyString(), anyString()))
                .thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.body(AccountResponseDto.class)).thenReturn(ivanAccount);
    }

    private void mockGetList() {
        lenient().when(requestHeadersUriSpec.uri(contains("/list"))).thenReturn(requestHeadersSpec);
        lenient().when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(otherAccounts);
    }

    @Test
    @DisplayName("index — редиректит на /account")
    void index_redirectsToAccount() {
        String view = controller.index();
        assertThat(view).isEqualTo("redirect:/account");
    }

    @Test
    @DisplayName("getAccount — заполняет модель данными аккаунта")
    void getAccount_fillsModel() {
        mockGetAccount();
        mockGetList();
        Model model = new ConcurrentModel();

        String view = controller.getAccount(model, authorizedClient);

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("name")).isEqualTo("Иван Иванов");
        assertThat(model.getAttribute("sum")).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(model.getAttribute("accounts")).isEqualTo(otherAccounts);
    }

    @Test
    @DisplayName("getAccount — birthdate в формате ISO (YYYY-MM-DD)")
    void getAccount_birthdateFormat() {
        mockGetAccount();
        mockGetList();
        Model model = new ConcurrentModel();

        controller.getAccount(model, authorizedClient);

        assertThat(model.getAttribute("birthdate")).isEqualTo("1990-01-01");
    }

    @Test
    @DisplayName("editAccount — валидный возраст сохраняет данные")
    void editAccount_validAge_savesData() {
        mockGetAccount();
        mockGetList();

        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(contains("/me"))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(UpdateAccountRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AccountResponseDto.class)).thenReturn(ivanAccount);

        Model model = new ConcurrentModel();
        String view = controller.editAccount(
                model, "Иванов Иван", LocalDate.of(1990, 1, 1), authorizedClient
        );

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("errors")).isNull();
        assertThat(model.getAttribute("info")).isEqualTo("Данные успешно сохранены");
    }

    @Test
    @DisplayName("editAccount — возраст меньше 18 лет добавляет ошибку")
    void editAccount_underage_addsError() {
        mockGetAccount();
        mockGetList();
        Model model = new ConcurrentModel();

        String view = controller.editAccount(
                model, "Юный Пользователь",
                LocalDate.now().minusYears(17),
                authorizedClient
        );

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("errors")).isNotNull();
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) model.getAttribute("errors");
        assertThat(errors).anyMatch(e -> e.contains("18"));
    }

    @Test
    @DisplayName("editCash PUT — при успехе добавляет info в модель")
    void editCash_put_success() {
        mockGetAccount();
        mockGetList();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        Model model = new ConcurrentModel();
        String view = controller.editCash(model, 500, CashAction.PUT, authorizedClient);

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("info")).isNotNull();
        assertThat(model.getAttribute("errors")).isNull();
    }

    @Test
    @DisplayName("editCash GET — при ошибке добавляет errors в модель")
    void editCash_get_error() {
        mockGetAccount();
        mockGetList();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenThrow(new RuntimeException("Недостаточно средств"));

        Model model = new ConcurrentModel();
        String view = controller.editCash(model, 9999, CashAction.GET, authorizedClient);

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("errors")).isNotNull();
        assertThat(model.getAttribute("info")).isNull();
    }

    @Test
    @DisplayName("transfer — при успехе добавляет info в модель")
    void transfer_success() {
        mockGetAccount();
        mockGetList();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        Model model = new ConcurrentModel();
        String view = controller.transfer(model, 300, "petr_petrov", authorizedClient);

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("info")).isNotNull();
        assertThat(model.getAttribute("errors")).isNull();
    }

    @Test
    @DisplayName("transfer — при недостатке средств добавляет errors в модель")
    void transfer_insufficientFunds() {
        mockGetAccount();
        mockGetList();

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenThrow(new RuntimeException("Недостаточно средств для перевода"));

        Model model = new ConcurrentModel();
        String view = controller.transfer(model, 9999, "petr_petrov", authorizedClient);

        assertThat(view).isEqualTo("main");
        assertThat(model.getAttribute("errors")).isNotNull();
        assertThat(model.getAttribute("info")).isNull();
    }
}