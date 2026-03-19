package ru.yandex.practicum.transferservice.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.transferservice.service.TransferService;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Validated
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transfer(
            @RequestParam String toLogin,
            @RequestParam
            @Positive(message = "Сумма должна быть положительной") int amount,
            JwtAuthenticationToken auth
    ) {
        String fromLogin = auth.getTokenAttributes()
                .get("preferred_username").toString();

        if (fromLogin.equals(toLogin)) {
            return ResponseEntity.badRequest().build();
        }

        transferService.transfer(fromLogin, toLogin, amount);
        return ResponseEntity.ok().build();
    }
}