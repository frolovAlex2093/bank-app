package ru.yandex.practicum.accountsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.accountsservice.repository.AccountRepository;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountRepository repository;

    @GetMapping("/me")
    public ResponseEntity<Account> getMyAccount(JwtAuthenticationToken auth) {
        String login = auth.getTokenAttributes().get("preferred_username").toString();
        return repository.findByLogin(login)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<Account> updateMyAccount(JwtAuthenticationToken auth, @RequestBody Account updatedAccount) {
        String login = auth.getTokenAttributes().get("preferred_username").toString();
        return repository.findByLogin(login).map(existing -> {
            existing.setFirstName(updatedAccount.getFirstName());
            existing.setLastName(updatedAccount.getLastName());
            existing.setBirthDate(updatedAccount.getBirthDate());
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/list")
    public List<AccountDto> getAllOtherAccounts(JwtAuthenticationToken auth) {
        String myLogin = auth.getTokenAttributes().get("preferred_username").toString();
        return repository.findAll().stream()
                .filter(a -> !a.getLogin().equals(myLogin))
                .map(a -> new AccountDto(a.getLogin(), a.getFirstName() + " " + a.getLastName()))
                .toList();
    }

    @PatchMapping("/{login}/balance")
    public ResponseEntity<Void> updateBalance(@PathVariable String login, @RequestParam BigDecimal amount) {
        Account account = repository.findByLogin(login).orElseThrow();
        account.setBalance(account.getBalance().add(amount));
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().build();
        }
        repository.save(account);
        return ResponseEntity.ok().build();
    }
}