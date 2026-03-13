package ru.yandex.practicum.notificationsservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.notificationsservice.dto.NotificationRequest;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping
    public ResponseEntity<Void> notify(@RequestBody NotificationRequest request) {
        log.info("📬 Уведомление для [{}]: {}", request.login(), request.message());
        return ResponseEntity.ok().build();
    }
}