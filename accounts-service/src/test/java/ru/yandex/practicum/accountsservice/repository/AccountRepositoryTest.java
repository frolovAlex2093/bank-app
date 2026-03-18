package ru.yandex.practicum.accountsservice.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.accountsservice.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AccountRepository repository;

    private Account makeAccount(String login) {
        Account a = new Account();
        a.setLogin(login);
        a.setFirstName("Тест");
        a.setLastName("Тестов");
        a.setBirthDate(LocalDate.of(1990, 1, 1));
        a.setBalance(BigDecimal.valueOf(500));
        return a;
    }

    @Test
    @DisplayName("findByLogin — находит существующий аккаунт")
    void findByLogin_exists() {
        em.persistAndFlush(makeAccount("test_user"));

        Optional<Account> result = repository.findByLogin("test_user");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Тест");
    }

    @Test
    @DisplayName("findByLogin — возвращает empty для несуществующего логина")
    void findByLogin_notFound() {
        Optional<Account> result = repository.findByLogin("nobody");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save — сохраняет и обновляет баланс")
    void save_updatesBalance() {
        Account saved = em.persistAndFlush(makeAccount("balance_user"));
        saved.setBalance(BigDecimal.valueOf(2000));
        repository.save(saved);
        em.flush();
        em.clear();

        Account found = repository.findByLogin("balance_user").orElseThrow();
        assertThat(found.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }
}