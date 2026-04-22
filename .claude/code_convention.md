# 코드 컨벤션 (Spring Gateway 프로젝트)

## 프로젝트 환경
- Java 21
- Spring Boot 4.0.x
- Spring Cloud Gateway Reactive (`spring-cloud-starter-gateway` + WebFlux)
- Spring Security Reactive (`spring-security-webflux`)

---

## 1. 패키지 구조

```
com.example.gateway
├── config/         # 설정 클래스 (Security, Gateway Route 등)
├── filter/         # Gateway 필터 (GlobalFilter, GatewayFilter)
├── handler/        # 핸들러 (에러 핸들러 등)
├── predicate/      # 커스텀 RoutePredicateFactory
└── GatewayApplication.java
```

---

## 2. 네이밍 규칙

### 클래스
- 설정 클래스: `XxxConfig` (예: `SecurityConfig`, `RouteConfig`)
- 필터 클래스: `XxxFilter` (예: `AuthenticationFilter`, `LoggingFilter`)
- 커스텀 Predicate: `XxxRoutePredicateFactory`

### 메서드
- `camelCase` 사용
- 동사로 시작 (예: `configure`, `apply`, `build`)

### 상수
- `UPPER_SNAKE_CASE` 사용
- `private static final`로 선언

### 변수
- `camelCase` 사용
- 의미 있는 이름 사용 (축약어 지양)

---

## 3. 애노테이션 순서

```java
@Component
@Slf4j
public class ExampleFilter implements GlobalFilter {
```

- 스프링 애노테이션 → Lombok 애노테이션 순서
- `@Configuration` 클래스에는 반드시 `@Bean` 메서드만 포함

---

## 4. Spring Cloud Gateway 컨벤션

### Route 정의 (Java DSL 방식 권장 - 학습 목적)

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("route-id", r -> r
            .path("/api/**")
            .filters(f -> f.stripPrefix(1))
            .uri("lb://service-name"))
        .build();
}
```

### GlobalFilter 구현 (Reactive - `Mono<Void>` 반환)

```java
@Component
@Slf4j
@Order(1)
public class LoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request: {}", exchange.getRequest().getPath());
        // Pre 처리 후 chain 위임, Post 처리는 .then() 또는 .doOnSuccess() 활용
        return chain.filter(exchange)
            .then(Mono.fromRunnable(() ->
                log.info("Response status: {}", exchange.getResponse().getStatusCode())
            ));
    }
}
```

> **핵심**: Reactive Gateway는 WebFlux 기반이므로 블로킹 코드(JDBC, 동기 HTTP 호출 등)를
> 필터 안에서 직접 호출하면 안 됩니다. 반드시 `Mono` / `Flux` 체인으로 감싸세요.

---

## 5. 예외 처리

- Gateway 레벨 예외는 `DefaultErrorWebExceptionHandler`를 확장하거나 `@ControllerAdvice` 대신 `WebExceptionHandler`를 구현하여 처리
- 비즈니스 예외는 커스텀 예외 클래스로 분리
- 에러 응답은 일관된 포맷 유지:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "설명"
}
```

---

## 6. 로깅

- `@Slf4j` (Lombok) 사용
- 로그 레벨 기준:
  - `DEBUG`: 요청/응답 상세 내용
  - `INFO`: 라우팅 처리, 주요 흐름
  - `WARN`: 예상 가능한 예외 상황
  - `ERROR`: 시스템 오류, 예상 불가 예외
- 민감 정보(토큰, 비밀번호 등)는 로그에 절대 포함 금지

---

## 7. 코드 스타일

- 들여쓰기: 4칸 스페이스
- 최대 줄 길이: 120자
- 중괄호: K&R 스타일 (같은 줄에 열기)
- `var` 키워드: 타입이 명확할 때만 사용
- 불필요한 주석 지양, 코드 자체로 의도 표현

---

## 8. 의존성 주입

- 생성자 주입 방식 우선 사용
- Lombok `@RequiredArgsConstructor` 활용

```java
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter {

    private final JwtTokenProvider jwtTokenProvider;
    // ...
}
```
