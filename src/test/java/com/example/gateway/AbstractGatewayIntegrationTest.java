package com.example.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 통합 테스트 공통 세팅을 모은 베이스 클래스.
 *
 * <p>Gateway 가 떠 있는 랜덤 포트를 기반으로 {@link WebTestClient} 를 구성해 서브클래스가 재사용하도록 한다.
 * WireMock 스텁 초기화처럼 테스트 별로 다른 세팅은 각 서브클래스의 {@link BeforeEach} 에서 별도로 처리한다.
 *
 * <p>JUnit 5 는 상위 클래스의 {@link BeforeEach} 를 서브클래스의 {@link BeforeEach} 보다 먼저 실행하므로
 * {@link #webTestClient} 는 서브클래스 콜백 시점에 이미 사용 가능하다.
 */
abstract class AbstractGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    protected WebTestClient webTestClient;

    @BeforeEach
    void initWebTestClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }
}
