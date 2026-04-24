package com.example.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(
        @ConfigureWireMock(
                name = "user-service",
                baseUrlProperties = "routes.user-service.url"
        )
)
class PathRoutingIntegrationTest extends AbstractGatewayIntegrationTest {

    @InjectWireMock("user-service")
    private WireMockServer userServiceMock;

    @BeforeEach
    void resetWireMock() {
        userServiceMock.resetAll();
    }

    @Test
    @DisplayName("Path /users/** 요청은 user-service 로 그대로 전달된다")
    void route_usersPath_forwardsToUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Hong Gil Dong\"}")));

        webTestClient.get()
                .uri("/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Hong Gil Dong");

        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
    }

    @Test
    @DisplayName("/users 로 시작하지 않는 경로는 user-service 로 전달되지 않는다")
    void route_nonMatchingPath_isNotForwarded() {
        webTestClient.get()
                .uri("/products/1")
                .exchange()
                .expectStatus().isNotFound();

        userServiceMock.verify(0, getRequestedFor(urlPathEqualTo("/products/1")));
    }
}
