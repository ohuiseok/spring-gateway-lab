# Codex 작업 가이드

이 저장소에서는 `.claude/` 문서를 원본 규칙으로 사용합니다. Codex는 아래 지침을 기준으로 프로젝트 전체에서 작업합니다.

## 기준 문서

- `.claude/workflow.md`: 개발 흐름, 학습 단계, 커밋 규칙, 작업 전 체크리스트
- `.claude/code_convention.md`: 구현 규칙과 코드 스타일
- `.claude/test_convention.md`: 테스트 전략과 네이밍 규칙
- `.claude/skills.md`: Spring Cloud Gateway, Reactor, Reactive Security 핵심 개념

## 프로젝트 맥락

- 기술 스택: Java 21, Spring Boot 4.0.x, Spring Cloud Gateway Reactive, Spring Security Reactive
- 이 프로젝트는 MVC/Tomcat 기반이 아니라 WebFlux/Netty 기반의 Reactive Gateway다
- Gateway 필터나 reactive 체인 안에 blocking 코드를 넣지 않는다
- 학습과 가독성을 위해 라우트 정의는 Java DSL 방식을 우선한다

## 개발 흐름

1. 현재 학습 단계와 요구사항을 먼저 파악한다.
2. 가능하면 테스트를 먼저 작성한다.
3. `config`, `filter`, `handler`, `predicate` 패키지 중심으로 구현한다.
4. 자동화 테스트를 먼저 검증하고, 필요하면 수동 확인을 이어서 한다.
5. 최종 코드는 아래 규칙과 일관되게 정리한다.

## 역할 분담

- Codex는 기본 구현 담당이다
- Codex는 요구사항 분석, 테스트 초안 작성, 기능 구현, 리팩터링, 실행 가능한 코드 완성을 우선한다
- Claude는 코드 품질 향상 담당이다
- Claude는 리뷰, 엣지 케이스 점검, 가독성 개선, 설계 피드백, 누락 테스트 제안에 집중한다
- Codex가 작업할 때는 Claude가 이후 품질 점검을 수행할 것을 고려해 변경 의도와 구조를 분명하게 유지한다
- 리뷰를 염두에 두고 불필요하게 복잡한 추상화보다 읽기 쉬운 구현을 우선한다

## Codex 작업 원칙

- 먼저 동작하는 최소 구현과 테스트를 만든 뒤, 필요한 범위에서 정리한다
- 사용자가 별도로 요청하지 않아도 테스트 가능한 형태의 결과물을 우선 만든다
- 구현 중 구조 개선이 필요하면 과도한 확장보다 현재 학습 단계에 맞는 단순한 설계를 선택한다
- 후속 리뷰어가 이해하기 쉽도록 네이밍, 책임 분리, 예외 흐름을 명확히 유지한다

## Claude 리뷰 원칙

- Claude의 피드백을 반영할 때는 버그 가능성, reactive 규칙 위반, 테스트 누락을 우선 확인한다
- 스타일 의견보다 동작 안정성, 유지보수성, 경계값 검증을 먼저 반영한다
- Codex가 만든 구조를 무조건 뒤집기보다, 현재 방향을 살리면서 품질을 높이는 수정을 우선한다

## 기본 학습 순서

사용자가 다른 방향을 명시하지 않으면 다음 순서를 우선한다.

1. 라우팅
2. 필터 개념
3. JWT 검증
4. 로깅
5. Rate Limiting

각 단계에서는 WireMock + WebTestClient 기반 통합 테스트를 함께 추가하는 것을 기본 원칙으로 한다.

## 전역 규칙

- 모든 코드는 reactive, non-blocking 방식으로 유지한다
- Gateway 필터 내부에서 `subscribe()`를 직접 호출하지 않는다
- 토큰, 비밀번호 같은 민감 정보를 로그에 남기지 않는다
- `application.properties` 등에 비밀값을 하드코딩하지 않는다
- 작업 마무리 전 디버그 코드와 불필요한 주석은 제거한다
- 줄 길이는 가능하면 120자 이내로 유지하고 들여쓰기는 4칸 스페이스를 사용한다
- 중괄호는 K&R 스타일을 사용한다
- 의존성 주입은 생성자 주입을 우선하고 Lombok `@RequiredArgsConstructor`를 선호한다

## 네이밍 규칙

- 설정 클래스: `XxxConfig`
- 필터 클래스: `XxxFilter`
- 커스텀 Predicate: `XxxRoutePredicateFactory`
- 메서드와 변수: `camelCase`
- 상수: `private static final UPPER_SNAKE_CASE`

## 패키지 역할

`src/main` 기준으로 아래 구조를 기본으로 삼는다.

- `config/`: 보안, 라우트, 애플리케이션 설정
- `filter/`: `GlobalFilter` 또는 라우트 필터 구현
- `handler/`: 에러 처리 등 핸들러 구현
- `predicate/`: 커스텀 라우트 조건 구현

## 테스트 원칙

- 통합 테스트는 `WebTestClient`를 우선 사용한다
- 뒷단 서비스 모킹은 WireMock을 우선 사용한다
- reactive 단위 검증은 `StepVerifier`를 우선 사용한다
- Gateway 응답뿐 아니라 뒷단 요청이 올바르게 전달됐는지도 가능하면 함께 검증한다
- 잘못된 토큰, 화이트리스트 경로, rate limit 초과 같은 경계 상황을 우선 검증한다

## 커밋 메시지 규칙

커밋 메시지가 필요하면 아래 형식을 사용한다.

`<type>: <subject>`

사용 가능한 타입:

- `feat`
- `fix`
- `refactor`
- `test`
- `docs`
- `chore`
- `study`

## 디렉터리별 추가 규칙

- `src/` 하위 작업은 `src/AGENTS.md`를 따른다
- `src/test/` 하위 작업은 `src/test/AGENTS.md`를 따른다
