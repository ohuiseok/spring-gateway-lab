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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;
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
class HeaderRoutingIntegrationTest {

    @LocalServerPort
    private int port;

    @InjectWireMock("user-service")
    private WireMockServer userServiceMock;

    @InjectWireMock("user-service-v2")
    private WireMockServer userServiceV2Mock;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        userServiceMock.resetAll();
        userServiceV2Mock.resetAll();
    }

    @Test
    @DisplayName("X-Version 헤더가 v2 이면 v2 서비스로 라우팅된다")
    void route_versionHeaderIsV2_forwardsToUserServiceV2() {
        userServiceV2Mock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"version\":\"v2\"}")));

        webTestClient.get()
                .uri("/users/1")
                .header("X-Version", "v2")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.version").isEqualTo("v2");

        userServiceV2Mock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
        userServiceMock.verify(0, getRequestedFor(urlPathEqualTo("/users/1")));
    }

    @Test
    @DisplayName("X-Version 헤더가 없으면 기본 user-service 로 라우팅된다")
    void route_versionHeaderMissing_forwardsToDefaultUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"version\":\"v1\"}")));

        webTestClient.get()
                .uri("/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.version").isEqualTo("v1");

        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
        userServiceV2Mock.verify(0, getRequestedFor(urlPathEqualTo("/users/1")));
    }

    @Test
    @DisplayName("X-Version 헤더 값이 v2 가 아니면 기본 user-service 로 라우팅된다")
    void route_versionHeaderIsNotV2_forwardsToDefaultUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"version\":\"v1\"}")));

        webTestClient.get()
                .uri("/users/1")
                .header("X-Version", "v1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.version").isEqualTo("v1");

        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
        userServiceV2Mock.verify(0, getRequestedFor(urlPathEqualTo("/users/1")));
    }

    @Test
    @DisplayName("X-Version 헤더 값이 대문자 V2 이면 v2 라우트에 매칭되지 않고 기본 user-service 로 라우팅된다")
    void route_versionHeaderIsUpperCaseV2_forwardsToDefaultUserService() {
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"version\":\"v1\"}")));

        webTestClient.get()
                .uri("/users/1")
                .header("X-Version", "V2")
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
