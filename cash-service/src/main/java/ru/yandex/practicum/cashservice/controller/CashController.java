package ru.yandex.practicum.cashservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.cashservice.service.CashService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;


    @PostMapping
    public ResponseEntity<Void> processCash(
            @RequestParam int value,
            @RequestParam String action,
            JwtAuthenticationToken auth
    ) {
        String login = auth.getTokenAttributes().get("preferred_username").toString();
        cashService.processCash(login, BigDecimal.valueOf(value), action);
        return ResponseEntity.ok().build();
    }
}
