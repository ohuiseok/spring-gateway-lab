# 소스 코드 규칙

이 규칙은 `src/` 아래 모든 파일에 적용한다.

## 구현 우선순위

- WebFlux와 Netty 기반 Reactive Gateway 구조를 유지한다
- blocking 호출 대신 `Mono`, `Flux` 체인을 사용한다
- 필터는 `Mono<Void>`를 반환하고 reactive 체인 안에서 pre/post 처리를 구성한다
- 라우트를 추가하거나 변경할 때는 Java DSL 방식을 우선한다

## 기본 패키지 구조

프로덕션 코드는 가능한 한 아래 구조를 따른다.

```text
com.example.gateway
- config
- filter
- handler
- predicate
```

새 패키지는 구조를 분명히 개선할 때만 추가한다.

## 코드 컨벤션

- 클래스 이름:
  - 설정 클래스는 `XxxConfig`
  - 필터 클래스는 `XxxFilter`
  - 커스텀 Predicate는 `XxxRoutePredicateFactory`
- 메서드 이름은 `configure`, `apply`, `build`처럼 동사로 시작한다
- 변수명은 축약보다 의미 전달을 우선한다
- 상수는 `private static final`로 선언한다
- 의존성 주입은 생성자 주입을 우선하고 Lombok `@RequiredArgsConstructor`를 선호한다
- 애노테이션 순서는 스프링 애노테이션 후 Lombok 애노테이션 순서를 따른다

예시:

```java
@Component
@Slf4j
public class ExampleFilter implements GlobalFilter {
}
```

## Gateway 구현 규칙

- 공통 관심사는 `GlobalFilter`로 구현한다
- 라우트별 동작은 필요 시 `GatewayFilterFactory`로 구현한다
- Pre 처리 로직은 `chain.filter(exchange)` 호출 전에 둔다
- Post 처리 로직은 `.then(...)`, `.doOnSuccess(...)` 같은 reactive 연산자로 연결한다
- 필터 내부에 blocking 연동 코드를 직접 넣지 않는다

## 예외 처리

- Gateway 레벨 예외는 `WebExceptionHandler` 같은 reactive 방식으로 처리한다
- 에러 응답 형식은 일관되게 유지한다

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "description"
}
```

## 로깅 규칙

- Lombok `@Slf4j`를 사용한다
- `DEBUG`: 요청/응답 상세 추적
- `INFO`: 주요 라우팅 흐름과 정상 처리
- `WARN`: 예상 가능한 예외 상황
- `ERROR`: 예상하지 못한 실패
- 민감 정보는 로그에 남기지 않는다

## 스타일

- 들여쓰기는 4칸 스페이스
- 중괄호는 K&R 스타일
- 줄 길이는 가능하면 120자 이내
- `var`는 타입이 명확할 때만 사용한다
- 설명용 주석보다 코드 자체의 명확성을 우선한다
