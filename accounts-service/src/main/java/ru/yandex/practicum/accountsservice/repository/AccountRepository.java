package ru.yandex.practicum.accountsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.accountsservice.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLogin(String login);
}