# 개발 워크플로우 (Spring Gateway 프로젝트)

## 목표
Spring Cloud Gateway의 핵심 개념을 학습하고, 실제 동작하는 게이트웨이를 직접 구현하며 익힌다.

---

## 1. 학습 순서

> 각 챕터마다 WireMock + WebTestClient 기반 통합 테스트를 함께 작성한다.

### 1단계 — 라우팅

| 챕터 | 내용 | 핵심 개념 |
|------|------|----------|
| 1-1 | Path 기반 라우팅: `/users/**` → 8081 | 기본 라우팅 |
| 1-2 | Header 기반 라우팅: `X-Version: v2` 이면 다른 서비스로 | Predicate |
| 1-3 | Path Rewrite: `/api/users` → `/users` 로 변환 | `RewritePath` |

### 2단계 — 필터 개념

| 챕터 | 내용 | 핵심 개념 |
|------|------|----------|
| 2-1 | 요청 Header에 값 추가하는 GlobalFilter 만들기 | `GlobalFilter` |
| 2-2 | 특정 라우트에만 동작하는 필터 만들기 | `GatewayFilter` |
| 2-3 | 필터 순서(`Order`) 지정해보기 | Filter Chain |
| 2-4 | Pre 필터 / Post 필터 차이 실습 | Pre/Post |

### 3단계 — JWT 검증

| 챕터 | 내용 | 핵심 개념 |
|------|------|----------|
| 3-1 | JWT 토큰 발급 엔드포인트 만들기 (`/auth/login`) | JWT 구조 이해 |
| 3-2 | `Authorization` Header에서 토큰 꺼내기 | 필터에서 Header 읽기 |
| 3-3 | 토큰 유효성 검사 → 실패 시 401 반환 | 필터에서 응답 끊기 |
| 3-4 | 토큰에서 userId 꺼내서 Header에 넣어 뒷단에 전달 | 필터에서 Header 추가 |
| 3-5 | 화이트리스트 (`/auth/**` 는 토큰 검사 안 함) | 예외 경로 처리 |

### 4단계 — 로깅

| 챕터 | 내용 | 핵심 개념 |
|------|------|----------|
| 4-1 | 요청 Method, Path, IP 로깅 | Pre 필터 활용 |
| 4-2 | 응답 StatusCode, 처리 시간 로깅 | Post 필터 활용 |
| 4-3 | 요청마다 고유한 ID(traceId) 부여 | MDC, 요청 추적 |

### 5단계 — Rate Limiting

| 챕터 | 내용 | 핵심 개념 |
|------|------|----------|
| 5-1 | InMemory 방식으로 요청 횟수 카운팅 | 기본 개념 |
| 5-2 | IP 기준으로 Rate Limit 적용 | `KeyResolver` |
| 5-3 | 초과 시 429 Too Many Requests 반환 | 응답 커스텀 |
| 5-4 | 경로별로 다른 Rate Limit 적용 | 라우트별 설정 |

> **Reactor 기초 선수 지식**: `Mono` / `Flux`, `map` / `flatMap` / `doOnNext`,
> `StepVerifier` 사용법을 1단계 시작 전에 별도로 익혀두면 필터 구현이 훨씬 수월합니다.

---

## 2. 기능 개발 흐름

```
요구사항 파악
    ↓
테스트 코드 먼저 작성 (TDD 권장)
    ↓
구현 (Config / Filter / Predicate)
    ↓
로컬 실행 및 수동 확인 (curl 또는 Postman)
    ↓
테스트 통과 확인
    ↓
코드 리뷰 및 정리
    ↓
커밋
```

---

## 3. 실행 방법

```bash
# 로컬 실행
./gradlew bootRun

# 빌드
./gradlew build

# 테스트만 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "com.example.gateway.*"
```

---

## 4. 로컬 테스트 환경

- **Gateway 기본 포트**: `8080`
- **테스트용 백엔드 서버**: `WireMock` (`wiremock-spring-boot:3.6.0`)
- **HTTP 클라이언트**: `curl`, Postman, 또는 `WebTestClient` (통합 테스트용)
- **서버**: Reactive Gateway는 기본적으로 **Netty** 위에서 동작 (Tomcat 아님)

```bash
# 기본 라우팅 테스트 예시
curl -v http://localhost:8080/api/test

# 헤더 포함 요청
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/secure
```

---

## 5. 커밋 컨벤션

```
<type>: <subject>

[body - optional]
```

| type | 의미 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩터링 |
| `test` | 테스트 코드 추가/수정 |
| `docs` | 문서 수정 |
| `chore` | 빌드, 설정 변경 |
| `study` | 학습 목적 실험 코드 |

예시:
```
feat: JWT 인증 GlobalFilter 구현

- Authorization 헤더에서 JWT 토큰 추출
- 유효하지 않은 토큰은 401 응답 반환
```

---

## 6. 브랜치 전략 (학습 프로젝트)

```
main          # 안정적인 코드
  └── step1/routing             # 1단계: 라우팅
  └── step2/filters             # 2단계: 필터 개념
  └── step3/jwt-auth            # 3단계: JWT 검증
  └── step4/logging             # 4단계: 로깅
  └── step5/rate-limiting       # 5단계: Rate Limiting
```

---

## 7. 체크리스트 (PR/커밋 전)

- [ ] 테스트 코드 작성 및 통과 확인 (`StepVerifier` 포함)
- [ ] `./gradlew build` 성공 확인
- [ ] 블로킹 코드가 Reactive 체인 안에 섞이지 않았는지 확인
- [ ] 불필요한 주석, 디버그 로그 제거
- [ ] 민감 정보 (토큰, 비밀번호) 하드코딩 여부 확인
- [ ] `application.properties` 에 비밀 정보 없는지 확인
