# 테스트 규칙

이 규칙은 `src/test/` 아래 모든 파일에 적용한다.

## 테스트 스택

- JUnit 5
- Spring Boot Test
- WebTestClient
- Spring Security Test
- Reactor Test의 `StepVerifier`
- `wiremock-spring-boot` 기반 WireMock

## 기본 테스트 형태

- 통합 테스트는 기본적으로 `@SpringBootTest(webEnvironment = RANDOM_PORT)`를 우선 사용한다
- 뒷단 서비스는 WireMock으로 모킹한다
- Gateway 호출은 `WebTestClient`로 수행한다
- 필터 단위 테스트는 `MockServerWebExchange`와 mocked `GatewayFilterChain`을 사용한다
- reactive 결과 검증은 `StepVerifier`를 사용한다

## 네이밍 규칙

- 단위 테스트: `XxxTest`
- 통합 테스트: `XxxIntegrationTest`
- 슬라이스 테스트: `XxxSliceTest`

권장 메서드 이름 패턴:

`method_scenario_expectedResult`

예시:

- `route_pathMatches_forwardsToBackend`
- `filter_invalidJwtToken_returns401`
- `rateLimiter_exceedsLimit_returns429`

가독성이 더 좋다면 한글 메서드명과 `@DisplayName` 조합도 허용한다.

## 검증해야 할 내용

- Gateway 응답 상태와 본문
- WireMock `verify(...)`를 통한 뒷단 요청 경로, 헤더, 라우팅 결과
- `StepVerifier`를 통한 reactive 완료/에러 동작
- 경계 상황과 실패 케이스를 우선 검증한다
  - 잘못되었거나 만료된 JWT
  - `/auth/**` 화이트리스트 우회
  - rate limit 초과
  - 필터 체인 에러 전파

## 테스트 품질 규칙

- 테스트끼리 서로 의존하지 않는다
- WireMock 상태는 테스트마다 초기화하거나 새로 구성한다
- Arrange / Act / Assert 흐름이 분명해야 한다
- reactive 코드는 blocking 방식 검증보다 reactive 테스트 도구를 우선 사용한다
- 단순 커버리지보다 경계값과 품질을 우선한다

## 보안 테스트

- 필요에 따라 `@WithMockUser`, `@WithAnonymousUser`, `SecurityMockServerConfigurers.mockUser(...)`를 사용한다

## 자주 쓰는 패턴

- 통합 테스트: `WebTestClient`로 Gateway 호출 후 WireMock으로 뒷단 요청 검증
- 단위 테스트: `StepVerifier.create(...)`로 필터 동작 검증
