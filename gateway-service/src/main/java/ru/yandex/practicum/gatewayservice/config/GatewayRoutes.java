package ru.yandex.practicum.gatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutes {

    @Value("${services.accounts-url:http://accounts-service:8081}")
    private String accountsServiceUrl;
    @Value("${services.cash-url:http://cash-service:8082}")
    private String cashServiceUrl;
    @Value("${services.transfer-url:http://transfer-service:8083}")
    private String transferServiceUrl;


    @Bean
    public RouterFunction<ServerResponse> accountsRoute() {
        return route("accounts-route")
                .route(path("/api/accounts/**"), http(accountsServiceUrl))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> cashRoute() {
        return route("cash-route")
                .route(path("/api/cash/**"), http(cashServiceUrl))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transferRoute() {
        return route("transfer-route")
                .route(path("/api/transfer/**"), http(transferServiceUrl))
                .filter(tokenRelay())
                .build();
    }

}