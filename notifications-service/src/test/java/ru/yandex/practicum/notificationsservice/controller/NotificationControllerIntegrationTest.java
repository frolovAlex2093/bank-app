package ru.yandex.practicum.notificationsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.notificationsservice.dto.NotificationRequest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/notifications — 200 с валидным токеном")
    void notify_authorized_returns200() throws Exception {
        var request = new NotificationRequest("ivan_ivanov", "Пополнение счёта на 500 руб.");

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/notifications — 401 без токена")
    void notify_unauthorized_returns401() throws Exception {
        var request = new NotificationRequest("ivan_ivanov", "Пополнение счёта на 500 руб.");

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/notifications — 400 при пустом теле")
    void notify_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/notifications — 400 без login")
    void notify_missingLogin_returns400() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"текст\"}")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }
}