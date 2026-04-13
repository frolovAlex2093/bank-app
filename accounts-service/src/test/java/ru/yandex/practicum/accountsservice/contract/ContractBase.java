package ru.yandex.practicum.accountsservice.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.accountsservice.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public abstract class ContractBase {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountRepository repository;

    @MockitoBean
    private RestClient restClient;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        Account account = new Account();
        account.setLogin("ivan_ivanov");
        account.setFirstName("Иван");
        account.setLastName("Иванов");
        account.setBirthDate(LocalDate.of(1990, 1, 1));
        account.setBalance(BigDecimal.valueOf(1000));
        repository.save(account);


        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .subject("ivan_ivanov")
                .claim("preferred_username", "ivan_ivanov")
                .claim("scope", "internal_api")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        RestAssuredMockMvc.mockMvc(mockMvc);

        RestAssuredMockMvc.requestSpecification = given()
                .header("Authorization", "Bearer fake-token");
    }
}