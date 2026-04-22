# 테스트 컨벤션 (Spring Gateway 프로젝트)

## 테스트 환경

| 라이브러리 | 역할 |
|-----------|------|
| JUnit 5 | 테스트 프레임워크 |
| Spring Boot Test | `@SpringBootTest`, `WebTestClient` |
| Spring Security Test | `@WithMockUser`, `SecurityMockServerConfigurers` |
| Reactor Test | `StepVerifier` — Mono/Flux 검증 |
| **WireMock** (`wiremock-spring-boot:3.6.0`) | 백엔드 서버 모킹 |

> **핵심 구조**: Gateway(WebFlux/Netty) ←→ `WebTestClient` 로 요청, WireMock이 뒷단 서비스 역할

```
[Test] --WebTestClient--> [Gateway:random port] --HTTP--> [WireMock:random port]
```

---

## 1. 테스트 클래스 네이밍

| 테스트 종류 | 클래스명 규칙 | 예시 |
|------------|-------------|------|
| 단위 테스트 | `XxxTest` | `JwtAuthFilterTest`, `LoggingFilterTest` |
| 통합 테스트 | `XxxIntegrationTest` | `PathRoutingIntegrationTest` |
| 슬라이스 테스트 | `XxxSliceTest` | `SecurityWebFluxSliceTest` |

---

## 2. 테스트 메서드 네이밍

**패턴**: `메서드명_시나리오_기대결과`

```java
@Test
void route_pathMatches_forwardsToBackend() { }

@Test
void filter_validJwtToken_passesThrough() { }

@Test
void filter_invalidJwtToken_returns401() { }

@Test
void rateLimiter_exceedsLimit_returns429() { }
```

또는 한글 + `@DisplayName` (가독성 우선):
```java
@Test
@DisplayName("유효한 JWT 토큰이면 요청이 정상 통과된다")
void 유효한JWT토큰이면_요청이_정상통과된다() { }
```

---

## 3. WireMock 통합 테스트 기본 패턴

### `@WireMockTest` — 가장 간단한 방식 (단위 수준)

```java
@WireMockTest
class SimpleRoutingTest {

    @Test
    void route_pathMatches_returns200(WireMockRuntimeInfo wmRuntimeInfo) {
        // WireMock 스텁 설정
        stubFor(get(urlPathEqualTo("/users"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 1}")));

        // Gateway가 wmRuntimeInfo.getHttpBaseUrl() 로 라우팅하도록 설정 필요
        // (application-test.yml 에서 dynamic port 주입)
    }
}
```

### `@SpringBootTest` + `@EnableWireMock` — 완전한 통합 테스트 (권장)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock({
    @ConfigureWireMock(name = "user-service", property = "routes.user-service.url")
})
class PathRoutingIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @InjectWireMock("user-service")
    private WireMockServer userServiceMock;

    @Test
    @DisplayName("Path /users/** 요청이 user-service로 라우팅된다")
    void route_usersPath_forwardsToUserService() {
        // Arrange — WireMock 응답 설정
        userServiceMock.stubFor(get(urlPathEqualTo("/users/1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 1, \"name\": \"홍길동\"}")));

        // Act & Assert — WebTestClient로 Gateway 호출
        webTestClient.get()
            .uri("/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.name").isEqualTo("홍길동");

        // WireMock 호출 검증
        userServiceMock.verify(getRequestedFor(urlPathEqualTo("/users/1")));
    }
}
```

### `application-test.yml` 설정 (dynamic port 주입)

```yaml
# src/test/resources/application-test.yml
routes:
  user-service:
    url: "http://localhost:0"   # @EnableWireMock이 실제 포트로 교체해줌
```

---

## 4. 챕터별 WireMock 테스트 패턴

### 1단계 — 라우팅 테스트

```java
@Test
@DisplayName("Header X-Version: v2 이면 v2 서비스로 라우팅된다")
void route_headerV2_forwardsToV2Service() {
    v2ServiceMock.stubFor(get(anyUrl())
        .willReturn(aResponse().withStatus(200).withBody("v2 response")));

    webTestClient.get()
        .uri("/api/resource")
        .header("X-Version", "v2")
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class).isEqualTo("v2 response");
}

@Test
@DisplayName("Path /api/users 는 뒷단 /users 로 재작성된다")
void route_rewritePath_stripsApiPrefix() {
    backendMock.stubFor(get(urlPathEqualTo("/users"))
        .willReturn(aResponse().withStatus(200)));

    webTestClient.get().uri("/api/users").exchange().expectStatus().isOk();

    // /users 로 실제 요청이 갔는지 검증
    backendMock.verify(getRequestedFor(urlPathEqualTo("/users")));
}
```

### 3단계 — JWT 필터 테스트

```java
@Test
@DisplayName("유효한 JWT 토큰이면 뒷단으로 userId 헤더가 전달된다")
void jwtFilter_validToken_addsUserIdHeader() {
    String token = jwtProvider.generateToken("user-42");

    backendMock.stubFor(get(anyUrl())
        .willReturn(aResponse().withStatus(200)));

    webTestClient.get()
        .uri("/api/resource")
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus().isOk();

    // 뒷단에 X-User-Id 헤더가 전달됐는지 검증
    backendMock.verify(getRequestedFor(anyUrl())
        .withHeader("X-User-Id", equalTo("user-42")));
}

