package ru.yandex.practicum.cashservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.cashservice.service.CashService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CashControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashService cashService;

    @MockitoBean
    private JwtDecoder jwtDecoder;  // мок, чтобы не было попыток реального соединения

    @Test
    @DisplayName("POST /api/cash?action=PUT — 200 для авторизованного пользователя")
    void cash_put_authorized() throws Exception {
        mockMvc.perform(post("/api/cash")
                        .param("value", "500")
                        .param("action", "PUT")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk());

        verify(cashService).processCash("ivan_ivanov", BigDecimal.valueOf(500), "PUT");
    }

    @Test
    @DisplayName("POST /api/cash?action=GET — 200 для авторизованного пользователя")
    void cash_get_authorized() throws Exception {
        mockMvc.perform(post("/api/cash")
                        .param("value", "200")
                        .param("action", "GET")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk());

        verify(cashService).processCash("ivan_ivanov", BigDecimal.valueOf(200), "GET");
    }

    @Test
    @DisplayName("POST /api/cash — 401 без токена")
    void cash_unauthorized() throws Exception {
        mockMvc.perform(post("/api/cash")
                        .param("value", "500")
                        .param("action", "PUT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/cash — 500 если сервис бросает исключение (недостаток средств)")
    void cash_serviceThrows_returns500() throws Exception {
        doThrow(new IllegalStateException("Недостаточно средств"))
                .when(cashService)
                .processCash(anyString(), any(), anyString());

        mockMvc.perform(post("/api/cash")
                        .param("value", "9999")
                        .param("action", "GET")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isInternalServerError());
    }
}