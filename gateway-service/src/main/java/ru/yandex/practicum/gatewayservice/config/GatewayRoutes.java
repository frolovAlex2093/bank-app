package ru.yandex.practicum.gatewayservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions.tokenRelay;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutes {

    @Bean
    public RouterFunction<ServerResponse> accountsRoute() {
        return route("accounts-route")
                .route(path("/api/accounts/**"), http("lb://accounts-service"))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> cashRoute() {
        return route("cash-route")
                .route(path("/api/cash/**"), http("lb://cash-service"))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transferRoute() {
        return route("transfer-route")
                .route(path("/api/transfer/**"), http("lb://transfer-service"))
                .filter(tokenRelay())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationsRoute() {
        return route("notifications-route")
                .route(path("/api/notifications/**"), http("lb://notifications-service"))
                .filter(tokenRelay())
                .build();
    }
}