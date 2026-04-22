package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private static final String USER_SERVICE_ROUTE_ID = "user-service-route";
    private static final String USER_SERVICE_PATH = "/users/**";

    private final String userServiceUrl;

    public RouteConfig(@Value("${routes.user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(USER_SERVICE_ROUTE_ID, route -> route
                        .path(USER_SERVICE_PATH)
                        .uri(userServiceUrl))
                .build();
    }
}
