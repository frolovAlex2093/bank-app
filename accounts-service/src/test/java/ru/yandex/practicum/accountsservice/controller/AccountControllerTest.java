package ru.yandex.practicum.accountsservice.controller;

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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.accountsservice.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountControllerTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private AccountController controller;

    @Mock
    private JwtAuthenticationToken auth;

    private Account ivan;

    @BeforeEach
    void setUp() {
        ivan = new Account();
        ivan.setId(1L);
        ivan.setLogin("ivan_ivanov");
        ivan.setFirstName("Иван");
        ivan.setLastName("Иванов");
        ivan.setBirthDate(LocalDate.of(1990, 1, 1));
        ivan.setBalance(BigDecimal.valueOf(1000));

        when(auth.getTokenAttributes())
                .thenReturn(Map.of("preferred_username", "ivan_ivanov"));
    }

    @Test
    @DisplayName("getMyAccount — возвращает аккаунт текущего пользователя")
    void getMyAccount_found() {
        when(repository.findByLogin("ivan_ivanov")).thenReturn(Optional.of(ivan));

        ResponseEntity<Account> response = controller.getMyAccount(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogin()).isEqualTo("ivan_ivanov");
    }

    @Test
    @DisplayName("getMyAccount — 404 если аккаунт не найден")
    void getMyAccount_notFound() {
        when(repository.findByLogin("ivan_ivanov")).thenReturn(Optional.empty());

        ResponseEntity<Account> response = controller.getMyAccount(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("updateMyAccount — обновляет имя и дату рождения")
    void updateMyAccount_success() {
        Account updated = new Account();
        updated.setFirstName("Пётр");
        updated.setLastName("Петров");
        updated.setBirthDate(LocalDate.of(1995, 5, 15));

        when(repository.findByLogin("ivan_ivanov")).thenReturn(Optional.of(ivan));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<Account> response = controller.updateMyAccount(auth, updated);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getFirstName()).isEqualTo("Пётр");
        assertThat(response.getBody().getLastName()).isEqualTo("Петров");
    }

    @Test
    @DisplayName("getAllOtherAccounts — не возвращает текущего пользователя")
    void getAllOtherAccounts_excludesCurrentUser() {
        Account petr = new Account();
        petr.setLogin("petr_petrov");
        petr.setFirstName("Пётр");
        petr.setLastName("Петров");

        when(repository.findAll()).thenReturn(List.of(ivan, petr));

        var result = controller.getAllOtherAccounts(auth);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).login()).isEqualTo("petr_petrov");
    }

    @Test
    @DisplayName("updateBalance — успешно пополняет баланс")
    void updateBalance_deposit() {
        when(repository.findByLogin("ivan_ivanov")).thenReturn(Optional.of(ivan));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<Void> response =
                controller.updateBalance("ivan_ivanov", BigDecimal.valueOf(500));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(ivan.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    @DisplayName("updateBalance — 400 при недостатке средств")
    void updateBalance_insufficientFunds() {
        when(repository.findByLogin("ivan_ivanov")).thenReturn(Optional.of(ivan));

        ResponseEntity<Void> response =
                controller.updateBalance("ivan_ivanov", BigDecimal.valueOf(-5000));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}