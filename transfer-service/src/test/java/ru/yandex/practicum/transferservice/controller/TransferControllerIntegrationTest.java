package ru.yandex.practicum.transferservice.controller;

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
import ru.yandex.practicum.transferservice.service.TransferService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @Test
    @DisplayName("POST /api/transfer — 200 для авторизованного пользователя")
    void transfer_authorized() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .param("toLogin", "petr_petrov")
                        .param("amount", "300")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk());

        verify(transferService).transfer("ivan_ivanov", "petr_petrov", 300);
    }

    @Test
    @DisplayName("POST /api/transfer — 401 без токена")
    void transfer_unauthorized() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .param("toLogin", "petr_petrov")
                        .param("amount", "300"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(transferService);
    }

    @Test
    @DisplayName("POST /api/transfer — login из JWT, не из параметров запроса")
    void transfer_loginFromJwt_notFromParams() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .param("toLogin", "petr_petrov")
                        .param("amount", "100")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isOk());

        verify(transferService).transfer(
                eq("ivan_ivanov"),
                eq("petr_petrov"),
                eq(100)
        );
    }


    @Test
    @DisplayName("POST /api/transfer — нельзя перевести самому себе")
    void transfer_toSelf_badRequest() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .param("toLogin", "ivan_ivanov")
                        .param("amount", "100")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan_ivanov"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }
}