package ru.yandex.practicum.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(GatewayServiceApplicationTests.TestOAuth2Config.class)
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class TestOAuth2Config {

        @Bean
        @Primary
        public ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }

        @Bean
        @Primary
        public OAuth2AuthorizedClientService oAuth2AuthorizedClientService() {
            return mock(OAuth2AuthorizedClientService.class);
        }

        @Bean
        @Primary
        public JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}