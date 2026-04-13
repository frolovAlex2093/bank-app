package ru.yandex.practicum.notificationsservice.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.notificationsservice.controller.NotificationController;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;


@SpringBootTest
@ActiveProfiles("test")
public abstract class ContractBase {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.standaloneSetup(notificationController);
        RestAssuredMockMvc.requestSpecification = given()
                .header("Authorization", "Bearer fake-token");
    }
}