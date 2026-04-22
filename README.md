# Spring Cloud Gateway 학습 프로젝트

Spring Cloud Gateway(Reactive)의 핵심 개념을 단계별로 직접 구현하며 익히는 학습용 프로젝트입니다.
단순히 설정 파일을 따라 치는 것이 아니라, **WireMock + WebTestClient 기반 테스트를 먼저 작성하고 구현**하는 흐름(TDD)으로 진행합니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Gateway | Spring Cloud Gateway Reactive (`spring-cloud-starter-gateway`) |
| Security | Spring Security Reactive |
| Build | Gradle |
| Test | JUnit 5, WireMock 3.6, WebTestClient, StepVerifier |
| Server | Netty (내장, 논블로킹) |

> Reactive Gateway는 WebFlux 기반으로 동작합니다.
> 필터의 반환 타입은 `Mono<Void>`이며, 블로킹 코드는 사용할 수 없습니다.

---

## 프로젝트 구조

```
src
├── main/java/com/example/gateway
│   ├── config/          # SecurityConfig, RouteConfig 등
│   ├── filter/          # GlobalFilter, GatewayFilterFactory 구현체
│   ├── predicate/       # 커스텀 RoutePredicateFactory
│   ├── handler/         # 에러 핸들러
│   └── GatewayApplication.java
└── test/java/com/example/gateway
    ├── routing/         # 1단계: 라우팅 통합 테스트
    ├── filter/          # 2단계: 필터 단위/통합 테스트
    ├── jwt/             # 3단계: JWT 검증 테스트
    ├── logging/         # 4단계: 로깅 테스트
    └── ratelimit/       # 5단계: Rate Limiting 테스트
```

---

## 실행 방법

```bash
# 로컬 실행 (포트 8080)
./gradlew bootRun

# 전체 빌드
./gradlew build

# 테스트만 실행
./gradlew test

# 특정 단계 테스트만 실행
./gradlew test --tests "com.example.gateway.routing.*"
```

---

## 학습 계획 및 진행 체크

### 선수 지식 — Reactor 기초

Spring Cloud Gateway Reactive는 `Mono` / `Flux` 기반입니다.
필터 구현 전에 아래 개념을 먼저 익혀두면 이후 단계가 훨씬 수월합니다.

| 번호 | 학습 항목 | 완료 |
|------|----------|:----:|
| 0-1 | `Mono` / `Flux` 개념 이해 | ☐ |
| 0-2 | `map`, `flatMap`, `doOnNext`, `then` 등 주요 연산자 | ☐ |
| 0-3 | `StepVerifier`로 Mono/Flux 테스트하기 | ☐ |
| 0-4 | 블로킹 코드 vs 논블로킹 코드 구분 기준 | ☐ |

---

### 1단계 — 라우팅

Gateway의 기본 동작 원리인 Route / Predicate 개념을 이해하고,
요청이 어떤 조건으로 어떤 서비스로 전달되는지 실습합니다.

| 챕터 | 구현 내용 | 핵심 개념 | 완료 |
|------|----------|----------|:----:|
| 1-1 | Path 기반 라우팅: `/users/**` 요청을 8081 서비스로 전달 | `RouteLocator`, `Path` Predicate | ☐ |
| 1-2 | Header 기반 라우팅: `X-Version: v2` 헤더가 있으면 v2 서비스로 전달 | `Header` Predicate | ☐ |
| 1-3 | Path Rewrite: Gateway로는 `/api/users` 로 들어오지만 뒷단엔 `/users` 로 전달 | `RewritePath` Filter | ☐ |

**브랜치**: `step1/routing`

---

### 2단계 — 필터 개념

Gateway의 핵심인 필터를 직접 구현합니다.
전역 필터와 라우트별 필터의 차이, 실행 순서, Pre/Post 흐름을 체감합니다.

| 챕터 | 구현 내용 | 핵심 개념 | 완료 |
|------|----------|----------|:----:|
| 2-1 | 모든 요청 헤더에 `X-Request-Id` 값을 추가하는 GlobalFilter 구현 | `GlobalFilter`, `Mono<Void>` | ☐ |
| 2-2 | 특정 라우트에만 동작하는 커스텀 `GatewayFilterFactory` 구현 | `AbstractGatewayFilterFactory` | ☐ |
| 2-3 | 여러 필터에 `@Order` 를 지정하고 실행 순서 확인 | `@Order`, `Ordered` | ☐ |
| 2-4 | Pre 필터(요청 전)와 Post 필터(응답 후) 동작 차이 실습 | `.then()`, `doOnSuccess()` | ☐ |

**브랜치**: `step2/filters`

---

### 3단계 — JWT 검증