@Test
@DisplayName("토큰 없이 /auth 외 경로 요청하면 401 반환")
void jwtFilter_noToken_returns401() {
    webTestClient.get()
        .uri("/api/resource")
        .exchange()
        .expectStatus().isUnauthorized();
}

@Test
@DisplayName("/auth/** 경로는 토큰 없이도 통과된다 (화이트리스트)")
void jwtFilter_whitelistedPath_passesWithoutToken() {
    backendMock.stubFor(post(urlPathEqualTo("/auth/login"))
        .willReturn(aResponse().withStatus(200).withBody("{\"token\":\"abc\"}")));

    webTestClient.post()
        .uri("/auth/login")
        .exchange()
        .expectStatus().isOk();
}
```

### 5단계 — Rate Limiting 테스트

```java
@Test
@DisplayName("허용 횟수 초과 요청은 429 반환")
void rateLimiter_exceedsLimit_returns429() {
    backendMock.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200)));

    // 허용 한도만큼 요청
    for (int i = 0; i < allowedRequests; i++) {
        webTestClient.get().uri("/api/resource").exchange().expectStatus().isOk();
    }

    // 초과 요청 → 429
    webTestClient.get()
        .uri("/api/resource")
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
}
```

---

## 5. GlobalFilter 단위 테스트 (WireMock 불필요)

필터 로직만 격리해 테스트할 때는 `MockServerWebExchange` + `StepVerifier`를 사용합니다.

```java
class LoggingFilterTest {

    private LoggingFilter filter;
    private ServerWebExchange exchange;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/test")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .build();
        exchange = MockServerWebExchange.from(request);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void filter_anyRequest_logsAndContinues() {
        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    void filter_chainError_propagatesError() {
        when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("upstream error")));

        StepVerifier.create(filter.filter(exchange, chain))
            .expectError(RuntimeException.class)
            .verify();
    }
}
```

---

## 6. StepVerifier 주요 사용법

```java
// 정상 완료 검증
StepVerifier.create(mono).verifyComplete();

// 값 검증
StepVerifier.create(mono)
    .expectNext("expected")
    .verifyComplete();

// 에러 타입 검증
StepVerifier.create(mono)
    .expectError(IllegalArgumentException.class)
    .verify();

// 에러 메시지까지 검증
StepVerifier.create(mono)
    .expectErrorMessage("토큰이 유효하지 않습니다")
    .verify();
```

---

## 7. Security 테스트

| 방법 | 용도 |
|------|------|
| `@WithMockUser` | 기본 USER 권한 사용자 모킹 |
| `@WithMockUser(roles = "ADMIN")` | 특정 권한 지정 |
| `@WithAnonymousUser` | 미인증 사용자 테스트 |
| `mutateWith(mockUser(...))` | WebTestClient 체인에서 인라인으로 설정 |

```java
// WebTestClient 체인에서 인증 사용자 설정
webTestClient
    .mutateWith(SecurityMockServerConfigurers.mockUser("testuser").roles("USER"))
    .get().uri("/api/secure")
    .exchange()
    .expectStatus().isOk();
```

---

## 8. 테스트 원칙

1. **독립성**: 각 테스트는 다른 테스트에 의존하지 않는다
2. **반복 가능**: WireMock 스텁은 `@BeforeEach`에서 초기화, `@AfterEach`에서 `reset()`
3. **논블로킹 유지**: 필터 단위 테스트는 `StepVerifier`, 통합 테스트는 `WebTestClient`
4. **명확한 검증**: WireMock `verify()`로 뒷단에 올바른 요청이 갔는지까지 확인
5. **커버리지보다 품질**: 경계값(토큰 만료, 한도 초과 등) 테스트 케이스 우선

---

## 9. 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 클래스 테스트
./gradlew test --tests "com.example.gateway.*IntegrationTest"

# 테스트 결과 확인
open build/reports/tests/test/index.html
```

---

## 10. WireMock 주요 API 레퍼런스

```java
// 스텁 등록
stubFor(get(urlPathEqualTo("/path"))
    .withHeader("Authorization", matching("Bearer .*"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{}")
        .withFixedDelay(100)));   // 응답 지연 (ms)

// 호출 검증
verify(exactly(1), getRequestedFor(urlPathEqualTo("/path")));
verify(getRequestedFor(anyUrl()).withHeader("X-User-Id", equalTo("42")));

// 스텁 초기화
wireMockServer.resetAll();
```
