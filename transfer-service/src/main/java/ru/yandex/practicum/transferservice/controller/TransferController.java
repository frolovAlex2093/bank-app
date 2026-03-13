package ru.yandex.practicum.transferservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.transferservice.service.TransferService;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transfer(
            @RequestParam String toLogin,
            @RequestParam int amount,
            JwtAuthenticationToken auth
    ) {
        String fromLogin = auth.getTokenAttributes().get("preferred_username").toString();
        transferService.transfer(fromLogin, toLogin, amount);
        return ResponseEntity.ok().build();
    }
}