실제 서비스에서 가장 많이 쓰이는 패턴인 JWT 기반 인증을 Gateway 레벨에서 구현합니다.
토큰 발급부터 검증, 뒷단 서비스로의 사용자 정보 전달까지 전체 흐름을 완성합니다.

| 챕터 | 구현 내용 | 핵심 개념 | 완료 |
|------|----------|----------|:----:|
| 3-1 | `/auth/login` 엔드포인트에서 JWT 토큰 발급 | JWT 구조(Header.Payload.Signature) | ☐ |
| 3-2 | `Authorization: Bearer <token>` 헤더에서 토큰 추출 | 필터에서 요청 헤더 읽기 | ☐ |
| 3-3 | 토큰 서명 / 만료 검증 → 실패 시 응답 차단 후 401 반환 | 필터에서 응답 직접 쓰기 | ☐ |
| 3-4 | 토큰에서 `userId` 꺼내 `X-User-Id` 헤더에 담아 뒷단에 전달 | 필터에서 요청 헤더 추가 | ☐ |
| 3-5 | `/auth/**` 경로는 토큰 검사를 건너뜀 (화이트리스트) | 예외 경로 처리 | ☐ |

**브랜치**: `step3/jwt-auth`

---

### 4단계 — 로깅

운영 환경에서 필수적인 요청/응답 로깅과 분산 추적을 위한 traceId를 구현합니다.
Pre/Post 필터의 실전 활용 사례를 익힙니다.

| 챕터 | 구현 내용 | 핵심 개념 | 완료 |
|------|----------|----------|:----:|
| 4-1 | 요청 Method, Path, Client IP를 로그로 출력 | Pre 필터, `ServerHttpRequest` | ☐ |
| 4-2 | 응답 StatusCode와 요청 처리 시간(ms)을 로그로 출력 | Post 필터, `System.nanoTime()` | ☐ |
| 4-3 | 요청마다 고유한 `traceId`(UUID)를 생성해 헤더 및 로그에 포함 | MDC, 요청 추적 | ☐ |

**브랜치**: `step4/logging`

---

### 5단계 — Rate Limiting

서비스 보호를 위한 요청 횟수 제한을 구현합니다.
InMemory 방식으로 직접 구현하며 KeyResolver, 경로별 설정까지 다룹니다.

| 챕터 | 구현 내용 | 핵심 개념 | 완료 |
|------|----------|----------|:----:|
| 5-1 | InMemory `ConcurrentHashMap`으로 IP별 요청 횟수 카운팅 | Rate Limit 기본 원리 | ☐ |
| 5-2 | Client IP를 키로 사용하는 `KeyResolver` 빈 등록 | `KeyResolver` | ☐ |
| 5-3 | 허용 횟수 초과 시 `429 Too Many Requests` 응답 반환 | 응답 커스텀, `HttpStatus` | ☐ |
| 5-4 | `/api/public/**` 과 `/api/admin/**` 에 다른 제한 횟수 적용 | 라우트별 필터 설정 | ☐ |

**브랜치**: `step5/rate-limiting`

---

## 전체 진행 현황

| 단계 | 챕터 수 | 완료 수 | 진행률 |
|------|:-------:|:-------:|:------:|
| 0 (Reactor 기초) | 4 | 0 | 0% |
| 1 (라우팅) | 3 | 0 | 0% |
| 2 (필터 개념) | 4 | 0 | 0% |
| 3 (JWT 검증) | 5 | 0 | 0% |
| 4 (로깅) | 3 | 0 | 0% |
| 5 (Rate Limiting) | 4 | 0 | 0% |
| **합계** | **23** | **0** | **0%** |

---

## 테스트 전략

각 챕터는 아래 두 가지 테스트를 함께 작성합니다.

**통합 테스트** (`@SpringBootTest` + `@EnableWireMock` + `WebTestClient`)
- WireMock이 뒷단 서비스 역할
- WebTestClient로 실제 Gateway에 HTTP 요청
- `verify()`로 뒷단에 올바른 요청이 전달됐는지 검증

**단위 테스트** (`MockServerWebExchange` + `StepVerifier`)
- 필터 로직만 격리하여 테스트
- 외부 의존성 없이 빠르게 실행

```
[테스트] --WebTestClient--> [Gateway] --HTTP--> [WireMock(뒷단 서비스 모킹)]
```

---

## 커밋 컨벤션

```
<type>: <subject>
```

| type | 의미 |
|------|------|
| `feat` | 새로운 기능 구현 |
| `test` | 테스트 코드 작성/수정 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 |
| `docs` | 문서 수정 |
| `chore` | 빌드, 설정 변경 |
| `study` | 학습/실험용 코드 |
