package ru.yandex.practicum.accountsservice.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.accountsservice.dto.NotificationMessage;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.accountsservice.repository.AccountRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerKafkaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    @Test
    void shouldSendProfileUpdateNotification() throws Exception {
        String login = "testuser";

        Account existingAccount = new Account();
        existingAccount.setLogin(login);
        when(accountRepository.findByLogin(login)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(existingAccount)).thenReturn(existingAccount);

        mockMvc.perform(put("/api/accounts/me")
                        .with(jwt().jwt(builder -> builder.claim("preferred_username", login)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ivan\", \"lastName\":\"Ivanov\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(kafkaTemplate).send(eq("bank-notifications"), eq(login), captor.capture());

        NotificationMessage sentMessage = captor.getValue();
        assertEquals(login, sentMessage.getAccountId());
        assertEquals("PROFILE_UPDATE", sentMessage.getAction());
    }
}