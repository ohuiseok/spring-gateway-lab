# 기술 스택 & 핵심 개념 (Spring Gateway)

## 프로젝트 의존성

| 의존성 | 버전 | 용도 |
|--------|------|------|
| Spring Boot | 4.0.5 | 기반 프레임워크 |
| Spring Cloud Gateway (Reactive) | 2025.1.1 | API Gateway (WebFlux 기반) |
| Spring Security Reactive | Boot 내장 | 인증/인가 |
| Reactor Core | Boot 내장 | 비동기/논블로킹 처리 |
| Reactor Test | Boot 내장 | `StepVerifier` 기반 테스트 |
| Java | 21 | 언어 버전 |

> **참고**: 이 프로젝트는 `spring-cloud-starter-gateway` (WebFlux/Reactive 기반)를 사용합니다.
> MVC 기반 `gateway-server-webmvc`와 다르며, 블로킹 코드를 사용할 수 없습니다.

---

## 1. Spring Cloud Gateway 핵심 개념

### Route (라우트)
Gateway의 기본 구성 단위. 조건(Predicate)이 맞으면 지정된 URI로 요청을 전달.

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("my-route", r -> r
            .path("/api/**")                          // Predicate
            .filters(f -> f                           // Filter
                .addRequestHeader("X-From", "gateway")
                .stripPrefix(1))
            .uri("http://localhost:9090"))             // 목적지
        .build();
}
```

### Predicate (조건)
요청이 라우트에 매핑될 조건을 정의. `AsyncPredicate<ServerWebExchange>` 기반.

| Predicate | 설명 |
|-----------|------|
| `Path` | URL 경로 매핑 |
| `Method` | HTTP 메서드 |
| `Header` | 요청 헤더 값 |
| `Query` | 쿼리 파라미터 |
| `Host` | 호스트 이름 |
| `Before` / `After` / `Between` | 시간 기반 |
| `RemoteAddr` | 클라이언트 IP |
| `Weight` | 가중치 기반 트래픽 분산 |

### Filter (필터)
요청/응답을 가로채 수정하거나 추가 처리. **모두 비동기(`Mono<Void>`)로 동작.**

**Pre Filter**: `chain.filter(exchange)` 호출 이전 로직
**Post Filter**: `.then()` / `.doOnSuccess()` 등 체인 이후 로직

---

## 2. 내장 필터 목록

| 필터 | 설명 |
|------|------|
| `AddRequestHeader` | 요청 헤더 추가 |
| `AddResponseHeader` | 응답 헤더 추가 |
| `RemoveRequestHeader` | 요청 헤더 제거 |
| `StripPrefix` | URL prefix 제거 |
| `RewritePath` | URL 경로 재작성 (정규식 지원) |
| `CircuitBreaker` | 서킷 브레이커 (Resilience4J) |
| `RequestRateLimiter` | 요청 속도 제한 (Redis 연동) |
| `Retry` | 실패 시 재시도 |
| `SetStatus` | 응답 상태 코드 설정 |
| `SaveSession` | 세션 저장 강제 실행 |
| `TokenRelay` | OAuth2 토큰 전달 |

---

## 3. 커스텀 필터 구현 방식

### GlobalFilter (전체 요청 적용)
```java
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("[PRE] {} {}", request.getMethod(), request.getPath());

        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
                log.info("[POST] status={}", response.getStatusCode());
            }));
    }
}
```

### GatewayFilterFactory (라우트별 적용)
```java
@Component
public class MyGatewayFilterFactory
        extends AbstractGatewayFilterFactory<MyGatewayFilterFactory.Config> {

    public MyGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Config 값을 활용한 처리
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Custom", config.getValue())
                .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    @Data
    public static class Config {
        private String value;
    }
}
```

> 필터 이름은 클래스명에서 `GatewayFilterFactory`를 제거한 부분이 됩니다.
> 예: `MyGatewayFilterFactory` → yml에서 `- My=파라미터`

---

## 4. Spring Security Reactive + Gateway 연동

Reactive Gateway는 `SecurityFilterChain` 대신 `SecurityWebFilterChain`을 사용합니다.

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/public/**").permitAll()
            .anyExchange().authenticated())
        .httpBasic(Customizer.withDefaults())
        .build();
}
```

- JWT 인증은 `AuthenticationWebFilter` + `ReactiveAuthenticationManager`로 구현
- `GlobalFilter`에서 JWT 파싱 후 `SecurityContext`에 주입 가능
- `ReactiveSecurityContextHolder.getContext()`로 컨텍스트 접근

---

## 5. Reactive 핵심 — Mono / Flux

| 타입 | 의미 |
|------|------|
| `Mono<T>` | 0 또는 1개의 비동기 결과 |
| `Flux<T>` | 0~N개의 비동기 스트림 |
| `Mono<Void>` | 반환 값 없는 비동기 작업 (필터 반환 타입) |

**체인 주요 연산자**

```java
Mono.just("value")
    .map(s -> s.toUpperCase())           // 동기 변환
    .flatMap(s -> Mono.just(s + "!"))    // 비동기 변환
    .doOnNext(s -> log.info(s))          // 사이드이펙트 (논블로킹)
    .doOnError(e -> log.error("err", e)) // 에러 사이드이펙트
    .onErrorReturn("fallback")           // 에러 시 기본값
    .subscribe();                        // 구독 시작 (필터에서는 직접 호출 X)
```

> 필터 안에서 `subscribe()`를 직접 호출하지 마세요. `Mono` 체인을 반환하면 Gateway가 구독합니다.

---

## 6. MVC Gateway vs Reactive Gateway 비교

| 항목 | Reactive Gateway (이 프로젝트) | MVC Gateway |
|------|-------------------------------|-------------|
| 의존성 | `spring-cloud-starter-gateway` | `gateway-server-webmvc` |
| 기반 | WebFlux (Netty) | Servlet (Tomcat) |
| 스레드 모델 | 이벤트 루프 (논블로킹) | 스레드 풀 (블로킹 가능) |
| 필터 반환 타입 | `Mono<Void>` | `void` / `Mono<Void>` |
| Security | `SecurityWebFilterChain` | `SecurityFilterChain` |
| 테스트 클라이언트 | `WebTestClient` | `MockMvc` / `TestRestTemplate` |
| 블로킹 코드 | **사용 불가** | 사용 가능 |

---

## 7. 유용한 설정 프로퍼티

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: example-route
          uri: http://localhost:9090
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1
            - AddRequestHeader=X-From, gateway
      default-filters:
        - AddResponseHeader=X-Gateway, spring-cloud-gateway
      httpclient:
        connect-timeout: 2000    # ms
        response-timeout: 5s

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: DEBUG
```

---

## 8. 학습 참고 자료

- [Spring Cloud Gateway 공식 문서](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Project Reactor 공식 문서](https://projectreactor.io/docs/core/release/reference/)
- [Spring Security Reactive 공식 문서](https://docs.spring.io/spring-security/reference/reactive/index.html)
- Spring Cloud 버전: `2025.1.1` (Spring Boot 4.x 호환)
