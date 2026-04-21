package ru.yandex.practicum.mybankfront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient restClient(
            RestClient.Builder builder,
            @Value("${gateway.base-url:http://localhost:8080}") String gatewayBaseUrl
    ) {
        return builder.baseUrl(gatewayBaseUrl).build();
    }
}