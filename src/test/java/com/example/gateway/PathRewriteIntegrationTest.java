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
@EnableWireMock({
        @ConfigureWireMock(
                name = "user-service",
                baseUrlProperties = "routes.user-service.url"
        ),
        @ConfigureWireMock(
                name = "user-service-v2",
                baseUrlProperties = "routes.user-service-v2.url"
        )
})
class PathRewriteIntegrationTest extends AbstractGatewayIntegrationTest {

    @InjectWireMock("user-service")
    private WireMockServer userServiceMock;

    @InjectWireMock("user-service-v2")
    private WireMockServer userServiceV2Mock;

    @BeforeEach
    void resetWireMocks() {
        userServiceMock.resetAll();
        userServiceV2Mock.resetAll();
    }

    @Test
    @DisplayName("/api/users/1 요청은 /users/1 로 재작성되어 user-service 로 전달된다")
    void route_apiUsersPath_rewritesPathAndForwardsToUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Hong Gil Dong\"}")));

        webTestClient.get()
                .uri("/api/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Hong Gil Dong");

        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
        userServiceMock.verify(0, getRequestedFor(urlPathEqualTo("/api/users/1")));
    }

    @Test
    @DisplayName("/api/users/1/profile 같은 중첩 경로도 /users/1/profile 로 재작성되어 전달된다")
    void route_apiUsersNestedPath_rewritesAndForwardsToUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1/profile"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"profile\":\"dev\"}")));

        webTestClient.get()
                .uri("/api/users/1/profile")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.profile").isEqualTo("dev");

        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1/profile")));
        userServiceMock.verify(0, getRequestedFor(urlPathEqualTo("/api/users/1/profile")));
    }

    @Test
    @DisplayName("/api 접두이지만 /api/users/** 가 아닌 /api/products/1 은 어떤 라우트에도 매칭되지 않아 404 를 반환한다")
    void route_apiNonUsersPath_isNotForwardedAndReturns404() {
        webTestClient.get()
                .uri("/api/products/1")
                .exchange()
                .expectStatus().isNotFound();

        userServiceMock.verify(0, getRequestedFor(urlPathEqualTo("/api/products/1")));
        userServiceMock.verify(0, getRequestedFor(urlPathEqualTo("/products/1")));
    }

    @Test
    @DisplayName("/api/users/** 에 X-Version: v2 헤더를 실어도 api 라우트가 먼저 매칭되어 기본 user-service 로 전달된다")
    void route_apiUsersPathWithV2Header_stillForwardsToDefaultUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"version\":\"v1\"}")));

        webTestClient.get()
                .uri("/api/users/1")
                .header("X-Version", "v2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.version").isEqualTo("v1");

        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
        userServiceV2Mock.verify(0, getRequestedFor(urlPathEqualTo("/users/1")));
    }
}
