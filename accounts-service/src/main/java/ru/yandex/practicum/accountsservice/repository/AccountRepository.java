package ru.yandex.practicum.accountsservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.accountsservice.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLogin(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.login = :login")
    Optional<Account> findWithLockByLogin(String login);
}