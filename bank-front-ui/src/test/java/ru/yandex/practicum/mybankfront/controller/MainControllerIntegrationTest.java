package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Заменяем реальный RestClient на мок
    @MockitoBean
    private RestClient restClient;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    private JwtDecoder jwtDecoder;

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

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(token);
        when(authorizedClientService.loadAuthorizedClient(anyString(), anyString()))
                .thenReturn(authorizedClient);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString()))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AccountResponseDto.class)).thenReturn(ivanAccount);
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(otherAccounts);
    }

    @Test
    @DisplayName("GET /account — 200 и страница main для авторизованного пользователя")
    void getAccount_authenticated_returnsMainPage() throws Exception {
        mockMvc.perform(get("/account")
                        .with(oauth2Login().attributes(a ->
                                a.put("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("name", "birthdate", "sum", "accounts"));
    }

    @Test
    @DisplayName("GET /account — страница содержит имя пользователя")
    void getAccount_pageContainsUserName() throws Exception {
        mockMvc.perform(get("/account")
                        .with(oauth2Login().attributes(a ->
                                a.put("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Иван Иванов")));
    }

    @Test
    @DisplayName("GET /account — страница содержит баланс")
    void getAccount_pageContainsBalance() throws Exception {
        mockMvc.perform(get("/account")
                        .with(oauth2Login().attributes(a ->
                                a.put("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1000")));
    }

    @Test
    @DisplayName("GET / — редирект на /account")
    void index_redirectsToAccount() throws Exception {
        mockMvc.perform(get("/")
                        .with(oauth2Login()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
    }

    @Test
    @DisplayName("POST /account — невалидный возраст показывает ошибку на странице")
    void editAccount_underage_showsError() throws Exception {
        mockMvc.perform(post("/account")
                        .param("name", "Юный Пользователь")
                        .param("birthdate", LocalDate.now().minusYears(10).toString())
                        .with(oauth2Login().attributes(a ->
                                a.put("preferred_username", "ivan_ivanov")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(content().string(containsString("18")));
    }

    @Test
    @DisplayName("POST /cash — 200 для авторизованного пользователя")
    void editCash_authorized_returns200() throws Exception {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(
                org.springframework.http.ResponseEntity.ok().build()
        );

        mockMvc.perform(post("/cash")
                        .param("value", "500")
                        .param("action", "PUT")
                        .with(oauth2Login().attributes(a ->
                                a.put("preferred_username", "ivan_ivanov")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"));
    }

    @Test
    @DisplayName("POST /transfer — 200 для авторизованного пользователя")
    void transfer_authorized_returns200() throws Exception {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(), any())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(
                org.springframework.http.ResponseEntity.ok().build()
        );

        mockMvc.perform(post("/transfer")
                        .param("value", "300")
                        .param("login", "petr_petrov")
                        .with(oauth2Login().attributes(a ->
                                a.put("preferred_username", "ivan_ivanov")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"));
    }
}