# 예외 처리 가이드

이 문서는 프로젝트의 예외 처리 전략에 대해 설명합니다. 모든 예외는 체계적으로 관리하여 일관된 에러 응답을 클라이언트에게 제공하는 것을 목표로 합니다.

## 핵심 구성 요소

예외 처리는 다음 4개의 주요 구성 요소로 이루어집니다.

1.  `ErrorCode.java`: 애플리케이션의 모든 도메인 에러를 정의하는 열거형(Enum).
2.  `ApplicationException.java`: `ErrorCode`를 포함하는 커스텀 런타임 예외.
3.  `GlobalExceptionHandler.java`: `@RestControllerAdvice`를 사용하여 전역적으로 예외를 처리하는 핸들러.
4.  `ErrorResponse.java`: 클라이언트에게 반환될 표준 에러 응답 DTO.

---

### 1. `ErrorCode.java`

애플리케이션에서 발생할 수 있는 모든 비즈니스 예외는 `ErrorCode` 열거형에 정의합니다.

-   **역할**: 에러의 종류를 명확하게 구분하고, 각 에러에 대한 HTTP 상태 코드, 고유 에러 코드, 에러 메시지를 중앙에서 관리합니다.
-   **위치**: `src/main/java/com/examp/Webflux/exception/ErrorCode.java`
-   **구조**:
    -   `httpStatus`: 에러에 해당하는 HTTP 상태 코드 (e.g., `HttpStatus.CONFLICT`)
    -   `code`: 클라이언트가 에러를 식별할 수 있는 고유 코드 (e.g., "UQ-0001")
    -   `message`: 에러에 대한 설명

**예시:**

```java
@AllArgsConstructor
@Getter
public enum ErrorCode {

    QUEUE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "UQ-0001", "Already registered user");

    private HttpStatus httpStatus;
    private String code;
    private String message;
}
```

---

### 2. `ApplicationException.java`

서비스 로직 내에서 예외적인 상황이 발생했을 때, `ApplicationException`을 발생시켜야 합니다.

-   **역할**: `ErrorCode`를 담아 예외 상황을 전파하는 커스텀 런타임 예외 클래스입니다.
-   **위치**: `src/main/java/com/examp/Webflux/exception/ApplicationException.java`

**예시:**

```java
@RequiredArgsConstructor
@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    @Override
    public String getMessage(){
        return errorCode.getMessage();
    }
}
```

---

### 3. `GlobalExceptionHandler.java`

`@RestControllerAdvice` 어노테이션을 사용하여 애플리케이션 전역에서 발생하는 `ApplicationException`을 처리합니다.

-   **역할**: `ApplicationException`이 발생하면 이를 감지하여, `ErrorCode`에 정의된 HTTP 상태 코드와 `ErrorResponse` 형식에 맞는 응답 본문을 생성하여 클라이언트에게 반환합니다.
-   **위치**: `src/main/java/com/examp/Webflux/exception/GlobalExceptionHandler.java`

**예시:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApplicationException(ApplicationException ex) {
        return Mono.just(ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ErrorResponse.from(ex.getErrorCode())));
    }
}
```

---

### 4. `ErrorResponse.java`

클라이언트에게 반환되는 표준 에러 응답의 형식을 정의하는 DTO입니다.

-   **역할**: 모든 에러 응답이 일관된 JSON 구조(`{"code": "...", "message": "..."}`)를 갖도록 보장합니다.
-   **위치**: `src/main/java/com/examp/Webflux/dto/ErrorResponse.java`
-   **구조**: Java 17의 `record` 타입을 사용하여 불변 DTO로 정의되었습니다.

**예시:**

```java
public record ErrorResponse(String code, String message) {
    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
```

---

## 사용 방법

서비스 로직에서 특정 비즈니스 규칙을 위반하는 상황이 발생하면, `ErrorCode`를 선택하여 `ApplicationException`을 발생시키면 됩니다.

**`UserQueueService.java` 예시:**

```java
// ...
import com.examp.Webflux.exception.ApplicationException;
import com.examp.Webflux.exception.ErrorCode;
// ...

public Mono<Long> registerWaitQueue(final String queue, final Long userId) {
    long unixTimeStamp = Instant.now().getEpochSecond();

    return reactiveRedisTemplate.opsForZSet().add(USER_WAIT_QUEUE_KEY.formatted(queue), userId.toString(), unixTimeStamp)
            .flatMap(added -> {
                // zadd의 결과가 false이면 이미 큐에 존재하는 사용자이므로 예외 발생
                if (!added) {
                    return Mono.error(new ApplicationException(ErrorCode.QUEUE_ALREADY_REGISTERED));
                }
                // 성공 시, 대기 순번 계산
                return reactiveRedisTemplate.opsForZSet().rank(USER_WAIT_QUEUE_KEY.formatted(queue), userId.toString())
                        .map(rank -> rank >= 0 ? rank + 1 : 0);
            });
}
```

위와 같이 코드를 작성하면, 사용자가 이미 대기열에 등록되어 있을 경우 `GlobalExceptionHandler`가 이를 처리하여 클라이언트에게 다음과 같은 `409 Conflict` 응답을 보내게 됩니다.

```json
{
  "code": "UQ-0001",
  "message": "Already registered user"
}
```
