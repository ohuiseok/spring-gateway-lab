package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private static final String USER_SERVICE_ROUTE_ID = "user-service-route";
    private static final String USER_SERVICE_V2_ROUTE_ID = "user-service-v2-route";
    private static final String USER_SERVICE_PATH = "/users/**";
    private static final String VERSION_HEADER_NAME = "X-Version";
    private static final String VERSION_V2_HEADER_VALUE = "v2";

    private final String userServiceUrl;
    private final String userServiceV2Url;

    public RouteConfig(
            @Value("${routes.user-service.url:http://localhost:8081}") String userServiceUrl,
            @Value("${routes.user-service-v2.url:http://localhost:8082}") String userServiceV2Url
    ) {
        this.userServiceUrl = userServiceUrl;
        this.userServiceV2Url = userServiceV2Url;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(USER_SERVICE_V2_ROUTE_ID, route -> route
                        .path(USER_SERVICE_PATH)
                        .and()
                        .header(VERSION_HEADER_NAME, VERSION_V2_HEADER_VALUE)
                        .uri(userServiceV2Url))
                .route(USER_SERVICE_ROUTE_ID, route -> route
                        .path(USER_SERVICE_PATH)
                        .uri(userServiceUrl))
                .build();
    }
}
