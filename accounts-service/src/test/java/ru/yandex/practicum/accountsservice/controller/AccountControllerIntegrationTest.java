package ru.yandex.practicum.accountsservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.accountsservice.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestClient restClient;

    @Autowired
    private AccountRepository repository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private JwtDecoder jwtDecoder;


    @BeforeEach
    void setUp() {
        repository.deleteAll();

        Account account = new Account();
        account.setLogin("ivan_ivanov");
        account.setFirstName("Иван");
        account.setLastName("Иванов");
        account.setBirthDate(LocalDate.of(1990, 1, 1));
        account.setBalance(BigDecimal.valueOf(1000));
        repository.save(account);
    }

    @Test
    @DisplayName("GET /api/accounts/me — 200 для авторизованного пользователя")
    void getMe_authorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ivan_ivanov"))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.balance").value(1000.0));
    }

    @Test
    @DisplayName("GET /api/accounts/me — 401 без токена")
    void getMe_unauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/accounts/me — 404 если аккаунт не заведён")
    void getMe_accountNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/me")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "unknown_user"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/accounts/{login}/balance — пополнение баланса")
    void updateBalance_deposit() throws Exception {
        mockMvc.perform(patch("/api/accounts/ivan_ivanov/balance")
                        .param("amount", "500")
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/accounts/{login}/balance — 400 при уходе в минус")
    void updateBalance_overdraft() throws Exception {
        mockMvc.perform(patch("/api/accounts/ivan_ivanov/balance")
                        .param("amount", "-9999")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/accounts/list — не содержит текущего пользователя")
    void getList_excludesSelf() throws Exception {
        Account petr = new Account();
        petr.setLogin("petr_petrov");
        petr.setFirstName("Пётр");
        petr.setLastName("Петров");
        petr.setBirthDate(LocalDate.of(1992, 3, 3));
        petr.setBalance(BigDecimal.ZERO);
        repository.save(petr);

        mockMvc.perform(get("/api/accounts/list")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].login").value("petr_petrov"));
    }

}