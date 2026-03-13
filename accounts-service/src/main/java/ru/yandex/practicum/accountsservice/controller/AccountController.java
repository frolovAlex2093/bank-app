package ru.yandex.practicum.accountsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.accountsservice.repository.AccountRepository;

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
}
